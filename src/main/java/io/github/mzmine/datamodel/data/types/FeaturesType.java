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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.data.DataTypeMap;
import io.github.mzmine.datamodel.data.ModularFeature;
import io.github.mzmine.datamodel.data.ModularFeatureListRow;
import io.github.mzmine.datamodel.data.types.fx.DataTypeCellFactory;
import io.github.mzmine.datamodel.data.types.fx.RawsMapCellValueFactory;
import io.github.mzmine.datamodel.data.types.modifiers.SubColumnsFactory;
import io.github.mzmine.datamodel.data.types.numbers.AreaType;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

/**
 * This FeaturesType contains features for each RawDataFile. Sub columns for samples and charts are
 * created.
 * 
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 *
 */
public class FeaturesType extends DataType<Map<RawDataFile, ModularFeature>>
    implements SubColumnsFactory {

  public FeaturesType(Map<RawDataFile, ModularFeature> map) {
    super(Collections.unmodifiableMap(map));
  }

  @Override
  public String getHeaderString() {
    return "Features";
  }

  @Override
  @Nonnull
  public List<TreeTableColumn<ModularFeatureListRow, ?>> createSubColumns() {
    List<TreeTableColumn<ModularFeatureListRow, ?>> cols = new ArrayList<>();
    // create bar chart
    TreeTableColumn<ModularFeatureListRow, FeaturesType> barsCol =
        new TreeTableColumn<>("Area Bars");
    barsCol.setCellValueFactory(new RawsMapCellValueFactory<>(null, this.getClass()));
    barsCol.setCellFactory(new DataTypeCellFactory<>(null, this.getClass(), 0));
    cols.add(barsCol);

    TreeTableColumn<ModularFeatureListRow, FeaturesType> sharesCol =
        new TreeTableColumn<>("Area Share");
    sharesCol.setCellValueFactory(new RawsMapCellValueFactory<>(null, this.getClass()));
    sharesCol.setCellFactory(new DataTypeCellFactory<>(null, this.getClass(), 1));
    cols.add(sharesCol);

    // create all sample columns
    for (Entry<RawDataFile, ModularFeature> entry : value.entrySet()) {
      RawDataFile raw = entry.getKey();
      // create column per name
      TreeTableColumn<ModularFeatureListRow, String> sampleCol =
          new TreeTableColumn<>(raw.getName());
      // TODO get RawDataFile -> Color and set column header
      // sampleCol.setStyle("-fx-background-color: #"+ColorsFX.getHexString(color));
      // add sub columns of feature
      DataTypeMap map = entry.getValue().getMap();
      map.stream().forEach(dataType -> {
        sampleCol.getColumns().add(dataType.createColumn(raw));
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
  public Node getBarChart(TreeTableCell<ModularFeatureListRow, ? extends DataType> cell,
      TreeTableColumn<ModularFeatureListRow, ? extends DataType> coll) {
    ModularFeatureListRow row = cell.getTreeTableRow().getItem();
    if (row == null)
      return null;

    XYChart.Series data = new XYChart.Series();
    int i = 1;
    for (Entry<RawDataFile, ModularFeature> entry : row.getFeatures().entrySet()) {
      Float area = entry.getValue().get(AreaType.class).map(DataType::getValue).orElse(0f);
      data.getData().add(new XYChart.Data("" + i, area));
      i++;
    }

    final CategoryAxis xAxis = new CategoryAxis();
    final NumberAxis yAxis = new NumberAxis();
    final BarChart<String, Number> bc = new BarChart<String, Number>(xAxis, yAxis);
    bc.setLegendVisible(false);
    bc.setMinHeight(100);
    bc.setPrefHeight(100);
    bc.setMaxHeight(100);
    bc.setBarGap(3);
    bc.setCategoryGap(3);
    bc.setPrefWidth(150);

    BorderPane pane = new BorderPane(bc);
    bc.getData().addAll(data);
    return pane;
  }

  /**
   * Create bar chart of data
   * 
   * @param cell
   * @param coll
   * @return
   */
  public Node getAreaShareChart(TreeTableCell<ModularFeatureListRow, ? extends DataType> cell,
      TreeTableColumn<ModularFeatureListRow, ? extends DataType> coll) {
    ModularFeatureListRow row = cell.getTreeTableRow().getItem();

    if (row == null)
      return null;

    Float sum = row.getFeatures().entrySet().stream().map(Entry::getValue)
        .map(e -> e.get(AreaType.class).map(DataType::getValue).orElse(0f)).reduce(0f, Float::sum);
    Map<RawDataFile, Color> rawColors = row.get(RawsColorsType.class).map(DataType::getValue)
        .orElse(FXCollections.emptyObservableMap());

    List<Rectangle> all = new ArrayList<>();
    int i = 0;
    for (Entry<RawDataFile, ModularFeature> entry : row.getFeatures().entrySet()) {
      Float area = entry.getValue().get(AreaType.class).map(DataType::getValue).orElse(null);
      if (area != null) {
        Color color = rawColors.get(entry.getKey());
        if (color == null)
          color = Color.LIGHTSLATEGREY;
        float ratio = area / sum;
        Rectangle rect = new Rectangle();
        rect.setFill(color);
        // bind width
        rect.widthProperty().bind(cell.widthProperty().multiply(ratio));
        rect.setHeight(i % 2 == 0 ? 20 : 25);
        all.add(rect);
        i++;
      }
    }
    HBox box = new HBox(0, all.toArray(Rectangle[]::new));
    box.setPrefWidth(100);
    box.setAlignment(Pos.CENTER_LEFT);

    return box;
  }

  @Override
  @Nullable
  public String getFormattedSubColValue(int subcolumn) {
    return "";
  }

  @Override
  @Nullable
  public Node getSubColNode(int subcolumn,
      TreeTableCell<ModularFeatureListRow, ? extends DataType> cell,
      TreeTableColumn<ModularFeatureListRow, ? extends DataType> coll, DataType<?> cellData,
      RawDataFile raw) {

    switch (subcolumn) {
      case 0:
        return getBarChart(cell, coll);
      case 1:
        return getAreaShareChart(cell, coll);
    }
    return null;
  }

}
