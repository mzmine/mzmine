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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.data.DataTypeMap;
import io.github.mzmine.datamodel.data.ModularFeature;
import io.github.mzmine.datamodel.data.ModularFeatureListRow;
import io.github.mzmine.datamodel.data.types.modifiers.SubColumnsFactory;
import io.github.mzmine.util.color.ColorsFX;
import io.github.mzmine.util.color.Vision;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableMap;
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
public class FeaturesType extends DataType<ObservableMap<RawDataFile, ModularFeature>>
    implements SubColumnsFactory {

  public FeaturesType(Map<RawDataFile, ModularFeature> map) {
    super(FXCollections.unmodifiableObservableMap(FXCollections.observableMap(map)));
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
    cols.add(createBarChartColl());
    cols.add(createAreaShareColl());

    // create all sample columns
    for (Entry<RawDataFile, ModularFeature> entry : value.entrySet()) {
      RawDataFile raw = entry.getKey();
      // create column per name
      TreeTableColumn<ModularFeatureListRow, String> sampleCol =
          new TreeTableColumn<>(raw.getName());
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
  public Node getBarChart(TreeTableCell<ModularFeatureListRow, DataType<?>> cell,
      TreeTableColumn<ModularFeatureListRow, ? extends DataType> coll) {
    ModularFeatureListRow row = cell.getTreeTableRow().getItem();
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
   * Create a bar chart column for area
   * 
   * @return
   */
  private TreeTableColumn<ModularFeatureListRow, ? extends DataType> createBarChartColl() {
    // create column
    TreeTableColumn<ModularFeatureListRow, ? extends DataType> col =
        new TreeTableColumn<>("Area Bars");

    // define observable
    col.setCellValueFactory(r -> {
      final DataTypeMap map = r.getValue().getValue().getMap();

      Optional<? extends DataType> o = map.get(this.getClass());
      final SimpleObjectProperty<DataType<?>> property = new SimpleObjectProperty<>(o.orElse(null));
      // listen for changes in this rows DataTypeMap
      map.getObservableMap().addListener((
          MapChangeListener.Change<? extends Class<? extends DataType>, ? extends DataType> change) -> {
        if (this.getClass().equals(change.getKey())) {
          property.set(map.get(this.getClass()).orElse(null));
        }
      });
      return property;
    });

    // value representation
    col.setCellFactory(param -> new TreeTableCell<ModularFeatureListRow, DataType<?>>() {
      @Override
      protected void updateItem(DataType<?> item, boolean empty) {
        super.updateItem(item, empty);
        if (item == null || empty) {
          setGraphic(null);
          setText(null);
        } else {
          setText(null);
          setGraphic(getBarChart(this, param));
        }
        setAlignment(Pos.CENTER);
      }
    });
    return col;
  }


  /**
   * Create a area share
   * 
   * @return
   */
  private TreeTableColumn<ModularFeatureListRow, ? extends DataType> createAreaShareColl() {
    // create column
    TreeTableColumn<ModularFeatureListRow, ? extends DataType> col =
        new TreeTableColumn<>("Area Share");

    // define observable
    col.setCellValueFactory(r -> {
      final DataTypeMap map = r.getValue().getValue().getMap();

      Optional<? extends DataType> o = map.get(this.getClass());
      final SimpleObjectProperty<DataType<?>> property = new SimpleObjectProperty<>(o.orElse(null));
      // listen for changes in this rows DataTypeMap
      map.getObservableMap().addListener((
          MapChangeListener.Change<? extends Class<? extends DataType>, ? extends DataType> change) -> {
        if (this.getClass().equals(change.getKey())) {
          property.set(map.get(this.getClass()).orElse(null));
        }
      });
      return property;
    });

    // value representation
    col.setCellFactory(param -> new TreeTableCell<ModularFeatureListRow, DataType<?>>() {
      @Override
      protected void updateItem(DataType<?> item, boolean empty) {
        super.updateItem(item, empty);
        if (item == null || empty) {
          setGraphic(null);
          setText(null);
        } else {
          setText(null);
          setGraphic(getAreaShareChart(this, param));
        }
        setAlignment(Pos.CENTER);
      }
    });
    return col;
  }


  /**
   * Create bar chart of data
   * 
   * @param cell
   * @param coll
   * @return
   */
  public Node getAreaShareChart(TreeTableCell<ModularFeatureListRow, DataType<?>> cell,
      TreeTableColumn<ModularFeatureListRow, ? extends DataType> coll) {
    ModularFeatureListRow row = cell.getTreeTableRow().getItem();

    List<Float> area = row.getFeatures().entrySet().stream().map(Entry::getValue)
        .map(e -> e.get(AreaType.class).map(DataType::getValue).orElse(0f))
        .collect(Collectors.toList());
    Float sum = area.stream().reduce(0f, Float::sum);


    Color[] colors = ColorsFX.getSevenColorPalette(Vision.DEUTERANOPIA, false);


    Rectangle[] all = new Rectangle[area.size()];
    for (int i = 0; i < area.size(); i++) {
      float ratio = area.get(i) / sum;
      Rectangle rect = new Rectangle();
      rect.setFill(colors[i % colors.length]);
      // bind width
      rect.widthProperty().bind(cell.widthProperty().multiply(ratio));
      rect.setHeight(i % 2 == 0 ? 20 : 25);
      all[i] = rect;
    }
    HBox box = new HBox(0, all);
    box.setPrefWidth(100);
    box.setAlignment(Pos.CENTER_LEFT);

    return box;
  }

}
