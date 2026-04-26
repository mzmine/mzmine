package io.github.mzmine.datamodel.features.compoundlist;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.columnar_data.ColumnarModularDataModelSchema;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.util.CompoundSchemaTypes;
import io.github.mzmine.util.MemoryMapStorage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Holds a columnar schema and an ordered list of {@link ModularCompoundRow}s derived from a
 * {@link ModularFeatureList}. Tracks the structural version of the source feature list at
 * construction time; call {@link #isStale()} to detect if the feature list changed since then.
 */
public class CompoundList {

  private static final Logger logger = Logger.getLogger(CompoundList.class.getName());

  @NotNull private final ModularFeatureList featureList;
  @NotNull private final ColumnarModularDataModelSchema schema;
  private final long sourceStructuralVersion;

  private final ObservableList<ModularCompoundRow> rows = FXCollections.observableArrayList();
  private final ObservableList<ModularCompoundRow> rowsReadOnly =
      FXCollections.unmodifiableObservableList(rows);

  // O(1) reverse lookup: member row ID → owning compound
  private final Map<Integer, ModularCompoundRow> byMemberRowId = new HashMap<>();

  public CompoundList(@NotNull final ModularFeatureList featureList,
      @Nullable final MemoryMapStorage storage,
      final int estimatedRows) {
    this.featureList = featureList;
    this.sourceStructuralVersion = featureList.getStructuralVersion();
    this.schema = new ColumnarModularDataModelSchema(
        storage, featureList.getName() + "_compounds", Math.max(estimatedRows, 1));
    // pre-register compound columns so the schema is queryable before any row is written
    schema.addDataTypes(CompoundSchemaTypes.REGISTERED.toArray(new DataType[0]));
  }

  /**
   * @return true if the source feature list changed structurally (rows added / removed) since
   *     this compound list was built.
   */
  public boolean isStale() {
    return sourceStructuralVersion != featureList.getStructuralVersion();
  }

  /** @return unmodifiable view of compound rows. */
  public @NotNull ObservableList<ModularCompoundRow> getRows() {
    return rowsReadOnly;
  }

  /**
   * Replace all rows and rebuild the reverse member index. Must be called on the FX thread when
   * the list is already bound to UI components.
   */
  public void setRows(@NotNull final List<ModularCompoundRow> newRows) {
    rows.setAll(newRows);
    byMemberRowId.clear();
    for (final ModularCompoundRow cr : newRows) {
      for (final CompoundFeatureMember m : cr.getCompoundMembers()) {
        byMemberRowId.put(m.row().getID(), cr);
      }
    }
  }

  public int size() {
    return rows.size();
  }

  public @NotNull ColumnarModularDataModelSchema getSchema() {
    return schema;
  }

  public @NotNull ModularFeatureList getFeatureList() {
    return featureList;
  }

  /** O(1) lookup: which compound owns this row? Returns null if not a member of any compound. */
  public @Nullable ModularCompoundRow findCompoundOf(@NotNull final FeatureListRow row) {
    return byMemberRowId.get(row.getID());
  }

  /** O(1) role lookup. Returns null if the row is not a member of any compound. */
  public @Nullable CompoundMemberRole getRoleOf(@NotNull final FeatureListRow row) {
    final ModularCompoundRow cr = findCompoundOf(row);
    if (cr == null) {
      return null;
    }
    return cr.getCompoundMembers().stream()
        .filter(m -> m.row().getID() == row.getID())
        .findFirst().map(CompoundFeatureMember::role).orElse(null);
  }
}
