/*
 * Copyright 2006-2016 The MZmine 3 Development Team
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

import java.awt.Color;
import java.awt.Font;
import java.awt.Taskbar;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import javax.annotation.Nonnull;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;

import org.controlsfx.control.StatusBar;

import io.github.msdk.datamodel.FeatureTable;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.PeakList;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.gui.NewVersionCheck.CheckType;
import io.github.mzmine.gui.mainwindow.MainWindowController;
import io.github.mzmine.main.GoogleAnalyticsTracker;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineRunnableModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.components.MultiLineToolTipUI;
import io.github.mzmine.util.javafx.FxIconUtil;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

/**
 * MZmine JavaFX Application class
 */
public class MZmineGUI extends Application implements Desktop {

    private final Logger logger = Logger.getLogger(this.getClass().getName());

    private static final Image mzMineIcon = FxIconUtil
            .loadImageFromResources("MZmineIcon.png");
    private static final String mzMineFXML = "mainwindow/MainWindow.fxml";

    private static MainWindowController mainWindowController;

    private static Stage mainStage;
    private static Scene rootScene;

   // private static TaskbarProgressbar taskProgressbar;

    @Override
    public void start(Stage stage) {

        MZmineGUI.mainStage = stage;
        MZmineCore.setDesktop(this);
        
        logger.finest("Initializing MZmine GUI");
        configureGUI();
        
        

      //  taskProgressbar = new TaskbarProgressbar(stage);

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
        stage.setMinWidth(300);
        stage.setMinHeight(300);

        // Set application icon
        stage.getIcons().setAll(mzMineIcon);

        stage.setOnCloseRequest(e -> {
            requestQuit();
            e.consume();
        });

        stage.show();

        // update the size and position of the main window
        /*
         * ParameterSet paramSet = configuration.getPreferences();
         * WindowSettingsParameter settings = paramSet
         * .getParameter(MZminePreferences.windowSetttings);
         * settings.applySettingsToWindow(desktop.getMainWindow());
         */
        // add last project menu items
        /*
         * if (desktop instanceof MainWindow) { ((MainWindow)
         * desktop).createLastUsedProjectsMenu(
         * configuration.getLastProjects()); // listen for changes
         * configuration.getLastProjectsParameter()
         * .addFileListChangedListener(list -> { // new list of last used
         * projects Desktop desk = getDesktop(); if (desk instanceof MainWindow)
         * { ((MainWindow) desk) .createLastUsedProjectsMenu(list); } }); }
         */

        // Check for updated version
        NewVersionCheck NVC = new NewVersionCheck(CheckType.DESKTOP);
        Thread nvcThread = new Thread(NVC);
        nvcThread.setPriority(Thread.MIN_PRIORITY);
        nvcThread.start();

        // Tracker
        GoogleAnalyticsTracker GAT = new GoogleAnalyticsTracker(
                "MZmine Loaded (GUI mode)", "/JAVA/Main/GUI");
        Thread gatThread = new Thread(GAT);
        gatThread.setPriority(Thread.MIN_PRIORITY);
        gatThread.start();

        // register shutdown hook only if we have GUI - we don't want to
        // save configuration on exit if we only run a batch
        ShutDownHook shutDownHook = new ShutDownHook();
        Runtime.getRuntime().addShutdownHook(shutDownHook);

    }

    public static void requestQuit() {
        Alert alert = new Alert(AlertType.CONFIRMATION);
        Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
        stage.getIcons().add(mzMineIcon);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Exit MZmine");
        String s = "Are you sure you want to exit?";
        alert.setContentText(s);
        Optional<ButtonType> result = alert.showAndWait();

        if ((result.isPresent()) && (result.get() == ButtonType.OK)) {
            Platform.exit();
            System.exit(0);
        }
    }

    public static void closeProject() {
        Alert alert = new Alert(AlertType.CONFIRMATION);
        Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
        stage.getIcons().add(mzMineIcon);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Close project");
        String s = "Are you sure you want to close the current project?";
        alert.setContentText(s);
        Optional<ButtonType> result = alert.showAndWait();

        if ((result.isPresent()) && (result.get() == ButtonType.OK)) {
            /*
             * MZmineProject newProject = new MZmineProject();
             * activateProject(newProject); setStatusBarMessage("");
             */
        }
    }

    public static void displayMessage(String msg) {
        Platform.runLater(() -> {
            Dialog<ButtonType> dialog = new Dialog<>();
            Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();
            stage.getIcons().add(mzMineIcon);
            dialog.setTitle("Warning");
            dialog.setContentText(msg);
            dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
            dialog.showAndWait();
        });
    }

    public static void setStatusBarMessage(String message) {
        Platform.runLater(() -> {
            StatusBar statusBar = mainWindowController.getStatusBar();
            statusBar.setText(message);
        });
    }

    public static MainWindowController getMainWindowController() {
        return mainWindowController;
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
        MZmineCore.getProjectManager().setCurrentProject(project);

        ListView<RawDataFile> rawDataTree = mainWindowController
                .getRawDataTree();
        rawDataTree.setItems(project.rawDataFiles());

        ListView<PeakList> featureTree = mainWindowController.getFeatureTree();
        featureTree.setItems(project.featureLists());

    }

    public static @Nonnull List<RawDataFile> getSelectedRawDataFiles() {

        final ArrayList<RawDataFile> list = new ArrayList<>();
        // final TreeView<Object> rawDataTree =
        // mainWindowController.getRawDataTree();
        // for (TreeItem<Object> item :
        // rawDataTree.getSelectionModel().getSelectedItems()) {
        // if (!(item.getValue() instanceof RawDataFile))
        // continue;
        // RawDataFile file = (RawDataFile) item.getValue();
        // list.add(file);
        // }

        return list;

    }

    public static @Nonnull List<FeatureTable> getSelectedFeatureTables() {

        final ArrayList<FeatureTable> list = new ArrayList<>();
        /*
         * final TreeView<Object> featureTableTree =
         * mainWindowController.getFeatureTree(); for (TreeItem<Object> item :
         * featureTableTree.getSelectionModel().getSelectedItems()) { if
         * (!(item.getValue() instanceof FeatureTable)) continue; FeatureTable
         * ft = (FeatureTable) item.getValue(); list.add(ft); }
         */
        return list;

    }

    /*
     * public static <ModuleType extends MZmineRunnableModule> void
     * setupAndRunModule(
     * 
     * @Nonnull Class<ModuleType> moduleClass) {
     * 
     * final ParameterSet moduleParameters =
     * MZmineCore.getConfiguration().getModuleParameters(moduleClass);
     * ButtonType result = moduleParameters.showSetupDialog(null); if (result ==
     * ButtonType.OK) { MZmineCore.runMZmineModule(moduleClass,
     * moduleParameters); }
     * 
     * }
     */

    private void configureGUI() {

        // Get tooltip manager instance
        ToolTipManager tooltipManager = ToolTipManager.sharedInstance();

        // Set tooltip display after 10 ms
        tooltipManager.setInitialDelay(10);

        // Never dismiss tooltips
        tooltipManager.setDismissDelay(Integer.MAX_VALUE);

        // Prepare default fonts
        Font defaultFont = new Font("SansSerif", Font.PLAIN, 13);
        Font smallFont = new Font("SansSerif", Font.PLAIN, 11);
        Font tinyFont = new Font("SansSerif", Font.PLAIN, 10);

        // Set default font
        Enumeration<Object> keys = UIManager.getDefaults().keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            Object value = UIManager.get(key);
            if (value instanceof Font)
                UIManager.put(key, defaultFont);
        }

        // Set small font where necessary
        UIManager.put("List.font", smallFont);
        UIManager.put("Table.font", smallFont);
        UIManager.put("ToolTip.font", tinyFont);

        // Set platform look & feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
            // ignore
        }

        // Set tooltip UI to support multi-line tooltips
        UIManager.put("ToolTipUI", MultiLineToolTipUI.class.getName());
        UIManager.put(MultiLineToolTipUI.class.getName(),
                MultiLineToolTipUI.class);

        // Set basic desktop handlers
        final java.awt.Desktop awtDesktop = java.awt.Desktop.getDesktop();
        if (awtDesktop != null) {

            // Setup About handler
            if (awtDesktop.isSupported(java.awt.Desktop.Action.APP_ABOUT)) {
                awtDesktop.setAboutHandler(e -> {
                    showAboutWindow();
                });
            }

            // Setup Quit handler
            if (awtDesktop
                    .isSupported(java.awt.Desktop.Action.APP_QUIT_HANDLER)) {
                awtDesktop.setQuitHandler((e, response) -> {
                    ExitCode exitCode = MZmineCore.getDesktop().exitMZmine();
                    if (exitCode == ExitCode.OK)
                        response.performQuit();
                    else
                        response.cancelQuit();
                });
            }
        }

        if (Taskbar.isTaskbarSupported()) {

            final Taskbar taskBar = Taskbar.getTaskbar();

            // Set the main app icon
            if ((mzMineIcon != null)
                    && taskBar.isSupported(Taskbar.Feature.ICON_IMAGE)) {
                final java.awt.Image mzMineIconAWT = SwingFXUtils
                        .fromFXImage(mzMineIcon, null);
                taskBar.setIconImage(mzMineIconAWT);
            }

            // Add a task controller listener to show task progress
            MZmineCore.getTaskController().addTaskControlListener(
                    (numOfWaitingTasks, percentDone) -> {
                        if (numOfWaitingTasks > 0) {
                            if (taskBar.isSupported(
                                    Taskbar.Feature.ICON_BADGE_NUMBER)) {
                                String badge = String
                                        .valueOf(numOfWaitingTasks);
                                taskBar.setIconBadge(badge);
                            }
                            
                            if (taskBar.isSupported(
                                    Taskbar.Feature.PROGRESS_VALUE))
                                taskBar.setProgressValue(percentDone);
                            

                        } else {
                            
                            if (taskBar.isSupported(
                                    Taskbar.Feature.ICON_BADGE_NUMBER))
                                taskBar.setIconBadge(null);
                            /*if (taskBar.isSupported(
                                    Taskbar.Feature.PROGRESS_STATE_WINDOW))
                                taskBar.setWindowProgressState(
                                        MZmineCore.getDesktop().getMainWindow(),
                                        Taskbar.State.OFF);*/
                            if (taskBar.isSupported(
                                    Taskbar.Feature.PROGRESS_VALUE))
                                taskBar.setProgressValue(-1);
                            /*if (taskBar.isSupported(
                                    Taskbar.Feature.PROGRESS_VALUE_WINDOW))
                                taskBar.setWindowProgressValue(
                                        MZmineCore.getDesktop().getMainWindow(),
                                        -1);
                                        */
                        }
                    });

        }

        // Let the OS decide the location of new windows. Otherwise, all windows
        // would appear at the top left corner by default.
        System.setProperty("java.awt.Window.locationByPlatform", "true");

    }

    public static <ModuleType extends MZmineRunnableModule> void setupAndRunModule(
            @Nonnull Class<ModuleType> moduleClass) {

        final ParameterSet moduleParameters = MZmineCore.getConfiguration()
                .getModuleParameters(moduleClass);
        ExitCode result = moduleParameters.showSetupDialog(null, true);
        if (result == ExitCode.OK) {
            MZmineCore.runMZmineModule(moduleClass, moduleParameters);
        }

    }

    public static void showAboutWindow() {
        // Show the about window
        try {
            final String aboutWindowFXML = "file:conf/AboutWindow.fxml";
            URL fxmlFile = new URL(aboutWindowFXML);
            FXMLLoader fxmlLoader = new FXMLLoader(fxmlFile);
            Pane pane = fxmlLoader.load();

            // Open the window
            MZmineGUI.addWindow(pane, "About MZmine");
        } catch (Exception e1) {
            e1.printStackTrace();
        }
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
    public void setStatusBarText(String message) {
        Platform.runLater(() -> {
            mainWindowController.getStatusBar().setText(message);
          });
    }

    @Override
    public void setStatusBarText(String message, Color textColor) {
        Platform.runLater(() -> {
            mainWindowController.getStatusBar().setText(message);
          });
    }

    @Override
    public void displayMessage(Stage window, String msg) {
        MZmineGUI.displayMessage(msg);
    }

    @Override
    public void displayMessage(Stage window, String title, String msg) {
        MZmineGUI.displayMessage(msg);
    }

    @Override
    public void displayErrorMessage(Stage window, String msg) {
        MZmineGUI.displayMessage(msg);
    }

    @Override
    public void displayErrorMessage(Stage window, String title, String msg) {
        MZmineGUI.displayMessage(msg);
        
    }

    @Override
    public void displayException(Stage window, Exception e) {
        MZmineGUI.displayMessage(e.toString());
    }

    @Override
    public RawDataFile[] getSelectedDataFiles() {
        return MZmineGUI.getSelectedRawDataFiles().toArray(new RawDataFile[0]);
    }

    @Override
    public PeakList[] getSelectedPeakLists() {
        return MZmineGUI.getSelectedFeatureTables().toArray(new PeakList[0]);
    }

    @Override
    public ExitCode exitMZmine() {
        
        requestQuit();
        return ExitCode.UNKNOWN;
    }
}
