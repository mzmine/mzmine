/*
 * Copyright 2006-2022 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.gui.mainwindow;

import com.google.common.collect.ImmutableList;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.ImagingRawDataFile;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.gui.MZmineGUI;
import io.github.mzmine.gui.colorpicker.ColorPickerMenuItem;
import io.github.mzmine.gui.mainwindow.introductiontab.MZmineIntroductionTab;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.MZmineRunnableModule;
import io.github.mzmine.modules.tools.rawfilerename.RawDataFileRenameModule;
import io.github.mzmine.modules.visualization.chromatogram.ChromatogramVisualizerModule;
import io.github.mzmine.modules.visualization.chromatogram.TICVisualizerParameters;
import io.github.mzmine.modules.visualization.fx3d.Fx3DVisualizerModule;
import io.github.mzmine.modules.visualization.fx3d.Fx3DVisualizerParameters;
import io.github.mzmine.modules.visualization.image.ImageVisualizerModule;
import io.github.mzmine.modules.visualization.image.ImageVisualizerParameters;
import io.github.mzmine.modules.visualization.msms.MsMsVisualizerModule;
import io.github.mzmine.modules.visualization.rawdataoverview.RawDataOverviewModule;
import io.github.mzmine.modules.visualization.rawdataoverview.RawDataOverviewParameters;
import io.github.mzmine.modules.visualization.rawdataoverview.RawDataOverviewWindowController;
import io.github.mzmine.modules.visualization.rawdataoverviewims.IMSRawDataOverviewModule;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraVisualizerModule;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraVisualizerParameters;
import io.github.mzmine.modules.visualization.twod.TwoDVisualizerModule;
import io.github.mzmine.modules.visualization.twod.TwoDVisualizerParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelectionType;
import io.github.mzmine.taskcontrol.TaskController;
import io.github.mzmine.taskcontrol.TaskPriority;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.taskcontrol.impl.WrappedTask;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.FeatureTableFXUtil;
import io.github.mzmine.util.javafx.FxIconUtil;
import io.github.mzmine.util.javafx.groupablelistview.GroupEntity;
import io.github.mzmine.util.javafx.groupablelistview.GroupableListView;
import io.github.mzmine.util.javafx.groupablelistview.GroupableListViewCell;
import io.github.mzmine.util.javafx.groupablelistview.GroupableListViewEntity;
import io.github.mzmine.util.javafx.groupablelistview.ValueEntity;
import io.github.mzmine.util.spectraldb.entry.SpectralLibrary;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;
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
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
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
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
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
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FilenameUtils;
import org.controlsfx.control.StatusBar;

public class MainWindowController {

  private static final Image featureListSingleIcon = FxIconUtil.loadImageFromResources(
      "icons/peaklisticon_single.png");
  private static final Image featureListAlignedIcon = FxIconUtil.loadImageFromResources(
      "icons/peaklisticon_aligned.png");
  private static final NumberFormat percentFormat = NumberFormat.getPercentInstance();
  private final Logger logger = Logger.getLogger(this.getClass().getName());
  @FXML
  public ContextMenu rawDataContextMenu;
  @FXML
  public ContextMenu featureListContextMenu;
  @FXML
  public ContextMenu spectralLibraryContextMenu;

  @FXML
  public MenuItem rawDataGroupMenuItem;
  @FXML
  public MenuItem rawDataRemoveMenuItem;
  @FXML
  public MenuItem rawDataRenameMenuItem;
  @FXML
  public MenuItem rawDataRemoveExtensionMenuItem;
  @FXML
  public Menu rawDataSetColorMenu;
  @FXML
  public MenuItem openFeatureListMenuItem;
  @FXML
  public MenuItem showFeatureListSummaryMenuItem;
  @FXML
  public MenuItem featureListsRenameMenuItem;
  @FXML
  public MenuItem featureListsRemoveMenuItem;
  public ColorPickerMenuItem rawDataFileColorPicker;
  @FXML
  private Scene mainScene;
  @FXML
  public VBox bottomBox;
  @FXML
  private GroupableListView<RawDataFile> rawDataList;
  @FXML
  private GroupableListView<FeatureList> featureListsList;
  @FXML
  private ListView<SpectralLibrary> spectralLibraryList;

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

    rawDataList.setEditable(false);
    rawDataList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

    featureListsList.setEditable(false);
    featureListsList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    featureListsList.setGrouping(featureList -> featureList.isAligned() ? "Aligned feature lists"
        : featureList.getRawDataFile(0).getName());

    spectralLibraryList.setEditable(false);
    spectralLibraryList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

    rawDataList.setCellFactory(
        rawDataListView -> new GroupableListViewCell<>(rawDataGroupMenuItem) {

          @Override
          protected void updateItem(GroupableListViewEntity item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || (item == null)) {
              setText("");
              setGraphic(null);
              return;
            }
            if (item instanceof GroupEntity) {
              return;
            }

            RawDataFile rawDataFile = ((ValueEntity<RawDataFile>) item).getValue();
            setText(rawDataFile.getName());
            rawDataFile.nameProperty()
                .addListener((observable, oldValue, newValue) -> setText(newValue));
            setGraphic(new ImageView(FxIconUtil.getFileIcon(rawDataFile.getColor())));

            rawDataFile.colorProperty().addListener((observable, oldColor, newColor) -> {
              // Check raw data file name to avoid 'setGraphic' invocation for other items from
              // different thread, where 'updateItem' is called. Can it be done better?!
              if (rawDataFile.getName().equals(getText())) {
                setGraphic(new ImageView(FxIconUtil.getFileIcon(newColor)));
              }
            });
          }

          @Override
          public void commitEdit(GroupableListViewEntity item) {
            super.commitEdit(item);
            if (item instanceof GroupEntity) {
              return;
            }

            RawDataFileRenameModule.renameFile(((ValueEntity<RawDataFile>) item).getValue(),
                getText());
          }
        });

    // Add mouse clicked event handler
    rawDataList.setOnMouseClicked(event -> {
      if (event.getClickCount() == 2) {
        RawDataFile clickedFile = MZmineGUI.getSelectedRawDataFiles().get(0);
        if (clickedFile instanceof IMSRawDataFile) {
          handleShowIMSDataOverview(event);
        } else if (clickedFile instanceof ImagingRawDataFile) {
          handleShowImage(event);
        } else {
          handleShowRawDataOverview(event);
        }
      }
    });

    // Add long mouse pressed event handler
    rawDataList.addEventHandler(MouseEvent.ANY, new EventHandler<MouseEvent>() {
      final PauseTransition timer = new PauseTransition(Duration.millis(800));

      @Override
      public void handle(MouseEvent event) {
        timer.setOnFinished(e -> {
          rawDataList.setEditable(true);
          rawDataList.edit(rawDataList.getSelectionModel().getSelectedIndex());
        });

        if (event.getEventType().equals(MouseEvent.MOUSE_PRESSED)) {
          timer.playFromStart();
        } else if (event.getEventType().equals(MouseEvent.MOUSE_RELEASED) || event.getEventType()
            .equals(MouseEvent.MOUSE_DRAGGED)) {
          timer.stop();
        }
      }
    });

    featureListsList.setCellFactory(featureListView -> new GroupableListViewCell<FeatureList>() {
      @Override
      protected void updateItem(GroupableListViewEntity item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || (item == null)) {
          setText("");
          setGraphic(null);
          return;
        }
        if (item instanceof GroupEntity) {
          return;
        }

        FeatureList featureList = ((ValueEntity<FeatureList>) item).getValue();

        setText(featureList.getName());
        if (featureList.isAligned()) {
          setGraphic(new ImageView(featureListAlignedIcon));
        } else {
          setGraphic(new ImageView(featureListSingleIcon));
        }
      }

      @Override
      public void commitEdit(GroupableListViewEntity item) {
        super.commitEdit(item);
        if (item instanceof GroupEntity) {
          return;
        }

        ((ValueEntity<FeatureList>) item).getValue().setName(getText());
      }
    });

    // Add mouse clicked event handler
    featureListsList.setOnMouseClicked(event -> {
      if (event.getClickCount() == 2) {
        handleOpenFeatureList(event);
      }
    });

    // Notify selected tab about raw data files selection change
    rawDataList.getSelectedValues().addListener((ListChangeListener<RawDataFile>) change -> {

      // Add listener body to the event queue to run it after all selected items are added to
      // the observable list, so the collections' elements equality test in the if statement will
      // compare final result of the multiple selection
      MZmineCore.runLater(() -> {
        change.next();

        for (Tab tab : MZmineCore.getDesktop().getAllTabs()) {
          if (tab instanceof MZmineTab && tab.isSelected()
              && ((MZmineTab) tab).isUpdateOnSelection() && !(CollectionUtils.isEqualCollection(
              ((MZmineTab) tab).getRawDataFiles(), change.getList()))) {
            ((MZmineTab) tab).onRawDataFileSelectionChanged(change.getList());
          }
        }
      });
    });

    featureListsList.getSelectedValues().addListener((ListChangeListener<FeatureList>) change -> {
      MZmineCore.runLater(() -> {
        change.next();
        for (Tab tab : MZmineCore.getDesktop().getAllTabs()) {
          if (tab instanceof MZmineTab && tab.isSelected()
              && ((MZmineTab) tab).isUpdateOnSelection() && !(CollectionUtils.isEqualCollection(
              ((MZmineTab) tab).getFeatureLists(), change.getList()))) {
            ((MZmineTab) tab).onFeatureListSelectionChanged(change.getList());
          }
        }
      });
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

    ObservableList<WrappedTask> tasksQueue = MZmineCore.getTaskController().getTaskQueue()
        .getTasks();
    tasksView.setItems(tasksQueue);

    taskNameColumn.setCellValueFactory(
        cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getActualTask().getTaskDescription()));
    taskPriorityColumn.setCellValueFactory(new PropertyValueFactory<>("priority"));

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

    // Update rawDataList context menu depending on selected items
    rawDataList.getSelectionModel().getSelectedItems()
        .addListener((ListChangeListener<GroupableListViewEntity>) change -> {
          while (change.next()) {
            if (change.getList() == null) {
              return;
            }

            // Setting color should be enabled only if files are selected
            rawDataSetColorMenu.setDisable(rawDataList.getSelectedValues().size() <= 0);

            if (rawDataList.getSelectedValues().size() == 1) {
              rawDataRemoveMenuItem.setText("Remove file");
              rawDataRemoveExtensionMenuItem.setText("Remove file extension");
            } else {
              rawDataRemoveMenuItem.setText("Remove files");
              rawDataRemoveExtensionMenuItem.setText("Remove files' extensions");
            }

            if (rawDataList.getSelectionModel().getSelectedItems().size() == 1) {
              rawDataRenameMenuItem.setDisable(false);
              if (rawDataList.getSelectionModel().getSelectedItems()
                  .get(0) instanceof GroupEntity) {
                rawDataRenameMenuItem.setText("Rename group");
              } else {
                rawDataRenameMenuItem.setText("Rename file");
              }
            } else {
              rawDataRenameMenuItem.setDisable(true);
            }
          }
        });

    featureListsList.getSelectionModel().getSelectedItems()
        .addListener((ListChangeListener<GroupableListViewEntity>) change -> {
          while (change.next()) {
            if (change.getList() == null) {
              return;
            }

            if (featureListsList.getSelectedValues().size() == 1) {
              openFeatureListMenuItem.setText("Open feature list");
              showFeatureListSummaryMenuItem.setText("Show feature list summary");
              featureListsRemoveMenuItem.setText("Remove feature list");
            } else {
              openFeatureListMenuItem.setText("Open feature lists");
              showFeatureListSummaryMenuItem.setText("Show feature lists summary");
              featureListsRemoveMenuItem.setText("Remove feature lists");
            }

            if (featureListsList.getSelectionModel().getSelectedItems().size() == 1) {
              featureListsRenameMenuItem.setDisable(false);
              if (featureListsList.getSelectionModel().getSelectedItems()
                  .get(0) instanceof GroupEntity) {
                featureListsRenameMenuItem.setText("Rename group");
              } else {
                featureListsRenameMenuItem.setText("Rename file");
              }
            } else {
              featureListsRenameMenuItem.setDisable(true);
            }
          }
        });

    addTab(new MZmineIntroductionTab());

    rawDataFileColorPicker.selectedColorProperty().addListener((observable, oldValue, newValue) -> {
      if (rawDataList.getSelectionModel() == null) {
        return;
      }

      ObservableList<RawDataFile> rows = rawDataList.getSelectedValues();
      if (rows == null) {
        return;
      }
      for (var row : rows) {
        row.setColor(newValue);
      }
    });
  }

  public GroupableListView<RawDataFile> getRawDataList() {
    return rawDataList;
  }

  public GroupableListView<FeatureList> getFeatureListsList() {
    return featureListsList;
  }

  public ListView<SpectralLibrary> getSpectralLibraryList() {
    return spectralLibraryList;
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
    ParameterSet parameters = MZmineCore.getConfiguration()
        .getModuleParameters(ChromatogramVisualizerModule.class);
    parameters.getParameter(TICVisualizerParameters.DATA_FILES)
        .setValue(RawDataFilesSelectionType.SPECIFIC_FILES,
            selectedFiles.toArray(new RawDataFile[0]));
    ExitCode exitCode = parameters.showSetupDialog(true);
    if (exitCode == ExitCode.OK) {
      MZmineCore.runMZmineModule(ChromatogramVisualizerModule.class, parameters);
    }
  }

  public void handleShowRawDataOverview(Event event) {
    logger.finest("Activated Show raw data overview menu item");
    var selectedFiles = MZmineGUI.getSelectedRawDataFiles();
    ParameterSet parameters = MZmineCore.getConfiguration()
        .getModuleParameters(RawDataOverviewModule.class);
    parameters.getParameter(RawDataOverviewParameters.rawDataFiles)
        .setValue(RawDataFilesSelectionType.SPECIFIC_FILES,
            selectedFiles.toArray(new RawDataFile[0]));
    MZmineCore.runMZmineModule(RawDataOverviewModule.class, parameters);
  }

  public void handleShowIMSDataOverview(Event event) {
    logger.finest("Activated Show ion mobility raw data overview");
    var selectedFiles = MZmineGUI.getSelectedRawDataFiles();
    ParameterSet parameters = MZmineCore.getConfiguration()
        .getModuleParameters(IMSRawDataOverviewModule.class);
    parameters.getParameter(RawDataOverviewParameters.rawDataFiles)
        .setValue(RawDataFilesSelectionType.SPECIFIC_FILES,
            selectedFiles.toArray(new RawDataFile[0]));
    MZmineCore.runMZmineModule(IMSRawDataOverviewModule.class, parameters);
  }

  public void handleShowImageViewer(Event event) {
    logger.finest("Activated Show image viewer");
    var selectedFiles = MZmineGUI.getSelectedRawDataFiles();
    ParameterSet parameters = MZmineCore.getConfiguration()
        .getModuleParameters(ImageVisualizerModule.class);
    parameters.getParameter(RawDataOverviewParameters.rawDataFiles)
        .setValue(RawDataFilesSelectionType.SPECIFIC_FILES,
            selectedFiles.toArray(new RawDataFile[0]));
    MZmineCore.runMZmineModule(ImageVisualizerModule.class, parameters);
  }


  public void handleShowMsSpectrum(Event event) {
    logger.finest("Activated Show MS spectrum menu item");
    var selectedFiles = MZmineGUI.getSelectedRawDataFiles();
    ParameterSet parameters = MZmineCore.getConfiguration()
        .getModuleParameters(SpectraVisualizerModule.class);
    parameters.getParameter(SpectraVisualizerParameters.dataFiles)
        .setValue(RawDataFilesSelectionType.SPECIFIC_FILES,
            selectedFiles.toArray(new RawDataFile[0]));
    ExitCode exitCode = parameters.showSetupDialog(true);
    if (exitCode == ExitCode.OK) {
      MZmineCore.runMZmineModule(SpectraVisualizerModule.class, parameters);
    }
  }

  public void handleShow2DPlot(Event event) {
    logger.finest("Activated Show 2D plot menu item");
    var selectedFiles = MZmineGUI.getSelectedRawDataFiles();
    ParameterSet parameters = MZmineCore.getConfiguration()
        .getModuleParameters(TwoDVisualizerModule.class);
    parameters.getParameter(TwoDVisualizerParameters.dataFiles)
        .setValue(RawDataFilesSelectionType.SPECIFIC_FILES,
            selectedFiles.toArray(new RawDataFile[0]));
    ExitCode exitCode = parameters.showSetupDialog(true);
    if (exitCode == ExitCode.OK) {
      MZmineCore.runMZmineModule(TwoDVisualizerModule.class, parameters);
    }
  }

  public void handleShow3DPlot(Event event) {
    logger.finest("Activated Show 3D plot menu item");
    var selectedFiles = MZmineGUI.getSelectedRawDataFiles();
    ParameterSet parameters = MZmineCore.getConfiguration()
        .getModuleParameters(Fx3DVisualizerModule.class);
    parameters.getParameter(Fx3DVisualizerParameters.dataFiles)
        .setValue(RawDataFilesSelectionType.SPECIFIC_FILES,
            selectedFiles.toArray(new RawDataFile[0]));
    ExitCode exitCode = parameters.showSetupDialog(true);
    if (exitCode == ExitCode.OK) {
      MZmineCore.runMZmineModule(Fx3DVisualizerModule.class, parameters);
    }
  }

  public void handleShowImage(Event event) {
    logger.finest("Activated Show image menu item");
    var selectedFiles = MZmineGUI.getSelectedRawDataFiles();
    ParameterSet parameters = MZmineCore.getConfiguration()
        .getModuleParameters(ImageVisualizerModule.class);
    parameters.getParameter(ImageVisualizerParameters.rawDataFiles)
        .setValue(RawDataFilesSelectionType.SPECIFIC_FILES,
            selectedFiles.toArray(new RawDataFile[0]));
    ExitCode exitCode = parameters.showSetupDialog(true);
    if (exitCode == ExitCode.OK) {
      MZmineCore.runMZmineModule(ImageVisualizerModule.class, parameters);
    }
  }

  public void handleShowMsMsPlot(Event event) {
    MsMsVisualizerModule module = MZmineCore.getModuleInstance(MsMsVisualizerModule.class);
    ParameterSet moduleParameters = MZmineCore.getConfiguration()
        .getModuleParameters(MsMsVisualizerModule.class);
    logger.info("Setting parameters for module " + module.getName());
    ExitCode exitCode = moduleParameters.showSetupDialog(true);
    if (exitCode != ExitCode.OK) {
      return;
    }
    ParameterSet parametersCopy = moduleParameters.cloneParameterSet();
    logger.finest("Starting module " + module.getName() + " with parameters " + parametersCopy);
    MZmineCore.runMZmineModule(MsMsVisualizerModule.class, parametersCopy);
  }

  public void handleRawDataSort(Event event) {
    rawDataList.sortSelectedItems();
  }

  public void handleFeatureListsSort(Event event) {
    featureListsList.sortSelectedItems();
  }

  public void handleRemoveFileExtension(Event event) {
    for (RawDataFile file : rawDataList.getSelectedValues()) {
      RawDataFileRenameModule.renameFile(file, FilenameUtils.removeExtension(file.getName()));
    }
    rawDataList.refresh();
  }

  public void handleExportFile(Event event) {
  }

  public void handleRenameRawData(Event event) {
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

    ParameterSet moduleParameters = MZmineCore.getConfiguration()
        .getModuleParameters(moduleJavaClass);

    logger.info("Setting parameters for module " + module.getName());

    ExitCode exitCode = moduleParameters.showSetupDialog(true);
    if (exitCode != ExitCode.OK) {
      return;
    }

    ParameterSet parametersCopy = moduleParameters.cloneParameterSet();
    logger.finest("Starting module " + module.getName() + " with parameters " + parametersCopy);
    MZmineCore.runMZmineModule(moduleJavaClass, parametersCopy);
  }

  public void handleRemoveRawData(Event event) {
    if (rawDataList.getSelectionModel() == null) {
      return;
    }

    // Show alert window
    Alert alert = new Alert(AlertType.CONFIRMATION,
        "Are you sure you want to remove selected files?", ButtonType.YES, ButtonType.NO,
        ButtonType.CANCEL);
    Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
    stage.getIcons().add(new Image("MZmineIcon.png"));
    alert.setHeaderText("Remove file");
    alert.showAndWait();

    if (alert.getResult() != ButtonType.YES) {
      return;
    }

    for (RawDataFile selectedItem : ImmutableList.copyOf(rawDataList.getSelectedValues())) {
      MZmineCore.getProjectManager().getCurrentProject().removeFile(selectedItem);
    }

    for (GroupEntity group : ImmutableList.copyOf(rawDataList.getSelectedGroups())) {
      rawDataList.ungroupItems(group);
    }
  }

  public void handleOpenFeatureList(Event event) {
    List<FeatureList> selectedFeatureLists = MZmineGUI.getSelectedFeatureLists();
    for (FeatureList fl : selectedFeatureLists) {
      // PeakListTableModule.showNewPeakListVisualizerWindow(fl);
      MZmineCore.runLater(() -> {
        FeatureTableFXUtil.addFeatureTableTab(fl);
      });
    }
  }

  public void handleShowFeatureListSummary(Event event) {
    FXMLLoader loader = new FXMLLoader(getClass().getResource("FeatureListSummary.fxml"));

    ObservableList<FeatureList> selectedValues = featureListsList.getSelectedValues();
    for (FeatureList selectedValue : selectedValues) {
      try {
        AnchorPane pane = loader.load();
        Stage stage = new Stage();
        stage.setTitle("Feature list summary - " + selectedValue.getName());
        stage.getIcons().add(FxIconUtil.loadImageFromResources("MZmineIcon.png"));
        stage.setScene(new Scene(pane));
        stage.getScene().getStylesheets()
            .addAll(MZmineCore.getDesktop().getMainWindow().getScene().getStylesheets());
        FeatureListSummaryController controller = loader.getController();
        controller.setFeatureList((ModularFeatureList) selectedValue);
        stage.show();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  public void handleShowFileSummary(Event event) {
    FXMLLoader loader = new FXMLLoader(getClass().getResource("FeatureListSummary.fxml"));

    ObservableList<RawDataFile> selectedValues = getRawDataList().getSelectedValues();
    for (RawDataFile selectedValue : selectedValues) {
      try {
        AnchorPane pane = loader.load();
        Stage stage = new Stage();
        stage.setTitle("MS data file list summary - " + selectedValue.getName());
        stage.getIcons().add(FxIconUtil.loadImageFromResources("MZmineIcon.png"));
        stage.setScene(new Scene(pane));
        stage.getScene().getStylesheets()
            .addAll(MZmineCore.getDesktop().getMainWindow().getScene().getStylesheets());
        FeatureListSummaryController controller = loader.getController();
        controller.setRawDataFile(selectedValue);
        stage.show();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  public void handleShowScatterPlot(Event event) {
    // TODO
  }

  public void handleRenameFeatureList(Event event) {
    if (featureListsList.getSelectionModel() == null) {
      return;
    }

    // Only one item must be selected
    if (featureListsList.getSelectionModel().getSelectedIndices().size() != 1) {
      return;
    }

    featureListsList.setEditable(true);
    featureListsList.edit(featureListsList.getSelectionModel().getSelectedIndex());
  }

  @FXML
  public void handleRemoveFeatureList(Event event) {
    FeatureList[] selectedFeatureLists = MZmineCore.getDesktop().getSelectedPeakLists();
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

  public void handleSpectralLibrarySort(Event event) {
    spectralLibraryList.getItems().sort(Comparator.comparing(SpectralLibrary::getName));
  }

  public void handleSpectralLibraryRemove(ActionEvent event) {
    SpectralLibrary[] libs = MZmineCore.getDesktop().getSelectedSpectralLibraries();
    if (libs != null && libs.length > 0) {
      MZmineCore.getProjectManager().getCurrentProject().removeSpectralLibrary(libs);
    }
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

  /**
   * Remove tab with matching title
   *
   * @param title the matching title
   */
  public void removeTab(String title) {
    getMainTabPane().getTabs().removeIf(t -> title.equals(t.getText()));
  }

  public void addTab(Tab tab) {
    if (tab instanceof MZmineTab) {
      ((MZmineTab) tab).updateOnSelectionProperty().addListener(((obs, old, val) -> {
        if (val) {
          if (((MZmineTab) tab).getRawDataFiles() != null && !((MZmineTab) tab).getRawDataFiles()
              .equals(rawDataList.getSelectionModel().getSelectedItems())) {
            ((MZmineTab) tab).onRawDataFileSelectionChanged(rawDataList.getSelectedValues());
          }

          if (((MZmineTab) tab).getFeatureLists() != null && !((MZmineTab) tab).getFeatureLists()
              .equals(featureListsList.getSelectionModel().getSelectedItems())) {
            ((MZmineTab) tab).onFeatureListSelectionChanged(featureListsList.getSelectedValues());
          }
        }
      }));
    }

    // sometimes we have duplicate tabs, which leads to exceptions. This is a dirty fix for now.
    getMainTabPane().getTabs().remove(tab);
    getMainTabPane().getTabs().add(tab);
    getMainTabPane().getSelectionModel().select(tab);
  }

  @FXML
  public void handleSetRawDataFileColor(Event event) {
    if (rawDataList.getSelectionModel() == null) {
      return;
    }

    ObservableList<RawDataFile> rows = rawDataList.getSelectedValues();
    // Only one file must be selected
    if (rows == null || rows.size() != 1 || rows.get(0) == null) {
      return;
    }
    // Creating new popup window
    Stage popup = new Stage();
    VBox box = new VBox(5);
    Label label = new Label("Please choose a color for \"" + rows.get(0) + "\":");
    ColorPicker picker = new ColorPicker(rows.get(0).getColor());

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
    if (rawDataList.onlyGroupsSelected()) {
      rawDataList.ungroupItems(rawDataList.getSelectedGroups());
    } else if (rawDataList.onlyGroupedItemsSelected()) {
      rawDataList.removeValuesFromGroup(rawDataList.getSelectedValues());
    } else if (rawDataList.onlyItemsSelected()) {
      rawDataList.groupSelectedItems();
    }
  }

  public VBox getBottomBox() {
    return bottomBox;
  }
}
