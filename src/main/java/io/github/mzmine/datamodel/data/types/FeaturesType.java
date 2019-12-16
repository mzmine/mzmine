/*
 * Copyright 2006-2020 The MZmine Development Team
 * 
 * This file is part of MZmine.
 * 
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.datamodel.data.types;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.google.common.util.concurrent.AtomicDouble;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.data.ModularFeature;
import io.github.mzmine.datamodel.data.ModularFeatureListRow;
import io.github.mzmine.datamodel.data.types.fx.DataTypeCellFactory;
import io.github.mzmine.datamodel.data.types.fx.DataTypeCellValueFactory;
import io.github.mzmine.datamodel.data.types.graphicalnodes.AreaBarChart;
import io.github.mzmine.datamodel.data.types.graphicalnodes.AreaShareChart;
import io.github.mzmine.datamodel.data.types.graphicalnodes.FeatureShapeChart;
import io.github.mzmine.datamodel.data.types.modifiers.SubColumnsFactory;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.taskcontrol.TaskPriority;
import io.github.mzmine.taskcontrol.TaskStatus;
import javafx.application.Platform;
import javafx.beans.property.MapProperty;
import javafx.beans.property.SimpleMapProperty;
import javafx.scene.Node;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.layout.StackPane;

/**
 * This FeaturesType contains features for each RawDataFile. Sub columns for samples and charts are
 * created.
 * 
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 *
 */
public class FeaturesType extends DataType<MapProperty<RawDataFile, ModularFeature>>
    implements SubColumnsFactory<MapProperty<RawDataFile, ModularFeature>> {

  @Override
  public String getHeaderString() {
    return "Features";
  }

  @Override
  public MapProperty<RawDataFile, ModularFeature> createProperty() {
    return new SimpleMapProperty<RawDataFile, ModularFeature>();
  }

  @Override
  @Nonnull
  public List<TreeTableColumn<ModularFeatureListRow, Object>> createSubColumns(
      final @Nullable RawDataFile raw) {
    List<TreeTableColumn<ModularFeatureListRow, Object>> cols = new ArrayList<>();
    // create bar chart
    TreeTableColumn<ModularFeatureListRow, Object> barsCol = new TreeTableColumn<>("Area Bars");
    barsCol.setCellValueFactory(new DataTypeCellValueFactory(null, this));
    barsCol.setCellFactory(new DataTypeCellFactory(null, this, cols.size()));
    cols.add(barsCol);

    TreeTableColumn<ModularFeatureListRow, Object> sharesCol = new TreeTableColumn<>("Area Share");
    sharesCol.setCellValueFactory(new DataTypeCellValueFactory(null, this));
    sharesCol.setCellFactory(new DataTypeCellFactory(null, this, cols.size()));
    cols.add(sharesCol);

    TreeTableColumn<ModularFeatureListRow, Object> shapes = new TreeTableColumn<>("Shapes");
    shapes.setCellValueFactory(new DataTypeCellValueFactory(null, this));
    shapes.setCellFactory(new DataTypeCellFactory(null, this, cols.size()));
    cols.add(shapes);

    /*
     * sample columns are created in the FeatureListFX class
     */

    return cols;
  }

  /**
   * Create bar chart of data
   * 
   * @param cell
   * @param coll
   * @return
   */
  public Node getBarChart(@Nonnull ModularFeatureListRow row, AtomicDouble progress) {
    return new AreaBarChart(row, progress);
  }

  /**
   * Create bar chart of data
   * 
   * @param cell
   * @param coll
   * @return
   */
  public Node getAreaShareChart(@Nonnull ModularFeatureListRow row, AtomicDouble progress) {
    return new AreaShareChart(row, progress);
  }

  @Override
  @Nullable
  public String getFormattedSubColValue(int subcolumn,
      TreeTableCell<ModularFeatureListRow, Object> cell,
      TreeTableColumn<ModularFeatureListRow, Object> coll, Object cellData, RawDataFile raw) {
    return "";
  }

  @Override
  @Nullable
  public Node getSubColNode(int subcolumn, TreeTableCell<ModularFeatureListRow, Object> cell,
      TreeTableColumn<ModularFeatureListRow, Object> coll, Object cellData, RawDataFile raw) {
    ModularFeatureListRow row = cell.getTreeTableRow().getItem();
    if (row == null)
      return null;

    // get existing buffered node from row (for column name)
    // TODO listen to changes in features data
    Node node = row.getBufferedColChart(coll.getText());
    if (node != null)
      return node;

    final StackPane pane = new StackPane();

    // TODO stop task if new task is started
    Task task = new AbstractTask() {
      private AtomicDouble progress = new AtomicDouble(0d);
      private int rowID = -1;

      @Override
      public void run() {
        rowID = row.getID();

        setStatus(TaskStatus.PROCESSING);
        final Node n;

        switch (subcolumn) {
          case 0:
            n = new AreaBarChart(row, progress);
            break;
          case 1:
            n = new AreaShareChart(row, progress);
            break;
          case 2:
            n = new FeatureShapeChart(row, progress);
            break;
          default:
            n = null;
            break;
        }
        // save chart for later
        row.addBufferedColChart(coll.getText(), n);
        if (n != null) {
          Platform.runLater(() -> {
            pane.getChildren().add(n);
          });
        }
        setStatus(TaskStatus.FINISHED);
        progress.set(1d);
      }

      @Override
      public String getTaskDescription() {
        return "Creating a graphical column for col: " + cell.getTableColumn().getText()
            + " in row: " + rowID;
      }

      @Override
      public double getFinishedPercentage() {
        return progress.get();
      }
    };
    if (MZmineCore.getTaskController() != null)
      MZmineCore.getTaskController().addTask(task, TaskPriority.NORMAL);

    return pane;
  }
}
