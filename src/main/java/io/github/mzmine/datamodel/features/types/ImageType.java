package io.github.mzmine.datamodel.features.types;

import io.github.mzmine.datamodel.ImagingRawDataFile;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.graphicalnodes.ImageChart;
import io.github.mzmine.datamodel.features.types.modifiers.GraphicalColumType;
import io.github.mzmine.datamodel.features.types.tasks.FeatureGraphicalNodeTask;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.taskcontrol.TaskPriority;
import javafx.scene.Node;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.layout.StackPane;
import javax.annotation.Nonnull;

public class ImageType extends LinkedDataType
    implements GraphicalColumType<Boolean> {

  @Nonnull
  @Override
  public String getHeaderString() {
    return "Images";
  }

  @Override
  public Node getCellNode(
      TreeTableCell<ModularFeatureListRow, Boolean> cell,
      TreeTableColumn<ModularFeatureListRow, Boolean> coll, Boolean cellData, RawDataFile raw) {
    ModularFeatureListRow row = cell.getTreeTableRow().getItem();
    if (row == null || row.getFeature(raw) == null || !(raw instanceof ImagingRawDataFile)) {
      return null;
    }

    ModularFeature feature = row.getFeature(raw);
    ImagingRawDataFile imagingFile = (ImagingRawDataFile) feature.getRawDataFile();
    if (Double.compare(imagingFile.getImagingParam().getLateralHeight(), 0d) == 0
        || Double.compare(imagingFile.getImagingParam().getLateralWidth(), 0d) == 0
        || feature == null || feature.getRawDataFile() == null
        || feature.getFeatureData() == null) {
      return null;
    }

    // get existing buffered node from row (for column name)
    // TODO listen to changes in features data
    Node node = feature.getBufferedColChart(coll.getText());
    if (node != null) {
      return node;
    }

    StackPane pane = new StackPane();

    // TODO stop task if new task is started
    Task task = new FeatureGraphicalNodeTask(ImageChart.class, pane, feature, coll.getText());
    MZmineCore.getTaskController().addTask(task, TaskPriority.NORMAL);

    return pane;
  }

  @Override
  public double getColumnWidth() {
    return 205;
  }
}
