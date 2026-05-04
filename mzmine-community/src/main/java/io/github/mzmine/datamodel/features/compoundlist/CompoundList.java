package io.github.mzmine.datamodel.features.compoundlist;

import io.github.mzmine.datamodel.features.DataTypeValueChangeListener;
import io.github.mzmine.datamodel.features.FeatureListRow;
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

  @NotNull private final List<CompoundRowBinding> bindings;
  // Each entry undoes one listener registration on dispose.
  private final List<Runnable> listenerRemovers = new ArrayList<>();
  private boolean listenersWired = false;
  private boolean disposed = false;

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
   * initialize derived values after {@link #setRows(List)}.
   */
  private void applyAllBindings() {
    for (final ModularCompoundRow row : rows) {
      applyBindingsTo(row);
    }
  }

  /**
   * Apply every binding to a single compound row, recursing into nested compound members first
   * so inner aggregates exist before the outer aggregates that depend on them are computed.
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
   * Register a listener on both the source feature list's row schema and on this compound list's
   * own row schema for each binding. Each registration's removal is tracked in
   * {@link #listenerRemovers} so {@link #dispose()} can undo it.
   * <p>
   * The compound-schema registration covers the nested case where one compound row is itself a
   * member of an outer compound: when the inner compound's value is recomputed, the listener fires
   * on the compound schema and looks up the outer owner.
   */
  private void wireListeners() {
    for (final CompoundRowBinding binding : bindings) {
      final DataTypeValueChangeListener<?> listener = (model, type, oldValue, newValue) -> {
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
      final DataType<?> memberType = binding.getMemberRowType();
      final DataType<?> compoundType = binding.getCompoundRowType();
      featureList.addRowTypeValueListener(memberType, listener);
      listenerRemovers.add(
          () -> featureList.removeRowTypeValueListener(memberType, listener));

      compoundRowSchema.addDataTypeValueChangeListener(compoundType, listener);
      listenerRemovers.add(
          () -> compoundRowSchema.removeDataTypeValueChangeListener(compoundType, listener));
    }
  }

  /**
   * Remove all listeners registered by {@link #wireListeners()}. Idempotent — safe to call
   * multiple times. After dispose, the CompoundList must not be used again.
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
