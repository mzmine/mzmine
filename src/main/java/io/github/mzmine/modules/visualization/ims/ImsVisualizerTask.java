package io.github.mzmine.modules.visualization.ims;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.gui.chartbasics.chartutils.XYBlockPixelSizePaintScales;
import io.github.mzmine.gui.chartbasics.chartutils.XYBlockPixelSizeRenderer;
import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.LookupPaintScale;
import org.jfree.chart.renderer.xy.XYBlockRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.PaintScaleLegend;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYZDataset;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Logger;

public class ImsVisualizerTask extends AbstractTask {

  static final Font legendFont = new Font("SansSerif", Font.PLAIN, 12);
  static final Font titleFont = new Font("SansSerif", Font.PLAIN, 12);

  private Logger logger = Logger.getLogger(this.getClass().getName());

  private XYDataset datasetMI;
  private XYDataset datasetMMZ;
  private XYDataset datasetIRT;
  private XYZDataset dataset3d;
  private XYZDataset datasetMF;
  private JFreeChart chartMI;
  private JFreeChart chartMMZ;
  private JFreeChart chart3dMF;
  private JFreeChart chartIRT;
  private JFreeChart chartHeatMap;
  private RawDataFile dataFiles[];
  private Scan scans[];
  private Range<Double> mzRange;
  private ParameterSet parameterSet;
  private int totalSteps = 3, appliedSteps = 0;
  private String paintScaleStyle;
  private JFreeChart chart3d;

  public ImsVisualizerTask(ParameterSet parameters) {
    dataFiles =
        parameters
            .getParameter(ImsVisualizerParameters.dataFiles)
            .getValue()
            .getMatchingRawDataFiles();

    scans =
        parameters
            .getParameter(ImsVisualizerParameters.scanSelection)
            .getValue()
            .getMatchingScans(dataFiles[0]);

    mzRange = parameters.getParameter(ImsVisualizerParameters.mzRange).getValue();

    paintScaleStyle = parameters.getParameter(ImsVisualizerParameters.paintScale).getValue();

    parameterSet = parameters;
  }

  @Override
  public String getTaskDescription() {
    return "Create IMS visualization of " + dataFiles[0];
  }

  @Override
  public double getFinishedPercentage() {
    return totalSteps == 0 ? 0 : (double) appliedSteps / totalSteps;
  }

  @Override
  public void run() {

    setStatus(TaskStatus.PROCESSING);
    logger.info("IMS visualization of " + dataFiles[0]);
    // Task canceled?
    if (isCanceled()) return;
    // Create Mobility Intesity plot Window
    chart3dMF = createPlotMF();
    EChartViewer eChartViewerMF = new EChartViewer(chart3dMF, true, true, true, true, false);

    // Create Mobility Intesity plot Window
    chartMI = createPlotMI();
    EChartViewer eChartViewerMI = new EChartViewer(chartMI, true, true, true, true, false);

    // create intensity retention time plot
    chartIRT = createPlotIRT();
    EChartViewer eChartViewerIRT = new EChartViewer(chartIRT, true, true, true, true, false);

    // create heatmap plot
    chartHeatMap = createPlot3D();
    EChartViewer eChartViewerHeatMap =
        new EChartViewer(chartHeatMap, true, true, true, true, false);

    // Create ims plot Window
    Platform.runLater(
        () -> {
          FXMLLoader loader = new FXMLLoader((getClass().getResource("ImsVisualizerWindow.fxml")));
          Stage stage = new Stage();

          try {
            AnchorPane root = (AnchorPane) loader.load();
            Scene scene = new Scene(root, 1028, 800);
            stage.setScene(scene);
            logger.finest("Stage has been successfully loaded from the FXML loader.");
          } catch (IOException e) {
            e.printStackTrace();
            return;
          }
          // Get controller
          ImsVisualizerWindowController controller = loader.getController();

          // add mobility-mz plot.
          BorderPane plotPaneMF = controller.getPlotPaneMF();
          plotPaneMF.setCenter(eChartViewerMF);

          // add mobility-intensity plot.
          BorderPane plotPaneMI = controller.getPlotPaneMI();
          plotPaneMI.setCenter(eChartViewerMI);

          // add mobility-m/z plot to border
          BorderPane plotePaneIRT = controller.getPlotePaneIRT();
          plotePaneIRT.setCenter(eChartViewerIRT);

          // add intensity retention time plot
          BorderPane heatmap = controller.getPlotePaneHeatMap();
          heatmap.setCenter(eChartViewerHeatMap);

          stage.setTitle("IMS of " + dataFiles[0] + "m/z Range " + mzRange);
          stage.show();
          stage.setMinWidth(stage.getWidth());
          stage.setMinHeight(stage.getHeight());
        });

    setStatus(TaskStatus.FINISHED);
    logger.info("Finished IMS visualization of" + dataFiles[0]);
  }

  /** Mobility Intensity plot */
  private JFreeChart createPlotMI() {

    logger.info("Creating new IMS chart instance");
    appliedSteps++;

    // load dataseta for IMS and XIC
    datasetMI = new ImsVisualizerXYDataset(parameterSet);
    String xAxisLabel = "mobility";
    String yAxisLabel = "intensity";
    JFreeChart chart =
        ChartFactory.createXYLineChart(
            "Mobility-intensity plot", xAxisLabel, yAxisLabel, datasetMI, PlotOrientation.VERTICAL, true, true, false);
    XYPlot plot = chart.getXYPlot();

    var renderer = new XYLineAndShapeRenderer();
    appliedSteps++;
    renderer.setSeriesPaint(0, Color.GREEN);
    renderer.setSeriesStroke(0, new BasicStroke(.01f));

    plot.setRenderer(renderer);
    plot.setBackgroundPaint(Color.BLACK);

    chart.getLegend().setFrame(BlockBorder.NONE);

    appliedSteps++;
    return chart;
  }
  /** Mobility Intensity plot */
  private JFreeChart createPlotMMZ() {

    logger.info("Creating new IMS chart instance");
    appliedSteps++;

    // load dataseta for IMS and XIC
    datasetMMZ = new ImsVisualizerMMZXYDataset(parameterSet);
    String xAxisLabel = "mobility";
    String yAxisLabel = "m/z";
    JFreeChart chart =
        ChartFactory.createXYLineChart(
            null, xAxisLabel, yAxisLabel, datasetMMZ, PlotOrientation.VERTICAL, true, true, false);
    XYPlot plot = chart.getXYPlot();

    var renderer = new XYLineAndShapeRenderer();
    appliedSteps++;
    renderer.setSeriesPaint(0, Color.GREEN);
    renderer.setSeriesStroke(0, new BasicStroke(.01f));

    plot.setRenderer(renderer);
    plot.setBackgroundPaint(Color.BLACK);

    chart.getLegend().setFrame(BlockBorder.NONE);

    appliedSteps++;
    return chart;
  }

  /** Intensity retentTime plot */
  private JFreeChart createPlotIRT() {

    logger.info("Creating new IMS chart instance");
    appliedSteps++;

    // load dataseta for IMS and XIC
    datasetIRT = new ImsVisualizerIRTXYDataset(parameterSet);
    String xAxisLabel = "retention time";
    String yAxisLabel = "intensity";
    JFreeChart chart =
        ChartFactory.createXYLineChart(
            null, xAxisLabel, yAxisLabel, datasetIRT, PlotOrientation.VERTICAL, true, true, false);
    XYPlot plot = chart.getXYPlot();

    var renderer = new XYLineAndShapeRenderer();
    appliedSteps++;
    renderer.setSeriesPaint(0, Color.GREEN);
    renderer.setSeriesStroke(0, new BasicStroke(2.0f));

    plot.setRenderer(renderer);
    plot.setBackgroundPaint(Color.BLACK);

    chart.getLegend().setFrame(BlockBorder.NONE);

    appliedSteps++;
    return chart;
  }

  /** Intensity retentTime plot */
  private JFreeChart createPlot3D() {

    logger.info("Creating new IMS chart instance");
    appliedSteps++;

    String xAxisLabel = "retention time";
    String yAxisLabel = "mobility";

    // load dataseta for IMS and XIC
    dataset3d = new ImsVisualizerXYZDataset(parameterSet);

    // copy and sort z-Values for min and max of the paint scale
    double[] copyZValues = new double[dataset3d.getItemCount(0)];
    for (int i = 0; i < dataset3d.getItemCount(0); i++) {
      copyZValues[i] = dataset3d.getZValue(0, i);
    }
    Arrays.sort(copyZValues);

    // copy and sort x-values.
    double[] copyXValues = new double[dataset3d.getItemCount(0)];
    for (int i = 0; i < dataset3d.getItemCount(0); i++) {
      copyXValues[i] = dataset3d.getXValue(0, i);
    }
    Arrays.sort(copyXValues);

    // copy and sort y-values.
    double[] copyYValues = new double[dataset3d.getItemCount(0)];
    for (int i = 0; i < dataset3d.getItemCount(0); i++) {
      copyYValues[i] = dataset3d.getYValue(0, i);
    }
    Arrays.sort(copyYValues);

    // get index in accordance to percentile windows
    int minIndexScale = 0;
    int maxIndexScale = copyZValues.length - 1;
    double min = copyZValues[minIndexScale];
    double max = copyZValues[maxIndexScale];

    Paint[] contourColors =
        XYBlockPixelSizePaintScales.getPaintColors(
            "percentile", Range.open(min, max), paintScaleStyle);
    LookupPaintScale scale = new LookupPaintScale(min, max, Color.BLACK);

    double[] scaleValues = new double[contourColors.length];
    double delta = (max - min) / (contourColors.length - 1);
    double value = min;
    for (int i = 0; i < contourColors.length; i++) {
      scaleValues[i] = value;
      scale.add(value, contourColors[i]);
      value = value + delta;
    }
    // create chart
    chart3d =
        ChartFactory.createScatterPlot(
            "Heatmap plot mobility-rt", xAxisLabel, yAxisLabel, dataset3d, PlotOrientation.VERTICAL, true, true, true);

    XYPlot plot = chart3d.getXYPlot();

    // set the pixel renderer
   XYBlockPixelSizeRenderer pixelRenderer = new XYBlockPixelSizeRenderer();
   pixelRenderer.setPaintScale(scale);
    // set the block renderer
    XYBlockRenderer blockRenderer = new XYBlockRenderer();
    double retentionWidth = 0.0;
    double mobilityWidth = 0.0;

    for (int i = 0; i + 1 < copyXValues.length; i++) {
      if (copyXValues[i] != copyXValues[i + 1]) {
        retentionWidth = copyXValues[i + 1] - copyXValues[i];
        break;
      }
    }
    for (int i = 0; i + 1 < copyYValues.length; i++) {
      if (copyYValues[i] != copyYValues[i + 1]) {
        mobilityWidth = copyYValues[i + 1] - copyYValues[i];
        break;
      }
    }

    if (mobilityWidth <= 0.0 || retentionWidth <= 0.0) {
      throw new IllegalArgumentException(
          "there must be atleast two unique value of retentio time and mobility");
    }
    blockRenderer.setBlockHeight(mobilityWidth);
    blockRenderer.setBlockWidth(retentionWidth);

    appliedSteps++;

    // Legend
    NumberAxis scaleAxis = new NumberAxis("Intensity");
    scaleAxis.setRange(min, max);
    scaleAxis.setAxisLinePaint(Color.white);
    scaleAxis.setTickMarkPaint(Color.white);
    PaintScaleLegend legend = new PaintScaleLegend(scale, scaleAxis);

    legend.setStripOutlineVisible(false);
    legend.setAxisLocation(AxisLocation.BOTTOM_OR_LEFT);
    legend.setAxisOffset(5.0);
    legend.setMargin(new RectangleInsets(5, 5, 5, 5));
    legend.setFrame(new BlockBorder(Color.white));
    legend.setPadding(new RectangleInsets(10, 10, 10, 10));
    legend.setStripWidth(10);
    legend.setPosition(RectangleEdge.LEFT);
    legend.getAxis().setLabelFont(legendFont);
    legend.getAxis().setTickLabelFont(legendFont);

    // Set paint scale
    blockRenderer.setPaintScale(scale);

    plot.setRenderer(blockRenderer);
    plot.setBackgroundPaint(Color.black);
    plot.setRangeGridlinePaint(Color.black);
    plot.setAxisOffset(new RectangleInsets(5, 5, 5, 5));
    plot.setOutlinePaint(Color.black);
    plot.setDomainCrosshairPaint(Color.GRAY);
    plot.setRangeCrosshairPaint(Color.GRAY);
    plot.setDomainCrosshairVisible(true);
    plot.setRangeCrosshairVisible(true);

    chart3d.addSubtitle(legend);

    return chart3d;
  }

  /** Mobility mz plot for one frame.* */
  private JFreeChart createPlotMF() {
    logger.info("Creating new Mobility frame chart instance");
    appliedSteps++;
    String xAxisLabel = "m/z";
    String yAxisLabel = "mobility";

    // load dataseta for IMS and XIC
    datasetMF = new ImsVisualizerMFXYZDataset(parameterSet);

    // copy and sort z-Values for min and max of the paint scale
    double[] copyZValues = new double[datasetMF.getItemCount(0)];
    for (int i = 0; i < datasetMF.getItemCount(0); i++) {
      copyZValues[i] = datasetMF.getZValue(0, i);
    }
    Arrays.sort(copyZValues);

    // copy and sort x-values.
    double[] copyXValues = new double[datasetMF.getItemCount(0)];
    for (int i = 0; i < datasetMF.getItemCount(0); i++) {
      copyXValues[i] = datasetMF.getXValue(0, i);
    }
    Arrays.sort(copyXValues);

    // copy and sort y-values.
    double[] copyYValues = new double[datasetMF.getItemCount(0)];
    for (int i = 0; i < datasetMF.getItemCount(0); i++) {
      copyYValues[i] = datasetMF.getYValue(0, i);
    }
    Arrays.sort(copyYValues);

    // get index in accordance to percentile windows
    int minIndexScale = 0;
    int maxIndexScale = copyZValues.length - 1;
    double min = copyZValues[minIndexScale];
    double max = copyZValues[maxIndexScale];

    Paint[] contourColors =
        XYBlockPixelSizePaintScales.getPaintColors(
            "percentile", Range.closed(min, max), paintScaleStyle);
    LookupPaintScale scale = new LookupPaintScale(min, max, Color.BLACK);

    double[] scaleValues = new double[contourColors.length];
    double delta = (max - min) / (contourColors.length - 1);
    double value = min;
    for (int i = 0; i < contourColors.length; i++) {

      scaleValues[i] = value;
      scale.add(value, contourColors[i]);
      value = value + delta;
    }

    // create chart
    chart3dMF =
        ChartFactory.createScatterPlot(
            null, xAxisLabel, yAxisLabel, datasetMF, PlotOrientation.VERTICAL, true, true, true);

    XYPlot plot = chart3dMF.getXYPlot();

    // set the block renderer renderer
    XYBlockRenderer renderer = new XYBlockRenderer();
    double mzWidth = 0.0;
    double mobilityWidth = 0.0;

    for (int i = 0; i + 1 < copyXValues.length; i++) {
      if (copyXValues[i] != copyXValues[i + 1]) {
        mobilityWidth = copyXValues[i + 1] - copyXValues[i];
        break;
      }
    }
    ArrayList<Double> deltas = new ArrayList<>();
    for (int i = 0; i + 1 < copyYValues.length; i++) {
      if (copyYValues[i] != copyYValues[i + 1]) {
        deltas.add(copyYValues[i+1] - copyYValues[i]);
      }
    }

    for(int i = 0; i < deltas.size(); i++ )
    {
      mzWidth += deltas.get(i);
    }

    mzWidth = deltas.size() > 0 ?mzWidth/deltas.size() : 0.0;

    if (mobilityWidth <= 0.0 || mzWidth <= 0.0) {
      throw new IllegalArgumentException(
          "there must be atleast two unique value of retentio time and mobility");
    }

    renderer.setBlockHeight(mobilityWidth);
    renderer.setBlockWidth(mzWidth);
    appliedSteps++;

    // Legend
    NumberAxis scaleAxis = new NumberAxis("Intensity");
    scaleAxis.setRange(min, max);
    scaleAxis.setAxisLinePaint(Color.white);
    scaleAxis.setTickMarkPaint(Color.white);
    PaintScaleLegend legend = new PaintScaleLegend(scale, scaleAxis);

    legend.setStripOutlineVisible(false);
    legend.setAxisLocation(AxisLocation.BOTTOM_OR_LEFT);
    legend.setAxisOffset(5.0);
    legend.setMargin(new RectangleInsets(5, 5, 5, 5));
    legend.setFrame(new BlockBorder(Color.white));
    legend.setPadding(new RectangleInsets(10, 10, 10, 10));
    legend.setStripWidth(10);
    legend.setPosition(RectangleEdge.LEFT);
    legend.getAxis().setLabelFont(legendFont);
    legend.getAxis().setTickLabelFont(legendFont);

    // Set paint scale
    renderer.setPaintScale(scale);

    plot.setRenderer(renderer);
    plot.setBackgroundPaint(Color.black);
    plot.setRangeGridlinePaint(Color.black);
    plot.setAxisOffset(new RectangleInsets(5, 5, 5, 5));
    plot.setOutlinePaint(Color.black);
    plot.setDomainCrosshairPaint(Color.GRAY);
    plot.setRangeCrosshairPaint(Color.GRAY);
    plot.setDomainCrosshairVisible(true);
    plot.setRangeCrosshairVisible(true);

    chart3dMF.addSubtitle(legend);

    return chart3dMF;
  }
}
