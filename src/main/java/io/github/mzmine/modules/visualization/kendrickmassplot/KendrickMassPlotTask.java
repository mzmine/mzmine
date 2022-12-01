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
import java.awt.Font;
import java.awt.Paint;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.fx.interaction.ChartMouseEventFX;
import org.jfree.chart.fx.interaction.ChartMouseListenerFX;
import org.jfree.chart.labels.ItemLabelAnchor;
import org.jfree.chart.labels.ItemLabelPosition;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.LookupPaintScale;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.title.PaintScaleLegend;
import org.jfree.chart.title.TextTitle;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.chart.ui.TextAnchor;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYZDataset;
import com.google.common.collect.Range;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.gui.chartbasics.chartutils.NameItemLabelGenerator;
import io.github.mzmine.gui.chartbasics.chartutils.ScatterPlotToolTipGenerator;
import io.github.mzmine.gui.chartbasics.chartutils.XYBlockPixelSizePaintScales;
import io.github.mzmine.gui.chartbasics.chartutils.XYCirclePixelSizeRenderer;
import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.intensityplot.IntensityPlotParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.dialogs.FeatureOverviewWindow;
import javafx.application.Platform;
import javafx.scene.input.MouseButton;

/**
 * Task to create a Kendrick mass plot of selected features of a selected feature list
 *
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public class KendrickMassPlotTask extends AbstractTask {

  static final Font legendFont = new Font("SansSerif", Font.PLAIN, 12);
  static final Font titleFont = new Font("SansSerif", Font.PLAIN, 12);

  private Logger logger = Logger.getLogger(this.getClass().getName());

  private ParameterSet parameters;
  private XYDataset dataset2D;
  private XYZDataset dataset3D;
  private JFreeChart chart;
  private FeatureList featureList;
  private String title;
  private String xAxisLabel;
  private String yAxisLabel;
  private String zAxisLabel;
  private String zAxisScaleType;
  private String bubbleSizeLabel;
  private Range<Double> zScaleRange;
  private String paintScaleStyle;
  private FeatureListRow[] rows;
  private int totalSteps = 3, appliedSteps = 0;

  public KendrickMassPlotTask(ParameterSet parameters, @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate);
    featureList = parameters.getParameter(KendrickMassPlotParameters.featureList).getValue()
        .getMatchingFeatureLists()[0];

    this.parameters = parameters;

    title = "Kendrick mass plot [" + featureList + "]";

    if (parameters.getParameter(KendrickMassPlotParameters.xAxisCustomKendrickMassBase)
        .getValue() == true) {
      xAxisLabel =
          "KMD (" + parameters.getParameter(KendrickMassPlotParameters.xAxisCustomKendrickMassBase)
              .getEmbeddedParameter().getValue() + ")";
    } else {
      xAxisLabel = parameters.getParameter(KendrickMassPlotParameters.xAxisValues).getValue();
    }

    yAxisLabel = "KMD ("
        + parameters.getParameter(KendrickMassPlotParameters.yAxisCustomKendrickMassBase).getValue()
        + ")";

    if (parameters.getParameter(KendrickMassPlotParameters.zAxisCustomKendrickMassBase)
        .getValue() == true) {
      zAxisLabel =
          "KMD (" + parameters.getParameter(KendrickMassPlotParameters.zAxisCustomKendrickMassBase)
              .getEmbeddedParameter().getValue() + ")";
    } else {
      zAxisLabel = parameters.getParameter(KendrickMassPlotParameters.zAxisValues).getValue();
    }

    zAxisScaleType = parameters.getParameter(KendrickMassPlotParameters.zScaleType).getValue();

    zScaleRange = parameters.getParameter(KendrickMassPlotParameters.zScaleRange).getValue();

    bubbleSizeLabel = parameters.getParameter(KendrickMassPlotParameters.bubbleSize).getValue();

    paintScaleStyle = parameters.getParameter(KendrickMassPlotParameters.paintScale).getValue();

    rows =
        parameters.getParameter(IntensityPlotParameters.selectedRows).getMatchingRows(featureList);

  }

  @Override
  public String getTaskDescription() {
    return "Create Kendrick mass plot for " + featureList;
  }

  @Override
  public double getFinishedPercentage() {
    return totalSteps == 0 ? 0 : (double) appliedSteps / totalSteps;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);
    logger.info("Create Kendrick mass plot of " + featureList);
    // Task canceled?
    if (isCanceled())
      return;

    JFreeChart chart = null;

    // 2D, if no third dimension was selected
    if (zAxisLabel.equals("none")) {
      chart = create2DKendrickMassPlot();
    }
    // 3D, if a third dimension was selected
    else {
      chart = create3DKendrickMassPlot();
    }
    chart.setBackgroundPaint(Color.white);

    // create chartViewer
    EChartViewer chartViewer = new EChartViewer(chart, true, true, true, true, false);

    // get plot
    XYPlot plot = (XYPlot) chart.getPlot();

    // mouse listener
    chartViewer.addChartMouseListener(new ChartMouseListenerFX() {

      @Override
      public void chartMouseMoved(ChartMouseEventFX event) {}

      @Override
      public void chartMouseClicked(ChartMouseEventFX event) {
        double xValue = plot.getDomainCrosshairValue();
        double yValue = plot.getRangeCrosshairValue();
        if (plot.getDataset() instanceof KendrickMassPlotXYZDataset) {
          KendrickMassPlotXYZDataset dataset = (KendrickMassPlotXYZDataset) plot.getDataset();
          double[] xValues = new double[dataset.getItemCount(0)];
          for (int i = 0; i < xValues.length; i++) {
            if ((event.getTrigger().getButton().equals(MouseButton.PRIMARY))
                && (event.getTrigger().getClickCount() == 2)) {
              if (dataset.getX(0, i).doubleValue() == xValue
                  && dataset.getY(0, i).doubleValue() == yValue) {
                new FeatureOverviewWindow(rows[i]);
              }
            }
          }
        }
        if (plot.getDataset() instanceof KendrickMassPlotXYDataset) {
          KendrickMassPlotXYDataset dataset = (KendrickMassPlotXYDataset) plot.getDataset();
          double[] xValues = new double[dataset.getItemCount(0)];
          for (int i = 0; i < xValues.length; i++) {
            if ((event.getTrigger().getButton().equals(MouseButton.PRIMARY))
                && (event.getTrigger().getClickCount() == 2)) {
              if (dataset.getX(0, i).doubleValue() == xValue
                  && dataset.getY(0, i).doubleValue() == yValue) {
                new FeatureOverviewWindow(rows[i]);
              }
            }
          }
        }
      }
    });


    // set title properties
    TextTitle chartTitle = chart.getTitle();
    chartTitle.setMargin(5, 0, 0, 0);
    chartTitle.setFont(titleFont);
    LegendTitle legend = chart.getLegend();
    legend.setVisible(false);

    // Create Kendrick mass plot Tab
    Platform.runLater(() -> {
      KendrickMassPlotTab newTab = new KendrickMassPlotTab(parameters, chartViewer);
      MZmineCore.getDesktop().addTab(newTab);
    });

    setStatus(TaskStatus.FINISHED);
    logger.info("Finished creating Kendrick mass plot of " + featureList);
  }

  /**
   * create 2D Kendrick mass plot
   */
  private JFreeChart create2DKendrickMassPlot() {

    if (zAxisLabel.equals("none")) {
      logger.info("Creating new 2D chart instance");
      appliedSteps++;

      // load dataset
      dataset2D = new KendrickMassPlotXYDataset(parameters);

      // create chart
      chart = ChartFactory.createScatterPlot(title, xAxisLabel, yAxisLabel, dataset2D,
          PlotOrientation.VERTICAL, true, true, true);

      XYPlot plot = (XYPlot) chart.getPlot();
      plot.setBackgroundPaint(
          MZmineCore.getConfiguration().getDefaultChartTheme().getPlotBackgroundPaint());
      plot.setDomainCrosshairPaint(
          MZmineCore.getConfiguration().getDefaultChartTheme().getCrosshairPaint());
      plot.setRangeCrosshairPaint(
          MZmineCore.getConfiguration().getDefaultChartTheme().getCrosshairPaint());
      plot.setDomainCrosshairVisible(true);
      plot.setRangeCrosshairVisible(true);
      appliedSteps++;

      // set axis
      NumberAxis domain = (NumberAxis) plot.getDomainAxis();
      NumberAxis range = (NumberAxis) plot.getRangeAxis();
      range.setRange(0, 1);
      if (xAxisLabel.contains("KMD")) {
        domain.setRange(0, 1);
      }

      // set renderer
      XYCirclePixelSizeRenderer renderer = new XYCirclePixelSizeRenderer();

      // set tooltip generator
      ScatterPlotToolTipGenerator tooltipGenerator =
          new ScatterPlotToolTipGenerator(xAxisLabel, yAxisLabel, zAxisLabel, rows);
      renderer.setSeriesToolTipGenerator(0, tooltipGenerator);
      plot.setRenderer(renderer);

      // set item label generator
      NameItemLabelGenerator generator = new NameItemLabelGenerator(rows);
      renderer.setDefaultItemLabelGenerator(generator);
      renderer.setDefaultItemLabelsVisible(false);
      renderer.setDefaultItemLabelFont(legendFont);
      renderer.setDefaultPositiveItemLabelPosition(new ItemLabelPosition(ItemLabelAnchor.CENTER,
          TextAnchor.TOP_RIGHT, TextAnchor.TOP_RIGHT, -45), true);
    }
    return chart;
  }

  /**
   * create 3D Kendrick mass plot
   */
  private JFreeChart create3DKendrickMassPlot() {

    logger.info("Creating new 3D chart instance");
    appliedSteps++;
    // load dataseta
    dataset3D = new KendrickMassPlotXYZDataset(parameters);

    // copy and sort z-Values for min and max of the paint scale
    double[] copyZValues = new double[dataset3D.getItemCount(0)];
    for (int i = 0; i < dataset3D.getItemCount(0); i++) {
      copyZValues[i] = dataset3D.getZValue(0, i);
    }
    Arrays.sort(copyZValues);
    // get index in accordance to percentile windows
    int minScaleIndex = 0;
    int maxScaleIndex = copyZValues.length - 1;
    double min = 0;
    double max = 0;

    if (zAxisScaleType.equals("percentile")) {
      minScaleIndex = (int) Math.ceil(copyZValues.length * (zScaleRange.lowerEndpoint() / 100));
      maxScaleIndex = copyZValues.length
          - (int) (Math.ceil(copyZValues.length * ((100 - zScaleRange.upperEndpoint()) / 100)));
      if (zScaleRange.upperEndpoint() == 100) {
        maxScaleIndex = copyZValues.length - 1;
      }
      if (zScaleRange.lowerEndpoint() == 0) {
        minScaleIndex = 0;
      }
      min = copyZValues[minScaleIndex];
      max = copyZValues[maxScaleIndex];
    }
    if (zAxisScaleType.equals("custom")) {
      min = zScaleRange.lowerEndpoint();
      max = zScaleRange.upperEndpoint();
    }

    Paint[] contourColors =
        XYBlockPixelSizePaintScales.getPaintColors(zAxisScaleType, zScaleRange, paintScaleStyle);
    LookupPaintScale scale = null;
    scale = new LookupPaintScale(copyZValues[0], copyZValues[copyZValues.length - 1],
        new Color(0, 0, 0));
    double[] scaleValues = new double[contourColors.length];
    double delta = (max - min) / (contourColors.length - 1);
    double value = min;
    for (int i = 0; i < contourColors.length; i++) {
      scale.add(value, contourColors[i]);
      scaleValues[i] = value;
      value = value + delta;
    }

    // create chart
    chart = ChartFactory.createScatterPlot(title, xAxisLabel, yAxisLabel, dataset3D,
        PlotOrientation.VERTICAL, true, true, true);
    XYPlot plot = chart.getXYPlot();

    // set axis
    NumberAxis domain = (NumberAxis) plot.getDomainAxis();
    NumberAxis range = (NumberAxis) plot.getRangeAxis();
    range.setRange(0, 1);
    if (xAxisLabel.contains("KMD")) {
      domain.setRange(0, 1);
    }
    // set renderer
    XYCirclePixelSizeRenderer renderer = new XYCirclePixelSizeRenderer();
    appliedSteps++;

    // Set paint scale
    renderer.setPaintScale(scale);

    ScatterPlotToolTipGenerator tooltipGenerator =
        new ScatterPlotToolTipGenerator(xAxisLabel, yAxisLabel, zAxisLabel, rows);
    renderer.setSeriesToolTipGenerator(0, tooltipGenerator);

    // set item label generator
    NameItemLabelGenerator generator = new NameItemLabelGenerator(rows);
    renderer.setDefaultItemLabelGenerator(generator);
    renderer.setDefaultItemLabelsVisible(false);
    renderer.setDefaultItemLabelFont(legendFont);
    renderer.setDefaultPositiveItemLabelPosition(new ItemLabelPosition(ItemLabelAnchor.CENTER,
        TextAnchor.TOP_RIGHT, TextAnchor.TOP_RIGHT, -45), true);

    plot.setRenderer(renderer);
    plot.setBackgroundPaint(
        MZmineCore.getConfiguration().getDefaultChartTheme().getPlotBackgroundPaint());
    plot.setRangeGridlinePaint(
        MZmineCore.getConfiguration().getDefaultChartTheme().getPlotBackgroundPaint());
    plot.setAxisOffset(new RectangleInsets(5, 5, 5, 5));
    plot.setOutlinePaint(
        MZmineCore.getConfiguration().getDefaultChartTheme().getPlotOutlinePaint());
    plot.setDomainCrosshairPaint(
        MZmineCore.getConfiguration().getDefaultChartTheme().getCrosshairPaint());
    plot.setRangeCrosshairPaint(
        MZmineCore.getConfiguration().getDefaultChartTheme().getCrosshairPaint());
    plot.setDomainCrosshairVisible(true);
    plot.setRangeCrosshairVisible(true);

    // Legend
    NumberAxis scaleAxis = new NumberAxis(zAxisLabel);
    scaleAxis.setRange(min, max);
    scaleAxis.setAxisLinePaint(Color.white);
    scaleAxis.setTickMarkPaint(Color.white);
    PaintScaleLegend legend = new PaintScaleLegend(scale, scaleAxis);

    legend.setBackgroundPaint(
        MZmineCore.getConfiguration().getDefaultChartTheme().getChartBackgroundPaint());
    legend.setStripOutlineVisible(false);
    legend.setAxisLocation(AxisLocation.BOTTOM_OR_LEFT);
    legend.setAxisOffset(5.0);
    legend.setMargin(new RectangleInsets(5, 5, 5, 5));
    legend.setFrame(new BlockBorder(
        MZmineCore.getConfiguration().getDefaultChartTheme().getChartBackgroundPaint()));
    legend.setPadding(new RectangleInsets(10, 10, 10, 10));
    legend.setStripWidth(10);
    legend.setPosition(RectangleEdge.LEFT);
    legend.getAxis().setLabelFont(legendFont);
    legend.getAxis().setTickLabelFont(legendFont);
    chart.addSubtitle(legend);

    return chart;
  }

}
