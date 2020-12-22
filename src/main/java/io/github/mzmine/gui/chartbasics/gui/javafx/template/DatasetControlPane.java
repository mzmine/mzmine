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

import io.github.mzmine.gui.chartbasics.chartthemes.EStandardChartTheme;
import io.github.mzmine.gui.chartbasics.gui.javafx.template.providers.PlotDatasetProvider;
import io.github.mzmine.util.components.ButtonCell;
import io.github.mzmine.util.components.ColorTableCell;
import java.util.Map;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableColumn.CellEditEvent;
import javafx.scene.control.TableView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.util.Callback;
import org.controlsfx.glyphfont.Glyph;
import org.jfree.data.xy.XYDataset;

public class DatasetControlPane<T extends PlotDatasetProvider> extends AnchorPane {

  private final TableView<XYDataset> tvOverview;
  private final TableColumn<XYDataset, Boolean> colShow;
  private final TableColumn<XYDataset, String> colDatasetType;
  private final TableColumn<XYDataset, String> colDatasetName;
  private final TableColumn<XYDataset, Color> colColor;
  private final SimpleXYLineChart<T> chart;

  public DatasetControlPane(final SimpleXYLineChart<T> chart) {
    this.chart = chart;
    tvOverview = new TableView<>();
    tvOverview.setEditable(true);

    colShow = new TableColumn<>();
    colDatasetType = new TableColumn<>("Dataset type");
    colDatasetType.setEditable(false);
    colDatasetName = new TableColumn<>("Dataset name");
    colDatasetName.setEditable(false);
    colColor = new TableColumn<>("Color");
    colColor.setEditable(true);
    tvOverview.getColumns().addAll(colShow, colDatasetType, colDatasetName, colColor);
    tvOverview.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    setTopAnchor(tvOverview, 0d);
    setBottomAnchor(tvOverview, 0d);
    setLeftAnchor(tvOverview, 0d);
    setRightAnchor(tvOverview, 0d);
    tvOverview.setMinSize(300, 50);
    tvOverview.setPrefSize(300, 50);
//    tvOverview.setMaxSize(-1, -1);

    this.getChildren().add(tvOverview);
    chart.addDatasetsChangedListener(this::onDatasetChanged);
    initialize();
  }

  public void initialize() {

    colColor.setCellFactory(ColorTableCell::new);
    colColor.setCellValueFactory(
        new Callback<CellDataFeatures<XYDataset, Color>, ObservableValue<Color>>() {
          @Override
          public ObservableValue<Color> call(CellDataFeatures<XYDataset, Color> param) {
            if (param.getValue() instanceof ColoredXYDataset) {
              return ((ColoredXYDataset) param.getValue()).fxColorProperty();
            }
            return null;
          }
        });
    colColor.setOnEditCommit(new EventHandler<CellEditEvent<XYDataset, Color>>() {
      @Override
      public void handle(CellEditEvent<XYDataset, Color> event) {
        ((ColoredXYDataset)event.getRowValue()).setFxColor(event.getNewValue());
        chart.getChart().fireChartChanged();
      }
    });

    colShow.setCellFactory(column -> new ButtonCell<>(column, new Glyph("FontAwesome", "EYE"),
        new Glyph("FontAwesome", "EYE_SLASH")));
    colDatasetName.setCellValueFactory(
        new Callback<CellDataFeatures<XYDataset, String>, ObservableValue<String>>() {
          @Override
          public ObservableValue<String> call(CellDataFeatures<XYDataset, String> param) {
            return new SimpleStringProperty(
                String.valueOf((param.getValue()).getSeriesKey(1)));
          }
        });
    colDatasetType.setCellValueFactory(
        new Callback<CellDataFeatures<XYDataset, String>, ObservableValue<String>>() {
          @Override
          public ObservableValue<String> call(CellDataFeatures<XYDataset, String> param) {
            if (param.getValue() instanceof ColoredXYDataset) {
              String name = ((ColoredXYDataset) param.getValue()).getValueProvider().getClass()
                  .getName();
              return new SimpleStringProperty(name.substring(name.lastIndexOf(".") + 1));
            } else {
              return new SimpleStringProperty(param.getValue().getClass().getTypeName());
            }
          }
        });
  }

  public void onDatasetChanged(Map<Integer, XYDataset> datasets) {
    if (chart == null || datasets == null) {
      return;
    }

    tvOverview.getItems().clear();
    tvOverview.getItems().addAll(datasets.values());
    EStandardChartTheme.fixLegend(chart.getChart());
  }

}