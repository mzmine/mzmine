package io.github.mzmine.modules.visualization.compounddashboard.table;

import io.github.mzmine.datamodel.features.ModularDataModel;
import io.github.mzmine.datamodel.features.types.DataType;
import java.util.logging.Logger;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.util.Callback;
import org.jetbrains.annotations.NotNull;

/**
 * Text cell factory for {@code TreeTableColumn<ModularDataModel, Object>} used in CompoundTableFX.
 * {@link io.github.mzmine.datamodel.features.types.fx.DataTypeCellFactory} is typed to
 * {@code ModularFeatureListRow} and cannot be used here directly.
 */
public class ModularDataModelCellFactory
    implements Callback<TreeTableColumn<ModularDataModel, Object>,
    TreeTableCell<ModularDataModel, Object>> {

  private static final Logger logger = Logger.getLogger(
      ModularDataModelCellFactory.class.getName());

  @NotNull private final DataType<?> type;

  public ModularDataModelCellFactory(@NotNull final DataType<?> type) {
    this.type = type;
  }

  @Override
  public TreeTableCell<ModularDataModel, Object> call(
      final TreeTableColumn<ModularDataModel, Object> col) {
    return new TreeTableCell<>() {
      @Override
      @SuppressWarnings("unchecked")
      protected void updateItem(final Object item, final boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
          setText(null);
          setGraphic(null);
        } else {
          setText(((DataType<Object>) type).getFormattedString(item, false));
          setGraphic(null);
        }
      }
    };
  }
}
