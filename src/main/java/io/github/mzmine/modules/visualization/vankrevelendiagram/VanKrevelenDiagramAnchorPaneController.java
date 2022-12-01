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

package io.github.mzmine.modules.visualization.vankrevelendiagram;

import java.awt.Color;
import java.util.logging.Logger;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import io.github.mzmine.gui.chartbasics.chartutils.XYBlockPixelSizeRenderer;
import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;

public class VanKrevelenDiagramAnchorPaneController {

  private final Logger logger = Logger.getLogger(this.getClass().getName());

  @FXML
  private BorderPane plotPane;

  @FXML
  private Button blockSizeButton;

  @FXML
  private Button backgroundButton;

  @FXML
  private Button gridButton;

  @FXML
  private Button annotationButton;

  // Plot style actions
  @FXML
  void toggleAnnotation(ActionEvent event) {
    logger.finest("Toggle annotations");
    XYPlot plot = getChart().getXYPlot();
    XYBlockPixelSizeRenderer renderer = (XYBlockPixelSizeRenderer) plot.getRenderer();
    Boolean itemNameVisible = renderer.getDefaultItemLabelsVisible();
    if (itemNameVisible == false) {
      renderer.setDefaultItemLabelsVisible(true);
    } else {
      renderer.setDefaultItemLabelsVisible(false);
    }
    if (plot.getBackgroundPaint() == Color.BLACK) {
      renderer.setDefaultItemLabelPaint(Color.WHITE);
    } else {
      renderer.setDefaultItemLabelPaint(Color.BLACK);
    }
  }

  @FXML
  void toggleBackColor(ActionEvent event) {
    logger.finest("Toggle background");
    XYPlot plot = getChart().getXYPlot();
    if (plot.getBackgroundPaint() == Color.WHITE) {
      plot.setBackgroundPaint(Color.BLACK);
    } else {
      plot.setBackgroundPaint(Color.WHITE);
    }
  }

  @FXML
  void toggleBolckSize(ActionEvent event) {
    logger.finest("Toggle block size");
    XYPlot plot = getChart().getXYPlot();
    XYBlockPixelSizeRenderer renderer = (XYBlockPixelSizeRenderer) plot.getRenderer();
    int height = (int) renderer.getBlockHeightPixel();

    if (height == 1) {
      height++;
    } else if (height == 5) {
      height = 1;
    } else if (height < 5 && height != 1) {
      height++;
    }
    renderer.setBlockHeightPixel(height);
    renderer.setBlockWidthPixel(height);
  }

  @FXML
  void toggleGrid(ActionEvent event) {
    logger.finest("Toggle grid");
    XYPlot plot = getChart().getXYPlot();
    if (plot.getDomainGridlinePaint() == Color.BLACK) {
      plot.setDomainGridlinePaint(Color.WHITE);
      plot.setRangeGridlinePaint(Color.WHITE);
    } else {
      plot.setDomainGridlinePaint(Color.BLACK);
      plot.setRangeGridlinePaint(Color.BLACK);
    }
  }

  private JFreeChart getChart() {
    if (plotPane.getChildren().get(0) instanceof EChartViewer) {
      EChartViewer viewer = (EChartViewer) plotPane.getChildren().get(0);
      return viewer.getChart();
    }
    return null;
  }

  public BorderPane getPlotPane() {
    return plotPane;
  }

  public void setPlotPane(BorderPane plotPane) {
    this.plotPane = plotPane;
  }

}
