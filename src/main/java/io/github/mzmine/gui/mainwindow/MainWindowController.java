/*
 * Copyright 2006-2020 The MZmine Development Team
 * 
 * This file is part of MZmine 3.
 * 
 * MZmine 3 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 3 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 3; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.gui.mainwindow;

import java.text.NumberFormat;
import java.util.logging.Logger;

import org.controlsfx.control.StatusBar;

import io.github.mzmine.datamodel.PeakList;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.taskcontrol.TaskPriority;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.taskcontrol.impl.WrappedTask;
import io.github.mzmine.util.javafx.FxIconUtil;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Tab;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.util.Duration;

/**
 * This class controls the main window of the application
 * 
 */
public class MainWindowController {

    private final Logger logger = Logger.getLogger(this.getClass().getName());

    private static final Image rawDataFileIcon = FxIconUtil
            .loadImageFromResources("icons/fileicon.png");

    private static final NumberFormat percentFormat = NumberFormat
            .getPercentInstance();

    @FXML
    private Scene mainScene;

    @FXML
    private BorderPane mainWindowPane;

    @FXML
    private ListView<RawDataFile> rawDataTree;

    @FXML
    private ListView<PeakList> featureTree;

    @FXML
    private TableView<WrappedTask> tasksView;

    @FXML
    private StatusBar statusBar;

    @FXML
    private ProgressBar memoryBar;

    @FXML
    private Label memoryBarLabel;

    @FXML
    private TableColumn<WrappedTask, String> taskNameColumn;

    @FXML
    private TableColumn<WrappedTask, TaskPriority> taskPriorityColumn;

    @FXML
    private TableColumn<WrappedTask, TaskStatus> taskStatusColumn;

    @FXML
    private TableColumn<WrappedTask, Double> taskProgressColumn;

    @FXML
    public void initialize() {

        rawDataTree.getSelectionModel()
                .setSelectionMode(SelectionMode.MULTIPLE);
        // rawDataTree.setShowRoot(true);

        rawDataTree.setCellFactory(rawDataListView -> new ListCell<>() {
            @Override
            protected void updateItem(RawDataFile item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || (item == null)) {
                    setText("");
                    setGraphic(null);
                    return;
                }
                setText(item.getName());
                setGraphic(new ImageView(rawDataFileIcon));
            }
        });

        ObservableList<WrappedTask> tasksQueue = MZmineCore.getTaskController()
                .getTaskQueue().getTasks();
        tasksView.setItems(tasksQueue);

        taskNameColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(
                cell.getValue().getActualTask().getTaskDescription()));
        taskPriorityColumn
                .setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(
                        cell.getValue().getActualTask().getTaskPriority()));

        taskStatusColumn
                .setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(
                        cell.getValue().getActualTask().getStatus()));
        taskProgressColumn
                .setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell
                        .getValue().getActualTask().getFinishedPercentage()));
        taskProgressColumn
                .setCellFactory(column -> new TableCell<WrappedTask, Double>() {

                    @Override
                    public void updateItem(Double value, boolean empty) {
                        super.updateItem(value, empty);
                        if (empty)
                            return;
                        ProgressBar progressBar = new ProgressBar(value);
                        progressBar.setOpacity(0.3);
                        progressBar.prefWidthProperty().bind(taskProgressColumn
                                .widthProperty().subtract(20));
                        String labelText = percentFormat.format(value);
                        Label percentLabel = new Label(labelText);
                        percentLabel.setTextFill(Color.BLACK);
                        StackPane stack = new StackPane();
                        stack.setManaged(true);
                        stack.getChildren().addAll(progressBar, percentLabel);
                        setGraphic(stack);
                        setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                    }
                });

        // Add mouse clicked event handler
        rawDataTree.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                handleShowTIC(null);
            }
        });

        featureTree.getSelectionModel()
                .setSelectionMode(SelectionMode.MULTIPLE);
        // featureTree.setShowRoot(true);

        // Add mouse clicked event handler
        featureTree.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                // Show feature table for selected row
                /*
                 * ParameterSet moduleParameters =
                 * MZmineCore.getConfiguration().getModuleParameters(
                 * FeatureTableModule.class); FeatureTablesParameter
                 * inputTablesParam =
                 * moduleParameters.getParameter(FeatureTableModuleParameters.
                 * featureTables);
                 * inputTablesParam.switchType(FeatureTablesSelectionType.
                 * GUI_SELECTED_FEATURE_TABLES);
                 * MZmineCore.runMZmineModule(FeatureTableModule.class,
                 * moduleParameters);
                 */
            }
        });

        // taskNameColumn.setPrefWidth(800.0);
        // taskNameColumn.setMinWidth(600.0);
        // taskNameColumn.setMinWidth(100.0);

        statusBar.setText("Welcome to MZmine " + MZmineCore.getMZmineVersion());

        /*
         * tasksView.setGraphicFactory(task -> { return new Glyph("FontAwesome",
         * FontAwesome.Glyph.COG).size(24.0) .color(Color.BLUE); });
         */

        // Setup the Timeline to update the memory indicator periodically
        final Timeline memoryUpdater = new Timeline();
        int UPDATE_FREQUENCY = 500; // ms
        memoryUpdater.setCycleCount(Animation.INDEFINITE);
        memoryUpdater.getKeyFrames()
                .add(new KeyFrame(Duration.millis(UPDATE_FREQUENCY), e -> {

                    tasksView.refresh();

                    final long freeMemMB = Runtime.getRuntime().freeMemory()
                            / (1024 * 1024);
                    final long totalMemMB = Runtime.getRuntime().totalMemory()
                            / (1024 * 1024);
                    final double memory = ((double) (totalMemMB - freeMemMB))
                            / totalMemMB;

                    memoryBar.setProgress(memory);
                    memoryBarLabel
                            .setText(freeMemMB + "/" + totalMemMB + " MB free");
                }));
        memoryUpdater.play();

        // Setup the Timeline to update the MZmine tasks periodically final
        /*
         * Timeline msdkTaskUpdater = new Timeline(); UPDATE_FREQUENCY = 50; //
         * ms msdkTaskUpdater.setCycleCount(Animation.INDEFINITE);
         * msdkTaskUpdater.getKeyFrames() .add(new
         * KeyFrame(Duration.millis(UPDATE_FREQUENCY), e -> {
         * 
         * Collection<Task<?>> tasks = tasksView.getTasks(); for (Task<?> task :
         * tasks) { if (task instanceof MZmineTask) { MZmineTask mzmineTask =
         * (MZmineTask) task; mzmineTask.refreshStatus(); } } }));
         * msdkTaskUpdater.play();
         */
    }

    @FXML
    public void handleMemoryBarClick(MouseEvent e) {
        // Run garbage collector on a new thread, so it does not block the GUI
        new Thread(() -> {
            logger.info("Running garbage collector...");
            System.gc();
            logger.info("Running garbage collector... done.");
        }).start();

    }

    public ListView<RawDataFile> getRawDataTree() {
        return rawDataTree;
    }

    public ListView<PeakList> getFeatureTree() {
        return featureTree;
    }

    /*
     * public TaskProgressView<Task<?>> getTaskTable() { return tasksView; }
     */

    public StatusBar getStatusBar() {
        return statusBar;
    }

    public TableView<WrappedTask> getTasksView() {
        return tasksView;
    }

    public void handleShowTIC(ActionEvent event) {
        logger.finest("Activated Show chromatogram menu item");
        /*
         * ParameterSet chromPlotParams =
         * MZmineCore.getConfiguration().getModuleParameters(
         * ChromatogramPlotModule.class); RawDataFilesParameter inputFilesParam
         * =
         * chromPlotParams.getParameter(ChromatogramPlotParameters.inputFiles);
         * inputFilesParam.switchType(RawDataFilesSelectionType.
         * GUI_SELECTED_FILES);
         * MZmineGUI.setupAndRunModule(ChromatogramPlotModule.class);
         */
    }

    public void handleShowMsSpectrum(ActionEvent event) {
        logger.finest("Activated Show MS spectrum menu item");
        /*
         * ParameterSet specPlotParams =
         * MZmineCore.getConfiguration().getModuleParameters(
         * MsSpectrumPlotModule.class); RawDataFilesParameter inputFilesParam =
         * specPlotParams.getParameter(MsSpectrumPlotParameters.inputFiles);
         * inputFilesParam.switchType(RawDataFilesSelectionType.
         * GUI_SELECTED_FILES);
         * MZmineGUI.setupAndRunModule(MsSpectrumPlotModule.class);
         */
    }

    public void removeRawData(ActionEvent event) {
        // Get selected tree items
        ObservableList<RawDataFile> rows = null;
        if (rawDataTree.getSelectionModel() != null) {
            rows = rawDataTree.getSelectionModel().getSelectedItems();
        }

        // Loop through all selected tree items
        if (rows != null) {
            for (int i = rows.size() - 1; i >= 0; i--) {
                RawDataFile row = rows.get(i);

                // Remove raw data from current project

                MZmineCore.getProjectManager().getCurrentProject()
                        .removeFile(row);

            }
            rawDataTree.getSelectionModel().clearSelection();
        }
    }

    public void removeFeatureTable(ActionEvent event) {
        // Get selected tree items
        ObservableList<PeakList> rows = null;
        if (featureTree.getSelectionModel() != null) {
            rows = featureTree.getSelectionModel().getSelectedItems();
        }

        // Loop through all selected tree items
        if (rows != null) {
            for (int i = rows.size() - 1; i >= 0; i--) {
                PeakList row = rows.get(i);

                // Remove feature table from current project

                MZmineCore.getProjectManager().getCurrentProject()
                        .removePeakList(row);

            }
            featureTree.getSelectionModel().clearSelection();
        }
    }

    public void handleCancelTask(ActionEvent event) {
    }

    public void handleCancelAllTasks(ActionEvent event) {
    }

    public void handleSetHighPriority(ActionEvent event) {
    }

    public void handleSetNormalPriority(ActionEvent event) {
    }

    public void updateTabName(Tab tab) {
        /*
         * String title = ""; if (tab.equals(rawDataFilesTab)) { title =
         * "Raw Data"; int rawDataFiles =
         * MZmineCore.getCurrentProject().getRawDataFiles() .size(); if
         * (rawDataFiles > 0) title += " (" + rawDataFiles + ")";
         * rawDataFilesTab.setText(title); return; } if
         * (tab.equals(featureTablesTab)) { title = "Feature Tables"; int
         * featureTables = MZmineCore.getCurrentProject()
         * .getFeatureTables().size(); if (featureTables > 0) title += " (" +
         * featureTables + ")"; featureTablesTab.setText(title); return; }
         */
    }

}
