package io.github.mzmine.datamodel.features.compoundlist;

import io.github.mzmine.datamodel.features.DataTypeValueChangeListener;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.columnar_data.ColumnarModularDataModelSchema;
import io.github.mzmine.datamodel.features.columnar_data.ColumnarModularFeatureListRowsSchema;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.util.CompoundSchemaTypes;
import io.github.mzmine.util.FeatureListUtils;
import io.github.mzmine.util.MemoryMapStorage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
  private final long sourceStructuralVersion;

  private final ObservableList<ModularCompoundRow> rows = FXCollections.observableArrayList();
  private final ObservableList<ModularCompoundRow> rowsReadOnly = FXCollections.unmodifiableObservableList(
      rows);

  // O(1) reverse lookup: member row ID → owning compound. ConcurrentHashMap so listeners on
  // background threads can safely read while setRows rebuilds on the FX thread.
  private final Map<Integer, ModularCompoundRow> byMemberRowId = new ConcurrentHashMap<>();

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
   * compound list was built.
   */
  public boolean isStale() {
    return sourceStructuralVersion != featureList.getStructuralVersion();
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
      case ALL_MAJOR_IONS -> getRowsCopy().stream().<FeatureListRow>mapMulti((comp, up) -> {
        // compound row is not returned, only the first level of main ions
        for (FeatureListRow member : comp.getMemberRows()) {
          up.accept(member);
        }
      }).toList();
      case ALL_ISOTOPES -> getRowsCopy().stream().<FeatureListRow>mapMulti((comp, up) -> {
        // compound row is not returned, just the first level of main ions and then their isotopes
        for (FeatureListRow member : comp.getMemberRows()) {
          if (member instanceof ModularCompoundRow compMember) {
            final List<FeatureListRow> isotopeRows = compMember.getMemberRows();
            for (FeatureListRow isotope : isotopeRows) {
              up.accept(isotope);
            }
          } else {
            up.accept(member);
          }
        }
      }).toList();
    };
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
    for (final ModularCompoundRow cr : newRows) {
      indexMembers(cr);
    }
    applyAllBindings();
    if (!listenersWired) {
      wireListeners();
      listenersWired = true;
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
      byMemberRowId.put(member.getID(), compound);
      if (member instanceof ModularCompoundRow nested) {
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
        final ModularCompoundRow owner = findCompoundOf(row);
        if (owner == null) {
          // top-level compound or unrelated row — stop propagation
          return;
        }
        binding.apply(owner);
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
        final ModularCompoundRow owner = findCompoundOf(row);
        if (owner == null) {
          return;
        }
        binding.apply(owner);
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
   * TODO think if we can support a single row being mapped to multiple parent rows, then all of those should update.
   *
   * O(1) lookup: which compound owns this row? Returns null if not a member of any compound.
   */
  public @Nullable ModularCompoundRow findCompoundOf(@NotNull final FeatureListRow row) {
    return byMemberRowId.get(row.getID());
  }

  /**
   * O(1) role lookup. Returns null if the row is not a member of any compound.
   */
  public @Nullable CompoundMemberRole getRoleOf(@NotNull final FeatureListRow row) {
    final ModularCompoundRow cr = findCompoundOf(row);
    if (cr == null) {
      return null;
    }
    return cr.getCompoundMembers().stream().filter(m -> m.row().getID() == row.getID()).findFirst()
        .map(CompoundFeatureMember::role).orElse(null);
  }

  @NotNull
  public List<ModularCompoundRow> getRowsCopy() {
    return List.copyOf(getRows());
  }
}
