package io.github.mzmine.modules.visualization.ims;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
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
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.awt.*;
import java.io.IOException;
import java.util.logging.Logger;

public class ImsVisualizerTask extends AbstractTask {

    static final Font legendFont = new Font("SansSerif", Font.PLAIN, 12);
    static final Font titleFont = new Font("SansSerif", Font.PLAIN, 12);

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private XYDataset datasetMI;
    private XYDataset datasetMMZ;
    private XYDataset datasetIRT;
    private JFreeChart chartMI;
    private JFreeChart chartMMZ;
    private JFreeChart chartIRT;
    private RawDataFile dataFiles[];
    private Scan scans[];
    private Range<Double> mzRange;
    private ParameterSet parameterSet;
    private int totalSteps = 3, appliedSteps = 0;

    public ImsVisualizerTask(ParameterSet parameters)
    {
        dataFiles = parameters.getParameter(ImsVisualizerParameters.dataFiles).getValue()
                .getMatchingRawDataFiles();

        scans = parameters.getParameter(ImsVisualizerParameters.scanSelection).getValue()
                .getMatchingScans(dataFiles[0]);

        mzRange = parameters.getParameter(ImsVisualizerParameters.mzRange).getValue();

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
        if (isCanceled())
            return;

        // Create Mobility Intesity plot Window
        chartMI = createPlotMI();

        EChartViewer eChartViewerMI  = new EChartViewer(chartMI, true, true, true, true, false);

        // create  mobility mz plot
        chartMMZ = createPlotMMZ();
        EChartViewer eChartViewerMMZ = new EChartViewer(chartMMZ, true, true, true, true, false);

        // create intensity rention time
        chartIRT  =  createPlotIRT();
        EChartViewer eChartViewerIRT = new EChartViewer(chartIRT, true, true, true, true, false);

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
          BorderPane plotPaneMI = controller.getPlotPaneMI();
          // add mobility-intensity plot.
          plotPaneMI.setCenter(eChartViewerMI);

          // add mobility-m/z plot to border
            BorderPane plotePaneMMZ = controller.getPlotPaneMMZ();
            plotePaneMMZ.setCenter(eChartViewerMMZ);

          // add intensity retention time plot
            BorderPane plotePaneIRT = controller.getPlotePaneIRT();
            plotePaneIRT.setCenter(eChartViewerIRT);

          stage.setTitle("IMS of " + dataFiles[0] + "m/z Range " + mzRange);
          stage.show();
          stage.setMinWidth(stage.getWidth());
          stage.setMinHeight(stage.getHeight());
        });

        setStatus(TaskStatus.FINISHED);
        logger.info("Finished IMS visualization of" + dataFiles[0]);
    }

    /**
     * Mobility Intensity plot
     */
    private JFreeChart createPlotMI() {

        logger.info("Creating new IMS chart instance");
        appliedSteps++;

        // load dataseta for IMS and XIC
        datasetMI = new ImsVisualizerXYDataset(parameterSet);
        String xAxisLabel = "mobility";
        String yAxisLabel = "intensity";
        JFreeChart chart = ChartFactory.createXYLineChart(
                null,
                xAxisLabel,
                yAxisLabel,
                datasetMI,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );
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
    /**
     * Mobility Intensity plot
     */
    private JFreeChart createPlotMMZ() {

        logger.info("Creating new IMS chart instance");
        appliedSteps++;

        // load dataseta for IMS and XIC
        datasetMMZ = new ImsVisualizerMMZXYDataset(parameterSet);
        String xAxisLabel = "mobility";
        String yAxisLabel = "m/z";
        JFreeChart chart = ChartFactory.createXYLineChart(
                null,
                xAxisLabel,
                yAxisLabel,
                datasetMMZ,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );
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

    /**
     * Intensity retentTime plot
     */
    private JFreeChart createPlotIRT() {

        logger.info("Creating new IMS chart instance");
        appliedSteps++;

        // load dataseta for IMS and XIC
        datasetIRT = new ImsVisualizerIRTXYDataset(parameterSet);
        String xAxisLabel = "retention time";
        String yAxisLabel = "intensity";
        JFreeChart chart = ChartFactory.createXYLineChart(
                null,
                xAxisLabel,
                yAxisLabel,
                datasetIRT,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );
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
}
