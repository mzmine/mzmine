/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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


import com.google.common.collect.ImmutableList;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.gui.NewVersionCheck.CheckType;
import static io.github.mzmine.gui.WindowLocation.TAB;
import io.github.mzmine.gui.mainwindow.AboutTab;
import io.github.mzmine.gui.mainwindow.GlobalKeyHandler;
import io.github.mzmine.gui.mainwindow.MZmineTab;
import io.github.mzmine.gui.mainwindow.MainWindowController;
import io.github.mzmine.gui.mainwindow.ProjectTab;
import io.github.mzmine.gui.mainwindow.SimpleTab;
import io.github.mzmine.gui.mainwindow.UsersTab;
import io.github.mzmine.gui.mainwindow.tasksview.TasksViewController;
import io.github.mzmine.gui.preferences.MZminePreferences;
import io.github.mzmine.javafx.concurrent.threading.FxThread;
import io.github.mzmine.javafx.dialogs.DialogLoggerUtil;
import io.github.mzmine.javafx.util.FxColorUtil;
import io.github.mzmine.javafx.util.FxIconUtil;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.main.TmpFileCleanup;
import io.github.mzmine.modules.MZmineRunnableModule;
import io.github.mzmine.modules.io.import_rawdata_all.AllSpectralDataImportModule;
import io.github.mzmine.modules.io.import_rawdata_all.AllSpectralDataImportParameters;
import io.github.mzmine.modules.io.import_spectral_library.SpectralLibraryImportParameters;
import io.github.mzmine.modules.io.projectload.ProjectLoadModule;
import static io.github.mzmine.modules.io.projectload.ProjectLoaderParameters.projectFile;
import io.github.mzmine.modules.tools.batchwizard.io.WizardSequenceIOUtils;
import static io.github.mzmine.modules.tools.batchwizard.io.WizardSequenceIOUtils.copyToUserDirectory;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.project.ProjectService;
import io.github.mzmine.project.impl.ProjectChangeEvent;
import io.github.mzmine.project.impl.ProjectChangeListener;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.GUIUtils;
import io.github.mzmine.util.files.ExtensionFilters;
import io.github.mzmine.util.io.SemverVersionReader;
import io.github.mzmine.util.javafx.groupablelistview.GroupableListView;
import io.github.mzmine.util.spectraldb.entry.SpectralLibrary;
import io.github.mzmine.util.web.WebUtils;
import io.mzio.mzmine.gui.workspace.Workspace;
import io.mzio.mzmine.gui.workspace.WorkspaceTags;
import io.mzio.users.client.UserAuthStore;
import io.mzio.users.gui.fx.UsersViewState;
import io.mzio.users.user.CurrentUserService;
import io.mzio.users.user.MZmineUser;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
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
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.controlsfx.control.NotificationPane;
import org.controlsfx.control.StatusBar;
import org.controlsfx.control.action.Action;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * MZmine JavaFX Application class
 */
public class MZmineGUI extends Application implements MZmineDesktop, JavaFxDesktop {

  public static final int MAX_TABS = 30;
  private static final Image mzMineIcon = FxIconUtil.loadImageFromResources("mzmineIcon.png");
  private static final String mzMineFXML = "mainwindow/MainWindow.fxml";
  private static final Logger logger = Logger.getLogger(MZmineGUI.class.getName());
  private static MainWindowController mainWindowController;
  private static Stage mainStage;
  private static Scene rootScene;
  private static WindowLocation currentTaskManagerLocation = WindowLocation.TAB;
  private static Stage currentTaskWindow;
  private Label statusLabel;

  public static void requestQuit() {
    FxThread.runLater(() -> {
      if (DialogLoggerUtil.showDialogYesNo("Exit mzmine", "Are you sure you want to exit?")) {
        // Quit the JavaFX thread
        Platform.exit();
        // Call System.exit() because there are probably some background
        // threads still running
        System.exit(0);
      }
    });
  }

  public static void requestCloseProject() {
    FxThread.runLater(() -> {
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
        ProjectService.getProjectManager().clearProject();

        DesktopService.getDesktop().setStatusBarText("Project space cleaned");

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
    ConfigService.getConfiguration().getTheme().apply(newScene.getStylesheets());

    Stage newStage = new Stage();
    newStage.setTitle(title);
    newStage.getIcons().add(mzMineIcon);
    newStage.setScene(newScene);
    newStage.show();
    return newStage;
  }

  public static void activateProject(@NotNull MZmineProject project) {

    FxThread.runLater(() -> {

      ProjectService.getProjectManager().setCurrentProject(project);
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
            FxThread.runLater(() -> fxLibs.setAll(project.getCurrentSpectralLibraries()));
          }
        });
      }
    });

  }

  /**
   * Currently the {@link GroupableListView} only allows sorting by name.
   */
  public static void sortRawDataFilesAlphabetically(final List<RawDataFile> raws) {
    if (mainWindowController == null) {
      return;
    }
    FxThread.runLater(() -> mainWindowController.getRawDataList().sortItemObjects(raws));
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

  public static void showAboutWindow() {
    MZmineCore.getDesktop().addTab(new AboutTab());
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
    List<String> messages = new ArrayList<>();

    Dragboard dragboard = event.getDragboard();
    boolean hasFileDropped = false;
    if (dragboard.hasFiles()) {
      hasFileDropped = true;

      final List<String> rawExtensions = ExtensionFilters.ALL_MS_DATA_FILTER.getExtensions()
          .stream().map(e -> e.replaceAll("\\*\\.", "")).toList();
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

        if (WizardSequenceIOUtils.isWizardFile(extension)) {
          boolean result = copyToUserDirectory(selectedFile);
          String resultStr = result ? "succeeded" : "failed";
          messages.add("Adding wizard file %s %s".formatted(selectedFile.getName(), resultStr));
        }
        if (UserAuthStore.isUserFile(extension)) {
          var result = UserAuthStore.copyAddUserFile(selectedFile);
          String resultStr = result ? "succeeded" : "failed";
          messages.add("Adding user %s %s".formatted(selectedFile.getName(), resultStr));
          if (result) {
            askChangeUser(selectedFile.getName());
          }
        }

//        if(selectedFile.getName().strip().toLowerCase().endsWith("mzbatch")) {
//          lastBatchFile = selectedFile;
//        }
      }

//      if (lastBatchFile != null) {
      // TODO not sure yet how to open the dialog and open the load batch dialog
//        MZmineCore.setupAndRunModule(BatchModeModule.class);
//      }

      if (!rawDataFiles.isEmpty() || !libraryFiles.isEmpty()) {
        if (!rawDataFiles.isEmpty()) {
          logger.finest(() -> "Importing " + rawDataFiles.size() + " raw files via drag and drop: "
                              + rawDataFiles.stream().map(File::getAbsolutePath)
                                  .collect(Collectors.joining(", ")));
        }
        if (!libraryFiles.isEmpty()) {
          logger.finest(() -> "Importing " + libraryFiles.size() + " raw files via drag and drop: "
                              + libraryFiles.stream().map(File::getAbsolutePath)
                                  .collect(Collectors.joining(", ")));
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
          module.runModule(ProjectService.getProjectManager().getCurrentProject(), param, tasks,
              Instant.now());
          MZmineCore.getTaskController().addTasks(tasks.toArray(Task[]::new));
        }
      }
    }
    event.setDropCompleted(hasFileDropped);
    event.consume();
  }

  private static void askChangeUser(final String fileName) {
    try {
      MZmineUser user = UserAuthStore.readUserByFileName(fileName);
      if (user == null) {
        return;
      }

      boolean changeUserResult = DialogLoggerUtil.showDialogYesNo("Changing active user",
          "Switch to user %s?".formatted(user.getNickname()));

      if (changeUserResult) {
        CurrentUserService.setUser(user);
      }
    } catch (IOException e) {
      logger.warning("Cannot find local user after copying user file by drag and drop");
    }
    UsersTab.showTab(UsersViewState.LOCAL_USERS);
  }

  public static void handleTaskManagerLocationChange(WindowLocation loc) {
    if (mainWindowController == null) {
      return;
    }

    if (loc == TAB && MZmineCore.getDesktop().getAllTabs().stream()
        .anyMatch(t -> t.getText().equals("Tasks")) || (loc != TAB && Objects.equals(loc,
        currentTaskManagerLocation))) {
      // only return if we have that tab
      return;
    }

    String title = "Tasks";
    Region tasksView = mainWindowController.getTasksView();

    // remove
    switch (currentTaskManagerLocation) {
      case TAB -> mainWindowController.removeTab(title);
      case MAIN -> mainWindowController.removeTasksFromBottom();
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
        mainWindowController.selectTab(title);
      }
      case EXTERNAL -> {
        currentTaskWindow = addWindow(tasksView, title);
      }
      case MAIN -> mainWindowController.addTasksToBottom();
      case HIDDEN -> {
      }
    }

    currentTaskManagerLocation = loc;
  }

  @Override
  public void handleShowTaskView() {
    switch (currentTaskManagerLocation) {
      case MAIN -> { // nothing, already visible
      }
      case TAB -> {
        handleTaskManagerLocationChange(TAB);
        mainWindowController.selectTab("Tasks");
      }
      case EXTERNAL -> {
        if (currentTaskWindow != null) {
          currentTaskWindow.hide();
          currentTaskWindow.show();
        } else {
          handleTaskManagerLocationChange(TAB);
        }
      }
      case HIDDEN -> handleTaskManagerLocationChange(TAB);

    }
  }

  @Override
  public void start(Stage stage) {

    MZmineGUI.mainStage = stage;

    DesktopService.setDesktop(this);

    logger.finest("Initializing mzmine main window");

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
      logger.log(Level.SEVERE, "Error loading MZmine GUI from FXML: " + e.getMessage(), e);
      Platform.exit();
    }

    stage.setTitle("mzmine " + SemverVersionReader.getMZmineVersion());
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

    setStatusBarText("Welcome to mzmine " + SemverVersionReader.getMZmineVersion());

    stage.show();

    // show message that temp folder should be setup
    if (preferences.getValue(MZminePreferences.showTempFolderAlert)) {
      File tmpPath = preferences.getValue(MZminePreferences.tempDirectory);
      File userDir = FileUtils.getUserDirectory();
      if (tmpPath == null || !tmpPath.exists() || tmpPath.getAbsolutePath().toLowerCase()
          .contains("users") || tmpPath.equals(userDir)) {
        FxThread.runLater(() -> displayNotification("""
                Set temp folder to a fast local drive (prefer a public folder over a user folder).
                mzmine stores data on disk. Ensure enough free space. Otherwise change the memory options.
                """, "Change", ConfigService::openTempPreferences,
            () -> preferences.setParameter(MZminePreferences.showTempFolderAlert, false)));
      }
    }

    // Activate project - bind it to the desktop's project tree
    MZmineGUI.activateProject(ProjectService.getProject());

    // Check for updated version
    NewVersionCheck NVC = new NewVersionCheck(CheckType.DESKTOP);
    Thread nvcThread = new Thread(NVC);
    nvcThread.setPriority(Thread.MIN_PRIORITY);
    nvcThread.start();

    // add global keys that may be added to other dialogs to receive the same key event handling
    // key typed does not work
    // using EventFilter instead of handler as this is a top level to get all events
    rootScene.addEventFilter(KeyEvent.KEY_RELEASED, GlobalKeyHandler.getInstance());

    // register shutdown hook only if we have GUI - we don't want to
    // save configuration on exit if we only run a batch
    Runtime.getRuntime().addShutdownHook(new ShutDownHook());
    Runtime.getRuntime().addShutdownHook(new Thread(new TmpFileCleanup()));
  }

  @Override
  public @NotNull String getName() {
    return "mzmine desktop";
  }

  @Override
  public Stage getMainWindow() {
    return mainStage;
  }

  @Override
  public @Nullable TasksViewController getTasksViewController() {
    return mainWindowController.getTasksViewController();
  }

  @Override
  public void openWebPage(@NotNull URL url) {
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
      FxThread.runLater(() -> mainWindowController.addTab(tab));
      return;
    } else if (mainWindowController.getTabs().size() < MAX_TABS && !getWindows().isEmpty()) {
      for (MZmineWindow window : getWindows()) {
        if (window.getNumberOfTabs() < MAX_TABS && !window.isExclusive()) {
          FxThread.runLater(() -> window.addTab(tab));
          return;
        }
      }
    }
    FxThread.runLater(() -> new MZmineWindow().addTab(tab));
  }

  @Override
  public void setStatusBarText(@Nullable String message, @Nullable Color textColor,
      @Nullable String url) {
    FxThread.runLater(() -> {
      if (mainWindowController == null) {
        return;
      }
      final StatusBar statusBar = mainWindowController.getStatusBar();
      if (statusBar == null) {
        return;
      }
      statusBar.setText(null);
      if (statusLabel != null) {
        statusBar.getLeftItems().remove(statusLabel);
      }
      statusLabel = new Label(message);
      statusBar.getLeftItems().add(statusLabel);
      if (textColor != null) {
        statusLabel.setStyle("-fx-text-fill: " + FxColorUtil.colorToHex(textColor));
      }
      if (url != null) {
        statusLabel.setOnMouseClicked(event -> {
          WebUtils.openURL(url);
          event.consume();
        });
      }
    });
  }

  @Override
  public void displayMessage(String msg) {
    displayMessage("Message", msg);
  }

  @Override
  public void displayMessage(String title, String msg) {
    displayMessage(title, msg, null);
  }

  @Override
  public void displayMessage(String title, String msg, @Nullable String url) {
    logger.info(() -> String.format("%s - %s - %s", title, msg, url));

    FxThread.runLater(() -> {

      Dialog<ButtonType> dialog = new Dialog<>();
      Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();
      stage.getScene().getStylesheets()
          .addAll(MZmineCore.getDesktop().getMainWindow().getScene().getStylesheets());
      stage.getIcons().add(mzMineIcon);
      dialog.setTitle(title);

      TextFlow flow = new TextFlow(new Text(msg + " "));
      if (url != null) {
        Hyperlink href = new Hyperlink(url);
        flow.getChildren().add(href);
        href.setOnAction(_ -> DesktopService.getDesktop().openWebPage(url));
      }

      var scroll = new ScrollPane(flow);
      scroll.setFitToWidth(true);
      scroll.setFitToHeight(true);
      var parent = new BorderPane(scroll);
      flow.setMaxWidth(720);
      stage.setMaxWidth(750);
      stage.setMaxHeight(500);
      dialog.getDialogPane().setMaxWidth(730);
      dialog.getDialogPane().setContent(parent);
      dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
      dialog.setResizable(true);
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
  public ButtonType displayConfirmation(final String title, String msg, ButtonType... buttonTypes) {
    return DialogLoggerUtil.showDialog(AlertType.CONFIRMATION, "null", msg, buttonTypes)
        .orElse(null);
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
    return getSelectedFeatureLists().stream().distinct().toArray(FeatureList[]::new);
  }

  @Override
  public SpectralLibrary[] getSelectedSpectralLibraries() {
    return getSelectedSpectralLibraryList().toArray(new SpectralLibrary[0]);
  }

  @Override
  public @NotNull ExitCode exit() {

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
  public @NotNull List<MZmineTab> getAllTabs() {
    if (mainWindowController == null) {
      return List.of();
    }
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
    if (mainWindowController == null) {
      return List.of();
    }

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
      ConfigService.getConfiguration().getTheme()
          .apply(alert.getDialogPane().getScene().getStylesheets());
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
      text.setWrappingWidth(370);
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
      FxThread.runLater(task);
    }

    try {
      return task.get();
    } catch (InterruptedException | ExecutionException e) {
      logger.log(Level.WARNING, e.getMessage(), e);
    }
    return ButtonType.NO;
  }

  public ProjectTab getSelectedProjectTab() {
    return mainWindowController.getSelectedProjectTab();
  }

  public void setWorkspace(@NotNull Workspace workspace, @NotNull EnumSet<WorkspaceTags> tags) {
    mainWindowController.setActiveWorkspace(workspace, tags);
  }

  public Workspace getActiveWorkspace() {
    return mainWindowController.getActiveWorkspace();
  }
}
