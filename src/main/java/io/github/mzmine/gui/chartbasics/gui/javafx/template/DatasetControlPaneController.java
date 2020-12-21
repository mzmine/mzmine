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

package io.github.mzmine.gui.chartbasics.gui.javafx.template;

import io.github.mzmine.util.components.ButtonCell;
import io.github.mzmine.util.components.ColorTableCell;
import java.util.Map;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.paint.Color;
import javafx.util.Callback;
import org.controlsfx.glyphfont.Glyph;
import org.jfree.data.xy.XYDataset;

public class DatasetControlPaneController {

  @FXML
  TableColumn<XYDataset, Boolean> colShow;

  @FXML
  TableColumn<XYDataset, String> colDatasetType;

  @FXML
  TableColumn<XYDataset, String> colDatasetName;

  @FXML
  TableColumn<XYDataset, Color> colColor;

  @FXML
  TableView<XYDataset> tvOverview;

  private SimpleXYLineChart<?> chart;

  public void initialize() {

    colColor.setCellFactory(ColorTableCell::new);
    colShow.setCellFactory(column -> new ButtonCell<>(column, new Glyph("FontAwesome", "EYE"),
        new Glyph("FontAwesome", "EYE_SLASH")));
    colDatasetName.setCellValueFactory(new PropertyValueFactory<>("getSeriesKey"));
    colDatasetType.setCellValueFactory(
        new Callback<CellDataFeatures<XYDataset, String>, ObservableValue<String>>() {
          @Override
          public ObservableValue<String> call(CellDataFeatures<XYDataset, String> param) {
            if(param.getValue() instanceof ColoredXYDataset) {
              return new SimpleStringProperty(
                  ((ColoredXYDataset) param.getValue()).getRangeValueProvider().getClass().getTypeName());
            } else {
              return new SimpleStringProperty(param.getValue().getClass().getTypeName());
            }
          }
        });
  }

  public void setChart(SimpleXYLineChart chart) {
    this.chart = chart;
    onDatasetChanged(chart.getAllDatasets());
  }

  public void onDatasetChanged(Map<Integer, XYDataset> datasets) {
    if(chart == null || datasets == null) {
      return;
    }

    tvOverview.getItems().clear();
    tvOverview.getItems().addAll(datasets.values());
  }

}