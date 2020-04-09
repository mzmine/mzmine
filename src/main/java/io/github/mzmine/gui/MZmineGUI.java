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

package io.github.mzmine.gui;


import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import javax.annotation.Nonnull;

import io.github.mzmine.modules.io.projectload.ProjectLoadModule;
import io.github.mzmine.modules.io.rawdataimport.RawDataImportModule;
import javafx.application.HostServices;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import org.apache.commons.io.FilenameUtils;
import org.controlsfx.control.StatusBar;
import com.google.common.collect.ImmutableList;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.PeakList;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.gui.NewVersionCheck.CheckType;
import io.github.mzmine.gui.helpwindow.HelpWindow;
import io.github.mzmine.gui.mainwindow.MainWindowController;
import io.github.mzmine.main.GoogleAnalyticsTracker;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineRunnableModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.project.ProjectManager;
import io.github.mzmine.project.impl.MZmineProjectImpl;
import io.github.mzmine.taskcontrol.impl.WrappedTask;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.GUIUtils;
import io.github.mzmine.util.javafx.FxColorUtil;
import io.github.mzmine.util.javafx.FxIconUtil;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.ListView;
import javafx.scene.control.TableView;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import static io.github.mzmine.modules.io.projectload.ProjectLoaderParameters.projectFile;
import static io.github.mzmine.modules.io.rawdataimport.RawDataImportParameters.fileNames;

/**
 * MZmine JavaFX Application class
 */
public class MZmineGUI extends Application implements Desktop {

  private final Logger logger = Logger.getLogger(this.getClass().getName());

  private static final Image mzMineIcon = FxIconUtil.loadImageFromResources("MZmineIcon.png");
  private static final String mzMineFXML = "mainwindow/MainWindow.fxml";

  private static MainWindowController mainWindowController;

  private static Stage mainStage;
  private static Scene rootScene;

  @Override
  public void start(Stage stage) {

    MZmineGUI.mainStage = stage;
    MZmineCore.setDesktop(this);

    logger.finest("Initializing MZmine main window");

    try {
      // Load the main window
        URL mainFXML = this.getClass().getResource(mzMineFXML);
        FXMLLoader loader = new FXMLLoader(mainFXML);

      rootScene = loader.load();
      mainWindowController = loader.getController();
      stage.setScene(rootScene);

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
    MZmineProjectImpl currentProject =
        (MZmineProjectImpl) MZmineCore.getProjectManager().getCurrentProject();
    MZmineGUI.activateProject(currentProject);


    // Check for updated version
    NewVersionCheck NVC = new NewVersionCheck(CheckType.DESKTOP);
    Thread nvcThread = new Thread(NVC);
    nvcThread.setPriority(Thread.MIN_PRIORITY);
    nvcThread.start();

    // Tracker
    GoogleAnalyticsTracker GAT =
        new GoogleAnalyticsTracker("MZmine Loaded (GUI mode)", "/JAVA/Main/GUI");
    Thread gatThread = new Thread(GAT);
    gatThread.setPriority(Thread.MIN_PRIORITY);
    gatThread.start();

    // register shutdown hook only if we have GUI - we don't want to
    // save configuration on exit if we only run a batch
    ShutDownHook shutDownHook = new ShutDownHook();
    Runtime.getRuntime().addShutdownHook(shutDownHook);

  }

  public static void requestQuit() {
    Platform.runLater(() -> {
      Alert alert = new Alert(AlertType.CONFIRMATION);
      Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
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
    Platform.runLater(() -> {
      Alert alert = new Alert(AlertType.CONFIRMATION);
      Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
      stage.getIcons().add(mzMineIcon);
      alert.setTitle("Confirmation");
      alert.setHeaderText("Close project");
      String s = "Are you sure you want to close the current project?";
      alert.setContentText(s);
      Optional<ButtonType> result = alert.showAndWait();

      if ((result.isPresent()) && (result.get() == ButtonType.OK)) {
        // Close all windows related to previous project
        GUIUtils.closeAllWindows();

        // Create a new, empty project
        MZmineProject newProject = new MZmineProjectImpl();

        // Replace the current project with the new one
        ProjectManager projectManager = MZmineCore.getProjectManager();
        projectManager.setCurrentProject(newProject);

        MZmineCore.getDesktop().setStatusBarText("Project space cleaned");

        // Ask the garbage collector to free the previously used memory
        System.gc();
      }
    });
  }



  public static void addWindow(Node node, String title) {

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

  }

  public static void activateProject(MZmineProject project) {

    Platform.runLater(() -> {

      MZmineCore.getProjectManager().setCurrentProject(project);

      ListView<RawDataFile> rawDataTree = mainWindowController.getRawDataTree();
      rawDataTree.setItems(project.getRawDataFiles());

      ListView<PeakList> featureTree = mainWindowController.getFeatureTree();
      featureTree.setItems(project.getFeatureLists());
    });

  }

  public static @Nonnull List<RawDataFile> getSelectedRawDataFiles() {

    final var rawDataListView = mainWindowController.getRawDataTree();
    final var selectedRawDataFiles =
        ImmutableList.copyOf(rawDataListView.getSelectionModel().getSelectedItems());
    return selectedRawDataFiles;

  }

  public static @Nonnull List<PeakList> getSelectedFeatureLists() {

    final var featureListView = mainWindowController.getFeatureTree();
    final var selectedFeatureLists =
        ImmutableList.copyOf(featureListView.getSelectionModel().getSelectedItems());
    return selectedFeatureLists;

  }

  public static <ModuleType extends MZmineRunnableModule> void setupAndRunModule(
      @Nonnull Class<ModuleType> moduleClass) {

    final ParameterSet moduleParameters =
        MZmineCore.getConfiguration().getModuleParameters(moduleClass);
    ExitCode result = moduleParameters.showSetupDialog(true);
    if (result == ExitCode.OK) {
      MZmineCore.runMZmineModule(moduleClass, moduleParameters);
    }

  }

  public static void showAboutWindow() {
    // Show the about window
    Platform.runLater(() -> {
      final URL aboutPage =
          MZmineGUI.class.getClassLoader().getResource("aboutpage/AboutMZmine.html");
      HelpWindow aboutWindow = new HelpWindow(aboutPage.toString());
      aboutWindow.show();
    });
  }

  /**
   * The method activateSetOnDragOver controlling what happens when something is dragged over.
   * Implemented activateSetOnDragOver to accept when files are dragged over it.
   * @param event - DragEvent
   */
  public static void activateSetOnDragOver(DragEvent event){
    Dragboard dragBoard = event.getDragboard();
    if (dragBoard.hasFiles()) {
      event.acceptTransferModes(TransferMode.COPY);
    } else {
      event.consume();
    }
  }

  /**
   * The method activateSetOnDragDropped controlling what happens when something is dropped on window.
   * Implemented activateSetOnDragDropped to select the module according to the dropped file type and open dropped file
   * @param event - DragEvent
   */

  public static void activateSetOnDragDropped(DragEvent event){
    Dragboard dragboard = event.getDragboard();
    boolean hasFileDropped = false;
    if (dragboard.hasFiles()) {
      hasFileDropped = true;
      for (File selectedFile:dragboard.getFiles()) {

        final String extension = FilenameUtils.getExtension(selectedFile.getName());
        String[] rawDataFile = {"cdf","nc","mzData","mzML","mzXML","raw"};
        final Boolean isRawDataFile = Arrays.asList(rawDataFile).contains(extension);
        final Boolean isMZmineProject = extension.equals("mzmine");

        Class<? extends MZmineRunnableModule> moduleJavaClass = null;
        if(isMZmineProject)
        {
          moduleJavaClass = ProjectLoadModule.class;
        } else if(isRawDataFile){
          moduleJavaClass = RawDataImportModule.class;
        }

        if(moduleJavaClass != null){
          ParameterSet moduleParameters =
                  MZmineCore.getConfiguration().getModuleParameters(moduleJavaClass);
          if(isMZmineProject){
            moduleParameters.getParameter(projectFile).setValue(selectedFile);
          } else if (isRawDataFile){
            File fileArray[] = { selectedFile };
            moduleParameters.getParameter(fileNames).setValue(fileArray);
          }
          ParameterSet parametersCopy = moduleParameters.cloneParameterSet();
          MZmineCore.runMZmineModule(moduleJavaClass, parametersCopy);
        }
      }
    }
    event.setDropCompleted(hasFileDropped);
    event.consume();
  }

  @Override
  public String getName() {
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
    HostServices openWPService = getHostServices();
    openWPService.showDocument(String.valueOf(url));
  }

  @Override
  public void setStatusBarText(String message) {
    setStatusBarText(message, Color.BLACK);
  }

  @Override
  public void setStatusBarText(String message, Color textColor) {
    Platform.runLater(() -> {
      if (mainWindowController == null)
        return;
      final StatusBar statusBar = mainWindowController.getStatusBar();
      if (statusBar == null)
        return;
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
    Platform.runLater(() -> {
      Dialog<ButtonType> dialog = new Dialog<>();
      Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();
      stage.getIcons().add(mzMineIcon);
      dialog.setTitle(title);
      dialog.setContentText(msg);
      dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
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
  public RawDataFile[] getSelectedDataFiles() {
    return getSelectedRawDataFiles().toArray(new RawDataFile[0]);
  }

  @Override
  public PeakList[] getSelectedPeakLists() {
    return getSelectedFeatureLists().toArray(new PeakList[0]);
  }

  @Override
  public ExitCode exitMZmine() {

    requestQuit();
    return ExitCode.UNKNOWN;
  }


}
