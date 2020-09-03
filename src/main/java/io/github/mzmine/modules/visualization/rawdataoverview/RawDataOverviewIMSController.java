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
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RawDataOverviewIMSController {

    public static final Logger logger =
        Logger.getLogger(RawDataOverviewWindowController.class.getName());
    public SplitPane pnMainSplit;

    private boolean initialized = false;
    private final ObservableMap<RawDataFile, RawDataFileInfoPaneController>
        rawDataFilesAndControllers = FXCollections.observableMap(new HashMap<>());
    private final ObservableMap<RawDataFile, Tab> rawDataFilesAndTabs =
        FXCollections.observableMap(new HashMap<>());

    private boolean scroll;
    @FXML private Label rawDataLabel;

    @FXML private BorderPane topLeftPane;

    @FXML private BorderPane topRightPane;

    @FXML private BorderPane bottomLeftPane;

    @FXML private BorderPane bottomRightPane;

    @FXML public Label rtLabel;

    @FXML public Label mobilityRTLabel;


    @FXML private TabPane tpRawDataInfoIMS;

    @FXML private BorderPane pnMaster;


    public void initialize() {

        //    this.rawDataFile = rawDataFile;
        // add meta data
        rawDataLabel.setText("Overview of raw data file(s): ");
        initGui();
        scroll = true;
        initialized = true;
    }

    void initGui() {
        ImsVisualizerModule module = new ImsVisualizerModule();
        Class<? extends MZmineRunnableModule> moduleJavaClass = module.getClass();
        ParameterSet parameters =
            MZmineCore.getConfiguration().getModuleParameters(moduleJavaClass);
        ImsVisualizerTask imsVisualizerTask = new ImsVisualizerTask(parameters);
        imsVisualizerTask.initDataOverview(this);

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
        filesToProcess.forEach(this::removeRawDataFile);

        // presence of file is checked in the add method
        rawDataFiles.forEach(this::addRawDataFileTab);
    }

    /**
     * Adds a raw data file table to the tab.
     *
     * @param raw The raw dataFile
     */
    public void addRawDataFileTab(RawDataFile raw) {

        if (!initialized) {
            initialize();
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

    public BorderPane getTopLeftPane() {
        return topLeftPane;
    }

    public BorderPane getTopRightPane() {
        return topRightPane;
    }

    public BorderPane getBottomLeftPane() {
        return bottomLeftPane;
    }

    public BorderPane getBottomRightPane() {
        return bottomRightPane;
    }

    public Label getRtLabel() {
        return rtLabel;
    }

    public Label getMobilityRTLabel() {
        return mobilityRTLabel;
    }
}
