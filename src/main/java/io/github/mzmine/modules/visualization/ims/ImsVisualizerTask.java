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

package io.github.mzmine.modules.visualization.ims;
/*
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYZDataset;
import com.google.common.collect.Range;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.gui.chartbasics.chartgroups.ChartGroup;
import io.github.mzmine.gui.chartbasics.chartutils.paintscales.PaintScale;
import io.github.mzmine.gui.chartbasics.gui.wrapper.ChartViewWrapper;
import io.github.mzmine.gui.preferences.MZminePreferences;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.ims.imsvisualizer.DataFactory;
import io.github.mzmine.modules.visualization.ims.imsvisualizer.IntensityMobilityPlot;
import io.github.mzmine.modules.visualization.ims.imsvisualizer.IntensityMobilityXYDataset;
import io.github.mzmine.modules.visualization.ims.imsvisualizer.MzMobilityHeatMapPlot;
import io.github.mzmine.modules.visualization.ims.imsvisualizer.MzMobilityXYZDataset;
import io.github.mzmine.modules.visualization.ims.imsvisualizer.RetentionTimeIntensityPlot;
import io.github.mzmine.modules.visualization.ims.imsvisualizer.RetentionTimeIntensityXYDataset;
import io.github.mzmine.modules.visualization.ims.imsvisualizer.RetentionTimeMobilityHeatMapPlot;
import io.github.mzmine.modules.visualization.ims.imsvisualizer.RetentionTimeMobilityXYZDataset;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class ImsVisualizerTask extends AbstractTask {

  private final Logger logger = Logger.getLogger(this.getClass().getName());

  private XYDataset datasetIntensityMobility;
  private XYZDataset datasetMzMobility;
  private final RawDataFile[] dataFiles;
  private final Range<Double> mzRange;
  private final ParameterSet parameterSet;
  private int appliedSteps = 0;
  private double selectedRetentionTime = 0.0;
  private ImsVisualizerWindowController controller;
  private DataFactory dataFactory;
  private MzMobilityHeatMapPlot mzMobilityHeatMapPlot;
  private IntensityMobilityPlot intensityMobilityPlot;
  private RetentionTimeMobilityHeatMapPlot retentionTimeMobilityHeatMapPlot;
  private RetentionTimeIntensityPlot retentionTimeIntensityPlot;
  private final PaintScale paintScaleParameter;
  private List<Scan> selectedScans;
  private BorderPane bottomRightpane;
  private BorderPane bottomLeftPane;
  private BorderPane topLeftPane;
  private BorderPane topRightPane;
  private static Label rtLabel;
  private static Label mzRangeLevel;
  private final Scan[] scans;
  private boolean containsMobility = true;


  public ImsVisualizerTask(ParameterSet parameters) {
    dataFiles = parameters.getParameter(ImsVisualizerParameters.dataFiles).getValue()
        .getMatchingRawDataFiles();

    mzRange = parameters.getParameter(ImsVisualizerParameters.mzRange).getValue();

    paintScaleParameter = parameters.getParameter(ImsVisualizerParameters.paintScale).getValue();

    parameterSet = parameters;
    scans = parameters.getParameter(ImsVisualizerParameters.scanSelection).getValue()
        .getMatchingScans(dataFiles[0]);
    for (int i = 0; i < scans.length; i++) {
      if (scans[i].getMobility() < 0) {
        containsMobility = false;
        break;
      }
    }
  }

  // Group the intensity-mobility and mz-mobility plots and place on the bottom
  private ChartGroup groupMobility = new ChartGroup(false, false, false, true);

  ChartGroup groupRetentionTime = new ChartGroup(false, false, true, false);

  @Override
  public String getTaskDescription() {
    return "Create IMS visualization of " + dataFiles[0];
  }

  @Override
  public double getFinishedPercentage() {
    int totalSteps = 3;
    return totalSteps == 0 ? 0 : (double) appliedSteps / totalSteps;
  }

  @Override
  public void run() {

    setStatus(TaskStatus.PROCESSING);
    logger.info("IMS visualization of " + dataFiles[0]);
    // Task canceled?
    if (isCanceled()) {
      return;
    }
    Platform.runLater(() -> {
      if (!containsMobility) {
        MZmineCore.getDesktop()
            .displayErrorMessage("The selected raw data does not have a mobility dimension.");
        return;
      }
      // Initialize dataFactories.
      initDataFactories();

      // Initialize Scene.
      initGui();

      setContainers();


      // Init all four plots
      initIntensityMobilityGui();
      initmzMobilityGui();
      initRetentionTimeMobilityGui();

      initRetentionTimeIntensityGui();
      initLabel();

      updateRTlabel();

    });

    setStatus(TaskStatus.FINISHED);
    logger.info("Finished IMS visualization of" + dataFiles[0]);
  }

  public void setGroupMobility() {
    groupMobility.add((new ChartViewWrapper(intensityMobilityPlot)));
    groupMobility.add(new ChartViewWrapper(mzMobilityHeatMapPlot));
  }

  public void setGroupRetentionTime() {
    // Group the rt-intensity-mobility and rt-mobility plots and place on the top
    groupRetentionTime.add(new ChartViewWrapper(retentionTimeIntensityPlot));
    groupRetentionTime.add(new ChartViewWrapper(retentionTimeMobilityHeatMapPlot));
  }

  public void initDataFactories() {
    appliedSteps++;
    // initialize data factory for the plots data.
    dataFactory = new DataFactory(parameterSet, 0f, this);
  }

  public void initmzMobilityGui() {
    appliedSteps++;
    datasetMzMobility = new MzMobilityXYZDataset(dataFactory);
    mzMobilityHeatMapPlot = new MzMobilityHeatMapPlot(datasetMzMobility,
        createPaintScale(dataFactory.getIntensityMzMobility()), this, intensityMobilityPlot);
    bottomRightpane.setCenter(mzMobilityHeatMapPlot);
  }

  public void initIntensityMobilityGui() {
    appliedSteps++;
    datasetIntensityMobility = new IntensityMobilityXYDataset(dataFactory);
    intensityMobilityPlot = new IntensityMobilityPlot(datasetIntensityMobility, this);
    bottomLeftPane.setCenter(intensityMobilityPlot);
  }


  public void setContainers() {
    appliedSteps++;
    bottomLeftPane = controller.getBottomLeftPane();
    bottomRightpane = controller.getBottomRightPane();
    topLeftPane = controller.getTopLeftPane();
    topRightPane = controller.getTopRightPane();
  }

  public void setTopLeftPane(BorderPane borderPane) {
    topLeftPane = borderPane;
  }

  public void setTopRightPane(BorderPane borderPane) {
    topRightPane = borderPane;
  }

  public void setBottomRightpane(BorderPane borderPane) {
    bottomRightpane = borderPane;
  }

  public void setBottomLeftPane(BorderPane borderPane) {
    bottomLeftPane = borderPane;
  }

  public void initRetentionTimeMobilityGui() {

    XYZDataset dataset3d = new RetentionTimeMobilityXYZDataset(dataFactory);
    retentionTimeMobilityHeatMapPlot = new RetentionTimeMobilityHeatMapPlot(dataset3d,
        createPaintScale(dataFactory.getIntensityretentionTimeMobility()));
    topRightPane.setCenter(retentionTimeMobilityHeatMapPlot);
  }

  public void initRetentionTimeIntensityGui() {
    appliedSteps++;
    XYDataset datasetRetentionTimeIntensity = new RetentionTimeIntensityXYDataset(dataFactory);
    retentionTimeIntensityPlot = new RetentionTimeIntensityPlot(datasetRetentionTimeIntensity, this,
        retentionTimeMobilityHeatMapPlot);
    topLeftPane.setCenter(retentionTimeIntensityPlot);
  }


  public void initGui() {
    appliedSteps++;
    FXMLLoader loader = new FXMLLoader((getClass().getResource("ImsVisualizerWindow.fxml")));
    Stage stage = new Stage();

    try {
      VBox root = (VBox) loader.load();
      Scene scene = new Scene(root);
      stage.setScene(scene);
      logger.finest("Stage has been successfully loaded from the FXML loader.");

      stage.setTitle("IMS of " + dataFiles[0] + "m/z Range " + mzRange);
      stage.show();
      stage.setMinWidth(stage.getWidth());
      stage.setMinHeight(stage.getHeight());
    } catch (IOException e) {
      e.printStackTrace();
      return;
    }
    // Get controller
    controller = loader.getController();
  }

  public void updateMobilityGroup() {
    dataFactory.updateFrameData(selectedRetentionTime);
    datasetMzMobility = new MzMobilityXYZDataset(dataFactory);

    mzMobilityHeatMapPlot = new MzMobilityHeatMapPlot(datasetMzMobility,
        createPaintScale(dataFactory.getIntensityMzMobility()), this, intensityMobilityPlot);
    bottomRightpane.setCenter(mzMobilityHeatMapPlot);

    datasetIntensityMobility = new IntensityMobilityXYDataset(dataFactory);
    intensityMobilityPlot = new IntensityMobilityPlot(datasetIntensityMobility, this);
    bottomLeftPane.setCenter(intensityMobilityPlot);

    groupMobility.add(new ChartViewWrapper(intensityMobilityPlot));
    groupMobility.add(new ChartViewWrapper(mzMobilityHeatMapPlot));
    initLabel();
    updateRTlabel();
  }

  public void initLabel() {
    rtLabel = controller.getRtLabel();
    mzRangeLevel = controller.getMobilityRTLabel();
  }

  public void setMzRangeLevel(Label level) {
    mzRangeLevel = level;
  }

  public void setRtLabel(Label label) {
    rtLabel = label;
  }

  public void updateRTlabel() {
    rtLabel.setText(
        "RT: " + MZminePreferences.rtFormat.getValue().format(selectedRetentionTime) + " min");

    // set the label for retentiontime-mobility plot.
    mzRangeLevel.setText("m/z: " + mzRange.lowerEndpoint() + " - " + mzRange.upperEndpoint());
  }

  private PaintScale createPaintScale(Double[] zValues) {
    Double[] zValuesCopy = Arrays.copyOf(zValues, zValues.length);
    Arrays.sort(zValuesCopy);
    Range<Double> zValueRange = Range.closed(zValuesCopy[0], zValuesCopy[zValues.length - 1]);
    return new PaintScale(paintScaleParameter.getPaintScaleColorStyle(),
        paintScaleParameter.getPaintScaleBoundStyle(), zValueRange);
  }

  public void setSelectedRetentionTime(double retentionTime) {
    this.selectedRetentionTime = retentionTime;
  }

  public double getSelectedRetentionTime() {
    return this.selectedRetentionTime;
  }

  public void setSelectedScans(List<Scan> selectedScans) {
    this.selectedScans = selectedScans;
  }

  public List<Scan> getSelectedScans() {
    return selectedScans;
  }

  public Scan[] getScans() {
    return scans;
  }

  public RawDataFile[] getDataFiles() {
    return dataFiles;
  }
}
*/