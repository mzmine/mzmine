/*
 * Copyright 2006-2021 The MZmine Development Team
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

import io.github.mzmine.gui.Desktop;
import io.github.mzmine.gui.MZmineGUI;
import io.github.mzmine.gui.NewVersionCheck;
import io.github.mzmine.gui.NewVersionCheck.CheckType;
import io.github.mzmine.gui.mainwindow.introductiontab.MZmineIntroductionTab;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.MZmineRunnableModule;
import io.github.mzmine.modules.io.projectload.ProjectOpeningTask;
import io.github.mzmine.modules.tools.batchwizard.BatchWizardModule;
import io.github.mzmine.modules.visualization.spectra.msn_tree.MSnTreeVisualizerModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.project.parameterssetup.ProjectParametersSetupDialog;
import io.github.mzmine.util.ExitCode;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;

/**
 * The controller class for MainMenu.fxml
 */
public class MainMenuController {

  private static final Logger logger = Logger.getLogger(MainMenuController.class.getName());

  @FXML
  private Menu recentProjectsMenu;

  @FXML
  public void initialize() {
    fillRecentProjects();
    // disable project
    recentProjectsMenu.setDisable(true);
  }

  public void closeProject(Event event) {
    MZmineGUI.requestCloseProject();
  }

  public void exitApplication(Event event) {
    MZmineGUI.requestQuit();
  }


  public void handleShowLogFile(Event event) {

    /*
     * There doesn't seem to be any way to obtain the log file name from the logging FileHandler, so
     * it is hard-coded here for now
     */
    final Path logFilePath = Paths.get(
        System.getProperty("user.home") + File.separator + "mzmine_0_0.log");

    try {
      Desktop gui = MZmineCore.getDesktop();
      gui.openWebPage(logFilePath.toUri().toURL());
    } catch (MalformedURLException e) {
      e.printStackTrace();
    }


  }

  public void openLink(Event event) {

    assert event.getSource() instanceof MenuItem;
    final MenuItem menuItem = (MenuItem) event.getSource();
    assert menuItem.getUserData() instanceof String;
    try {
      final URL linkURL = new URL((String) menuItem.getUserData());
      // Open link in browser
      Desktop gui = MZmineCore.getDesktop();
      gui.openWebPage(linkURL);
    } catch (MalformedURLException e) {
      e.printStackTrace();
    }

  }

  public void versionCheck(Event event) {
    // Check for new version of MZmine
    logger.info("Checking for new MZmine version");
    NewVersionCheck NVC = new NewVersionCheck(CheckType.MENU);
    Thread nvcThread = new Thread(NVC);
    nvcThread.setPriority(Thread.MIN_PRIORITY);
    nvcThread.start();
  }

  public void setPreferences(Event event) {
    // Show the Preferences dialog
    logger.info("Showing the Preferences dialog");
    MZmineCore.getConfiguration().getPreferences().showSetupDialog(true);
  }

  public void showAbout(Event event) {
    MZmineGUI.showAboutWindow();
  }

  public void setSampleParams(Event event) {
    ProjectParametersSetupDialog dialog = new ProjectParametersSetupDialog();
    dialog.show();
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
      e.printStackTrace();
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

    try {
      ExitCode exitCode = moduleParameters.showSetupDialog(true);
      if (exitCode != ExitCode.OK) {
        return;
      }
    } catch (Exception e) {
      logger.log(Level.WARNING, e.getMessage(), e);
    }

    ParameterSet parametersCopy = moduleParameters.cloneParameterSet();
    logger.finest("Starting module " + module.getName() + " with parameters " + parametersCopy);
    MZmineCore.runMZmineModule(moduleJavaClass, parametersCopy);
  }

  public void fillRecentProjects(Event event) {
    fillRecentProjects();
  }

  private void fillRecentProjects() {

    recentProjectsMenu.getItems().clear();

    var recentProjects = MZmineCore.getConfiguration().getLastProjectsParameter().getValue();

    if ((recentProjects == null) || (recentProjects.isEmpty())) {
      recentProjectsMenu.setDisable(true);
      return;
    }

    recentProjectsMenu.setDisable(false);

    // add items to load last used projects directly
    final MenuItem[] items = recentProjects.stream().map(File::getAbsolutePath).map(name -> {
      MenuItem item = new MenuItem(name);

      item.setOnAction(e -> {
        MenuItem c = (MenuItem) e.getSource();
        if (c == null) {
          return;
        }
        File f = new File(c.getText());
        if (f.exists()) {
          // load file
          ProjectOpeningTask newTask = new ProjectOpeningTask(f, Instant.now());
          MZmineCore.getTaskController().addTask(newTask);
        }
      });
      return item;
    }).toArray(MenuItem[]::new);
    recentProjectsMenu.getItems().addAll(items);
  }

  public void handleAddIntroductionTab(ActionEvent event) {
    assert MZmineCore.getDesktop() != null;
    MZmineCore.getDesktop().addTab(new MZmineIntroductionTab());
  }

  public void showWizardTab(ActionEvent actionEvent) {
    BatchWizardModule inst = MZmineCore.getModuleInstance(BatchWizardModule.class);
    if (inst != null) {
      inst.showTab();
    }
  }

  public void showMSnTreeTab(ActionEvent actionEvent) {
    MSnTreeVisualizerModule.showNewTab();
  }
}


