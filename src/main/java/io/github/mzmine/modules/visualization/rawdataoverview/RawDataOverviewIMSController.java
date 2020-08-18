package io.github.mzmine.modules.visualization.rawdataoverview;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineRunnableModule;
import io.github.mzmine.modules.visualization.ims.ImsVisualizerModule;
import io.github.mzmine.modules.visualization.ims.ImsVisualizerTask;
import io.github.mzmine.parameters.ParameterSet;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RawDataOverviewIMSController {

    public static final Logger logger = Logger
            .getLogger(RawDataOverviewWindowController.class.getName());

    private boolean initialized = false;
    private ObservableMap<RawDataFile, RawDataFileInfoPaneController> rawDataFilesAndControllers = FXCollections
            .observableMap(new HashMap<>());
    private ObservableMap<RawDataFile, Tab> rawDataFilesAndTabs = FXCollections
            .observableMap(new HashMap<>());

    private boolean scroll;
    public ParameterSet parameters;

    @FXML
    private Label rawDataLabel;

    @FXML
    private BorderPane topLeftPane;

    @FXML
    private BorderPane topRightPane;

    @FXML
    private BorderPane bottomLeftPane;

    @FXML
    private BorderPane bottomRightPane;

    @FXML public Label rtLabel;

    @FXML public Label mobilityRTLabel;


    @FXML
    private TabPane tpRawDataInfoIMS;

    @FXML
    private BorderPane pnMaster;

    @FXML
    private SplitPane pnMain;

    public void initialize(ParameterSet parameterSet) {

//    this.rawDataFile = rawDataFile;
        // add meta data
        rawDataLabel.setText("Overview of raw data file(s): ");
        InitGui(parameterSet);
        if(parameterSet != null){
            System.out.println("Yaha bhi null hai....");
        }
        parameters = parameterSet;

        scroll = true;
        initialized = true;
    }

    void InitGui(ParameterSet parameterSet){
        if(parameterSet == null){
            System.out.println("Lollllll");
            return;
        }
        ImsVisualizerModule module = new ImsVisualizerModule();
        Class<? extends MZmineRunnableModule> moduleJavaClass = module.getClass();
        ParameterSet moduleParameters =
                MZmineCore.getConfiguration().getModuleParameters(moduleJavaClass);
        ImsVisualizerTask imsVisualizerTask = new ImsVisualizerTask(moduleParameters);
        imsVisualizerTask.setControllerIMS(this);
        imsVisualizerTask.setTopRightPane(topRightPane);
        imsVisualizerTask.setTopLeftPane(topLeftPane);
        imsVisualizerTask.setBottomRightpane(bottomRightPane);
        imsVisualizerTask.setBottomLeftPane(bottomLeftPane);
        imsVisualizerTask.InitDataFactories();
        imsVisualizerTask.InitmzMobilityGui();
        imsVisualizerTask.InitIntensityMobilityGui();
        imsVisualizerTask.InitRetentionTimeMobilityGui();
        imsVisualizerTask.InitRetentionTimeIntensityGui();
        imsVisualizerTask.setGroupMobility();
        imsVisualizerTask.setGroupRetentionTime();
        imsVisualizerTask.setRtLabel(rtLabel);
        imsVisualizerTask.setMzRangeLevel(mobilityRTLabel);

    }

    /**
     * Sets the raw data files to be displayed. Already present files are not removed to optimise
     * performance. This should be called over {@link RawDataOverviewWindowController#addRawDataFileTab}
     * if possible.
     *
     * @param rawDataFiles
     */
    public void setRawDataFiles(Collection<RawDataFile> rawDataFiles) {
        // remove files first
        List<RawDataFile> filesToProcess = new ArrayList<>();
        for (RawDataFile rawDataFile : rawDataFilesAndTabs.keySet()) {
            if (!rawDataFiles.contains(rawDataFile)) {
                filesToProcess.add(rawDataFile);
            }
        }
        filesToProcess.forEach(r -> removeRawDataFile(r));

        // presence of file is checked in the add method
        rawDataFiles.forEach(r -> addRawDataFileTab(r));
    }

    /**
     * Adds a raw data file table to the tab.
     *
     * @param raw The raw dataFile
     */
    public void addRawDataFileTab(RawDataFile raw) {

        if (!initialized) {
            initialize(parameters);
        }
        if (rawDataFilesAndControllers.containsKey(raw)) {
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("RawDataFileInfoPane.fxml"));
            BorderPane pane = loader.load();
            rawDataFilesAndControllers.put(raw, loader.getController());
            RawDataFileInfoPaneController con = rawDataFilesAndControllers.get(raw);

            Tab rawDataFileTab = new Tab(raw.getName());
            rawDataFileTab.setContent(pane);
            tpRawDataInfoIMS.getTabs().add(rawDataFileTab);

            rawDataFileTab.selectedProperty().addListener((obs, o, n) -> {
                if (n == true) {
                    logger.fine("Populating table for raw data file " + raw.getName());
                    con.populate(raw);
                }
            });

            rawDataFileTab.setOnClosed((e) -> {
                logger.fine("Removing raw data file " + raw.getName());
                removeRawDataFile(raw);
            });

            if (rawDataFileTab.selectedProperty().getValue()) {
                con.populate(raw);
            }

            rawDataFilesAndTabs.put(raw, rawDataFileTab);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Could not load RawDataFileInfoPane.fxml", e);
        }

        logger.fine("Added raw data file tab for " + raw.getName());
    }

    public void removeRawDataFile(RawDataFile raw) {
        rawDataFilesAndControllers.remove(raw);
        Tab tab = rawDataFilesAndTabs.remove(raw);
        tpRawDataInfoIMS.getTabs().remove(tab);
    }
}
