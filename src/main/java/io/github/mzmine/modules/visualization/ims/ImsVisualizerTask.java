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

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Logger;

public class ImsVisualizerTask extends AbstractTask {

    static final Font legendFont = new Font("SansSerif", Font.PLAIN, 12);
    static final Font titleFont = new Font("SansSerif", Font.PLAIN, 12);

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private XYDataset dataset_IntensityMobility;
    private XYDataset dataset_RetentionTimeIntensity;
    private XYZDataset dataset3d;
    private XYZDataset dataset_MzMobility;
    private RawDataFile dataFiles[];
    private Scan scans[];
    private Range<Double> mzRange;
    private ParameterSet parameterSet;
    private int totalSteps = 3, appliedSteps = 0;
    private String paintScaleStyle;
    private double selectedRetentionTime = 0.0;
    private FXMLLoader loader;
    private ImsVisualizerWindowController controller;
    private DataFactory dataFactory;
    private Stage stage;
    private Scene scene;
    private MzMobilityHeatMapPlot mzMobilityHeatMapPlot;
    private IntensityMobilityPlot intensityMobilityPlot;
    private RetentionTimeMobilityHeatMapPlot retentionTimeMobilityHeatMapPlot;
    private RetentionTimeIntensityPlot retentionTimeIntensityPlot;
    private ArrayList<Scan> selectedScans;
    private BorderPane bottomRightpane;
    private BorderPane bottomLeftPane;
    private BorderPane topLeftPane;
    private BorderPane topRightPane;
    private static Label rtLabel;
    private static Label mzRangeLevel;
    private boolean isIonMobility = true;
    private static RawDataOverviewIMSController controllerIMS;


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

    // Group the intensity-mobility and mz-mobility plots and place on the bottom
    private ChartGroup groupMobility = new ChartGroup(false, false, false, true);

    ChartGroup groupRetentionTime = new ChartGroup(false, false, true, false);

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
                    // Initialize dataFactories.
                    InitDataFactories();

                    // Initialize Scene.
                    InitGui();

                    setContainers();


                    // Init all four plots
                    InitmzMobilityGui();
                    InitIntensityMobilityGui();
                    InitRetentionTimeMobilityGui();

                    InitRetentionTimeIntensityGui();
                    isIonMobility = false;
                    InitLebel();

                    updateRTlebel();

                    stage.setTitle("IMS of " + dataFiles[0] + "m/z Range " + mzRange);
                    stage.show();
                    stage.setMinWidth(stage.getWidth());
                    stage.setMinHeight(stage.getHeight());
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

    public void InitDataFactories() {

        // initialize data factory for the plots data.
        dataFactory = new DataFactory(parameterSet, 0.0, this);
    }

    public void InitmzMobilityGui() {

        dataset_MzMobility = new MzMobilityXYZDataset(dataFactory);
        mzMobilityHeatMapPlot =
                new MzMobilityHeatMapPlot(dataset_MzMobility, paintScaleStyle, parameterSet, this);
        bottomRightpane.setCenter(mzMobilityHeatMapPlot);
    }

    public void InitIntensityMobilityGui() {

        dataset_IntensityMobility = new IntensityMobilityXYDataset(dataFactory);
        intensityMobilityPlot = new IntensityMobilityPlot(dataset_IntensityMobility, parameterSet, this);
        bottomLeftPane.setCenter(intensityMobilityPlot);
    }
     public void InitDataOverview(RawDataOverviewIMSController con){
        controllerIMS = con;

        // create the plot
        setTopRightPane(con.getTopRightPane());
        setTopLeftPane(con.getTopLeftPane());
        setBottomLeftPane(con.getBottomLeftPane());
        setBottomRightpane(con.getBottomRightPane());
        setRtLabel(con.getRtLabel());
        setMzRangeLevel(con.getMobilityRTLabel());

        //prepare data
         InitDataFactories();
         // Initialize the gui
         InitmzMobilityGui();
         InitIntensityMobilityGui();
         InitRetentionTimeMobilityGui();
         InitRetentionTimeIntensityGui();
         setGroupMobility();
         setGroupRetentionTime();

     }

    public void setContainers() {
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

    public void InitRetentionTimeMobilityGui() {

        dataset3d = new RetentionTimeMobilityXYZDataset(dataFactory);
        retentionTimeMobilityHeatMapPlot =
                new RetentionTimeMobilityHeatMapPlot(dataset3d, paintScaleStyle);
        topRightPane.setCenter(retentionTimeMobilityHeatMapPlot);
    }

    public void InitRetentionTimeIntensityGui() {
        dataset_RetentionTimeIntensity = new RetentionTimeIntensityXYDataset(dataFactory);
        retentionTimeIntensityPlot =
                new RetentionTimeIntensityPlot(
                        dataset_RetentionTimeIntensity, this, retentionTimeMobilityHeatMapPlot);
        topLeftPane.setCenter(retentionTimeIntensityPlot);
    }


    public void InitGui() {
        loader = new FXMLLoader((getClass().getResource("ImsVisualizerWindow.fxml")));
        stage = new Stage();

        try {
            VBox root = (VBox) loader.load();
            scene = new Scene(root);
            stage.setScene(scene);
            logger.finest("Stage has been successfully loaded from the FXML loader.");
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        // Get controller
        controller = loader.getController();
    }

    public void updateMobilityGroup() {

        dataFactory.updateFrameData(selectedRetentionTime);
        dataset_MzMobility = new MzMobilityXYZDataset(dataFactory);

        mzMobilityHeatMapPlot =
                new MzMobilityHeatMapPlot(dataset_MzMobility, paintScaleStyle, parameterSet, this);
        bottomRightpane.setCenter(mzMobilityHeatMapPlot);

        dataset_IntensityMobility = new IntensityMobilityXYDataset(dataFactory);
        intensityMobilityPlot = new IntensityMobilityPlot(dataset_IntensityMobility, parameterSet, this);
        bottomLeftPane.setCenter(intensityMobilityPlot);

        groupMobility.add(new ChartViewWrapper(intensityMobilityPlot));
        groupMobility.add(new ChartViewWrapper(mzMobilityHeatMapPlot));
        if (!isIonMobility)
            InitLebel();
        else{
          rtLabel =  controllerIMS.rtLabel;
          mzRangeLevel = controllerIMS.mobilityRTLabel;
        }
        updateRTlebel();
    }

    public void InitLebel() {
        rtLabel = controller.getRtLabel();
        mzRangeLevel = controller.getMobilityRTLabel();
    }

    public void setMzRangeLevel(Label level) {
        mzRangeLevel = level;
    }

    public void setRtLabel(Label label) {
        rtLabel = label;
    }

    public void updateRTlebel() {
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

    public void setSelectedScans(ArrayList<Scan> arrayList) {
        selectedScans = arrayList;
    }

    public ArrayList<Scan> getScans() {
        return selectedScans;
    }

}
