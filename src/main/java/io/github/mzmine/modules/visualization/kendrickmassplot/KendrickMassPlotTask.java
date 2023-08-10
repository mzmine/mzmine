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

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.awt.Font;
import java.time.Instant;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYZDataset;

/**
 * Task to create a Kendrick mass plot of selected features of a selected feature list
 *
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public class KendrickMassPlotTask extends AbstractTask {

  static final Font legendFont = new Font("SansSerif", Font.PLAIN, 12);
  static final Font titleFont = new Font("SansSerif", Font.PLAIN, 12);

  private final Logger logger = Logger.getLogger(this.getClass().getName());

  private final ParameterSet parameters;
  private XYDataset dataset2D;
  private XYZDataset dataset3D;
  private JFreeChart chart;
  private final FeatureList featureList;
  private final String title;
  private final String xAxisLabel;
  private final String yAxisLabel;
  private final String zAxisLabel;
  private final String zAxisScaleType;
  private final String bubbleSizeLabel;
  private Range<Double> zScaleRange;
  private String paintScaleStyle;
  private final FeatureListRow[] rows;

  public KendrickMassPlotTask(ParameterSet parameters, @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate);
    featureList = parameters.getParameter(KendrickMassPlotParameters.featureList).getValue()
        .getMatchingFeatureLists()[0];

    this.parameters = parameters;

    this.title = "Kendrick mass plot of" + featureList;

    if (parameters.getParameter(KendrickMassPlotParameters.xAxisValues).getValue()
        .isKendrickType()) {
      xAxisLabel =
          "KMD (" + parameters.getParameter(KendrickMassPlotParameters.xAxisCustomKendrickMassBase)
              .getValue() + ")";
    } else {
      xAxisLabel = parameters.getParameter(KendrickMassPlotParameters.xAxisValues).getValue()
          .getName();
    }

    if (parameters.getParameter(KendrickMassPlotParameters.yAxisValues).getValue()
        .isKendrickType()) {
      yAxisLabel =
          "KMD (" + parameters.getParameter(KendrickMassPlotParameters.yAxisCustomKendrickMassBase)
              .getValue() + ")";
    } else {
      yAxisLabel = parameters.getParameter(KendrickMassPlotParameters.yAxisValues).getValue()
          .getName();
    }

    if (parameters.getParameter(KendrickMassPlotParameters.colorScaleValues).getValue()
        .isKendrickType()) {
      zAxisLabel = "KMD (" + parameters.getParameter(
          KendrickMassPlotParameters.colorScaleCustomKendrickMassBase).getValue() + ")";
    } else {
      zAxisLabel = parameters.getParameter(KendrickMassPlotParameters.colorScaleValues).getValue()
          .getName();
    }

    zAxisScaleType = parameters.getParameter(KendrickMassPlotParameters.colorScaleValues).getValue()
        .getName();

    bubbleSizeLabel = parameters.getParameter(KendrickMassPlotParameters.bubbleSizeValues)
        .getValue().getName();

    rows = featureList.getRows().toArray(new FeatureListRow[0]);

  }

  @Override
  public String getTaskDescription() {
    return "Create Kendrick mass plot for " + featureList;
  }

  @Override
  public double getFinishedPercentage() {
    return 0;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);
    logger.info("Create Kendrick mass plot of " + featureList);
    // Task canceled?
    if (isCanceled()) {
      return;
    }

    KendrickMassPlotXYZDataset kendrickMassPlotXYZDataset = new KendrickMassPlotXYZDataset(
        parameters);
    KendrickMassPlotChart kendrickMassPlotChart = new KendrickMassPlotChart(title, xAxisLabel,
        yAxisLabel, zAxisLabel, kendrickMassPlotXYZDataset);

//    // 2D, if no third dimension was selected
//    if (zAxisLabel.equals("none")) {
//      chart = create2DKendrickMassPlot();
//    }
//    // 3D, if a third dimension was selected
//    else {
//      chart = create3DKendrickMassPlot();
//    }
//    chart.setBackgroundPaint(Color.white);
//
//    // create chartViewer
//    EChartViewer chartViewer = new EChartViewer(chart, true, true, true, true, false);
//
//    // get plot
//    XYPlot plot = (XYPlot) chart.getPlot();
//
//    // mouse listener
//    chartViewer.addChartMouseListener(new ChartMouseListenerFX() {
//
//      @Override
//      public void chartMouseMoved(ChartMouseEventFX event) {}
//
//      @Override
//      public void chartMouseClicked(ChartMouseEventFX event) {
//        double xValue = plot.getDomainCrosshairValue();
//        double yValue = plot.getRangeCrosshairValue();
//        if (plot.getDataset() instanceof KendrickMassPlotXYZDataset) {
//          KendrickMassPlotXYZDataset dataset = (KendrickMassPlotXYZDataset) plot.getDataset();
//          double[] xValues = new double[dataset.getItemCount(0)];
//          for (int i = 0; i < xValues.length; i++) {
//            if ((event.getTrigger().getButton().equals(MouseButton.PRIMARY))
//                && (event.getTrigger().getClickCount() == 2)) {
//              if (dataset.getX(0, i).doubleValue() == xValue
//                  && dataset.getY(0, i).doubleValue() == yValue) {
//                new FeatureOverviewWindow(rows[i]);
//              }
//            }
//          }
//        }
//        if (plot.getDataset() instanceof KendrickMassPlotXYDataset) {
//          KendrickMassPlotXYDataset dataset = (KendrickMassPlotXYDataset) plot.getDataset();
//          double[] xValues = new double[dataset.getItemCount(0)];
//          for (int i = 0; i < xValues.length; i++) {
//            if ((event.getTrigger().getButton().equals(MouseButton.PRIMARY))
//                && (event.getTrigger().getClickCount() == 2)) {
//              if (dataset.getX(0, i).doubleValue() == xValue
//                  && dataset.getY(0, i).doubleValue() == yValue) {
//                new FeatureOverviewWindow(rows[i]);
//              }
//            }
//          }
//        }
//      }
//    });
//

    // Create Kendrick mass plot Tab
    MZmineCore.runLater(() -> {
      KendrickMassPlotTab newTab = new KendrickMassPlotTab(parameters, kendrickMassPlotChart);
      MZmineCore.getDesktop().addTab(newTab);
    });

    setStatus(TaskStatus.FINISHED);
    logger.info("Finished creating Kendrick mass plot of " + featureList);
  }

}
