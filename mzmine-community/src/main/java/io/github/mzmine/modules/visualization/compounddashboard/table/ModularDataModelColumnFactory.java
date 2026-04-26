package io.github.mzmine.modules.visualization.compounddashboard.table;

import io.github.mzmine.datamodel.features.ModularDataModel;
import io.github.mzmine.datamodel.features.compoundlist.CompoundList;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.modifiers.IgnoreAutoColumn;
import javafx.scene.control.TreeTableColumn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Builds row-level {@link TreeTableColumn}s for {@code CompoundTableFX}. Never creates
 * per-RawDataFile feature columns — those belong to FeatureTableFX only.
 */
public final class ModularDataModelColumnFactory {

  private ModularDataModelColumnFactory() {
  }

  /**
   * Creates a column for {@code type}, wired to the given {@code compoundList} for role lookups.
   * Returns {@code null} for {@link IgnoreAutoColumn} types — callers must skip null results.
   */
  public static @Nullable TreeTableColumn<ModularDataModel, Object> createRowColumn(
      @NotNull final DataType<?> type,
      @NotNull final CompoundList compoundList) {
    if (type instanceof IgnoreAutoColumn) {
      return null;
    }
    final TreeTableColumn<ModularDataModel, Object> col =
        new TreeTableColumn<>(type.getHeaderString());
    col.setUserData(type);
    col.setCellValueFactory(new CompoundAwareCellValueFactory(type, compoundList));
    col.setCellFactory(new ModularDataModelCellFactory(type));
    col.setPrefWidth(type.getPrefColumnWidth());
    col.setSortable(true);
    return col;
  }
}
