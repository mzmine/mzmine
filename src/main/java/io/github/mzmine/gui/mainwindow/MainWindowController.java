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

package io.github.mzmine.gui.mainwindow;

import java.text.NumberFormat;
import java.util.List;
import java.util.logging.Logger;
import org.controlsfx.control.StatusBar;
import com.google.common.collect.Ordering;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.gui.MZmineGUI;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.MZmineRunnableModule;
import com.google.common.collect.ImmutableList;
import io.github.mzmine.util.javafx.DraggableListCellWithDraggableFiles;
import io.github.mzmine.util.javafx.listviewgroups.ListViewGroupsEntity;
import io.github.mzmine.util.javafx.listviewgroups.ListViewGroups;
import io.github.mzmine.modules.visualization.chromatogram.ChromatogramVisualizerModule;
import io.github.mzmine.modules.visualization.chromatogram.TICVisualizerParameters;
import io.github.mzmine.modules.visualization.fx3d.Fx3DVisualizerModule;
import io.github.mzmine.modules.visualization.fx3d.Fx3DVisualizerParameters;
import io.github.mzmine.modules.visualization.image.ImageVisualizerModule;
import io.github.mzmine.modules.visualization.image.ImageVisualizerParameters;
import io.github.mzmine.modules.visualization.rawdataoverview.RawDataOverviewPane;
import io.github.mzmine.modules.visualization.rawdataoverview.RawDataOverviewWindowController;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraVisualizerModule;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraVisualizerParameters;
import io.github.mzmine.modules.visualization.twod.TwoDVisualizerModule;
import io.github.mzmine.modules.visualization.twod.TwoDVisualizerParameters;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelectionType;
import io.github.mzmine.util.FeatureTableFXUtil;
import io.github.mzmine.util.javafx.listviewgroups.ListViewGroupsCell;
import javafx.application.Platform;
import javafx.scene.control.ContextMenu;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.project.impl.ImagingRawDataFileImpl;
import io.github.mzmine.taskcontrol.TaskController;
import io.github.mzmine.taskcontrol.TaskPriority;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.taskcontrol.impl.WrappedTask;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.javafx.FxIconUtil;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

public class MainWindowController {

  private final Logger logger = Logger.getLogger(this.getClass().getName());
  private Boolean reorderListItem = false;
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
  private ListViewGroups<RawDataFile> rawDataList;

  @FXML
  private ListView<FeatureList> featuresList;

  @FXML
  private ListView<FeatureList> alignedFeaturesList;

  @FXML
  public ContextMenu rawDataContextMenu;

  @FXML
  public MenuItem rawDataGroupMenuItem;

  @FXML
  private Tab tvAligned;

  @FXML
  private AnchorPane tbRawData;

  @FXML
  private AnchorPane tbFeatureTable;

  @FXML
  private BorderPane rawDataOverview;

  @FXML
  private RawDataOverviewWindowController rawDataOverviewController;

  @FXML
  private TabPane mainTabPane;

  @FXML
  private Tab tabRawDataOverview;

  @FXML
  private Tab tabFeatureListOverview;

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

    rawDataList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

    featuresList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

    alignedFeaturesList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

    rawDataList.setCellFactory(rawDataListView -> new ListViewGroupsCell<>(rawDataGroupMenuItem) {

      @Override
      protected void updateItem(ListViewGroupsEntity<RawDataFile> item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || (item == null)) {
          setText("");
          setGraphic(null);
          return;
        }
        if (item.isGroupHeader()) {
          return;
        }

        setText(item.getValue().getName());
        setGraphic(new ImageView(rawDataFileIcon));
        textFillProperty().bind(item.getValue().colorProperty());
      }

      @Override
      public void commitEdit(ListViewGroupsEntity<RawDataFile> item) {
        super.commitEdit(item);
        if (item == null || item.isGroupHeader()) {
          return;
        }

        item.getValue().setName(getText());
      }
    });

    // Add mouse clicked event handler
    rawDataList.setOnMouseClicked(event -> {
      if (event.getClickCount() == 2) {
        List<RawDataFile> selectedFiles = MZmineGUI.getSelectedRawDataFiles();
        if (selectedFiles.stream().anyMatch(f -> f instanceof ImagingRawDataFileImpl)) {
          handleShowImage(event);
        } else {
          handleShowChromatogram(event);
        }
      }
    });

    // Add long mouse pressed event handler
    rawDataList.addEventHandler(MouseEvent.ANY, new EventHandler<MouseEvent>() {
      final PauseTransition timer = new PauseTransition(Duration.millis(400));

      @Override
      public void handle(MouseEvent event) {
        timer.setOnFinished(e -> {
          rawDataList.setEditable(true);
          rawDataList.edit(rawDataList.getSelectionModel().getSelectedIndex());
        });

        if (event.getEventType().equals(MouseEvent.MOUSE_PRESSED)) {
          timer.playFromStart();
        } else if (event.getEventType().equals(MouseEvent.MOUSE_RELEASED)
            || event.getEventType().equals(MouseEvent.MOUSE_DRAGGED)) {
          timer.stop();
        }
      }
    });

    featuresList.setCellFactory(featureListView -> new DraggableListCellWithDraggableFiles<>() {
      @Override
      protected void updateItem(FeatureList item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || (item == null)) {
          setText("");
          setGraphic(null);
          return;
        }
        setText(item.getName());
        setGraphic(new ImageView(featureListSingleIcon));
      }
    });
    alignedFeaturesList.setCellFactory(featureListView -> new DraggableListCellWithDraggableFiles<>() {
      @Override
      protected void updateItem(FeatureList item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || (item == null)) {
          setText("");
          setGraphic(null);
          return;
        }
        setText(item.getName());
        setGraphic(new ImageView(featureListAlignedIcon));
      }
    });

    // Add mouse clicked event handler
    featuresList.setOnMouseClicked(event -> {
      if (event.getClickCount() == 2) {
        handleOpenFeatureList(event);
      }
    });
    alignedFeaturesList.setOnMouseClicked(event -> {
      if (event.getClickCount() == 2) {
        handleOpenAlignedFeatureList(event);
      }
    });

    // notify selected tab about raw file selection change
    rawDataList.getSelectedItems().addListener((ListChangeListener<RawDataFile>) c -> {
      c.next();
      for (Tab tab : MZmineCore.getDesktop().getAllTabs()) {
        if (tab instanceof MZmineTab && tab.isSelected()
            && ((MZmineTab) tab).isUpdateOnSelection()
            && !c.getList().isEmpty()) {
          ((MZmineTab) tab).onRawDataFileSelectionChanged(c.getList());
        }
      }
    });

    featuresList.getSelectionModel().getSelectedItems()
      .addListener((ListChangeListener<FeatureList>) c -> {
        c.next();
        for (Tab tab : MZmineCore.getDesktop().getAllTabs()) {
          if (tab instanceof MZmineTab && tab.isSelected()
              && ((MZmineTab) tab).isUpdateOnSelection()) {
            ((MZmineTab) tab).onFeatureListSelectionChanged(c.getList());
          }
        }
      });

    alignedFeaturesList.getSelectionModel().getSelectedItems()
      .addListener((ListChangeListener<FeatureList>) c -> {
        c.next();
        for (Tab tab : MZmineCore.getDesktop().getAllTabs()) {
          if (tab instanceof MZmineTab && tab.isSelected()
              && ((MZmineTab) tab).isUpdateOnSelection()) {
            ((MZmineTab) tab).onAlignedFeatureListSelectionChanged(c.getList());
          }
        }
      });

    /*
     * // update if tab selection in main window changes
     * getMainTabPane().getSelectionModel().selectedItemProperty().addListener((obs, old, val) -> {
     * if (val instanceof MZmineTab && ((MZmineTab) val).getRawDataFiles() != null) { if
     * (!((MZmineTab) val).getRawDataFiles()
     * .containsAll(rawDataTree.getSelectionModel().getSelectedItems()) || ((MZmineTab)
     * val).getRawDataFiles().size() != rawDataTree.getSelectionModel() .getSelectedItems().size())
     * { if (((MZmineTab) val).isUpdateOnSelection()) { ((MZmineTab) val)
     * .onRawDataFileSelectionChanged(rawDataTree.getSelectionModel().getSelectedItems()); } } //
     * TODO: Add the same for feature lists } });
     */

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
      if (empty) {
        return;
      }
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

    RawDataOverviewPane rop = new RawDataOverviewPane(true, true);
    addTab(rop);
  }

  public ListViewGroups<RawDataFile> getRawDataList() {
    return rawDataList;
  }

  public ListView<FeatureList> getFeaturesList() {
    return featuresList;
  }

  public ListView<FeatureList> getAlignedFeaturesList() {
    return alignedFeaturesList;
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

  public void handleShowChromatogram(Event event) {
    logger.finest("Activated Show chromatogram menu item");
    var selectedFiles = MZmineGUI.getSelectedRawDataFiles();
    ParameterSet parameters =
        MZmineCore.getConfiguration().getModuleParameters(ChromatogramVisualizerModule.class);
    parameters.getParameter(TICVisualizerParameters.DATA_FILES).setValue(
        RawDataFilesSelectionType.SPECIFIC_FILES, selectedFiles.toArray(new RawDataFile[0]));
    ExitCode exitCode = parameters.showSetupDialog(true);
    if (exitCode == ExitCode.OK) {
      MZmineCore.runMZmineModule(ChromatogramVisualizerModule.class, parameters);
    }
  }

  public void handleShowMsSpectrum(Event event) {
    logger.finest("Activated Show MS spectrum menu item");
    var selectedFiles = MZmineGUI.getSelectedRawDataFiles();
    ParameterSet parameters =
        MZmineCore.getConfiguration().getModuleParameters(SpectraVisualizerModule.class);
    parameters.getParameter(SpectraVisualizerParameters.dataFiles).setValue(
        RawDataFilesSelectionType.SPECIFIC_FILES, selectedFiles.toArray(new RawDataFile[0]));
    ExitCode exitCode = parameters.showSetupDialog(true);
    if (exitCode == ExitCode.OK) {
      MZmineCore.runMZmineModule(SpectraVisualizerModule.class, parameters);
    }
  }

  public void handleShow2DPlot(Event event) {
    logger.finest("Activated Show 2D plot menu item");
    var selectedFiles = MZmineGUI.getSelectedRawDataFiles();
    ParameterSet parameters =
        MZmineCore.getConfiguration().getModuleParameters(TwoDVisualizerModule.class);
    parameters.getParameter(TwoDVisualizerParameters.dataFiles).setValue(
        RawDataFilesSelectionType.SPECIFIC_FILES, selectedFiles.toArray(new RawDataFile[0]));
    ExitCode exitCode = parameters.showSetupDialog(true);
    if (exitCode == ExitCode.OK) {
      MZmineCore.runMZmineModule(TwoDVisualizerModule.class, parameters);
    }
  }

  public void handleShow3DPlot(Event event) {
    logger.finest("Activated Show 3D plot menu item");
    var selectedFiles = MZmineGUI.getSelectedRawDataFiles();
    ParameterSet parameters =
        MZmineCore.getConfiguration().getModuleParameters(Fx3DVisualizerModule.class);
    parameters.getParameter(Fx3DVisualizerParameters.dataFiles).setValue(
        RawDataFilesSelectionType.SPECIFIC_FILES, selectedFiles.toArray(new RawDataFile[0]));
    ExitCode exitCode = parameters.showSetupDialog(true);
    if (exitCode == ExitCode.OK) {
      MZmineCore.runMZmineModule(Fx3DVisualizerModule.class, parameters);
    }
  }

  public void handleShowImage(Event event) {
    logger.finest("Activated Show image menu item");
    var selectedFiles = MZmineGUI.getSelectedRawDataFiles();
    ParameterSet parameters =
        MZmineCore.getConfiguration().getModuleParameters(ImageVisualizerModule.class);
    parameters.getParameter(ImageVisualizerParameters.rawDataFiles).setValue(
        RawDataFilesSelectionType.SPECIFIC_FILES, selectedFiles.toArray(new RawDataFile[0]));
    ExitCode exitCode = parameters.showSetupDialog(true);
    if (exitCode == ExitCode.OK) {
      MZmineCore.runMZmineModule(ImageVisualizerModule.class, parameters);
    }
  }


  public void handleShowMsMsPlot(Event event) {}

  public void handleSort(Event event) {
    if (!(event.getSource() instanceof ListView)) {
      return;
    }
    ListView<?> sourceList = (ListView<?>) event.getSource();
    List<?> files = sourceList.getItems();
    files.sort(Ordering.usingToString());
  }

  public void handleRemoveFileExtension(Event event) {}

  public void handleExportFile(Event event) {}

  public void handleRenameFile(Event event) {
    if (rawDataList.getSelectionModel() == null) {
      return;
    }

    // Only one file must be selected
    if (rawDataList.getSelectionModel().getSelectedIndices().size() != 1) {
      return;
    }

    rawDataList.setEditable(true);
    rawDataList.edit(rawDataList.getSelectionModel().getSelectedIndex());
  }

  @SuppressWarnings("unchecked")
  public void runModule(Event event) {
    assert event.getSource() instanceof MenuItem;
    final MenuItem menuItem = (MenuItem) event.getSource();
    assert menuItem.getUserData() instanceof String;
    final String moduleClass = (String) menuItem.getUserData();
    assert moduleClass != null;

    logger.info("Menu item activated for module " + moduleClass);
    Class<? extends MZmineRunnableModule> moduleJavaClass;
    try {
      moduleJavaClass = (Class<? extends MZmineRunnableModule>) Class.forName(moduleClass);
    } catch (Throwable e) {
      MZmineCore.getDesktop().displayMessage("Cannot load module class " + moduleClass);
      return;
    }

    MZmineModule module = MZmineCore.getModuleInstance(moduleJavaClass);

    if (module == null) {
      MZmineCore.getDesktop().displayMessage("Cannot find module of class " + moduleClass);
      return;
    }

    ParameterSet moduleParameters =
        MZmineCore.getConfiguration().getModuleParameters(moduleJavaClass);

    logger.info("Setting parameters for module " + module.getName());

    ExitCode exitCode = moduleParameters.showSetupDialog(true);
    if (exitCode != ExitCode.OK)
      return;

    ParameterSet parametersCopy = moduleParameters.cloneParameterSet();
    logger.finest("Starting module " + module.getName() + " with parameters " + parametersCopy);
    MZmineCore.runMZmineModule(moduleJavaClass, parametersCopy);
  }

  public void handleRemoveRawData(Event event) {
    if (rawDataList.getSelectionModel() == null) {
      return;
    }

    // Show alert window
    Alert alert =
        new Alert(AlertType.CONFIRMATION, "Are you sure you want to remove selected files?",
            ButtonType.YES, ButtonType.NO, ButtonType.CANCEL);
    Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
    stage.getIcons().add(new Image("MZmineIcon.png"));
    alert.setHeaderText("Remove file");
    alert.showAndWait();

    if (alert.getResult() != ButtonType.YES) {
      return;
    }

    for (RawDataFile selectedItem : ImmutableList.copyOf(rawDataList.getSelectedItems())) {
      MZmineCore.getProjectManager().getCurrentProject().removeFile(selectedItem);
    }

    for (String group : ImmutableList.copyOf(rawDataList.getSelectedGroups())) {
      rawDataList.ungroupItems(group);
    }
  }

  public void handleOpenFeatureList(Event event) {
    List<FeatureList> selectedFeatureLists = MZmineGUI.getSelectedFeatureLists();
    for (FeatureList fl : selectedFeatureLists) {
      // PeakListTableModule.showNewPeakListVisualizerWindow(fl);
      Platform.runLater(() -> {
        FeatureTableFXUtil.addFeatureTableTab(fl);
      });
    }
  }

  public void handleOpenAlignedFeatureList(Event event) {
    List<FeatureList> selectedFeatureLists = MZmineGUI.getSelectedAlignedFeatureLists();
    for (FeatureList fl : selectedFeatureLists) {
      Platform.runLater(() -> {
        FeatureTableFXUtil.addFeatureTableTab(fl);
      });
    }
  }

  public void handleShowFeatureListSummary(Event event) {}

  public void handleShowScatterPlot(Event event) {}

  public void handleRenameFeatureList(Event event) {}

  @FXML
  public void handleRemoveFeatureList(Event event) {
    FeatureList selectedFeatureLists[] = MZmineCore.getDesktop().getSelectedPeakLists();
    for (FeatureList fl : selectedFeatureLists) {
      MZmineCore.getProjectManager().getCurrentProject().removeFeatureList(fl);
    }
  }

  @FXML
  public void handleCancelTask(Event event) {
    var selectedTasks = tasksView.getSelectionModel().getSelectedItems();
    for (WrappedTask t : selectedTasks) {
      t.getActualTask().cancel();
    }
  }

  @FXML
  public void handleCancelAllTasks(Event event) {
    for (WrappedTask t : tasksView.getItems()) {
      t.getActualTask().cancel();
    }
  }

  @FXML
  public void handleSetHighPriority(Event event) {
    TaskController taskController = MZmineCore.getTaskController();
    var selectedTasks = tasksView.getSelectionModel().getSelectedItems();
    for (WrappedTask t : selectedTasks) {
      taskController.setTaskPriority(t.getActualTask(), TaskPriority.HIGH);
    }
  }

  @FXML
  public void handleSetNormalPriority(Event event) {
    TaskController taskController = MZmineCore.getTaskController();
    var selectedTasks = tasksView.getSelectionModel().getSelectedItems();
    for (WrappedTask t : selectedTasks) {
      taskController.setTaskPriority(t.getActualTask(), TaskPriority.NORMAL);
    }
  }

  @FXML
  public void handleMemoryBarClick(Event e) {
    // Run garbage collector on a new thread, so it doesn't block the GUI
    new Thread(() -> {
      logger.info("Freeing unused memory");
      System.gc();
    }).start();
  }

  private TabPane getMainTabPane() {
    return mainTabPane;
  }

  /**
   * @return Current tabs wrapped in {@link FXCollections#unmodifiableObservableList}
   */
  public ObservableList<Tab> getTabs() {
    return FXCollections.unmodifiableObservableList(getMainTabPane().getTabs());
  }

  public void addTab(Tab tab) {
    if (tab instanceof MZmineTab) {
      ((MZmineTab) tab).updateOnSelectionProperty().addListener(((obs, old, val) -> {
        if (val) {
          if (((MZmineTab) tab).getRawDataFiles() != null && !((MZmineTab) tab).getRawDataFiles()
              .equals(rawDataList.getSelectionModel().getSelectedItems())) {
            ((MZmineTab) tab)
                .onRawDataFileSelectionChanged(rawDataList.getSelectedItems());
          }

          if(((MZmineTab) tab).getFeatureLists() != null && !((MZmineTab) tab).getFeatureLists()
              .equals(featuresList.getSelectionModel().getSelectedItems())) {
            ((MZmineTab) tab)
                .onFeatureListSelectionChanged(featuresList.getSelectionModel().getSelectedItems());
          }

          if(((MZmineTab) tab).getAlignedFeatureLists() != null && !((MZmineTab) tab).getAlignedFeatureLists()
              .equals(alignedFeaturesList.getSelectionModel().getSelectedItems())) {
            ((MZmineTab) tab)
                .onAlignedFeatureListSelectionChanged(alignedFeaturesList.getSelectionModel().getSelectedItems());
          }
        }
      }));
    }

    getMainTabPane().getTabs().add(tab);
    getMainTabPane().getSelectionModel().select(tab);
  }

  @FXML
  public void handleSetRawDataFileColor(Event event) {
    if (rawDataList.getSelectionModel() == null) {
      return;
    }

    ObservableList<RawDataFile> rows = rawDataList.getSelectedItems();
    // Only one file must be selected
    if (rows == null || rows.size() != 1 || rows.get(0) == null) {
      return;
    }
    // Creating new popup window
    Stage popup = new Stage();
    VBox box = new VBox(5);
    Label label = new Label("Please choose a color for \""
        + rows.get(0) + "\":");
    ColorPicker picker =
        new ColorPicker(rows.get(0).getColor());

    BooleanProperty apply = new SimpleBooleanProperty(false);

    box.setAlignment(Pos.CENTER);
    box.setPadding(new Insets(10, 10, 10, 10));

    label.setMinWidth(Control.USE_PREF_SIZE);

    picker.getCustomColors().addAll(MZmineCore.getConfiguration().getDefaultColorPalette());
    picker.setMinWidth(label.getText().length() * 6);

    Button btnApply = new Button("Apply");
    Button btnCancel = new Button("Cancel");
    btnApply.setOnAction(e -> {
      apply.set(true);
      popup.hide();
    });
    btnCancel.setOnAction(e -> popup.hide());
    ButtonBar.setButtonData(btnApply, ButtonData.APPLY);
    ButtonBar.setButtonData(btnCancel, ButtonData.CANCEL_CLOSE);
    ButtonBar btnBar = new ButtonBar();
    btnBar.getButtons().addAll(btnApply, btnCancel);

    box.getChildren().addAll(label, picker, btnBar);

    popup.setScene(new Scene(box));
    popup.setTitle("Set color");
    popup.setResizable(false);
    popup.initModality(Modality.APPLICATION_MODAL);
    popup.getIcons().add(new Image("MZmineIcon.png"));
    popup.show();

    popup.setOnHiding(e -> {
      if (picker.getValue() != null && apply.get()) {
        rows.get(0).setColor(picker.getValue());
      }
    });
  }

  public void handleGroupRawDataFiles(Event event) {
    if (rawDataList.onlyGroupHeadersSelected()) {
      rawDataList.ungroupItems(ImmutableList.copyOf(rawDataList.getSelectedGroups()));
    } else if (rawDataList.onlyItemsSelected()) {
      rawDataList.groupSelectedItems();
    }
  }
}
