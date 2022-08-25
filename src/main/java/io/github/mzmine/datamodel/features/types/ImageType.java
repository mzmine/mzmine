/*
 *  Copyright 2006-2022 The MZmine Development Team
 *
 *  This file is part of MZmine.
 *
 *  MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 *  General Public License as published by the Free Software Foundation; either version 2 of the
 *  License, or (at your option) any later version.
 *
 *  MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 *  the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 *  Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with MZmine; if not,
 *  write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 *  USA
 */

package io.github.mzmine.datamodel.features.types;

import io.github.mzmine.datamodel.ImagingRawDataFile;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.graphicalnodes.ImageChart;
import io.github.mzmine.datamodel.features.types.tasks.FeatureGraphicalNodeTask;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.taskcontrol.TaskPriority;
import javafx.scene.Node;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.layout.StackPane;
import org.jetbrains.annotations.NotNull;

public class ImageType extends LinkedGraphicalType {

  @NotNull
  @Override
  public final String getUniqueID() {
    // Never change the ID for compatibility during saving/loading of type
    return "image_map";
  }

  @NotNull
  @Override
  public String getHeaderString() {
    return "Images";
  }

  @Override
  public Node getCellNode(TreeTableCell<ModularFeatureListRow, Boolean> cell,
      TreeTableColumn<ModularFeatureListRow, Boolean> coll, Boolean value, RawDataFile raw) {
    ModularFeatureListRow row = cell.getTableRow().getItem();
    if (row == null || !value || row.getFeature(raw) == null
        || !(raw instanceof ImagingRawDataFile)) {
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
