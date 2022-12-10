/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package io.github.mzmine.datamodel.features.types;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.graphicalnodes.FeatureShapeChart;
import io.github.mzmine.datamodel.features.types.tasks.FeaturesGraphicalNodeTask;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.chromatogram.ChromatogramVisualizerModule;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.taskcontrol.TaskPriority;
import java.util.List;
import javafx.scene.Node;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.layout.StackPane;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Contains no data - listens to changes in all {@link ModularFeature} in a {@link
 * ModularFeatureListRow} On change of any {@link FeatureDataType} - the chart is updated
 *
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public class FeatureShapeType extends LinkedGraphicalType {

  @NotNull
  @Override
  public final String getUniqueID() {
    // Never change the ID for compatibility during saving/loading of type
    return "feature_shape";
  }

  @NotNull
  @Override
  public String getHeaderString() {
    return "Shapes";
  }

  @Override
  public Node getCellNode(TreeTableCell<ModularFeatureListRow, Boolean> cell,
      TreeTableColumn<ModularFeatureListRow, Boolean> coll, Boolean cellData, RawDataFile raw) {
    ModularFeatureListRow row = cell.getTreeTableRow().getItem();
    if (row == null || !cellData) {
      return null;
    }

    // get existing buffered node from row (for column name)
    // TODO listen to changes in features data
    Node node = row.getBufferedColChart(coll.getText());
    if (node != null) {
      return node;
    }

    StackPane pane = new StackPane();

    // TODO stop task if new task is started
    Task task = new FeaturesGraphicalNodeTask(FeatureShapeChart.class, pane, row, coll.getText());
    // TODO change to TaskPriority.LOW priority
    MZmineCore.getTaskController().addTask(task, TaskPriority.HIGH);

    return pane;
  }


  @Override
  public double getColumnWidth() {
    return DEFAULT_GRAPHICAL_CELL_WIDTH;
  }

  @Nullable
  @Override
  public Runnable getDoubleClickAction(@NotNull ModularFeatureListRow row,
      @NotNull List<RawDataFile> rawDataFiles, DataType<?> superType, @Nullable final Object value) {

    return () -> {
      MZmineCore.runLater(() -> ChromatogramVisualizerModule.visualizeFeatureListRows(List.of(row)));
    };
  }

  @Override
  public boolean getDefaultVisibility() {
    return true;
  }
}
