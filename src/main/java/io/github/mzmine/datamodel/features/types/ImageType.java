package io.github.mzmine.datamodel.features.types;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.graphicalnodes.ImageChart;
import io.github.mzmine.datamodel.features.types.modifiers.GraphicalColumType;
import io.github.mzmine.datamodel.features.types.tasks.FeaturesGraphicalNodeTask;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.taskcontrol.TaskPriority;
import javafx.beans.property.MapProperty;
import javafx.beans.property.SimpleMapProperty;
import javafx.scene.Node;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.layout.StackPane;
import javax.annotation.Nonnull;

public class ImageType extends DataType<MapProperty<RawDataFile, ModularFeature>>
    implements GraphicalColumType<MapProperty<RawDataFile, ModularFeature>> {

  @Nonnull
  @Override
  public String getHeaderString() {
    return "Images";
  }

  @Override
  public MapProperty<RawDataFile, ModularFeature> createProperty() {
    return new SimpleMapProperty<RawDataFile, ModularFeature>();
  }

  @Override
  public Node getCellNode(
      TreeTableCell<ModularFeatureListRow, MapProperty<RawDataFile, ModularFeature>> cell,
      TreeTableColumn<ModularFeatureListRow, MapProperty<RawDataFile, ModularFeature>> coll,
      MapProperty<RawDataFile, ModularFeature> cellData, RawDataFile raw) {
    ModularFeatureListRow row = cell.getTreeTableRow().getItem();
    if (row == null)
      return null;

    // get existing buffered node from row (for column name)
    // TODO listen to changes in features data
    Node node = row.getBufferedColChart(coll.getText());
    if (node != null)
      return node;

    StackPane pane = new StackPane();

    // TODO stop task if new task is started
    Task task = new FeaturesGraphicalNodeTask(ImageChart.class, pane, row, coll.getText());
    MZmineCore.getTaskController().addTask(task, TaskPriority.NORMAL);

    return pane;
  }

  @Override
  public double getColumnWidth() {
    return 205;
  }
}
