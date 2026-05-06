package io.github.mzmine.modules.visualization.compounddashboard.table;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularDataModel;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.compoundlist.CompoundList;
import io.github.mzmine.datamodel.features.compoundlist.CompoundMemberRole;
import io.github.mzmine.datamodel.features.compoundlist.ModularCompoundRow;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.compoundlist.CompoundMemberRoleType;
import io.github.mzmine.util.CompoundSchemaTypes;
import java.util.logging.Logger;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TreeTableColumn;
import javafx.util.Callback;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Routes cell value reads for the two-level tree in CompoundTableFX:
 * <ul>
 *   <li>Parent row ({@link ModularCompoundRow}) — reads compound-schema types directly; delegates
 *       all other types to the preferred row.</li>
 *   <li>Child row ({@link ModularFeatureListRow}) — compound-schema columns are blank; role column
 *       is resolved via the {@link CompoundList} reverse index.</li>
 * </ul>
 */
public class CompoundAwareCellValueFactory
    implements Callback<TreeTableColumn.CellDataFeatures<ModularDataModel, Object>,
    ObservableValue<Object>> {

  private static final Logger logger = Logger.getLogger(
      CompoundAwareCellValueFactory.class.getName());

  @NotNull private final DataType<?> type;
  @NotNull private final CompoundList compoundList;

  public CompoundAwareCellValueFactory(@NotNull final DataType<?> type,
      @NotNull final CompoundList compoundList) {
    this.type = type;
    this.compoundList = compoundList;
  }

  @Override
  public ObservableValue<Object> call(
      final TreeTableColumn.CellDataFeatures<ModularDataModel, Object> cdf) {
    return new ReadOnlyObjectWrapper<>(resolve(cdf.getValue().getValue()));
  }

  @SuppressWarnings("unchecked")
  private @Nullable Object resolve(@Nullable final ModularDataModel row) {
    if (row instanceof ModularCompoundRow cr) {
      if (type instanceof CompoundMemberRoleType) {
        return null;  // role column is blank at the compound level
      }
      if (CompoundSchemaTypes.isCompoundOwned(type)) {
        return cr.get((DataType<Object>) type);
      }
      // row-level types (mz, rt, annotations…) delegate to preferred row
      final FeatureListRow preferred = cr.getPreferredRow();
      return preferred instanceof ModularFeatureListRow mflr ? mflr.get((DataType<Object>) type)
          : null;
    }
    if (row instanceof ModularFeatureListRow mflr) {
      if (type instanceof CompoundMemberRoleType) {
        final CompoundMemberRole role = compoundList.getRoleOf(mflr);
        return role == null ? null : role.getLabel();
      }
      if (CompoundSchemaTypes.isCompoundOwned(type)) {
        return null;  // compound-schema columns are blank for member rows
      }
      return mflr.get((DataType<Object>) type);
    }
    return null;
  }
}
