/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.datamodel.features.types;

import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.featuredata.IonMobilogramTimeSeries;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.graphicalnodes.FeatureShapeIonMobilityRetentionTimeHeatMapChart;
import io.github.mzmine.datamodel.features.types.modifiers.GraphicalColumType;
import io.github.mzmine.datamodel.features.types.tasks.FeatureGraphicalNodeTask;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.ims_featurevisualizer.IMSFeatureVisualizerTab;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.taskcontrol.TaskPriority;
import java.util.List;
import javafx.scene.Node;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.layout.StackPane;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class FeatureShapeIonMobilityRetentionTimeHeatMapType extends LinkedDataType
    implements GraphicalColumType<Boolean> {

  @Nonnull
  @Override
  public String getHeaderString() {
    return "Ion mobility trace";
  }

  @Override
  public Node getCellNode(TreeTableCell<ModularFeatureListRow, Boolean> cell,
      TreeTableColumn<ModularFeatureListRow, Boolean> coll, Boolean cellData, RawDataFile raw) {
    ModularFeatureListRow row = cell.getTreeTableRow().getItem();
    if (row == null || row.getFeature(raw) == null || !(raw instanceof IMSRawDataFile)) {
      return null;
    }
    ModularFeature feature = row.getFeature(raw);
    if (feature == null || !(feature.getFeatureData() instanceof IonMobilogramTimeSeries)) {
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
    Task task = new FeatureGraphicalNodeTask(
        FeatureShapeIonMobilityRetentionTimeHeatMapChart.class, pane, feature, coll.getText());
    MZmineCore.getTaskController().addTask(task, TaskPriority.NORMAL);

    return pane;
  }

  @Override
  public double getColumnWidth() {
    return DEFAULT_GRAPHICAL_CELL_WIDTH + 50;
  }

  @Nullable
  @Override
  public Runnable getDoubleClickAction(@Nonnull ModularFeatureListRow row,
      @Nonnull List<RawDataFile> file) {
    return () -> MZmineCore.getDesktop()
        .addTab(new IMSFeatureVisualizerTab(row.getFeature(file.get(0))));
  }
}
