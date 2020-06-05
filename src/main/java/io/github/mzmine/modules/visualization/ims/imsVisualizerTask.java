package io.github.mzmine.modules.visualization.ims;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.gui.chartbasics.chartutils.XYBlockPixelSizePaintScales;
import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import io.github.mzmine.gui.chartbasics.gui.swing.EChartPanel;
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
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.LookupPaintScale;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.chart.renderer.xy.XYBlockRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.title.PaintScaleLegend;
import org.jfree.chart.title.TextTitle;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYZDataset;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Logger;

public class imsVisualizerTask extends AbstractTask {

    static final Font legendFont = new Font("SansSerif", Font.PLAIN, 12);
    static final Font titleFont = new Font("SansSerif", Font.PLAIN, 12);

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private XYDataset datasetXIC;
    private JFreeChart chart;
    private RawDataFile dataFiles[];
    private Scan scans[];
    private Range<Double> mzRange;
    private ParameterSet parameterSet;
    private int totalSteps = 3, appliedSteps = 0;

    public imsVisualizerTask (ParameterSet parameters)
    {
        dataFiles = parameters.getParameter(imsVisualizerParameters.dataFiles).getValue()
                .getMatchingRawDataFiles();

        scans = parameters.getParameter(imsVisualizerParameters.scanSelection).getValue()
                .getMatchingScans(dataFiles[0]);

        mzRange = parameters.getParameter(imsVisualizerParameters.mzRange).getValue();

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

        // Create IMS plot Window
        chart = createPlot();

        XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(Color.BLACK);
        plot.setDomainGridlinesVisible(true);
        // create chart EchartViewer.
        EChartViewer eChartViewer = new EChartViewer(chart, true, true, true, false, false);

        // Create ims plot Window
            Platform.runLater(()-> {
                FXMLLoader loader = new FXMLLoader((getClass().getResource("ImsVisualizerWindow.fxml")));
                Stage stage = new Stage();

                try {
                    AnchorPane root = (AnchorPane)loader.load();
                    Scene scene = new Scene(root, 700, 500);
                    stage.setScene(scene);
                    logger.finest("Stage has been successfully loaded from the FXML loader.");
                }
                catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
                 //Get controller
                ImsVisualizerWindowController controller = loader.getController();
                BorderPane plotPane = controller.getPlotPane();
                plotPane.setCenter(eChartViewer);

                stage.setTitle("IMS of " + dataFiles[0] + "m/z Range " + mzRange);
                stage.show();
                stage.setMinWidth(stage.getWidth());
                stage.setMinHeight(stage.getHeight());

            });

        setStatus(TaskStatus.FINISHED);
        logger.info("Finished IMS visualization of" + dataFiles[0]);
    }

    /**
     * IMS plot
     */
    private JFreeChart createPlot() {

        logger.info("Creating new IMS chart instance");
        appliedSteps++;

        // load dataseta for IMS and XIC
        datasetXIC = new imsVisualizerXYDataset(parameterSet);
        String xAxisLabel = "mobility";
        String yAxisLabel = "intensity";
        JFreeChart chart = ChartFactory.createXYLineChart(
                null,
                xAxisLabel,
                yAxisLabel,
                datasetXIC,
                PlotOrientation.VERTICAL,
                true,
                false,
                false
        );
        XYPlot plot = chart.getXYPlot();

        var renderer = new XYLineAndShapeRenderer();
        appliedSteps++;
        renderer.setSeriesPaint(0, Color.GREEN);
        renderer.setSeriesStroke(0, new BasicStroke(.01f));

        plot.setRenderer(renderer);
        plot.setBackgroundPaint(Color.BLACK);
//
//        plot.setRangeGridlinesVisible(true);
//        plot.setRangeGridlinePaint(Color.WHITE);
//
//        plot.setDomainGridlinePaint(Color.WHITE);
//        plot.setDomainGridlinesVisible(true);

        chart.getLegend().setFrame(BlockBorder.NONE);

        appliedSteps++;
        return chart;
    }
}
