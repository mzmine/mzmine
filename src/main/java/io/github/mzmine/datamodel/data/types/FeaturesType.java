/*
 * Copyright 2006-2018 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.datamodel.data.types;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
import io.github.mzmine.datamodel.data.types.numbers.IDType;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.taskcontrol.TaskPriority;
import io.github.mzmine.taskcontrol.TaskStatus;
import javafx.application.Platform;
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
public class FeaturesType extends DataType<Map<RawDataFile, ModularFeature>>
    implements SubColumnsFactory {

  /**
   * TODO listen to changes in features, hold list of open FeatureTablesFX instances
   */
  private Map<Integer, Node> buffertCharts = new HashMap<>();


  public FeaturesType(Map<RawDataFile, ModularFeature> map) {
    super(Collections.unmodifiableMap(map));
  }

  @Override
  public String getHeaderString() {
    return "Features";
  }

  @Override
  @Nonnull
  public List<TreeTableColumn<ModularFeatureListRow, ?>> createSubColumns(
      final @Nullable RawDataFile raw) {
    List<TreeTableColumn<ModularFeatureListRow, ?>> cols = new ArrayList<>();
    // create bar chart
    TreeTableColumn<ModularFeatureListRow, FeaturesType> barsCol =
        new TreeTableColumn<>("Area Bars");
    barsCol.setCellValueFactory(new DataTypeCellValueFactory<>(null, this.getClass()));
    barsCol.setCellFactory(new DataTypeCellFactory<>(null, this.getClass(), cols.size()));
    cols.add(barsCol);

    TreeTableColumn<ModularFeatureListRow, FeaturesType> sharesCol =
        new TreeTableColumn<>("Area Share");
    sharesCol.setCellValueFactory(new DataTypeCellValueFactory<>(null, this.getClass()));
    sharesCol.setCellFactory(new DataTypeCellFactory<>(null, this.getClass(), cols.size()));
    cols.add(sharesCol);

    TreeTableColumn<ModularFeatureListRow, FeaturesType> shapes = new TreeTableColumn<>("Shapes");
    shapes.setCellValueFactory(new DataTypeCellValueFactory<>(null, this.getClass()));
    shapes.setCellFactory(new DataTypeCellFactory<>(null, this.getClass(), cols.size()));
    cols.add(shapes);

    // create all sample columns
    for (Entry<RawDataFile, ModularFeature> entry : value.entrySet()) {
      RawDataFile subRaw = entry.getKey();
      // create column per name
      TreeTableColumn<ModularFeatureListRow, String> sampleCol =
          new TreeTableColumn<>(subRaw.getName());
      // TODO get RawDataFile -> Color and set column header
      // sampleCol.setStyle("-fx-background-color: #"+ColorsFX.getHexString(color));
      // add sub columns of feature
      entry.getValue().stream().forEach(dataType -> {
        TreeTableColumn<ModularFeatureListRow, ?> subCol = dataType.createColumn(subRaw);
        if (subCol != null)
          sampleCol.getColumns().add(subCol);
      });

      // add all
      cols.add(sampleCol);
    }
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
  public String getFormattedSubColValue(int subcolumn, final @Nullable RawDataFile raw) {
    return "";
  }

  @Override
  @Nullable
  public Node getSubColNode(final int subcolumn,
      TreeTableCell<ModularFeatureListRow, ? extends DataType> cell,
      TreeTableColumn<ModularFeatureListRow, ? extends DataType> coll, DataType<?> cellData,
      RawDataFile raw) {
    ModularFeatureListRow row = cell.getTreeTableRow().getItem();
    if (row == null)
      return null;

    // find existing chart in rows FeaturesType
    FeaturesType type = (FeaturesType) cell.getItem();
    if (type != null) {
      Node node = type.getExistingChart(subcolumn);
      if (node != null)
        return node;
    }

    final StackPane pane = new StackPane();

    // TODO stop task if new task is started
    Task task = new AbstractTask() {
      private AtomicDouble progress = new AtomicDouble(0d);
      private int rowID = -1;

      @Override
      public void run() {
        rowID = row.get(IDType.class).map(DataType::getValue).orElse(-1);

        setStatus(TaskStatus.PROCESSING);
        final Node n;

        switch (subcolumn) {
          case 0:
            n = new AreaBarChart(row, progress);
            buffertCharts.put(subcolumn, n);
            break;
          case 1:
            n = new AreaShareChart(row, progress);
            buffertCharts.put(subcolumn, n);
            break;
          case 2:
            n = new FeatureShapeChart(row, progress);
            buffertCharts.put(subcolumn, n);
            break;
          default:
            n = null;
            break;
        }
        if (n != null)
          Platform.runLater(() -> {
            pane.getChildren().add(n);
          });

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
    MZmineCore.getTaskController().addTask(task, TaskPriority.NORMAL);

    return pane;
  }

  public Node getExistingChart(int subcolumn) {
    return buffertCharts.get(subcolumn);
  }
}
