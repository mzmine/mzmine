/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.datamodel.features.compoundlist;

import io.github.mzmine.datamodel.features.DataTypeValueChangeListener;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.FeatureListRowID;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.columnar_data.ColumnarModularDataModelSchema;
import io.github.mzmine.datamodel.features.columnar_data.ColumnarModularFeatureListRowsSchema;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.util.CompoundSchemaTypes;
import io.github.mzmine.util.FeatureListUtils;
import io.github.mzmine.util.MemoryMapStorage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Holds columnar schemas and an ordered list of {@link ModularCompoundRow}s derived from a
 * {@link ModularFeatureList}. Tracks the structural version of the source feature list at
 * construction time; call {@link #isStale()} to detect if the feature list changed since then.
 */
public class CompoundList {

  private static final Logger logger = Logger.getLogger(CompoundList.class.getName());

  @NotNull
  private final ModularFeatureList featureList;
  @NotNull
  private final ColumnarModularFeatureListRowsSchema compoundRowSchema;
  @NotNull
  private final ColumnarModularDataModelSchema compoundFeaturesSchema;
  // Captured at construction and re-synced by {@link #syncSourceStructuralVersion()} whenever the
  // owning feature list mutates rows in a controlled way (e.g. row deletion that we propagated into
  // this compound list). Compared against the feature list's current structural version in
  // {@link #isStale()}.
  private volatile long sourceStructuralVersion;

  private final ObservableList<ModularCompoundRow> rows = FXCollections.observableArrayList();
  private final ObservableList<ModularCompoundRow> rowsReadOnly = FXCollections.unmodifiableObservableList(
      rows);

  // O(1) reverse lookup: member row ID → owning compounds. A row may belong to multiple compounds
  // (e.g. a non-IIN row correlated to two distinct IIN-seeded compounds is a member of both).
  // ConcurrentHashMap so listeners on background threads can safely read while setRows rebuilds
  // on the FX thread. Contains all members: classical FeatureListRow and CompoundRow.
  private final Map<FeatureListRowID, List<ModularCompoundRow>> byMemberRowId = new ConcurrentHashMap<>();

  /// O(1) lookup compoundId → compound row. Populated by setRows() and registerCompoundRowStub() so
  /// findRowByCompoundId resolves in either runtime construction or load-time stub-first flows.
  /// Contains every compound row at every level of the tree (top-level plus nested).
  private final Map<Integer, ModularCompoundRow> byCompoundId = new ConcurrentHashMap<>();

  @NotNull
  private final List<CompoundRowBinding> bindings;
  // Each entry undoes one listener registration on dispose.
  private final List<Runnable> listenerRemovers = new ArrayList<>();
  private boolean listenersWired = false;
  private boolean disposed = false;
  // True while applyAllBindings() is running. Listeners check this to avoid redundant cascading
  // re-applies caused by binding writes firing change events (inner compound writes propagate to
  // outer compounds during the bulk pass — but the outer is already scheduled to apply next).
  private volatile boolean bulkApplyInProgress = false;

  public CompoundList(@NotNull final ModularFeatureList featureList,
      @Nullable final MemoryMapStorage storage, final int estimatedRows) {
    this(featureList, storage, estimatedRows, CompoundRowBindings.defaultBindings());
  }

  public CompoundList(@NotNull final ModularFeatureList featureList,
      @Nullable final MemoryMapStorage storage, final int estimatedRows,
      @NotNull final List<CompoundRowBinding> bindings) {
    this.featureList = featureList;
    this.sourceStructuralVersion = featureList.getStructuralVersion();
    this.bindings = List.copyOf(bindings);

    final int rows = Math.max(estimatedRows, 1000);
    final int nFiles = featureList.getRawDataFiles().size();
    // row schema: one feature column per raw file, mirrors ModularFeatureList.rowsSchema
    this.compoundRowSchema = new ColumnarModularFeatureListRowsSchema(storage,
        featureList.getName() + "_compound_rows", rows, featureList.getRawDataFiles());
    // feature schema: stores compound-level feature data (height, area, etc.)
    this.compoundFeaturesSchema = new ColumnarModularDataModelSchema(storage,
        featureList.getName() + "_compound_features",
        FeatureListUtils.estimateFeatures(rows, nFiles));

    // pre-register compound column types so the schema is queryable before any row is written
    compoundRowSchema.addDataTypes(CompoundSchemaTypes.REGISTERED.toArray(new DataType[0]));
  }

  /**
   * @return true if the source feature list changed structurally (rows added / removed) since this
   * compound list was built or last {@link #syncSourceStructuralVersion()}.
   */
  public boolean isStale() {
    return sourceStructuralVersion != featureList.getStructuralVersion();
  }

  /**
   * Re-snapshot the source feature list's structural version. Call this after applying a controlled
   * structural change (e.g. propagating row removal into the compound list) so the compound list is
   * not falsely reported as stale.
   */
  public void syncSourceStructuralVersion() {
    this.sourceStructuralVersion = featureList.getStructuralVersion();
  }

  /**
   * @return unmodifiable view of compound rows.
   */
  public @NotNull ObservableList<ModularCompoundRow> getRows() {
    return rowsReadOnly;
  }

  /**
   * Get list of rows depending on which level: Compounds, all major ions (first level), all
   * isotopes (second level)
   *
   * @return unmodifiable view of compound rows.
   */
  public @NotNull List<? extends FeatureListRow> getRowsCopy(
      @NotNull CompoundRowSelection selection) {
    return switch (selection) {
      case COMPOUNDS -> getRowsCopy();
      case ALL_MAJOR_IONS -> {
        final Set<FeatureListRow> seen = new HashSet<>();
        yield getRowsCopy().stream().<FeatureListRow>mapMulti((comp, up) -> {
          // compound row is not returned, only the first level of main ions
          for (FeatureListRow member : comp.getMemberRows()) {
            if (seen.add(member)) {
              up.accept(member);
            }
          }
        }).toList();
      }
      case ALL_FEATURE_ROWS -> featureList.getRowsCopy();
    };
  }

  /**
   * Number of rows Get list of rows depending on which level: Compounds, all major ions (first
   * level), all feature list rows
   *
   * @return number of rows (either compound rows or feature list rows)
   */
  public int getNumberOfCompoundRows(@NotNull CompoundRowSelection selection) {
    return switch (selection) {
      case COMPOUNDS -> rows.size();
      case ALL_FEATURE_ROWS -> featureList.getNumberOfRows();
      case ALL_MAJOR_IONS -> getRowsCopy(selection).size();
    };
  }

  /**
   * @return number of compound rows
   */
  public int getNumberOfCompoundRows() {
    return getNumberOfCompoundRows(CompoundRowSelection.COMPOUNDS);
  }

  /**
   * Replace all rows and rebuild the reverse member index. Must be called on the FX thread when the
   * list is already bound to UI components. After rebuilding, all bindings are applied to every
   * row, and listeners are wired (once) so subsequent member-row changes recompute the owning
   * compound.
   */
  public void setRows(@NotNull final List<ModularCompoundRow> newRows) {
    if (disposed) {
      throw new IllegalStateException("setRows called on a disposed CompoundList");
    }
    rows.setAll(newRows);
    byMemberRowId.clear();
    byCompoundId.clear();
    for (final ModularCompoundRow cr : newRows) {
      indexMembers(cr);
      byCompoundId.put(cr.getCompoundId(), cr);
    }
    applyAllBindings();
    if (!listenersWired) {
      wireListeners();
      listenersWired = true;
    }
    // sorting
    applyDefaultRowsSorting();
  }

  public void applyDefaultRowsSorting() {
    final Comparator<FeatureListRow> comparator = FeatureListUtils.getDefaultRowSorter(featureList);
    rows.sort(comparator);
  }

  /**
   * Register a compound row stub in the {@code byCompoundId} id index without adding it to the
   * top-level {@link #rows} list and without applying bindings or wiring listeners. Used during
   * project load (pass A) so that {@link #findRowByCompoundId(int)} resolves any compound id —
   * top-level or nested — before any {@code <compoundrow>} content is parsed (forward references
   * inside nested-compound member lists, plus references to nested-only compound rows that are not
   * in the top-level list at all).
   * <p>
   * Must be followed by {@link #finalizeLoaded(List)} once all stubs are populated, which sets the
   * top-level rows and wires listeners.
   */
  public void registerCompoundRowStub(@NotNull final ModularCompoundRow stub) {
    if (disposed) {
      throw new IllegalStateException("registerCompoundRowStub called on a disposed CompoundList");
    }
    byCompoundId.put(stub.getCompoundId(), stub);
  }

  /**
   * Finalize a load: set the top-level rows from {@code topLevelRows} (the previously-registered
   * stubs that should appear in {@link #getRows()}), rebuild the {@code byMemberRowId} reverse
   * index by recursively walking each top-level row's now-populated {@link CompoundMembers}, and
   * wire listeners on source-row/feature and compound-row/feature schemas. Nested compound rows
   * (registered via {@link #registerCompoundRowStub(ModularCompoundRow)} but not passed here) stay
   * resolvable via {@link #findRowByCompoundId(int)} and are reachable through their parent's
   * member list. Skips {@code applyAllBindings()} — saved binding outputs are authoritative.
   */
  public void finalizeLoaded(@NotNull final List<ModularCompoundRow> topLevelRows) {
    if (disposed) {
      throw new IllegalStateException("finalizeLoaded called on a disposed CompoundList");
    }
    bulkApplyInProgress = true;
    try {
      rows.setAll(topLevelRows);
      byMemberRowId.clear();
      for (final ModularCompoundRow cr : rows) {
        indexMembers(cr);
      }
      if (!listenersWired) {
        wireListeners();
        listenersWired = true;
      }
    } finally {
      bulkApplyInProgress = false;
    }
  }

  /**
   * Index direct members of {@code compound} as belonging to it. If a member is itself a
   * {@link ModularCompoundRow} (nested case, e.g. an ion compound containing isotopes), also
   * recurse so leaf rows are mapped to their innermost compound.
   */
  private void indexMembers(@NotNull final ModularCompoundRow compound) {
    for (final CompoundFeatureMember m : compound.getCompoundMembers()) {
      final FeatureListRow member = m.row();
      // append — a row may be a member of multiple compounds (bridge rows)
      byMemberRowId.computeIfAbsent(member.getTypedID(), id -> new ArrayList<>(1)).add(compound);
      if (member instanceof ModularCompoundRow nested) {
        byCompoundId.put(nested.getCompoundId(), nested);
        indexMembers(nested);
      }
    }
  }

  /**
   * Apply every binding's {@link CompoundRowBinding#apply} to every compound row. Used to
   * initialize derived values after {@link #setRows(List)} and to refresh after configuration
   * changes via {@link #reapplyBindings()}.
   * <p>
   * {@link #bulkApplyInProgress} is set during the pass so that listeners on values written by
   * bindings short-circuit instead of triggering redundant re-applies.
   */
  private void applyAllBindings() {
    bulkApplyInProgress = true;
    try {
      for (final ModularCompoundRow row : rows) {
        applyBindingsTo(row);
      }
    } finally {
      bulkApplyInProgress = false;
    }
  }

  /**
   * Re-run all bindings on every compound row. Use this after writing a configuration applied
   * method on the source feature list so bindings that read configuration from applied methods pick
   * up the new values.
   */
  public void reapplyBindings() {
    if (disposed) {
      throw new IllegalStateException("reapplyBindings called on a disposed CompoundList");
    }
    applyAllBindings();
  }

  /**
   * Apply every binding to a single compound row, recursing into nested compound members first so
   * inner aggregates exist before the outer aggregates that depend on them are computed.
   */
  private void applyBindingsTo(@NotNull final ModularCompoundRow row) {
    for (final CompoundFeatureMember m : row.getCompoundMembers()) {
      if (m.row() instanceof ModularCompoundRow nested) {
        applyBindingsTo(nested);
      }
    }
    for (final CompoundRowBinding binding : bindings) {
      binding.apply(row);
    }
  }

  /**
   * Register listeners on the source feature list's row + features schemas and on this compound
   * list's own row + features schemas for each binding. Each registration's removal is tracked in
   * {@link #listenerRemovers} so {@link #dispose()} can undo it.
   * <p>
   * The compound-schema registrations cover the nested case where one compound row is itself a
   * member of an outer compound: when the inner compound's value or feature is recomputed, the
   * listener fires on the compound schema and looks up the outer owner.
   * <p>
   * Listeners short-circuit while {@link #bulkApplyInProgress} is true so that the bulk pass
   * (already inner-first then outer) is not duplicated by cascading single re-applies.
   */
  private void wireListeners() {
    for (final CompoundRowBinding binding : bindings) {
      final DataTypeValueChangeListener<?> rowListener = (model, type, oldValue, newValue) -> {
        if (bulkApplyInProgress) {
          return;
        }
        if (!(model instanceof FeatureListRow row)) {
          return;
        }
        final List<ModularCompoundRow> owners = findCompoundsOf(row);
        if (owners.isEmpty()) {
          // top-level compound or unrelated row — stop propagation
          return;
        }
        // a row may belong to multiple compounds; re-apply for each owner
        for (final ModularCompoundRow owner : owners) {
          binding.apply(owner);
        }
      };
      final DataTypeValueChangeListener<?> featureListener = (model, type, oldValue, newValue) -> {
        if (bulkApplyInProgress) {
          return;
        }
        if (!(model instanceof ModularFeature feature)) {
          return;
        }
        final FeatureListRow row = feature.getRow();
        if (row == null) {
          return;
        }
        final List<ModularCompoundRow> owners = findCompoundsOf(row);
        if (owners.isEmpty()) {
          return;
        }
        for (final ModularCompoundRow owner : owners) {
          binding.apply(owner);
        }
      };

      // primary row-level type — source list and compound row schema
      final DataType<?> memberType = binding.getMemberRowType();
      final DataType<?> compoundType = binding.getCompoundRowType();
      featureList.addRowTypeValueListener(memberType, rowListener);
      listenerRemovers.add(() -> featureList.removeRowTypeValueListener(memberType, rowListener));

      compoundRowSchema.addDataTypeValueChangeListener(compoundType, rowListener);
      listenerRemovers.add(
          () -> compoundRowSchema.removeDataTypeValueChangeListener(compoundType, rowListener));

      // additional row-level types
      for (final DataType<?> additional : binding.getAdditionalMemberRowTypes()) {
        featureList.addRowTypeValueListener(additional, rowListener);
        listenerRemovers.add(() -> featureList.removeRowTypeValueListener(additional, rowListener));
        compoundRowSchema.addDataTypeValueChangeListener(additional, rowListener);
        listenerRemovers.add(
            () -> compoundRowSchema.removeDataTypeValueChangeListener(additional, rowListener));
      }

      // member feature-level types — register on source feature list's features schema
      for (final DataType<?> memberFeatureType : binding.getMemberFeatureTypes()) {
        featureList.addFeatureTypeValueListener(memberFeatureType, featureListener);
        listenerRemovers.add(
            () -> featureList.removeFeatureTypeListener(memberFeatureType, featureListener));
      }

      // compound feature-level types — register on this compound list's features schema, so a
      // nested compound's feature change propagates to its outer owner.
      for (final DataType<?> compoundFeatureType : binding.getCompoundFeatureTypes()) {
        compoundFeaturesSchema.addDataTypeValueChangeListener(compoundFeatureType, featureListener);
        listenerRemovers.add(
            () -> compoundFeaturesSchema.removeDataTypeValueChangeListener(compoundFeatureType,
                featureListener));
      }
    }
  }

  /**
   * Remove all listeners registered by {@link #wireListeners()}. Idempotent — safe to call multiple
   * times. After dispose, the CompoundList must not be used again.
   */
  public synchronized void dispose() {
    if (disposed) {
      return;
    }
    disposed = true;
    for (final Runnable remove : listenerRemovers) {
      remove.run();
    }
    listenerRemovers.clear();
    listenersWired = false;
  }

  public int size() {
    return rows.size();
  }

  public @NotNull ColumnarModularFeatureListRowsSchema getCompoundRowSchema() {
    return compoundRowSchema;
  }

  public @NotNull ColumnarModularDataModelSchema getCompoundFeaturesSchema() {
    return compoundFeaturesSchema;
  }

  public @NotNull ModularFeatureList getFeatureList() {
    return featureList;
  }

  /**
   * O(1) lookup: which compounds contain this row? A row may belong to multiple compounds (bridge
   * rows in correlation-aware grouping). Returns an empty list if the row is not a member of any
   * compound.
   */
  public @NotNull List<ModularCompoundRow> findCompoundsOf(@NotNull final FeatureListRow row) {
    final List<ModularCompoundRow> owners = byMemberRowId.get(row.getTypedID());
    return owners == null ? List.of() : Collections.unmodifiableList(owners);
  }

  /**
   * Convenience: the first compound this row belongs to, or empty if none. Equivalent to the old
   * single-owner contract; suitable for UI display where a row's role only needs to be picked from
   * one compound (the primary owner).
   */
  public @NotNull Optional<ModularCompoundRow> findFirstCompoundOf(
      @NotNull final FeatureListRow row) {
    final List<ModularCompoundRow> owners = byMemberRowId.get(row.getTypedID());
    return owners == null || owners.isEmpty() ? Optional.empty() : Optional.of(owners.get(0));
  }

  /**
   * O(1) lookup by compound id. Returns null if no compound row exists for this id. During project
   * load this resolves stubs registered by {@link #registerCompoundRowStub(ModularCompoundRow)}
   * before their content has been populated. Returns nested compound rows too — those that are not
   * in {@link #getRows()} but are reachable through some top-level compound row's member tree.
   */
  public @Nullable ModularCompoundRow findRowByCompoundId(final int compoundId) {
    return byCompoundId.get(compoundId);
  }

  /**
   * O(1) role lookup. Returns the role of the row in its first owning compound, or empty if the row
   * is not a member of any compound. For multi-membership cases use {@link #getRolesOf}.
   */
  public @NotNull Optional<CompoundMemberRole> getRoleOf(@NotNull final FeatureListRow row) {
    final FeatureListRowID rowId = row.getTypedID();
    return findFirstCompoundOf(row).flatMap(
        cr -> cr.getCompoundMembers().stream().filter(m -> m.row().getTypedID().equals(rowId))
            .findFirst().map(CompoundFeatureMember::role));
  }

  /**
   * Returns the role this row plays in each compound it belongs to. A row can have different roles
   * in different compounds (e.g. CORRELATED in compound A, ADDUCT in compound B if it's bridged).
   */
  public @NotNull List<CompoundMemberRole> getRolesOf(@NotNull final FeatureListRow row) {
    final FeatureListRowID rowId = row.getTypedID();
    final List<ModularCompoundRow> owners = byMemberRowId.get(row.getTypedID());
    if (owners == null || owners.isEmpty()) {
      return List.of();
    }
    final List<CompoundMemberRole> roles = new ArrayList<>(owners.size());
    for (final ModularCompoundRow cr : owners) {
      cr.getCompoundMembers().stream().filter(m -> m.row().getTypedID().equals(rowId)).findFirst()
          .map(CompoundFeatureMember::role).ifPresent(roles::add);
    }
    return roles;
  }

  @NotNull
  public List<ModularCompoundRow> getRowsCopy() {
    return List.copyOf(getRows());
  }

  /**
   * @return true if any top-level compound row holds a member that is itself a
   * {@link ModularCompoundRow} (i.e. a major ion row with isotope sub-rows).
   */
  public boolean hasNestedCompoundRows() {
    for (final ModularCompoundRow cr : rows) {
      for (final CompoundFeatureMember m : cr.getCompoundMembers()) {
        if (m.row() instanceof ModularCompoundRow mod) {
          // should have more than 1 isotope row
          if (mod.getMemberRows().size() > 1) {
            return true;
          }
        }
      }
    }
    return false;
  }

}
