/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.visualization.ims;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.gui.chartbasics.chartgroups.ChartGroup;
import io.github.mzmine.gui.chartbasics.gui.wrapper.ChartViewWrapper;
import io.github.mzmine.gui.preferences.MZminePreferences;
import io.github.mzmine.modules.visualization.ims.imsVisualizer.*;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
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
  private double selectedRetentionTime = -1;
  private FXMLLoader loader;
  private ImsVisualizerWindowController controller;
  private ChartGroup groupMobility;

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

  public double getSelectedRetentionTime() {
    return this.selectedRetentionTime;
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
            Scene scene = new Scene(root);
            stage.setScene(scene);
            logger.finest("Stage has been successfully loaded from the FXML loader.");
          } catch (IOException e) {
            e.printStackTrace();
            return;
          }
          // Get controller
          controller = loader.getController();

          // add mobility-mz plot.
          datasetMF = new MzMobilityXYZDataset(parameterSet, selectedRetentionTime, this);
          MzMobilityPlotHeatMapPlot mzMobilityPlotHeatMapPlot =
              new MzMobilityPlotHeatMapPlot(datasetMF, paintScaleStyle, parameterSet);
          BorderPane bottomRightpane = controller.getBottomRightPane();
          bottomRightpane.setCenter(mzMobilityPlotHeatMapPlot);
          updateRTlebel();

          datasetMI = new IntensityMobilityXYDataset(parameterSet);
          IntensityMobilityPlot intensityMobilityPlot =
              new IntensityMobilityPlot(datasetMI, parameterSet);
          BorderPane bottomLeftPane = controller.getBottomLeftPane();
          bottomLeftPane.setCenter(intensityMobilityPlot);

          groupMobility = new ChartGroup(false, true, false, true);
          groupMobility.setShowCrosshair(true, true);

          groupMobility.add((new ChartViewWrapper(intensityMobilityPlot)));
          groupMobility.add(new ChartViewWrapper(mzMobilityPlotHeatMapPlot));

          // add intensity retention time plot
          dataset3d = new RetentionTimeMobilityXYZDataset(parameterSet);
          RetentionTimeMobilityHeatMapPlot retentionTimeMobilityHeatMapPlot =
              new RetentionTimeMobilityHeatMapPlot(dataset3d, paintScaleStyle);
          BorderPane topRightPane = controller.getTopRightPane();
          topRightPane.setCenter(retentionTimeMobilityHeatMapPlot);

          // set the label for retentiontime-mobility plot.
          Label mobilityRTLabel = controller.getMobilityRTLabel();
          mobilityRTLabel.setText(
              "m/z: " + mzRange.lowerEndpoint() + " - " + mzRange.upperEndpoint());

          // add mobility-m/z plot to border
          BorderPane topLeftPane = controller.getTopLeftPane();
          datasetIRT = new RetentionTimeIntensityXYDataset(parameterSet);
          RetentionTimeIntensityPlot retentionTimeIntensityPlot =
              new RetentionTimeIntensityPlot(datasetIRT, this, retentionTimeMobilityHeatMapPlot);
          retentionTimeIntensityPlot.setLegend(retentionTimeMobilityHeatMapPlot.getLegend());
          topLeftPane.setCenter(retentionTimeIntensityPlot);

          ChartGroup groupRetentionTime = new ChartGroup(true, false, true, false);
          groupRetentionTime.setShowCrosshair(true, true);

          groupRetentionTime.add(new ChartViewWrapper(retentionTimeIntensityPlot));
          groupRetentionTime.add(new ChartViewWrapper(retentionTimeMobilityHeatMapPlot));

          stage.setTitle("IMS of " + dataFiles[0] + "m/z Range " + mzRange);
          stage.show();
          stage.setMinWidth(stage.getWidth());
          stage.setMinHeight(stage.getHeight());
        });

    setStatus(TaskStatus.FINISHED);
    logger.info("Finished IMS visualization of" + dataFiles[0]);
  }

  public void updateMobilityGroup() {
    datasetMF = new MzMobilityXYZDataset(parameterSet, selectedRetentionTime, this);
    BorderPane plotPaneMF = controller.getBottomRightPane();
    MzMobilityPlotHeatMapPlot mzMobilityPlotHeatMapPlot =
        new MzMobilityPlotHeatMapPlot(datasetMF, paintScaleStyle, parameterSet);
    plotPaneMF.setCenter(mzMobilityPlotHeatMapPlot);

    datasetMI = new IntensityMobilityXYDataset(parameterSet);
    IntensityMobilityPlot intensityMobilityPlot =
        new IntensityMobilityPlot(datasetMI, parameterSet);
    BorderPane bottomLeftPane = controller.getBottomLeftPane();
    bottomLeftPane.setCenter(intensityMobilityPlot);
    groupMobility.add(new ChartViewWrapper(mzMobilityPlotHeatMapPlot));
    groupMobility.add(new ChartViewWrapper(intensityMobilityPlot));
    updateRTlebel();
  }

  public void updateRTlebel() {
    Label rtLabel = controller.getRtLabel();

    rtLabel.setText(
        "RT: " + MZminePreferences.rtFormat.getValue().format(selectedRetentionTime) + " min");
  }
}
