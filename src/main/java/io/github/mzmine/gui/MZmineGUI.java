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

package io.github.mzmine.gui;


import static io.github.mzmine.modules.io.projectload.ProjectLoaderParameters.projectFile;

import com.google.common.collect.ImmutableList;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.gui.NewVersionCheck.CheckType;
import io.github.mzmine.gui.helpwindow.HelpWindow;
import io.github.mzmine.gui.mainwindow.MZmineTab;
import io.github.mzmine.gui.mainwindow.MainWindowController;
import io.github.mzmine.gui.mainwindow.SimpleTab;
import io.github.mzmine.gui.preferences.MZminePreferences;
import io.github.mzmine.main.GoogleAnalyticsTracker;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.main.TmpFileCleanup;
import io.github.mzmine.modules.MZmineRunnableModule;
import io.github.mzmine.modules.io.import_rawdata_all.AllSpectralDataImportModule;
import io.github.mzmine.modules.io.import_rawdata_all.AllSpectralDataImportParameters;
import io.github.mzmine.modules.io.import_spectral_library.SpectralLibraryImportParameters;
import io.github.mzmine.modules.io.projectload.ProjectLoadModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.project.impl.ProjectChangeEvent;
import io.github.mzmine.project.impl.ProjectChangeListener;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.taskcontrol.impl.WrappedTask;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.GUIUtils;
import io.github.mzmine.util.javafx.FxColorUtil;
import io.github.mzmine.util.javafx.FxIconUtil;
import io.github.mzmine.util.javafx.groupablelistview.GroupableListView;
import io.github.mzmine.util.spectraldb.entry.SpectralLibrary;
import java.io.File;
import java.net.URL;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javafx.application.Application;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.TableView;
import javafx.scene.image.Image;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.controlsfx.control.NotificationPane;
import org.controlsfx.control.StatusBar;
import org.controlsfx.control.action.Action;
import org.jetbrains.annotations.NotNull;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * MZmine JavaFX Application class
 */
public class MZmineGUI extends Application implements Desktop {

  public static final int MAX_TABS = 30;
  private static final Image mzMineIcon = FxIconUtil.loadImageFromResources("MZmineIcon.png");
  private static final String mzMineFXML = "mainwindow/MainWindow.fxml";
  private static final Logger logger = Logger.getLogger(MZmineGUI.class.getName());
  private static MainWindowController mainWindowController;
  private static Stage mainStage;
  private static Scene rootScene;
  private static WindowLocation currentTaskManagerLocation = WindowLocation.MAIN;
  private static Stage currentTaskWindow;

  public static void requestQuit() {
    MZmineCore.runLater(() -> {
      Alert alert = new Alert(AlertType.CONFIRMATION);
      Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
      stage.getScene().getStylesheets()
          .addAll(MZmineCore.getDesktop().getMainWindow().getScene().getStylesheets());
      stage.getIcons().add(mzMineIcon);
      alert.setTitle("Confirmation");
      alert.setHeaderText("Exit MZmine");
      String s = "Are you sure you want to exit?";
      alert.setContentText(s);
      Optional<ButtonType> result = alert.showAndWait();

      if ((result.isPresent()) && (result.get() == ButtonType.OK)) {
        // Quit the JavaFX thread
        Platform.exit();
        // Call System.exit() because there are probably some background
        // threads still running
        System.exit(0);
      }
    });
  }

  public static void requestCloseProject() {
    MZmineCore.runLater(() -> {
      Alert alert = new Alert(AlertType.CONFIRMATION);
      Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
      stage.getScene().getStylesheets()
          .addAll(MZmineCore.getDesktop().getMainWindow().getScene().getStylesheets());
      stage.getIcons().add(mzMineIcon);
      alert.setTitle("Confirmation");
      alert.setHeaderText("Close project");
      String s = "Are you sure you want to close the current project?";
      alert.setContentText(s);
      Optional<ButtonType> result = alert.showAndWait();

      if ((result.isPresent()) && (result.get() == ButtonType.OK)) {
        // Close all windows related to previous project
        GUIUtils.closeAllWindows();

        // Set a new project clears the old
        MZmineCore.getProjectManager().clearProject();

        MZmineCore.getDesktop().setStatusBarText("Project space cleaned");

        // Ask the garbage collector to free the previously used memory
        System.gc();
      }
    });
  }

  public static Stage addWindow(Node node, String title) {

    BorderPane parent = new BorderPane();
    parent.setCenter(node);
    Scene newScene = new Scene(parent);

    // Copy CSS styles
    newScene.getStylesheets().addAll(rootScene.getStylesheets());

    Stage newStage = new Stage();
    newStage.setTitle(title);
    newStage.getIcons().add(mzMineIcon);
    newStage.setScene(newScene);
    newStage.show();
    return newStage;
  }

  public static void activateProject(@NotNull MZmineProject project) {

    MZmineCore.runLater(() -> {

      MZmineCore.getProjectManager().setCurrentProject(project);
      if (mainWindowController != null) {
        GroupableListView<RawDataFile> rawDataList = mainWindowController.getRawDataList();
        rawDataList.setValues(project.getCurrentRawDataFiles());

        GroupableListView<FeatureList> featureListsList = mainWindowController.getFeatureListsList();
        featureListsList.setValues(project.getCurrentFeatureLists());

        var libraryList = mainWindowController.getSpectralLibraryList();
        final var fxLibs = FXCollections.observableArrayList(project.getCurrentSpectralLibraries());
        libraryList.setItems(fxLibs);

        // add project listener to update the views
        project.addProjectListener(new ProjectChangeListener() {
          @Override
          public void dataFilesChanged(ProjectChangeEvent<RawDataFile> event) {
            switch (event.change()) {
              case ADDED -> rawDataList.addItems(event.changedLists());
              case REMOVED -> rawDataList.removeItems(event.changedLists());
              case UPDATED, RENAMED -> rawDataList.updateItems();
            }
          }

          @Override
          public void featureListsChanged(ProjectChangeEvent<FeatureList> event) {
            switch (event.change()) {
              case ADDED -> featureListsList.addItems(event.changedLists());
              case REMOVED -> featureListsList.removeItems(event.changedLists());
              case UPDATED, RENAMED -> featureListsList.updateItems();
            }
          }

          @Override
          public void librariesChanged(ProjectChangeEvent<SpectralLibrary> event) {
            MZmineCore.runLater(() -> fxLibs.setAll(project.getCurrentSpectralLibraries()));
          }
        });
      }
    });

  }

  @NotNull
  public static List<RawDataFile> getSelectedRawDataFiles() {
    final GroupableListView<RawDataFile> rawDataListView = mainWindowController.getRawDataList();
    return ImmutableList.copyOf(rawDataListView.getSelectedValues());
  }

  @NotNull
  public static List<FeatureList> getSelectedFeatureLists() {
    final GroupableListView<FeatureList> featureListView = mainWindowController.getFeatureListsList();
    return ImmutableList.copyOf(featureListView.getSelectedValues());
  }

  @NotNull
  public static List<SpectralLibrary> getSelectedSpectralLibraryList() {
    final var spectralLibraryView = mainWindowController.getSpectralLibraryList();
    return FXCollections.unmodifiableObservableList(
        spectralLibraryView.getSelectionModel().getSelectedItems());
  }

  @NotNull
  public static <ModuleType extends MZmineRunnableModule> void setupAndRunModule(
      Class<ModuleType> moduleClass) {

    final ParameterSet moduleParameters = MZmineCore.getConfiguration()
        .getModuleParameters(moduleClass);
    ExitCode result = moduleParameters.showSetupDialog(true);
    if (result == ExitCode.OK) {
      MZmineCore.runMZmineModule(moduleClass, moduleParameters);
    }

  }

  public static void showAboutWindow() {
    // Show the about window
    MZmineCore.runLater(() -> {
      final URL aboutPage = MZmineGUI.class.getClassLoader()
          .getResource("aboutpage/AboutMZmine.html");
      HelpWindow aboutWindow = new HelpWindow(aboutPage.toString());
      aboutWindow.show();
    });
  }

  /**
   * The method activateSetOnDragOver controlling what happens when something is dragged over.
   * Implemented activateSetOnDragOver to accept when files are dragged over it.
   *
   * @param event - DragEvent
   */
  public static void activateSetOnDragOver(DragEvent event) {
    Dragboard dragBoard = event.getDragboard();
    if (dragBoard.hasFiles()) {
      event.acceptTransferModes(TransferMode.COPY);
    } else {
      event.consume();
    }
  }

  /**
   * The method activateSetOnDragDropped controlling what happens when something is dropped on
   * window. Implemented activateSetOnDragDropped to select the module according to the dropped file
   * type and open dropped file
   *
   * @param event - DragEvent
   */

  public static void activateSetOnDragDropped(DragEvent event) {
    Dragboard dragboard = event.getDragboard();
    boolean hasFileDropped = false;
    if (dragboard.hasFiles()) {
      hasFileDropped = true;

      final List<String> rawExtensions = List.of("mzml", "mzxml", "raw", "cdf", "netcdf", "nc",
          "mzdata", "imzml", "tdf", "d", "tsf", "zip", "gz");
      final List<String> libraryExtensions = List.of("json", "mgf", "msp", "jdx");

      final List<File> rawDataFiles = new ArrayList<>();
      final List<File> libraryFiles = new ArrayList<>();

      for (File selectedFile : dragboard.getFiles()) {
        final String extension = FilenameUtils.getExtension(selectedFile.getName()).toLowerCase();
        final boolean isRawDataFile = rawExtensions.contains(extension);
        final boolean isLibraryFile = libraryExtensions.contains(extension);
        final boolean isMZmineProject = extension.equals("mzmine");

        Class<? extends MZmineRunnableModule> moduleJavaClass = null;
        if (isMZmineProject) {
          logger.finest(
              () -> "Importing project " + selectedFile.getAbsolutePath() + " via drag and drop.");
          moduleJavaClass = ProjectLoadModule.class;
          ParameterSet moduleParameters = MZmineCore.getConfiguration()
              .getModuleParameters(moduleJavaClass);
          moduleParameters.getParameter(projectFile).setValue(selectedFile);
          ParameterSet parametersCopy = moduleParameters.cloneParameterSet();
          MZmineCore.runMZmineModule(moduleJavaClass, parametersCopy);
        } else if (isRawDataFile) {
          // add to raw files list
          rawDataFiles.add(selectedFile);
        }

        // in case a library format is also supported as raw data format - import as both
        if (isLibraryFile) {
          libraryFiles.add(selectedFile);
        }
      }

      if (!rawDataFiles.isEmpty() || !libraryFiles.isEmpty()) {
        if (!rawDataFiles.isEmpty()) {
          logger.finest(() -> "Importing " + rawDataFiles.size() + " raw files via drag and drop: "
              + rawDataFiles.stream().map(File::getAbsolutePath).collect(Collectors.joining(", ")));
        }
        if (!libraryFiles.isEmpty()) {
          logger.finest(() -> "Importing " + libraryFiles.size() + " raw files via drag and drop: "
              + libraryFiles.stream().map(File::getAbsolutePath).collect(Collectors.joining(", ")));
        }

        // set raw and library files to parameter
        ParameterSet param = MZmineCore.getConfiguration()
            .getModuleParameters(AllSpectralDataImportModule.class).cloneParameterSet();
        param.setParameter(AllSpectralDataImportParameters.advancedImport, false);
        param.setParameter(AllSpectralDataImportParameters.fileNames,
            rawDataFiles.toArray(File[]::new));
        param.setParameter(SpectralLibraryImportParameters.dataBaseFiles,
            libraryFiles.toArray(File[]::new));

        // start import task for libraries and raw data files
        AllSpectralDataImportModule module = MZmineCore.getModuleInstance(
            AllSpectralDataImportModule.class);
        if (module != null) {
          List<Task> tasks = new ArrayList<>();
          module.runModule(MZmineCore.getProjectManager().getCurrentProject(), param, tasks,
              Instant.now());
          MZmineCore.getTaskController().addTasks(tasks.toArray(Task[]::new));
        }
      }
    }
    event.setDropCompleted(hasFileDropped);
    event.consume();
  }

  public static TableView<WrappedTask> removeTasksFromBottom() {
    TableView<WrappedTask> tasksView = mainWindowController.getTasksView();
    mainWindowController.getBottomBox().getChildren().remove(tasksView);
    return tasksView;
  }

  public static void addTasksToBottom() {
    TableView<WrappedTask> tasksView = mainWindowController.getTasksView();
    ObservableList<Node> children = mainWindowController.getBottomBox().getChildren();
    if (!children.contains(tasksView)) {
      children.add(0, tasksView);
    }
  }

  public static void handleTaskManagerLocationChange(WindowLocation loc) {
    if (Objects.equals(loc, currentTaskManagerLocation)) {
      return;
    }

    String title = "Tasks";
    TableView<WrappedTask> tasksView = mainWindowController.getTasksView();

    // remove
    switch (currentTaskManagerLocation) {
      case TAB -> mainWindowController.removeTab(title);
      case MAIN -> removeTasksFromBottom();
      case HIDDEN -> {
      }
      case EXTERNAL -> {
        if (currentTaskWindow != null) {
          currentTaskWindow.close();
          currentTaskWindow = null;
        }
      }
    }

    // add
    switch (loc) {
      case TAB -> {
        MZmineTab tab = new SimpleTab(title);
        tab.setContent(tasksView);
        MZmineCore.getDesktop().addTab(tab);
      }
      case EXTERNAL -> {
        currentTaskWindow = addWindow(tasksView, title);
      }
      case MAIN -> addTasksToBottom();
      case HIDDEN -> {
      }
    }

    currentTaskManagerLocation = loc;
  }

  @Override
  public void start(Stage stage) {

    MZmineGUI.mainStage = stage;
    MZmineCore.setDesktop(this);

    logger.finest("Initializing MZmine main window");

    MZminePreferences preferences = MZmineCore.getConfiguration().getPreferences();
    try {
      // Load the main window
      URL mainFXML = this.getClass().getResource(mzMineFXML);
      FXMLLoader loader = new FXMLLoader(mainFXML);

      rootScene = loader.load();
      mainWindowController = loader.getController();
      stage.setScene(rootScene);
      preferences.getValue(MZminePreferences.theme).apply(rootScene.getStylesheets());

    } catch (Exception e) {
      e.printStackTrace();
      logger.severe("Error loading MZmine GUI from FXML: " + e);
      Platform.exit();
    }

    stage.setTitle("MZmine " + MZmineCore.getMZmineVersion());
    stage.setMinWidth(600);
    stage.setMinHeight(400);

    // Set application icon
    stage.getIcons().setAll(mzMineIcon);

    stage.setOnCloseRequest(e -> {
      requestQuit();
      e.consume();
    });

    // Drag over surface
    rootScene.setOnDragOver(MZmineGUI::activateSetOnDragOver);
    // Dropping over surface
    rootScene.setOnDragDropped(MZmineGUI::activateSetOnDragDropped);

    // Configure desktop properties such as the application taskbar icon
    // on a new thread. It is important to start this thread after the
    // JavaFX subsystem has started. Otherwise we could be treated like a
    // Swing application.
    Thread desktopSetupThread = new Thread(new DesktopSetup());
    desktopSetupThread.setPriority(Thread.MIN_PRIORITY);
    desktopSetupThread.start();

    setStatusBarText("Welcome to MZmine " + MZmineCore.getMZmineVersion());

    stage.show();

    // show message that temp folder should be setup
    if (preferences.getValue(MZminePreferences.showTempFolderAlert)) {
      File tmpPath = preferences.getValue(MZminePreferences.tempDirectory);
      File userDir = FileUtils.getUserDirectory();
      if (tmpPath == null || !tmpPath.exists() || tmpPath.getAbsolutePath().toLowerCase()
          .contains("users") || tmpPath.equals(userDir)) {
        MZmineCore.runLater(() -> displayNotification("""
                Set temp folder to a fast local drive (prefer a public folder over a user folder).
                MZmine stores data on disk. Ensure enough free space. Otherwise change the memory options.
                """, "Change", MZmineCore::openTempPreferences,
            () -> preferences.setParameter(MZminePreferences.showTempFolderAlert, false)));
      }
    }
    // update the size and position of the main window
    /*
     * ParameterSet paramSet = configuration.getPreferences(); WindowSettingsParameter settings =
     * paramSet .getParameter(MZminePreferences.windowSetttings);
     * settings.applySettingsToWindow(desktop.getMainWindow());
     */
    // add last project menu items
    /*
     * if (desktop instanceof MainWindow) { ((MainWindow) desktop).createLastUsedProjectsMenu(
     * configuration.getLastProjects()); // listen for changes
     * configuration.getLastProjectsParameter() .addFileListChangedListener(list -> { // new list of
     * last used projects Desktop desk = getDesktop(); if (desk instanceof MainWindow) {
     * ((MainWindow) desk) .createLastUsedProjectsMenu(list); } }); }
     */

    // Activate project - bind it to the desktop's project tree
    MZmineGUI.activateProject(MZmineCore.getProject());

    // Check for updated version
    NewVersionCheck NVC = new NewVersionCheck(CheckType.DESKTOP);
    Thread nvcThread = new Thread(NVC);
    nvcThread.setPriority(Thread.MIN_PRIORITY);
    nvcThread.start();

    // Tracker
    GoogleAnalyticsTracker.track("MZmine Loaded (GUI mode)", "/JAVA/Main/GUI");

    // register shutdown hook only if we have GUI - we don't want to
    // save configuration on exit if we only run a batch
    ShutDownHook shutDownHook = new ShutDownHook();
    Runtime.getRuntime().addShutdownHook(shutDownHook);
    Runtime.getRuntime().addShutdownHook(new Thread(new TmpFileCleanup()));
  }

  @Override
  public @NotNull String getName() {
    return "MZmine desktop";
  }

  @Override
  public Class<? extends ParameterSet> getParameterSetClass() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Stage getMainWindow() {
    return mainStage;
  }

  @Override
  public TableView<WrappedTask> getTasksView() {
    return mainWindowController.getTasksView();
  }

  @Override
  public void openWebPage(URL url) {
    openWebPage(String.valueOf(url));
  }

  @Override
  public void openWebPage(String url) {
    HostServices openWPService = getHostServices();
    openWPService.showDocument(url);
  }

  @Override
  public void addTab(MZmineTab tab) {
    if (mainWindowController.getTabs().size() < MAX_TABS) {
      MZmineCore.runLater(() -> mainWindowController.addTab(tab));
      return;
    } else if (mainWindowController.getTabs().size() < MAX_TABS && !getWindows().isEmpty()) {
      for (MZmineWindow window : getWindows()) {
        if (window.getNumberOfTabs() < MAX_TABS && !window.isExclusive()) {
          MZmineCore.runLater(() -> window.addTab(tab));
          return;
        }
      }
    }
    MZmineCore.runLater(() -> new MZmineWindow().addTab(tab));
  }

  @Override
  public void setStatusBarText(String message) {
    Color messageColor = MZmineCore.getConfiguration().isDarkMode() ? Color.LIGHTGRAY : Color.BLACK;
    setStatusBarText(message, messageColor);
  }

  @Override
  public void setStatusBarText(String message, Color textColor) {
    MZmineCore.runLater(() -> {
      if (mainWindowController == null) {
        return;
      }
      final StatusBar statusBar = mainWindowController.getStatusBar();
      if (statusBar == null) {
        return;
      }
      statusBar.setText(message);
      statusBar.setStyle("-fx-text-fill: " + FxColorUtil.colorToHex(textColor));
    });
  }

  @Override
  public void displayMessage(String msg) {
    displayMessage("Message", msg);
  }

  @Override
  public void displayMessage(String title, String msg) {
    MZmineCore.runLater(() -> {

      Dialog<ButtonType> dialog = new Dialog<>();
      Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();
      stage.getScene().getStylesheets()
          .addAll(MZmineCore.getDesktop().getMainWindow().getScene().getStylesheets());
      stage.getIcons().add(mzMineIcon);
      dialog.setTitle(title);
      dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);

      final Text text = new Text();
//      text.getStyleClass().add("label");
//      text.getStyleClass().add("text-id");

      text.setWrappingWidth(400);
      text.setText(msg);
      final FlowPane pane = new FlowPane(text);
      pane.setPadding(new Insets(5));
      dialog.getDialogPane().setContent(pane);

      dialog.showAndWait();
    });
  }

  @Override
  public void displayErrorMessage(String msg) {
    displayMessage("Error", msg);
  }

  @Override
  public void displayException(Exception e) {
    displayErrorMessage(e.toString());
  }

  @Override
  public ButtonType displayConfirmation(String msg, ButtonType... buttonTypes) {

    FutureTask<ButtonType> alertTask = new FutureTask<>(() -> {
      Alert alert = new Alert(AlertType.CONFIRMATION, "", buttonTypes);
      alert.getDialogPane().getScene().getStylesheets()
          .addAll(MZmineCore.getDesktop().getMainWindow().getScene().getStylesheets());
      Text text = new Text(msg);
      text.setWrappingWidth(400);
      final FlowPane pane = new FlowPane(text);
      pane.setPadding(new Insets(5));
      alert.getDialogPane().setContent(pane);
      alert.setWidth(400);
      alert.showAndWait();
      return alert.getResult();
    });

    // Show the dialog
    try {
      if (Platform.isFxApplicationThread()) {
        alertTask.run();
      } else {
        MZmineCore.runLater(alertTask);
      }
      return alertTask.get();
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  @Override
  public void displayNotification(String msg, String buttonText, Runnable action,
      Runnable hideForeverAction) {
    logger.log(Level.INFO, msg);
    NotificationPane pane = mainWindowController.getNotificationPane();
    pane.getActions().clear();
    if (hideForeverAction != null) {
      pane.getActions().add(new Action("Hide ∞", ae -> {
        hideForeverAction.run();
        pane.hide();
      }));
    }

    Action buttonAction = new Action(buttonText, ae -> {
      action.run();
      pane.hide();
    });
    FontIcon fontIcon = null;
    try {
      fontIcon = new FontIcon("bi-exclamation-triangle:30");
      fontIcon.setOnMouseClicked(event -> buttonAction.handle(null));
    } catch (Exception ex) {
      logger.log(Level.WARNING, "Cannot load icon from Ikonli" + ex.getMessage(), ex);
    }
    pane.show(msg, fontIcon, buttonAction);
  }

  @Override
  public RawDataFile[] getSelectedDataFiles() {
    return getSelectedRawDataFiles().toArray(new RawDataFile[0]);
  }

  @Override
  public FeatureList[] getSelectedPeakLists() {
    return getSelectedFeatureLists().toArray(new FeatureList[0]);
  }

  @Override
  public SpectralLibrary[] getSelectedSpectralLibraries() {
    return getSelectedSpectralLibraryList().toArray(new SpectralLibrary[0]);
  }

  @Override
  public ExitCode exitMZmine() {

    requestQuit();
    return ExitCode.UNKNOWN;
  }

  @Override
  public List<MZmineWindow> getWindows() {
    ObservableList<Window> windows = Stage.getWindows();
    List<MZmineWindow> mzmineWindows = new ArrayList<>();
    for (Window window : windows) {
      if (window instanceof MZmineWindow) {
        mzmineWindows.add((MZmineWindow) window);
      }
    }
    return mzmineWindows;
  }

  @Override
  public List<MZmineTab> getAllTabs() {
    List<MZmineTab> tabs = new ArrayList<>();

    mainWindowController.getTabs().forEach(t -> {
      if (t instanceof MZmineTab) {
        tabs.add((MZmineTab) t);
      }
    });

    getWindows().forEach(w -> w.getTabs().forEach(t -> {
      if (t instanceof MZmineTab) {
        tabs.add((MZmineTab) t);
      }
    }));

    return tabs;
  }

  @NotNull
  @Override
  public List<MZmineTab> getTabsInMainWindow() {
    List<MZmineTab> tabs = new ArrayList<>();

    mainWindowController.getTabs().forEach(t -> {
      if (t instanceof MZmineTab) {
        tabs.add((MZmineTab) t);
      }
    });

    return tabs;
  }

  public ButtonType createAlertWithOptOut(String title, String headerText, String message,
      String optOutMessage, Consumer<Boolean> optOutAction) {
    // Credits: https://stackoverflow.com/questions/36949595/how-do-i-create-a-javafx-alert-with-a-check-box-for-do-not-ask-again
    FutureTask<ButtonType> task = new FutureTask<>(() -> {
      Alert alert = new Alert(AlertType.WARNING);
      alert.getDialogPane().getScene().getStylesheets()
          .addAll(MZmineCore.getDesktop().getMainWindow().getScene().getStylesheets());
      // Need to force the alert to layout in order to grab the graphic,
      // as we are replacing the dialog pane with a custom pane
      alert.getDialogPane().applyCss();
      Node graphic = alert.getDialogPane().getGraphic();
      // Create a new dialog pane that has a checkbox instead of the hide/show details button
      // Use the supplied callback for the action of the checkbox
      alert.setDialogPane(new DialogPane() {
        @Override
        protected Node createDetailsButton() {
          CheckBox optOut = new CheckBox();
          optOut.setText(optOutMessage);
          optOut.setOnAction(e -> optOutAction.accept(optOut.isSelected()));
          return optOut;
        }
      });
      alert.getDialogPane().getButtonTypes().addAll(ButtonType.YES, ButtonType.NO);

      Text text = new Text(message);
      text.setWrappingWidth(400);
      final FlowPane pane = new FlowPane(text);
      pane.setPadding(new Insets(5));
      alert.getDialogPane().setContent(pane);
      // Fool the dialog into thinking there is some expandable content
      // a Group won't take up any space if it has no children
      alert.getDialogPane().setExpandableContent(new Group());
      alert.getDialogPane().setExpanded(true);
      // Reset the dialog graphic using the default style
      alert.getDialogPane().setGraphic(graphic);
      alert.setTitle(title);
      alert.setHeaderText(headerText);
      alert.showAndWait();
      return alert.getResult();
    });

    if (Platform.isFxApplicationThread()) {
      task.run();
    } else {
      MZmineCore.runLater(task);
    }

    try {
      return task.get();
    } catch (InterruptedException e) {
      e.printStackTrace();
    } catch (ExecutionException e) {
      e.printStackTrace();
    }
    return ButtonType.NO;
  }
}
