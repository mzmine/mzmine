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
import java.util.ArrayList;
import java.util.logging.Logger;
import org.controlsfx.control.StatusBar;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.PeakList;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.gui.MZmineGUI;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.chromatogram.ChromatogramVisualizerModule;
import io.github.mzmine.modules.visualization.featurelisttable.PeakListTableModule;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraVisualizerModule;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraVisualizerParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelectionType;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.taskcontrol.TaskPriority;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.taskcontrol.impl.WrappedTask;
import io.github.mzmine.util.ExitCode;
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

  private static final Image rawDataFileIcon =
      FxIconUtil.loadImageFromResources("icons/fileicon.png");

  private static final Image featureListSingleIcon =
      FxIconUtil.loadImageFromResources("icons/peaklisticon_single.png");

  private static final Image featureListAlignedIcon =
      FxIconUtil.loadImageFromResources("icons/peaklisticon_aligned.png");

  private static final NumberFormat percentFormat = NumberFormat.getPercentInstance();

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

    rawDataTree.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
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

    // Add mouse clicked event handler
    rawDataTree.setOnMouseClicked(event -> {
      if (event.getClickCount() == 2) {
        handleShowTIC(null);
      }
    });

    featureTree.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    // featureTree.setShowRoot(true);

    featureTree.setCellFactory(featureListView -> new ListCell<>() {
      @Override
      protected void updateItem(PeakList item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || (item == null)) {
          setText("");
          setGraphic(null);
          return;
        }
        setText(item.getName());
        if (item.getNumberOfRawDataFiles() > 1)
          setGraphic(new ImageView(featureListAlignedIcon));
        else
          setGraphic(new ImageView(featureListSingleIcon));
      }
    });
    // Add mouse clicked event handler
    featureTree.setOnMouseClicked(event -> {
      if (event.getClickCount() == 2) {
        var selectedFeatureLists = MZmineGUI.getSelectedFeatureLists();
        for (PeakList fl : selectedFeatureLists)
          PeakListTableModule.showNewPeakListVisualizerWindow(fl);
      }
    });

    // taskNameColumn.setPrefWidth(800.0);
    // taskNameColumn.setMinWidth(600.0);
    // taskNameColumn.setMinWidth(100.0);

    ObservableList<WrappedTask> tasksQueue =
        MZmineCore.getTaskController().getTaskQueue().getTasks();
    tasksView.setItems(tasksQueue);

    taskNameColumn.setCellValueFactory(
        cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getActualTask().getTaskDescription()));
    taskPriorityColumn.setCellValueFactory(
        cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getActualTask().getTaskPriority()));

    taskStatusColumn.setCellValueFactory(
        cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getActualTask().getStatus()));
    taskProgressColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(
        cell.getValue().getActualTask().getFinishedPercentage()));
    taskProgressColumn.setCellFactory(column -> new TableCell<WrappedTask, Double>() {

      @Override
      public void updateItem(Double value, boolean empty) {
        super.updateItem(value, empty);
        if (empty)
          return;
        ProgressBar progressBar = new ProgressBar(value);
        progressBar.setOpacity(0.3);
        progressBar.prefWidthProperty().bind(taskProgressColumn.widthProperty().subtract(20));
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

    statusBar.setText("Welcome to MZmine " + MZmineCore.getMZmineVersion());

    /*
     * tasksView.setGraphicFactory(task -> { return new Glyph("FontAwesome",
     * FontAwesome.Glyph.COG).size(24.0) .color(Color.BLUE); });
     */

    // Setup the Timeline to update the memory indicator periodically
    final Timeline memoryUpdater = new Timeline();
    int UPDATE_FREQUENCY = 500; // ms
    memoryUpdater.setCycleCount(Animation.INDEFINITE);
    memoryUpdater.getKeyFrames().add(new KeyFrame(Duration.millis(UPDATE_FREQUENCY), e -> {

      tasksView.refresh();

      final long freeMemMB = Runtime.getRuntime().freeMemory() / (1024 * 1024);
      final long totalMemMB = Runtime.getRuntime().totalMemory() / (1024 * 1024);
      final double memory = ((double) (totalMemMB - freeMemMB)) / totalMemMB;

      memoryBar.setProgress(memory);
      memoryBarLabel.setText(freeMemMB + "/" + totalMemMB + " MB free");
    }));
    memoryUpdater.play();

    // Setup the Timeline to update the MZmine tasks periodically final
    /*
     * Timeline msdkTaskUpdater = new Timeline(); UPDATE_FREQUENCY = 50; // ms
     * msdkTaskUpdater.setCycleCount(Animation.INDEFINITE); msdkTaskUpdater.getKeyFrames() .add(new
     * KeyFrame(Duration.millis(UPDATE_FREQUENCY), e -> {
     * 
     * Collection<Task<?>> tasks = tasksView.getTasks(); for (Task<?> task : tasks) { if (task
     * instanceof MZmineTask) { MZmineTask mzmineTask = (MZmineTask) task;
     * mzmineTask.refreshStatus(); } } })); msdkTaskUpdater.play();
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
    var selectedFiles = MZmineGUI.getSelectedRawDataFiles();
    ChromatogramVisualizerModule.setupNewTICVisualizer(selectedFiles.toArray(new RawDataFile[0]));

  }

  public void handleShowMsSpectrum(ActionEvent event) {
    logger.finest("Activated Show MS spectrum menu item");
    var selectedFiles = MZmineGUI.getSelectedRawDataFiles();
    SpectraVisualizerModule module = MZmineCore.getModuleInstance(SpectraVisualizerModule.class);
    ParameterSet parameters =
        MZmineCore.getConfiguration().getModuleParameters(SpectraVisualizerModule.class);
    parameters.getParameter(SpectraVisualizerParameters.dataFiles).setValue(
        RawDataFilesSelectionType.SPECIFIC_FILES, selectedFiles.toArray(new RawDataFile[0]));
    ExitCode exitCode = parameters.showSetupDialog(null, true);
    MZmineProject project = MZmineCore.getProjectManager().getCurrentProject();
    if (exitCode == ExitCode.OK)
      module.runModule(project, parameters, new ArrayList<Task>());
  }

  public void removeRawData(ActionEvent event) {

    if (rawDataTree.getSelectionModel() == null)
      return;

    // Get selected tree items
    ObservableList<RawDataFile> rows = rawDataTree.getSelectionModel().getSelectedItems();

    // Loop through all selected tree items
    if (rows != null) {
      for (int i = rows.size() - 1; i >= 0; i--) {
        RawDataFile row = rows.get(i);
        MZmineCore.getProjectManager().getCurrentProject().removeFile(row);

      }
      // rawDataTree.getSelectionModel().clearSelection();
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

        MZmineCore.getProjectManager().getCurrentProject().removePeakList(row);

      }
      featureTree.getSelectionModel().clearSelection();
    }
  }

  public void handleCancelTask(ActionEvent event) {}

  public void handleCancelAllTasks(ActionEvent event) {}

  public void handleSetHighPriority(ActionEvent event) {}

  public void handleSetNormalPriority(ActionEvent event) {}

  public void updateTabName(Tab tab) {
    /*
     * String title = ""; if (tab.equals(rawDataFilesTab)) { title = "Raw Data"; int rawDataFiles =
     * MZmineCore.getCurrentProject().getRawDataFiles() .size(); if (rawDataFiles > 0) title += " ("
     * + rawDataFiles + ")"; rawDataFilesTab.setText(title); return; } if
     * (tab.equals(featureTablesTab)) { title = "Feature Tables"; int featureTables =
     * MZmineCore.getCurrentProject() .getFeatureTables().size(); if (featureTables > 0) title +=
     * " (" + featureTables + ")"; featureTablesTab.setText(title); return; }
     */
  }

}
