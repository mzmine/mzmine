package io.github.mzmine.modules.visualization.ims;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYZDataset;

import java.awt.*;
import java.io.IOException;
import java.util.logging.Logger;

public class ImsVisualizerTask extends AbstractTask {

  static final Font legendFont = new Font("SansSerif", Font.PLAIN, 12);
  static final Font titleFont = new Font("SansSerif", Font.PLAIN, 12);

  private Logger logger = Logger.getLogger(this.getClass().getName());

  private XYDataset datasetMI;
  private XYDataset datasetIRT;
  private XYZDataset dataset3d;
  private XYZDataset datasetMF;
  private RawDataFile dataFiles[];
  private Scan scans[];
  private Range<Double> mzRange;
  private ParameterSet parameterSet;
  private int totalSteps = 3, appliedSteps = 0;
  private String paintScaleStyle;
  private double selectedRetentionTime = 21.5631;
  private FXMLLoader loader;
  private ImsVisualizerWindowController controller;

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

  public void setSelectedRetentionTime(double retentionTime) {
    this.selectedRetentionTime = retentionTime;
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
    Platform.runLater(
        () -> {
          loader = new FXMLLoader((getClass().getResource("ImsVisualizerWindow.fxml")));
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
          controller = loader.getController();

          // add mobility-mz plot.
          //          if (selectedRetentionTime >= 0.0) {
          //            datasetMF = new ImsVisualizerMFXYZDataset(parameterSet,
          // selectedRetentionTime);
          //            BorderPane plotPaneMF = controller.getPlotPaneMF();
          //            plotPaneMF.setCenter(new MzMobilityPlotHeatMap(datasetMF, paintScaleStyle));
          //          }

          // add mobility-intensity plot.
          BorderPane plotPaneMI = controller.getPlotPaneMI();
          datasetMI = new ImsVisualizerMIXYDataset(parameterSet);
          plotPaneMI.setCenter(new MobilityIntensityPlot(datasetMI));

          // add mobility-m/z plot to border
          BorderPane plotePaneIRT = controller.getPlotePaneIRT();
          datasetIRT = new ImsVisualizerIRTXYDataset(parameterSet);
          plotePaneIRT.setCenter(
              new IntensityRetentionTimePlot(datasetIRT, this));

          // add intensity retention time plot
          BorderPane heatmap = controller.getPlotePaneHeatMap();

          dataset3d = new ImsVisualizerXYZDataset(parameterSet);
          heatmap.setCenter(new MobilityRetentionHeatMap(dataset3d, paintScaleStyle));

          stage.setTitle("IMS of " + dataFiles[0] + "m/z Range " + mzRange);
          stage.show();
          stage.setMinWidth(stage.getWidth());
          stage.setMinHeight(stage.getHeight());
        });

    setStatus(TaskStatus.FINISHED);
    logger.info("Finished IMS visualization of" + dataFiles[0]);
  }

  public void runMoblityMZHeatMap() {
    datasetMF = new ImsVisualizerMFXYZDataset(parameterSet, selectedRetentionTime);
    BorderPane plotPaneMF = controller.getPlotPaneMF();
    plotPaneMF.setCenter(new MzMobilityPlotHeatMap(datasetMF, paintScaleStyle));
  }
}
