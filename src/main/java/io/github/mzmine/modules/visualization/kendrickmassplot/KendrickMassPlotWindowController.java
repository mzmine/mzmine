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

package io.github.mzmine.modules.visualization.kendrickmassplot;

import java.awt.Color;
import java.text.DecimalFormat;
import java.util.logging.Logger;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.title.PaintScaleLegend;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.gui.chartbasics.chartutils.XYCirclePixelSizeRenderer;
import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.FormulaUtils;
import io.github.mzmine.util.javafx.FxIconUtil;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;

public class KendrickMassPlotWindowController {

  private final Logger logger = Logger.getLogger(this.getClass().getName());

  private FeatureListRow[] selectedRows;
  private FeatureList featureList;
  private String xAxisKMBase;
  private String zAxisKMBase;
  private String customYAxisKMBase;
  private String customXAxisKMBase;
  private String customZAxisKMBase;
  private boolean useCustomXAxisKMBase;
  private boolean useCustomZAxisKMBase;
  private boolean useRKM_X;
  private boolean useRKM_Y;
  private boolean useRKM_Z;
  private double xAxisShift;
  private double yAxisShift;
  private double zAxisShift;
  private int yAxisCharge;
  private int xAxisCharge;
  private int zAxisCharge;
  private int yAxisDivisor;
  private int xAxisDivisor;
  private int zAxisDivisor;

  private static final Image iconKMD = FxIconUtil.loadImageFromResources("icons/KMDIcon.png");

  private static final Image iconRKM = FxIconUtil.loadImageFromResources("icons/RKMIcon.png");

  DecimalFormat shiftFormat = new DecimalFormat("0.##");

  @FXML
  private BorderPane plotPane;

  @FXML
  private Button backgroundButton;

  @FXML
  private Button gridButton;

  @FXML
  private Button annotationButton;

  @FXML
  private Tooltip tooltipYAxisLabel;

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

  @FXML
  private Button toggleKMDRKMY;

  @FXML
  private ImageView imageViewKMDRKMY;

  @FXML
  private GridPane gridPaneXAxis;

  @FXML
  private Tooltip tooltipXAxisLabel;

  @FXML
  private Button shiftUpXAxis;

  @FXML
  private Button chargeUpXAxis;

  @FXML
  private Button divisorUpXAxis;

  @FXML
  private Button shiftDownXAxis;

  @FXML
  private Button chargeDownXAxis;

  @FXML
  private Button divisorDownXAxis;

  @FXML
  private Label shiftLabelXAxis;

  @FXML
  private Label chargeLabelXAxis;

  @FXML
  private Label divisorLabelXAxis;

  @FXML
  private Button toggleKMDRKMX;

  @FXML
  private ImageView imageViewKMDRKMX;

  @FXML
  private GridPane gridPaneZAxis;

  @FXML
  private Tooltip tooltipZAxisLabel;

  @FXML
  private Button shiftUpZAxis;

  @FXML
  private Button chargeUpZAxis;

  @FXML
  private Button divisorUpZAxis;

  @FXML
  private Button shiftDownZAxis;

  @FXML
  private Button chargeDownZAxis;

  @FXML
  private Button divisorDownZAxis;

  @FXML
  private Label shiftLabelZAxis;

  @FXML
  private Label chargeLabelZAxis;

  @FXML
  private Label divisorLabelZAxis;

  @FXML
  private Button toggleKMDRKMZ;

  @FXML
  private ImageView imageViewKMDRKMZ;

  public void initialize(ParameterSet parameters) {

    this.featureList = parameters.getParameter(KendrickMassPlotParameters.featureList).getValue()
        .getMatchingFeatureLists()[0];

    this.selectedRows = parameters.getParameter(KendrickMassPlotParameters.selectedRows)
        .getMatchingRows(featureList);

    this.selectedRows = parameters.getParameter(KendrickMassPlotParameters.selectedRows)
        .getMatchingRows(featureList);

    this.customYAxisKMBase =
        parameters.getParameter(KendrickMassPlotParameters.yAxisCustomKendrickMassBase).getValue();

    this.useCustomXAxisKMBase =
        parameters.getParameter(KendrickMassPlotParameters.xAxisCustomKendrickMassBase).getValue();

    if (useCustomXAxisKMBase == true) {
      this.customXAxisKMBase =
          parameters.getParameter(KendrickMassPlotParameters.xAxisCustomKendrickMassBase)
              .getEmbeddedParameter().getValue();
      gridPaneXAxis.setDisable(false);
    } else {
      this.xAxisKMBase = parameters.getParameter(KendrickMassPlotParameters.xAxisValues).getValue();
    }

    this.useCustomZAxisKMBase =
        parameters.getParameter(KendrickMassPlotParameters.zAxisCustomKendrickMassBase).getValue();

    if (useCustomZAxisKMBase == true) {
      this.customZAxisKMBase =
          parameters.getParameter(KendrickMassPlotParameters.zAxisCustomKendrickMassBase)
              .getEmbeddedParameter().getValue();
      gridPaneZAxis.setDisable(false);
    } else {
      this.zAxisKMBase = parameters.getParameter(KendrickMassPlotParameters.zAxisValues).getValue();
    }

    this.yAxisCharge = 1;
    this.xAxisCharge = 1;
    this.zAxisCharge = 1;
    if (customYAxisKMBase != null)
      this.yAxisDivisor = getDivisorKM(customYAxisKMBase);
    else
      this.yAxisDivisor = 1;
    if (customXAxisKMBase != null)
      this.xAxisDivisor = getDivisorKM(customXAxisKMBase);
    else
      this.xAxisDivisor = 1;
    if (customZAxisKMBase != null)
      this.zAxisDivisor = getDivisorKM(customZAxisKMBase);
    else
      this.zAxisDivisor = 1;
    this.xAxisShift = 0;
    this.yAxisShift = 0;
    this.zAxisShift = 0;
    this.useRKM_X = false;
    this.useRKM_Y = false;
    this.useRKM_Z = false;
  }

  // Plot style actions
  @FXML
  void toggleAnnotation(ActionEvent event) {
    logger.finest("Toggle annotations");
    XYPlot plot = getChart().getXYPlot();
    XYCirclePixelSizeRenderer renderer = (XYCirclePixelSizeRenderer) plot.getRenderer();
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
  void chargeDownY(ActionEvent event) {
    if (yAxisCharge > 1) {
      yAxisCharge = yAxisCharge - 1;
    } else
      yAxisCharge = 1;
    XYPlot plot = getChart().getXYPlot();
    kendrickVariableChanged(plot);
  }

  @FXML
  void chargeDownX(ActionEvent event) {
    if (xAxisCharge > 1) {
      xAxisCharge = xAxisCharge - 1;
    } else
      xAxisCharge = 1;
    XYPlot plot = getChart().getXYPlot();
    kendrickVariableChanged(plot);
  }

  @FXML
  void chargeDownZ(ActionEvent event) {
    if (zAxisCharge > 1) {
      zAxisCharge = zAxisCharge - 1;
    } else
      zAxisCharge = 1;
    XYPlot plot = getChart().getXYPlot();
    kendrickVariableChanged(plot);
  }

  @FXML
  void chargeUpY(ActionEvent event) {
    logger.finest("Charge Y-axis up");
    yAxisCharge = yAxisCharge + 1;
    XYPlot plot = getChart().getXYPlot();
    kendrickVariableChanged(plot);
  }

  @FXML
  void chargeUpX(ActionEvent event) {
    logger.finest("Charge X-axis up");
    xAxisCharge = xAxisCharge + 1;
    XYPlot plot = getChart().getXYPlot();
    kendrickVariableChanged(plot);
  }

  @FXML
  void chargeUpZ(ActionEvent event) {
    logger.finest("Charge Z-axis up");
    zAxisCharge = zAxisCharge + 1;
    XYPlot plot = getChart().getXYPlot();
    kendrickVariableChanged(plot);
  }

  @FXML
  void divisorDownY(ActionEvent event) {
    logger.finest("Divisor Y-axis down");
    int minDivisor = getMinimumRecommendedDivisor(customYAxisKMBase);
    int maxDivisor = getMaximumRecommendedDivisor(customYAxisKMBase);
    if (yAxisDivisor > minDivisor && yAxisDivisor <= maxDivisor) {
      yAxisDivisor--;
      yAxisDivisor = checkDivisor(yAxisDivisor, useRKM_Y, customYAxisKMBase, false);
    }
    XYPlot plot = getChart().getXYPlot();
    kendrickVariableChanged(plot);
  }

  @FXML
  void divisorDownX(ActionEvent event) {
    logger.finest("Divisor X-axis down");
    int minDivisor = getMinimumRecommendedDivisor(customXAxisKMBase);
    int maxDivisor = getMaximumRecommendedDivisor(customXAxisKMBase);
    if (xAxisDivisor > minDivisor && xAxisDivisor <= maxDivisor) {
      xAxisDivisor--;
      xAxisDivisor = checkDivisor(xAxisDivisor, useRKM_X, customXAxisKMBase, false);
    }
    XYPlot plot = getChart().getXYPlot();
    kendrickVariableChanged(plot);
  }

  @FXML
  void divisorDownZ(ActionEvent event) {
    logger.finest("Divisor Z-axis down");
    int minDivisor = getMinimumRecommendedDivisor(customZAxisKMBase);
    int maxDivisor = getMaximumRecommendedDivisor(customZAxisKMBase);
    if (zAxisDivisor > minDivisor && zAxisDivisor <= maxDivisor) {
      zAxisDivisor--;
      zAxisDivisor = checkDivisor(zAxisDivisor, useRKM_Z, customZAxisKMBase, false);
    }
    XYPlot plot = getChart().getXYPlot();
    kendrickVariableChanged(plot);
  }

  @FXML
  void divisorUpY(ActionEvent event) {
    logger.finest("Divisor Y-axis up");
    int minDivisor = getMinimumRecommendedDivisor(customYAxisKMBase);
    int maxDivisor = getMaximumRecommendedDivisor(customYAxisKMBase);
    if (yAxisDivisor >= minDivisor && yAxisDivisor < maxDivisor) {
      yAxisDivisor++;
      yAxisDivisor = checkDivisor(yAxisDivisor, useRKM_Y, customYAxisKMBase, true);
    }
    XYPlot plot = getChart().getXYPlot();
    kendrickVariableChanged(plot);
  }

  @FXML
  void divisorUpX(ActionEvent event) {
    logger.finest("Divisor X-axis up");
    int minDivisor = getMinimumRecommendedDivisor(customXAxisKMBase);
    int maxDivisor = getMaximumRecommendedDivisor(customXAxisKMBase);
    if (xAxisDivisor >= minDivisor && xAxisDivisor < maxDivisor) {
      xAxisDivisor++;
      xAxisDivisor = checkDivisor(xAxisDivisor, useRKM_X, customXAxisKMBase, true);
    }
    XYPlot plot = getChart().getXYPlot();
    kendrickVariableChanged(plot);
  }

  @FXML
  void divisorUpZ(ActionEvent event) {
    logger.finest("Divisor Z-axis up");
    int minDivisor = getMinimumRecommendedDivisor(customZAxisKMBase);
    int maxDivisor = getMaximumRecommendedDivisor(customZAxisKMBase);
    if (zAxisDivisor >= minDivisor && zAxisDivisor < maxDivisor) {
      zAxisDivisor++;
      zAxisDivisor = checkDivisor(zAxisDivisor, useRKM_Z, customZAxisKMBase, true);
    }
    XYPlot plot = getChart().getXYPlot();
    kendrickVariableChanged(plot);
  }

  @FXML
  void shiftDownY(ActionEvent event) {
    logger.finest("Shift Y-axis down");
    Double shiftValue = -0.01;
    yAxisShift = yAxisShift + shiftValue;
    XYPlot plot = getChart().getXYPlot();
    kendrickVariableChanged(plot);
  }

  @FXML
  void shiftDownX(ActionEvent event) {
    logger.finest("Shift X-axis down");
    Double shiftValue = -0.01;
    xAxisShift = xAxisShift + shiftValue;
    XYPlot plot = getChart().getXYPlot();
    kendrickVariableChanged(plot);
  }

  @FXML
  void shiftDownZ(ActionEvent event) {
    logger.finest("Shift Z-axis down");
    Double shiftValue = -0.01;
    zAxisShift = zAxisShift + shiftValue;
    XYPlot plot = getChart().getXYPlot();
    kendrickVariableChanged(plot);
  }

  @FXML
  void shiftUpYAxis(ActionEvent event) {
    logger.finest("Shift Y-axis up");
    Double shiftValue = 0.01;
    yAxisShift = yAxisShift + shiftValue;
    XYPlot plot = getChart().getXYPlot();
    kendrickVariableChanged(plot);
  }

  @FXML
  void shiftUpXAxis(ActionEvent event) {
    logger.finest("Shift X-axis up");
    Double shiftValue = 0.01;
    xAxisShift = xAxisShift + shiftValue;
    XYPlot plot = getChart().getXYPlot();
    kendrickVariableChanged(plot);
  }

  @FXML
  void shiftUpZAxis(ActionEvent event) {
    logger.finest("Shift Z-axis up");
    Double shiftValue = 0.01;
    zAxisShift = zAxisShift + shiftValue;
    XYPlot plot = getChart().getXYPlot();
    kendrickVariableChanged(plot);
  }

  @FXML
  void toggleKMDRKMY(ActionEvent event) {
    logger.finest("Toggle KMD and RKM Y-Axis");
    XYPlot plot = getChart().getXYPlot();
    if (useRKM_Y) {
      useRKM_Y = false;
      plot.getRangeAxis().setLabel("KMD(" + customYAxisKMBase + ")");
      imageViewKMDRKMY.setImage(iconKMD);
    } else {
      useRKM_Y = true;

      // if the divisor is round(R) switch to round(R)-1 for RKM plot
      yAxisDivisor = checkDivisor(yAxisDivisor, useRKM_Y, customYAxisKMBase, false);
      plot.getRangeAxis().setLabel("RKM(" + customYAxisKMBase + ")");
      imageViewKMDRKMY.setImage(iconRKM);
    }
    kendrickVariableChanged(plot);
  }

  @FXML
  void toggleKMDRKMX(ActionEvent event) {
    logger.finest("Toggle KMD and RKM X-Axis");
    XYPlot plot = getChart().getXYPlot();
    if (useRKM_X) {
      useRKM_X = false;
      plot.getDomainAxis().setLabel("KMD(" + customXAxisKMBase + ")");
      imageViewKMDRKMX.setImage(iconKMD);
    } else {
      useRKM_X = true;

      // if the divisor is round(R) switch to round(R)-1 for RKM plot
      xAxisDivisor = checkDivisor(xAxisDivisor, useRKM_X, customXAxisKMBase, false);
      plot.getDomainAxis().setLabel("RKM(" + customXAxisKMBase + ")");
      imageViewKMDRKMX.setImage(iconRKM);
    }
    kendrickVariableChanged(plot);
  }

  @FXML
  void toggleKMDRKMZ(ActionEvent event) {
    logger.finest("Toggle KMD and RKM Z-Axis");
    XYPlot plot = getChart().getXYPlot();
    if (plot.getDataset() instanceof KendrickMassPlotXYZDataset) {
      if (useRKM_Z) {
        useRKM_Z = false;
        PaintScaleLegend legend = (PaintScaleLegend) getChart().getSubtitle(1);
        legend.getAxis().setLabel("KMD(" + customZAxisKMBase + ")");
      } else {
        useRKM_Z = true;

        // if the divisor is round(R) switch to round(R)-1 for RKM plot
        zAxisDivisor = checkDivisor(zAxisDivisor, useRKM_Z, customZAxisKMBase, false);
        PaintScaleLegend legend = (PaintScaleLegend) getChart().getSubtitle(1);
        legend.getAxis().setLabel("RKM(" + customZAxisKMBase + ")");
      }
      kendrickVariableChanged(plot);
    }
  }

  public BorderPane getPlotPane() {
    return plotPane;
  }

  public void setPlotPane(BorderPane plotPane) {
    this.plotPane = plotPane;
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

  /*
   * Method to calculate the data sets for a Kendrick mass plot
   */
  private void kendrickVariableChanged(XYPlot plot) {

    if (plot.getDataset() instanceof KendrickMassPlotXYDataset) {
      KendrickMassPlotXYDataset dataset = (KendrickMassPlotXYDataset) plot.getDataset();
      double[] xValues = new double[dataset.getItemCount(0)];

      // Calc xValues
      xValues = new double[selectedRows.length];
      if (useCustomXAxisKMBase == true) {
        if (useRKM_X == false) {
          for (int i = 0; i < selectedRows.length; i++) {
            double unshiftedValue = Math
                .ceil(xAxisCharge * selectedRows[i].getAverageMZ()
                    * getKendrickMassFactor(customXAxisKMBase, xAxisDivisor))
                - xAxisCharge * selectedRows[i].getAverageMZ()
                    * getKendrickMassFactor(customXAxisKMBase, xAxisDivisor);
            xValues[i] = unshiftedValue + xAxisShift - Math.floor(unshiftedValue + xAxisShift);
          }
        } else {
          for (int i = 0; i < selectedRows.length; i++) {
            double unshiftedValue = (xAxisCharge
                * (xAxisDivisor - Math.round(FormulaUtils.calculateExactMass(customXAxisKMBase)))
                * selectedRows[i].getAverageMZ())
                / FormulaUtils.calculateExactMass(customXAxisKMBase)//
                - Math.floor((xAxisCharge
                    * (xAxisDivisor
                        - Math.round(FormulaUtils.calculateExactMass(customXAxisKMBase)))
                    * selectedRows[i].getAverageMZ())
                    / FormulaUtils.calculateExactMass(customXAxisKMBase));
            xValues[i] = unshiftedValue + xAxisShift - Math.floor(unshiftedValue + xAxisShift);
          }
        }
      } else {
        for (int i = 0; i < selectedRows.length; i++) {

          // simply plot m/z values as x axis
          if (xAxisKMBase.equals("m/z")) {
            xValues[i] = selectedRows[i].getAverageMZ();
          }

          // plot Kendrick masses as x axis
          else if (xAxisKMBase.equals("KM")) {
            xValues[i] = selectedRows[i].getAverageMZ()
                * getKendrickMassFactor(customYAxisKMBase, yAxisDivisor);
          }
        }
      }

      // Calc yValues
      double[] yValues = new double[selectedRows.length];
      if (useRKM_Y == false) {
        for (int i = 0; i < selectedRows.length; i++) {
          double unshiftedValue = Math
              .ceil(yAxisCharge * (selectedRows[i].getAverageMZ())
                  * getKendrickMassFactor(customYAxisKMBase, yAxisDivisor))
              - yAxisCharge * (selectedRows[i].getAverageMZ())
                  * getKendrickMassFactor(customYAxisKMBase, yAxisDivisor);
          yValues[i] = unshiftedValue + yAxisShift - Math.floor(unshiftedValue + yAxisShift);
        }
      } else {
        for (int i = 0; i < selectedRows.length; i++) {
          double unshiftedValue = (yAxisCharge
              * (yAxisDivisor - Math.round(FormulaUtils.calculateExactMass(customYAxisKMBase)))
              * selectedRows[i].getAverageMZ()) / FormulaUtils.calculateExactMass(customYAxisKMBase)//
              - Math.floor((yAxisCharge
                  * (yAxisDivisor - Math.round(FormulaUtils.calculateExactMass(customYAxisKMBase)))
                  * selectedRows[i].getAverageMZ())
                  / FormulaUtils.calculateExactMass(customYAxisKMBase));
          yValues[i] = unshiftedValue + yAxisShift - Math.floor(unshiftedValue + yAxisShift);
        }
      }
      dataset.setyValues(yValues);
      dataset.setxValues(xValues);
      getChart().fireChartChanged();

    } else if (plot.getDataset() instanceof KendrickMassPlotXYZDataset) {
      KendrickMassPlotXYZDataset dataset = (KendrickMassPlotXYZDataset) plot.getDataset();
      double[] xValues = new double[dataset.getItemCount(0)];

      // Calc xValues
      xValues = new double[selectedRows.length];
      if (useCustomXAxisKMBase == true) {
        if (useRKM_X == false) {
          for (int i = 0; i < selectedRows.length; i++) {
            double unshiftedValue = Math
                .ceil(xAxisCharge * selectedRows[i].getAverageMZ()
                    * getKendrickMassFactor(customXAxisKMBase, xAxisDivisor))
                - xAxisCharge * selectedRows[i].getAverageMZ()
                    * getKendrickMassFactor(customXAxisKMBase, xAxisDivisor);
            xValues[i] = unshiftedValue + xAxisShift - Math.floor(unshiftedValue + xAxisShift);
          }
        } else {
          for (int i = 0; i < selectedRows.length; i++) {
            double unshiftedValue = (xAxisCharge
                * (xAxisDivisor - Math.round(FormulaUtils.calculateExactMass(customXAxisKMBase)))
                * selectedRows[i].getAverageMZ())
                / FormulaUtils.calculateExactMass(customXAxisKMBase)//
                - Math.floor((xAxisCharge
                    * (xAxisDivisor
                        - Math.round(FormulaUtils.calculateExactMass(customXAxisKMBase)))
                    * selectedRows[i].getAverageMZ())
                    / FormulaUtils.calculateExactMass(customXAxisKMBase));
            xValues[i] = unshiftedValue + xAxisShift - Math.floor(unshiftedValue + xAxisShift);
          }
        }
      } else {
        for (int i = 0; i < selectedRows.length; i++) {

          // simply plot m/z values as x axis
          if (xAxisKMBase.equals("m/z")) {
            xValues[i] = selectedRows[i].getAverageMZ();
          }

          // plot Kendrick masses as x axis
          else if (xAxisKMBase.equals("KM")) {
            xValues[i] = selectedRows[i].getAverageMZ()
                * getKendrickMassFactor(customYAxisKMBase, yAxisDivisor);
          }
        }
      }

      // Calc yValues
      double[] yValues = new double[selectedRows.length];
      if (useRKM_Y == false) {
        for (int i = 0; i < selectedRows.length; i++) {
          double unshiftedValue = Math
              .ceil(yAxisCharge * (selectedRows[i].getAverageMZ())
                  * getKendrickMassFactor(customYAxisKMBase, yAxisDivisor))
              - yAxisCharge * (selectedRows[i].getAverageMZ())
                  * getKendrickMassFactor(customYAxisKMBase, yAxisDivisor);
          yValues[i] = unshiftedValue + yAxisShift - Math.floor(unshiftedValue + yAxisShift);
        }
      } else {
        for (int i = 0; i < selectedRows.length; i++) {
          double unshiftedValue = (yAxisCharge
              * (yAxisDivisor - Math.round(FormulaUtils.calculateExactMass(customYAxisKMBase)))
              * selectedRows[i].getAverageMZ()) / FormulaUtils.calculateExactMass(customYAxisKMBase)//
              - Math.floor((yAxisCharge
                  * (yAxisDivisor - Math.round(FormulaUtils.calculateExactMass(customYAxisKMBase)))
                  * selectedRows[i].getAverageMZ())
                  / FormulaUtils.calculateExactMass(customYAxisKMBase));
          yValues[i] = unshiftedValue + yAxisShift - Math.floor(unshiftedValue + yAxisShift);
        }
      }

      // Calc zValues
      double[] zValues = new double[selectedRows.length];
      if (useCustomZAxisKMBase == true) {
        if (useRKM_Z == false) {
          for (int i = 0; i < selectedRows.length; i++) {
            double unshiftedValue = Math
                .ceil(zAxisCharge * (selectedRows[i].getAverageMZ())
                    * getKendrickMassFactor(customZAxisKMBase, zAxisDivisor))
                - zAxisCharge * (selectedRows[i].getAverageMZ())
                    * getKendrickMassFactor(customZAxisKMBase, zAxisDivisor);
            zValues[i] = unshiftedValue + zAxisShift - Math.floor(unshiftedValue + zAxisShift);
          }
        } else {
          for (int i = 0; i < selectedRows.length; i++) {
            double unshiftedValue = (zAxisCharge
                * (zAxisDivisor - Math.round(FormulaUtils.calculateExactMass(customZAxisKMBase)))
                * selectedRows[i].getAverageMZ())
                / FormulaUtils.calculateExactMass(customZAxisKMBase)//
                - Math.floor((zAxisCharge
                    * (zAxisDivisor
                        - Math.round(FormulaUtils.calculateExactMass(customZAxisKMBase)))
                    * selectedRows[i].getAverageMZ())
                    / FormulaUtils.calculateExactMass(customZAxisKMBase));
            zValues[i] = unshiftedValue + zAxisShift - Math.floor(unshiftedValue + zAxisShift);
          }
        }
      } else
        for (int i = 0; i < selectedRows.length; i++) {

          // plot selected feature characteristic as z Axis
          if (zAxisKMBase.equals("Retention time")) {
            zValues[i] = selectedRows[i].getAverageRT();
          } else if (zAxisKMBase.equals("Intensity")) {
            zValues[i] = selectedRows[i].getAverageHeight();
          } else if (zAxisKMBase.equals("Area")) {
            zValues[i] = selectedRows[i].getAverageArea();
          } else if (zAxisKMBase.equals("Tailing factor")) {
            zValues[i] = selectedRows[i].getBestFeature().getTailingFactor();
          } else if (zAxisKMBase.equals("Asymmetry factor")) {
            zValues[i] = selectedRows[i].getBestFeature().getAsymmetryFactor();
          } else if (zAxisKMBase.equals("FWHM")) {
            zValues[i] = selectedRows[i].getBestFeature().getFWHM();
          } else if (zAxisKMBase.equals("m/z")) {
            zValues[i] = selectedRows[i].getBestFeature().getMZ();
          }
        }
      dataset.setyValues(yValues);
      dataset.setxValues(xValues);
      dataset.setzValues(zValues);
      getChart().fireChartChanged();
    }

    // update toolbar
    updateToolBar();

    // set tooltip
    setTooltips();
  }

  private void setTooltips() {
    if (customYAxisKMBase != null)
      tooltipYAxisLabel.setText("The KM-Plot for divisor " + //
          getDivisorKM(customYAxisKMBase) + " is equal to a regular KM-Plot with divisor 1");
    if (customXAxisKMBase != null)
      tooltipXAxisLabel.setText("The KM-Plot for divisor " + //
          getDivisorKM(customXAxisKMBase) + " is equal to a regular KM-Plot with divisor 1");
    if (customZAxisKMBase != null)
      tooltipZAxisLabel.setText("The KM-Plot for divisor " + //
          getDivisorKM(customZAxisKMBase) + " is equal to a regular KM-Plot with divisor 1");
  }

  /*
   * Method to calculate the Kendrick mass factor for a given sum formula
   */
  private double getKendrickMassFactor(String formula, int divisor) {
    double exactMassFormula = FormulaUtils.calculateExactMass(formula);
    return Math.round(exactMassFormula / divisor) / (exactMassFormula / divisor);
  }

  /*
   * Method to calculate the divisor for Kendrick mass defect analysis
   */
  private int getDivisorKM(String formula) {
    double exactMass = FormulaUtils.calculateExactMass(formula);
    return (int) Math.round(exactMass);
  }

  /*
   * Method to calculate the recommended minimum of a divisor for Kendrick mass defect analysis
   */
  private int getMinimumRecommendedDivisor(String formula) {
    double exactMass = FormulaUtils.calculateExactMass(formula);
    return (int) Math.round((2.0 / 3.0) * exactMass);
  }

  /*
   * Method to calculate the recommended maximum of a divisor for Kendrick mass defect analysis
   */
  private int getMaximumRecommendedDivisor(String formula) {
    double exactMass = FormulaUtils.calculateExactMass(formula);
    return (int) Math.round(2.0 * exactMass);
  }

  /*
   * Method to avoid round(R) as divisor for RKM plots All RKM values would be 0 in that case
   */
  private int checkDivisor(int divisor, boolean useRKM, String kmdBase, boolean divisorUp) {
    if (useRKM && divisor == getDivisorKM(kmdBase) && divisorUp) {
      divisor++;
      return divisor;
    } else if (useRKM && divisor == getDivisorKM(kmdBase) && !divisorUp) {
      divisor--;
      return divisor;
    } else
      return divisor;
  }

  /*
   * Method to update buttons in tool bar
   */
  private void updateToolBar() {

    // Y-Axis
    shiftLabelYAxis.setText(shiftFormat.format(yAxisShift));
    chargeLabelYAxis.setText(Integer.toString(yAxisCharge));
    divisorLabelYAxis.setText(Integer.toString(yAxisDivisor));

    // X-Axis
    shiftLabelXAxis.setText(shiftFormat.format(xAxisShift));
    chargeLabelXAxis.setText(Integer.toString(xAxisCharge));
    divisorLabelXAxis.setText(Integer.toString(xAxisDivisor));

    // Z-Axis
    shiftLabelZAxis.setText(shiftFormat.format(zAxisShift));
    chargeLabelZAxis.setText(Integer.toString(zAxisCharge));
    divisorLabelZAxis.setText(Integer.toString(zAxisDivisor));
  }

}
