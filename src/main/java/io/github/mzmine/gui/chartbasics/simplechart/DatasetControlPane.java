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

package io.github.mzmine.gui.chartbasics.simplechart;

import io.github.mzmine.gui.chartbasics.chartthemes.EStandardChartTheme;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYDataset;
import io.github.mzmine.gui.chartbasics.simplechart.providers.ColorPropertyProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYDataProvider;
import io.github.mzmine.util.components.ButtonCell;
import io.github.mzmine.util.components.ColorPickerTableCell;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import org.controlsfx.glyphfont.Glyph;
import org.jfree.data.general.DatasetChangeEvent;
import org.jfree.data.xy.XYDataset;

/**
 * Can be bound to a {@link SimpleXYChart} to control the color and visibility of datasets.
 *
 * @param <T>
 */
public class DatasetControlPane<T extends PlotXYDataProvider> extends AnchorPane {

  private final TableView<XYDataset> tvOverview;
  private final TableColumn<XYDataset, Boolean> colShow;
  private final TableColumn<XYDataset, String> colDatasetType;
  private final TableColumn<XYDataset, String> colDatasetName;
  private final TableColumn<XYDataset, Color> colColor;
  private final SimpleXYChart<T> chart;

  public DatasetControlPane(final SimpleXYChart<T> chart) {
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
    chart.addDatasetChangeListener(this::datasetChanged);
    initialize();
  }

  public void initialize() {

    colColor.setCellFactory(ColorPickerTableCell::new);
    colColor.setCellValueFactory(param -> {
      if (param.getValue() instanceof ColorPropertyProvider) {
        return ((ColoredXYDataset) param.getValue()).fxColorProperty();
      }
      return null;
    });
    colColor.setOnEditCommit(event -> {
      ((ColorPropertyProvider) event.getRowValue()).fxColorProperty().set(event.getNewValue());
      chart.getChart().fireChartChanged();
    });

    colShow.setCellFactory(column -> new ButtonCell<>(column, new Glyph("FontAwesome", "EYE"),
        new Glyph("FontAwesome", "EYE_SLASH")));
    colDatasetName.setCellValueFactory(
        param -> new SimpleStringProperty(
            String.valueOf((param.getValue()).getSeriesKey(1))));
    colDatasetType.setCellValueFactory(
        param -> {
          if (param.getValue() instanceof ColoredXYDataset) {
            String name = ((ColoredXYDataset) param.getValue()).getValueProvider().getClass()
                .getName();
            return new SimpleStringProperty(name.substring(name.lastIndexOf(".") + 1));
          } else {
            return new SimpleStringProperty(param.getValue().getClass().getTypeName());
          }
        });
  }

  public void datasetChanged(DatasetChangeEvent event) {
    if (chart == null || event == null) {
      return;
    }

    tvOverview.getItems().clear();
    tvOverview.getItems().addAll(chart.getAllDatasets().values());
    EStandardChartTheme.fixLegend(chart.getChart());
  }

}
