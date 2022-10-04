/*
 * Copyright (c) 2004-2022 The MZmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
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
