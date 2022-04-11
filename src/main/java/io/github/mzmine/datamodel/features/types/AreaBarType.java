/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.datamodel.features.types;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.graphicalnodes.AreaBarChart;
import io.github.mzmine.datamodel.features.types.modifiers.GraphicalColumType;
import io.github.mzmine.datamodel.features.types.tasks.FeaturesGraphicalNodeTask;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.taskcontrol.TaskPriority;
import java.util.Map;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.layout.StackPane;
import org.jetbrains.annotations.NotNull;

public class AreaBarType extends DataType<Map<RawDataFile, ModularFeature>>
    implements GraphicalColumType<Map<RawDataFile, ModularFeature>> {

  @NotNull
  @Override
  public final String getUniqueID() {
    // Never change the ID for compatibility during saving/loading of type
    return "area_bar";
  }

  @NotNull
  @Override
  public String getHeaderString() {
    return "Area Bars";
  }

  @Override
  public Property<Map<RawDataFile, ModularFeature>> createProperty() {
    return new SimpleObjectProperty<>();
  }

  @Override
  public Class<Map<RawDataFile, ModularFeature>> getValueClass() {
    return (Class) Map.class;
  }

  @Override
  public Node getCellNode(
      TreeTableCell<ModularFeatureListRow, Map<RawDataFile, ModularFeature>> cell,
      TreeTableColumn<ModularFeatureListRow, Map<RawDataFile, ModularFeature>> coll,
      Map<RawDataFile, ModularFeature> cellData, RawDataFile raw) {
    ModularFeatureListRow row = cell.getTreeTableRow().getItem();
    if (row == null) {
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
    Task task = new FeaturesGraphicalNodeTask(AreaBarChart.class, pane, row, coll.getText());
    MZmineCore.getTaskController().addTask(task, TaskPriority.NORMAL);

    return pane;
  }

  @Override
  public double getColumnWidth() {
    return 205;
  }
}
