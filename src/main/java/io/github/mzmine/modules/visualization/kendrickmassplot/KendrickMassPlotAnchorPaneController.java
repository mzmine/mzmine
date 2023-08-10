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

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.FormulaUtils;
import io.github.mzmine.util.javafx.FxIconUtil;
import java.text.DecimalFormat;
import java.util.logging.Logger;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;

public class KendrickMassPlotAnchorPaneController {

  private final Logger logger = Logger.getLogger(this.getClass().getName());

  private FeatureListRow[] selectedRows;
  private FeatureList featureList;
  private String xAxisKMBase;
  private String customYAxisKMBase;
  private String customXAxisKMBase;
  private boolean useCustomXAxisKMBase;
  private boolean useCustomYAxisKMBase;
  private boolean useRKM_X;
  private boolean useRKM_Y;
  private double xAxisShift;
  private double yAxisShift;
  private int yAxisCharge;
  private int xAxisCharge;
  private int yAxisDivisor;
  private int xAxisDivisor;

  private static final Image iconKMD = FxIconUtil.loadImageFromResources("icons/KMDIcon.png");

  private static final Image iconRKM = FxIconUtil.loadImageFromResources("icons/RKMIcon.png");

  private static final DecimalFormat shiftFormat = new DecimalFormat("0.##");

  @FXML
  private BorderPane plotPane;

  @FXML
  private Tooltip tooltipYAxisLabel;

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
  private GridPane gridPaneYAxis;

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

  public void initialize(ParameterSet parameters) {

    this.featureList = parameters.getParameter(KendrickMassPlotParameters.featureList).getValue()
        .getMatchingFeatureLists()[0];

    this.selectedRows = featureList.getRows().toArray(new FeatureListRow[0]);

    this.useCustomXAxisKMBase = parameters.getParameter(KendrickMassPlotParameters.xAxisValues)
        .getValue().isKendrickType();

    if (useCustomXAxisKMBase) {
      this.customXAxisKMBase = parameters.getParameter(
          KendrickMassPlotParameters.xAxisCustomKendrickMassBase).getValue();
      gridPaneXAxis.setDisable(false);
    } else {
      this.xAxisKMBase = null;
    }

    this.useCustomYAxisKMBase = parameters.getParameter(KendrickMassPlotParameters.yAxisValues)
        .getValue().isKendrickType();

    if (useCustomYAxisKMBase) {
      this.customYAxisKMBase = parameters.getParameter(
          KendrickMassPlotParameters.yAxisCustomKendrickMassBase).getValue();
      gridPaneYAxis.setDisable(false);
    }

    this.yAxisCharge = 1;
    this.xAxisCharge = 1;
    if (customYAxisKMBase != null) {
      this.yAxisDivisor = getDivisorKM(customYAxisKMBase);
    } else {
      this.yAxisDivisor = 1;
    }
    if (customXAxisKMBase != null) {
      this.xAxisDivisor = getDivisorKM(customXAxisKMBase);
    } else {
      this.xAxisDivisor = 1;
    }

    this.xAxisShift = 0;
    this.yAxisShift = 0;
    this.useRKM_X = false;
    this.useRKM_Y = false;
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
  void toggleKMDRKMY(ActionEvent event) {
    logger.finest("Toggle KMD and RKM Y-Axis");
    XYPlot plot = getChart().getXYPlot();
    if (useCustomYAxisKMBase) {
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
  }

  @FXML
  void toggleKMDRKMX(ActionEvent event) {
    logger.finest("Toggle KMD and RKM X-Axis");
    XYPlot plot = getChart().getXYPlot();
    if (useCustomXAxisKMBase) {
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
  }

  public BorderPane getPlotPane() {
    return plotPane;
  }

  private JFreeChart getChart() {
    if (plotPane.getChildren().get(0) instanceof EChartViewer viewer) {
      return viewer.getChart();
    }
    return null;
  }

  /*
   * Method to calculate the data sets for a Kendrick mass plot
   */
  private void kendrickVariableChanged(XYPlot plot) {

    if (plot.getDataset() instanceof KendrickMassPlotXYZDataset dataset) {
      double[] xValues = new double[dataset.getItemCount(0)];

      // Calc xValues
      xValues = new double[selectedRows.length];
      if (useCustomXAxisKMBase) {
        if (!useRKM_X) {
          for (int i = 0; i < selectedRows.length; i++) {
            double unshiftedValue = Math.ceil(
                xAxisCharge * selectedRows[i].getAverageMZ() * getKendrickMassFactor(
                    customXAxisKMBase, xAxisDivisor))
                - xAxisCharge * selectedRows[i].getAverageMZ() * getKendrickMassFactor(
                customXAxisKMBase, xAxisDivisor);
            xValues[i] = unshiftedValue + xAxisShift - Math.floor(unshiftedValue + xAxisShift);
          }
        } else {
          for (int i = 0; i < selectedRows.length; i++) {
            double unshiftedValue = (xAxisCharge * (xAxisDivisor - Math.round(
                FormulaUtils.calculateExactMass(customXAxisKMBase)))
                * selectedRows[i].getAverageMZ()) / FormulaUtils.calculateExactMass(
                customXAxisKMBase)//
                - Math.floor((xAxisCharge * (xAxisDivisor - Math.round(
                FormulaUtils.calculateExactMass(customXAxisKMBase)))
                * selectedRows[i].getAverageMZ()) / FormulaUtils.calculateExactMass(
                customXAxisKMBase));
            xValues[i] = unshiftedValue + xAxisShift - Math.floor(unshiftedValue + xAxisShift);
          }
        }
      } else {
        xValues = dataset.getxValues();
      }

      // Calc yValues
      double[] yValues = new double[selectedRows.length];
      if (!useRKM_Y) {
        for (int i = 0; i < selectedRows.length; i++) {
          double unshiftedValue = Math.ceil(
              yAxisCharge * (selectedRows[i].getAverageMZ()) * getKendrickMassFactor(
                  customYAxisKMBase, yAxisDivisor))
              - yAxisCharge * (selectedRows[i].getAverageMZ()) * getKendrickMassFactor(
              customYAxisKMBase, yAxisDivisor);
          yValues[i] = unshiftedValue + yAxisShift - Math.floor(unshiftedValue + yAxisShift);
        }
      } else {
        for (int i = 0; i < selectedRows.length; i++) {
          double unshiftedValue = (yAxisCharge * (yAxisDivisor - Math.round(
              FormulaUtils.calculateExactMass(customYAxisKMBase))) * selectedRows[i].getAverageMZ())
              / FormulaUtils.calculateExactMass(customYAxisKMBase)//
              - Math.floor((yAxisCharge * (yAxisDivisor - Math.round(
              FormulaUtils.calculateExactMass(customYAxisKMBase))) * selectedRows[i].getAverageMZ())
              / FormulaUtils.calculateExactMass(customYAxisKMBase));
          yValues[i] = unshiftedValue + yAxisShift - Math.floor(unshiftedValue + yAxisShift);
        }
      }


      dataset.setyValues(yValues);
      dataset.setxValues(xValues);
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

  }

  public FeatureList getFeatureList() {
    return featureList;
  }

}
