/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.gui.chartbasics.chartutils.ColoredBubbleDatasetRenderer;
import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import io.github.mzmine.gui.chartbasics.simplechart.RegionSelectionWrapper;
import io.github.mzmine.javafx.components.factories.FxLabels;
import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.javafx.concurrent.threading.FxThread;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.kendrickmassplot.regionextraction.RegionExtractionModule;
import io.github.mzmine.modules.visualization.kendrickmassplot.regionextraction.RegionExtractionParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.ParameterUtils;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.FormulaUtils;
import java.awt.geom.Point2D;
import java.util.List;
import java.util.Objects;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.shape.Circle;
import javafx.scene.shape.StrokeType;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;

public class KendrickMassPlotAnchorPaneController {

  private FeatureList featureList;
  private String customYAxisKMBase;
  private String customXAxisKMBase;
  private boolean useRKM_X;
  private boolean useRKM_Y;
  private int yAxisCharge;
  private int xAxisCharge;
  private int yAxisDivisor;
  private int xAxisDivisor;

  @FXML
  private BorderPane plotPane;

  @FXML
  private Tooltip tooltipYAxisLabel;

  @FXML
  private Label chargeLabelYAxis;

  @FXML
  private Label divisorLabelYAxis;

  @FXML
  private BorderPane bubbleLegendPane;

  @FXML
  private GridPane gridPaneXAxis;

  @FXML
  private GridPane gridPaneYAxis;

  @FXML
  private Tooltip tooltipXAxisLabel;

  @FXML
  private Button chargeUpXAxis;

  @FXML
  private Button divisorUpXAxis;

  @FXML
  private Button chargeDownXAxis;

  @FXML
  private Button divisorDownXAxis;

  @FXML
  private Button chargeUpYAxis;

  @FXML
  private Button divisorUpYAxis;

  @FXML
  private Button chargeDownYAxis;

  @FXML
  private Button divisorDownYAxis;

  @FXML
  private Label chargeLabelXAxis;

  @FXML
  private Label divisorLabelXAxis;

  @FXML
  private CheckBox cbHighlightAnnotated;

  private ParameterSet parameters;
  private KendrickMassPlotChart kendrickChart;

  @FXML
  public void initialize(ParameterSet parameters) {
    this.parameters = parameters.cloneParameterSet();
    this.featureList = parameters.getParameter(KendrickMassPlotParameters.featureList).getValue()
        .getMatchingFeatureLists()[0];

    this.useRKM_X = parameters.getParameter(KendrickMassPlotParameters.xAxisValues).getValue()
        .equals(KendrickPlotDataTypes.REMAINDER_OF_KENDRICK_MASS);
    this.useRKM_Y = parameters.getParameter(KendrickMassPlotParameters.yAxisValues).getValue()
        .equals(KendrickPlotDataTypes.REMAINDER_OF_KENDRICK_MASS);
    boolean useCustomXAxisKMBase = parameters.getParameter(KendrickMassPlotParameters.xAxisValues)
        .getValue().isKendrickType();

    if (useCustomXAxisKMBase) {
      this.customXAxisKMBase = parameters.getParameter(
          KendrickMassPlotParameters.xAxisCustomKendrickMassBase).getValue();
      gridPaneXAxis.setDisable(false);
      this.xAxisDivisor = getDivisorKM(customXAxisKMBase);
      if (useRKM_X) {
        xAxisDivisor++;
      }
    }

    boolean useCustomYAxisKMBase = parameters.getParameter(KendrickMassPlotParameters.yAxisValues)
        .getValue().isKendrickType();

    if (useCustomYAxisKMBase) {
      this.customYAxisKMBase = parameters.getParameter(
          KendrickMassPlotParameters.yAxisCustomKendrickMassBase).getValue();
      gridPaneYAxis.setDisable(false);
      this.yAxisDivisor = getDivisorKM(customYAxisKMBase);
      if (useRKM_Y) {
        yAxisDivisor++;
      }
    }

    this.yAxisCharge = 1;
    this.xAxisCharge = 1;

    this.useRKM_X = parameters.getParameter(KendrickMassPlotParameters.xAxisValues).getValue()
        .equals(KendrickPlotDataTypes.REMAINDER_OF_KENDRICK_MASS);
    this.useRKM_Y = parameters.getParameter(KendrickMassPlotParameters.yAxisValues).getValue()
        .equals(KendrickPlotDataTypes.REMAINDER_OF_KENDRICK_MASS);
    setArrowIcon(chargeDownXAxis, FontAwesomeIcon.ARROW_DOWN);
    setArrowIcon(divisorDownXAxis, FontAwesomeIcon.ARROW_DOWN);
    setArrowIcon(chargeDownYAxis, FontAwesomeIcon.ARROW_DOWN);
    setArrowIcon(divisorDownYAxis, FontAwesomeIcon.ARROW_DOWN);
    setArrowIcon(chargeUpXAxis, FontAwesomeIcon.ARROW_UP);
    setArrowIcon(divisorUpXAxis, FontAwesomeIcon.ARROW_UP);
    setArrowIcon(chargeUpYAxis, FontAwesomeIcon.ARROW_UP);
    setArrowIcon(divisorUpYAxis, FontAwesomeIcon.ARROW_UP);

    String title = "Kendrick mass plot of" + featureList;

    String xAxisLabel;
    if (parameters.getParameter(KendrickMassPlotParameters.xAxisValues).getValue()
        .isKendrickType()) {
      String kmdRkm;
      if (parameters.getParameter(KendrickMassPlotParameters.xAxisValues).getValue()
          .equals(KendrickPlotDataTypes.REMAINDER_OF_KENDRICK_MASS)) {
        kmdRkm = "RKM";
      } else {
        kmdRkm = "KMD";
      }
      xAxisLabel = kmdRkm + " (" + parameters.getParameter(
          KendrickMassPlotParameters.xAxisCustomKendrickMassBase).getValue() + ")";
    } else {
      xAxisLabel = parameters.getParameter(KendrickMassPlotParameters.xAxisValues).getValue()
          .getName();
    }

    String yAxisLabel;
    if (parameters.getParameter(KendrickMassPlotParameters.yAxisValues).getValue()
        .isKendrickType()) {
      String kmdRkm;
      if (parameters.getParameter(KendrickMassPlotParameters.yAxisValues).getValue()
          .equals(KendrickPlotDataTypes.REMAINDER_OF_KENDRICK_MASS)) {
        kmdRkm = "RKM";
      } else {
        kmdRkm = "KMD";
      }
      yAxisLabel = kmdRkm + " (" + parameters.getParameter(
          KendrickMassPlotParameters.yAxisCustomKendrickMassBase).getValue() + ")";
    } else {
      yAxisLabel = parameters.getParameter(KendrickMassPlotParameters.yAxisValues).getValue()
          .getName();
    }

    String zAxisLabel;
    if (parameters.getParameter(KendrickMassPlotParameters.colorScaleValues).getValue()
        .isKendrickType()) {
      String kmdRkm;
      if (parameters.getParameter(KendrickMassPlotParameters.colorScaleValues).getValue()
          .equals(KendrickPlotDataTypes.REMAINDER_OF_KENDRICK_MASS)) {
        kmdRkm = "RKM";
      } else {
        kmdRkm = "KMD";
      }
      zAxisLabel = kmdRkm + " (" + parameters.getParameter(
          KendrickMassPlotParameters.colorScaleCustomKendrickMassBase).getValue() + ")";
    } else {
      zAxisLabel = parameters.getParameter(KendrickMassPlotParameters.colorScaleValues).getValue()
          .getName();
    }

    KendrickMassPlotXYZDataset kendrickMassPlotXYZDataset = new KendrickMassPlotXYZDataset(
        parameters, 1, 1);

    kendrickMassPlotXYZDataset.addTaskStatusListener((_, newStatus, _) -> {
      if (newStatus == TaskStatus.FINISHED) {
        kendrickChart = new KendrickMassPlotChart(title, xAxisLabel, yAxisLabel, zAxisLabel,
            kendrickMassPlotXYZDataset);
        KendrickMassPlotBubbleLegend kendrickMassPlotBubbleLegend = new KendrickMassPlotBubbleLegend(
            kendrickMassPlotXYZDataset);
        var selectionWrapper = new RegionSelectionWrapper<>(kendrickChart, this::onExtractPressed);
        FxThread.runLater(() -> {
          plotPane.setCenter(selectionWrapper);
          bubbleLegendPane.setCenter(kendrickMassPlotBubbleLegend);
          updateToolBar();
          setTooltips();
        });
      }
    });

    cbHighlightAnnotated.selectedProperty().subscribe(selected -> {
      setHighlightToRenderer(Objects.requireNonNullElse(selected, false));
    });
    final Circle circle = new Circle(5);
    circle.setStroke(ConfigService.getDefaultColorPalette().getNegativeColor());
    circle.setStrokeWidth(2f);
    circle.setStrokeType(StrokeType.OUTSIDE);
    circle.setFill(null);
    cbHighlightAnnotated.setGraphic(
        FxLayout.newFlowPane(FxLabels.newLabel("Highlight annotated"), circle));
  }

  private void setHighlightToRenderer(boolean highlight) {
    final boolean hasAnnotations = featureList.stream().anyMatch(FeatureListRow::isIdentified);
    cbHighlightAnnotated.setDisable(!hasAnnotations);

    if (kendrickChart == null) {
      return;
    }

    for (int i = 0; i < kendrickChart.getChart().getXYPlot().getRendererCount(); i++) {
      final XYItemRenderer renderer = kendrickChart.getChart().getXYPlot().getRenderer(i);
      if (renderer instanceof ColoredBubbleDatasetRenderer r) {
        r.setHighlightAnnotated(highlight);
      }
    }
    kendrickChart.fireChangeEvent();
  }

  public void onExtractPressed(List<List<Point2D>> regionPointLists) {
    final ParameterSet param = ConfigService.getConfiguration()
        .getModuleParameters(RegionExtractionModule.class);

    final ParameterSet kendrickParam = param.getEmbeddedParameterValue(
        RegionExtractionParameters.kendrickParam);
    ParameterUtils.copyParameters(parameters,
        kendrickParam); // use the settings used for this plot.

    param.setParameter(RegionExtractionParameters.xAxisDivisor, xAxisDivisor);
    param.setParameter(RegionExtractionParameters.xAxisCharge, xAxisCharge);
    param.setParameter(RegionExtractionParameters.yAxisCharge, yAxisCharge);
    param.setParameter(RegionExtractionParameters.yAxisDivisor, yAxisDivisor);
    param.setParameter(RegionExtractionParameters.regions, regionPointLists);

    MZmineCore.setupAndRunModule(RegionExtractionModule.class);
  }

  private void setArrowIcon(Button button, FontAwesomeIcon icon) {
    if (icon.equals(FontAwesomeIcon.ARROW_UP)) {
      FontAwesomeIcon upArrowIcon = FontAwesomeIcon.ARROW_UP;
      FontAwesomeIconView upArrowIconView = new FontAwesomeIconView(upArrowIcon);
      button.setGraphic(upArrowIconView);
    } else if (icon.equals(FontAwesomeIcon.ARROW_DOWN)) {
      FontAwesomeIcon downArrowIcon = FontAwesomeIcon.ARROW_DOWN;
      FontAwesomeIconView downArrowIconView = new FontAwesomeIconView(downArrowIcon);
      button.setGraphic(downArrowIconView);
    }
  }

  @FXML
  void chargeDownY(ActionEvent event) {
    updateCharge(yAxisCharge - 1, yAxisCharge, yAxisDivisor);
  }

  @FXML
  void chargeDownX(ActionEvent event) {
    updateCharge(xAxisCharge - 1, xAxisCharge, xAxisDivisor);
  }

  @FXML
  void chargeUpY(ActionEvent event) {
    updateCharge(yAxisCharge + 1, yAxisCharge, yAxisDivisor);
  }

  @FXML
  void chargeUpX(ActionEvent event) {
    updateCharge(xAxisCharge + 1, xAxisCharge, xAxisDivisor);
  }

  private void updateCharge(int newCharge, int currentCharge, int divisor) {
    if (newCharge < 1) {
      newCharge = 1;
    }

    if (newCharge != currentCharge) {
      if (divisor >= 1) {
        if (divisor == xAxisDivisor) {
          xAxisCharge = newCharge;
        } else {
          yAxisCharge = newCharge;
        }
        updatePlot();
      }
    }
  }

  @FXML
  void divisorDownY(ActionEvent event) {
    updateDivisor(yAxisDivisor - 1, yAxisDivisor, useRKM_Y, customYAxisKMBase);
  }

  @FXML
  void divisorDownX(ActionEvent event) {
    updateDivisor(xAxisDivisor - 1, xAxisDivisor, useRKM_X, customXAxisKMBase);
  }

  @FXML
  void divisorUpY(ActionEvent event) {
    updateDivisor(yAxisDivisor + 1, yAxisDivisor, useRKM_Y, customYAxisKMBase);
  }

  @FXML
  void divisorUpX(ActionEvent event) {
    updateDivisor(xAxisDivisor + 1, xAxisDivisor, useRKM_X, customXAxisKMBase);
  }

  private void updateDivisor(int newDivisor, int currentDivisor, boolean useRKM, String kmdBase) {
    int minDivisor = getMinimumRecommendedDivisor(kmdBase);
    int maxDivisor = getMaximumRecommendedDivisor(kmdBase);

    if (newDivisor < minDivisor) {
      newDivisor = minDivisor;
    } else if (newDivisor > maxDivisor) {
      newDivisor = maxDivisor;
    }

    if (newDivisor != currentDivisor) {
      newDivisor = checkDivisor(newDivisor, useRKM, kmdBase, newDivisor > currentDivisor);

      if (newDivisor != currentDivisor) {
        if (useRKM) {
          if (newDivisor == getDivisorKM(kmdBase)) {
            newDivisor += (newDivisor > currentDivisor) ? 1 : -1;
          }
        }

        if (useRKM_X) {
          xAxisDivisor = newDivisor;
        } else {
          yAxisDivisor = newDivisor;
        }
        updatePlot();
      }
    }
  }

  private void updatePlot() {
    bubbleLegendPane.setDisable(true);
    XYPlot plot = Objects.requireNonNull(getChart()).getXYPlot();
    KendrickMassPlotXYZDataset newDataset = new KendrickMassPlotXYZDataset(parameters, xAxisDivisor,
        xAxisCharge, yAxisDivisor, yAxisCharge);
    newDataset.addTaskStatusListener((_, newStatus, _) -> {
      if (newStatus == TaskStatus.FINISHED) {
        FxThread.runLater(() -> {
          plot.setDataset(newDataset);
          updateToolBar();
          setTooltips();
          bubbleLegendPane.setDisable(false);
          setHighlightToRenderer(cbHighlightAnnotated.isSelected());
        });
      }
    });
    updateToolBar();
    setTooltips();
  }

  public BorderPane getBubbleLegendPane() {
    return bubbleLegendPane;
  }

  private JFreeChart getChart() {
    if (kendrickChart instanceof EChartViewer viewer) {
      return viewer.getChart();
    }
    return null;
  }

  private void setTooltips() {
    if (customYAxisKMBase != null) {
      tooltipYAxisLabel.setText("The KM-Plot for divisor " + //
          getDivisorKM(customYAxisKMBase) + " is equal to a regular KM-Plot with divisor 1");
    }
    if (customXAxisKMBase != null) {
      tooltipXAxisLabel.setText("The KM-Plot for divisor " + //
          getDivisorKM(customXAxisKMBase) + " is equal to a regular KM-Plot with divisor 1");
    }

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
    } else {
      return divisor;
    }
  }

  /*
   * Method to update buttons in toolbar
   */
  private void updateToolBar() {
    chargeLabelYAxis.setText(Integer.toString(yAxisCharge));
    divisorLabelYAxis.setText(Integer.toString(yAxisDivisor));
    chargeLabelXAxis.setText(Integer.toString(xAxisCharge));
    divisorLabelXAxis.setText(Integer.toString(xAxisDivisor));
  }

  public FeatureList getFeatureList() {
    return featureList;
  }

}
