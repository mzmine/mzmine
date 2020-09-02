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
import io.github.mzmine.modules.visualization.ims.imsvisualizer.*;
import io.github.mzmine.modules.visualization.rawdataoverview.RawDataOverviewIMSController;
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
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYZDataset;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class ImsVisualizerTask extends AbstractTask {

    private final Logger logger = Logger.getLogger(this.getClass().getName());

    private XYDataset datasetIntensityMobility;
    private XYZDataset datasetMzMobility;
    private final RawDataFile[] dataFiles;
    private final Range<Double> mzRange;
    private final ParameterSet parameterSet;
    private int appliedSteps = 0;
    private final String paintScaleStyle;
    private double selectedRetentionTime = 0.0;
    private ImsVisualizerWindowController controller;
    private DataFactory dataFactory;
    private MzMobilityHeatMapPlot mzMobilityHeatMapPlot;
    private IntensityMobilityPlot intensityMobilityPlot;
    private RetentionTimeMobilityHeatMapPlot retentionTimeMobilityHeatMapPlot;
    private RetentionTimeIntensityPlot retentionTimeIntensityPlot;
    private List<Scan> selectedScans;
    private BorderPane bottomRightpane;
    private BorderPane bottomLeftPane;
    private BorderPane topLeftPane;
    private BorderPane topRightPane;
    private static Label rtLabel;
    private static Label mzRangeLevel;
    private boolean isIonMobility = true;
    private final Scan[] scans;
    private RawDataOverviewIMSController controllerIMS;


    public ImsVisualizerTask(ParameterSet parameters) {
        dataFiles =
                parameters
                        .getParameter(ImsVisualizerParameters.dataFiles)
                        .getValue()
                        .getMatchingRawDataFiles();

        mzRange = parameters.getParameter(ImsVisualizerParameters.mzRange).getValue();

        paintScaleStyle = parameters.getParameter(ImsVisualizerParameters.paintScale).getValue();

        parameterSet = parameters;
        scans =
                parameters
                        .getParameter(ImsVisualizerParameters.scanSelection)
                        .getValue()
                        .getMatchingScans(dataFiles[0]);
        for(int i=0;i<scans.length;i++){
            if(scans[i].getMobility()<0){
                isIonMobility = false;
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
        if (isCanceled()) return;
        Platform.runLater(
                () -> {
                    // Initialize dataFactories.
                    if (!isIonMobility) {
                        logger.info("data does not contains ion mobility field");
                        return;
                    }
                    initDataFactories();

                    // Initialize Scene.
                    initGui();

                    setContainers();


                    // Init all four plots
                    initIntensityMobilityGui();
                    initmzMobilityGui();
                    initRetentionTimeMobilityGui();

                    initRetentionTimeIntensityGui();
                    isIonMobility = false;
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
        dataFactory = new DataFactory(parameterSet, 0.0, this);
    }

    public void initmzMobilityGui() {
        appliedSteps++;
        datasetMzMobility = new MzMobilityXYZDataset(dataFactory);
        mzMobilityHeatMapPlot =
                new MzMobilityHeatMapPlot(datasetMzMobility, paintScaleStyle, this, intensityMobilityPlot);
        bottomRightpane.setCenter(mzMobilityHeatMapPlot);
    }

    public void initIntensityMobilityGui() {
        appliedSteps++;
        datasetIntensityMobility = new IntensityMobilityXYDataset(dataFactory);
        intensityMobilityPlot = new IntensityMobilityPlot(datasetIntensityMobility, this);
        bottomLeftPane.setCenter(intensityMobilityPlot);
    }

    public void initDataOverview(RawDataOverviewIMSController con) {
        controllerIMS = con;
        appliedSteps++;

        // create the plot
        setTopRightPane(con.getTopRightPane());
        setTopLeftPane(con.getTopLeftPane());
        setBottomLeftPane(con.getBottomLeftPane());
        setBottomRightpane(con.getBottomRightPane());
        setRtLabel(con.getRtLabel());
        setMzRangeLevel(con.getMobilityRTLabel());

        //prepare data
        initDataFactories();
        // Initialize the gui
        initIntensityMobilityGui();
        initmzMobilityGui();
        initRetentionTimeMobilityGui();
        initRetentionTimeIntensityGui();
        setGroupMobility();
        setGroupRetentionTime();

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
        retentionTimeMobilityHeatMapPlot =
                new RetentionTimeMobilityHeatMapPlot(dataset3d, paintScaleStyle);
        topRightPane.setCenter(retentionTimeMobilityHeatMapPlot);
    }

    public void initRetentionTimeIntensityGui() {
        appliedSteps++;
        XYDataset datasetRetentionTimeIntensity = new RetentionTimeIntensityXYDataset(dataFactory);
        retentionTimeIntensityPlot =
                new RetentionTimeIntensityPlot(
                        datasetRetentionTimeIntensity, this, retentionTimeMobilityHeatMapPlot);
        topLeftPane.setCenter(retentionTimeIntensityPlot);
    }


    public void initGui() {
        isIonMobility = false;
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

        mzMobilityHeatMapPlot =
                new MzMobilityHeatMapPlot(datasetMzMobility, paintScaleStyle, this, intensityMobilityPlot);
        bottomRightpane.setCenter(mzMobilityHeatMapPlot);

        datasetIntensityMobility = new IntensityMobilityXYDataset(dataFactory);
        intensityMobilityPlot = new IntensityMobilityPlot(datasetIntensityMobility, this);
        bottomLeftPane.setCenter(intensityMobilityPlot);

        groupMobility.add(new ChartViewWrapper(intensityMobilityPlot));
        groupMobility.add(new ChartViewWrapper(mzMobilityHeatMapPlot));
        if (!isIonMobility)
            initLabel();
        else {
            rtLabel = controllerIMS.rtLabel;
            mzRangeLevel = controllerIMS.mobilityRTLabel;
        }
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
