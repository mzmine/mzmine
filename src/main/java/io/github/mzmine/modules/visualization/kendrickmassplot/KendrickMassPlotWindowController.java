/*
 * Copyright 2006-2020 The MZmine Development Team
 * 
 * This file is part of MZmine 3.
 * 
 * MZmine 3 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 3 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 3; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.visualization.kendrickmassplot;

import java.awt.Color;
import java.util.logging.Logger;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import io.github.mzmine.gui.chartbasics.chartutils.XYBlockPixelSizeRenderer;
import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;

public class KendrickMassPlotWindowController {

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

  @FXML
  private Button shiftUpYAxis;

  @FXML
  private Button chargeUpYAxis;

  @FXML
  private Button divisorUpYAxis;

  @FXML
  private Button shiftDownYAxis;

  @FXML
  private Button chargeDownYAxis;

  @FXML
  private Button divisorDownYAxis;

  @FXML
  private Label shiftLabelYAxis;

  @FXML
  private Label chargeLabelYAxis;

  @FXML
  private Label divisorLabelYAxis;

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

  @FXML
  void shiftUpYAxis(ActionEvent e) {

  }

  public BorderPane getPlotPane() {
    return plotPane;
  }

  public void setPlotPane(BorderPane plotPane) {
    this.plotPane = plotPane;
  }

  public Button getBlockSizeButton() {
    return blockSizeButton;
  }

  public void setBlockSizeButton(Button blockSizeButton) {
    this.blockSizeButton = blockSizeButton;
  }

  public Button getBackgroundButton() {
    return backgroundButton;
  }

  public void setBackgroundButton(Button backgroundButton) {
    this.backgroundButton = backgroundButton;
  }

  public Button getGridButton() {
    return gridButton;
  }

  public void setGridButton(Button gridButton) {
    this.gridButton = gridButton;
  }

  public Button getAnnotationButton() {
    return annotationButton;
  }

  public void setAnnotationButton(Button annotationButton) {
    this.annotationButton = annotationButton;
  }

  public Button getShiftUpYAxis() {
    return shiftUpYAxis;
  }

  public void setShiftUpYAxis(Button shiftUpYAxis) {
    this.shiftUpYAxis = shiftUpYAxis;
  }

  public Button getChargeUpYAxis() {
    return chargeUpYAxis;
  }

  public void setChargeUpYAxis(Button chargeUpYAxis) {
    this.chargeUpYAxis = chargeUpYAxis;
  }

  public Button getDivisorUpYAxis() {
    return divisorUpYAxis;
  }

  public void setDivisorUpYAxis(Button divisorUpYAxis) {
    this.divisorUpYAxis = divisorUpYAxis;
  }

  public Button getShiftDownYAxis() {
    return shiftDownYAxis;
  }

  public void setShiftDownYAxis(Button shiftDownYAxis) {
    this.shiftDownYAxis = shiftDownYAxis;
  }

  public Button getChargeDownYAxis() {
    return chargeDownYAxis;
  }

  public void setChargeDownYAxis(Button chargeDownYAxis) {
    this.chargeDownYAxis = chargeDownYAxis;
  }

  public Button getDivisorDownYAxis() {
    return divisorDownYAxis;
  }

  public void setDivisorDownYAxis(Button divisorDownYAxis) {
    this.divisorDownYAxis = divisorDownYAxis;
  }

  public Label getShiftLabelYAxis() {
    return shiftLabelYAxis;
  }

  public void setShiftLabelYAxis(Label shiftLabelYAxis) {
    this.shiftLabelYAxis = shiftLabelYAxis;
  }

  public Label getChargeLabelYAxis() {
    return chargeLabelYAxis;
  }

  public void setChargeLabelYAxis(Label chargeLabelYAxis) {
    this.chargeLabelYAxis = chargeLabelYAxis;
  }

  public Label getDivisorLabelYAxis() {
    return divisorLabelYAxis;
  }

  public void setDivisorLabelYAxis(Label divisorLabelYAxis) {
    this.divisorLabelYAxis = divisorLabelYAxis;
  }

  private JFreeChart getChart() {
    if (plotPane.getChildren().get(0) instanceof EChartViewer) {
      EChartViewer viewer = (EChartViewer) plotPane.getChildren().get(0);
      return viewer.getChart();
    }
    return null;
  }

}
