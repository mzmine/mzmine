/*
 * Copyright (c) 2004-2023 The MZmine Development Team
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

package io.github.mzmine.gui.mainwindow;

import io.github.mzmine.gui.Desktop;
import io.github.mzmine.gui.MZmineGUI;
import io.github.mzmine.gui.NewVersionCheck;
import io.github.mzmine.gui.NewVersionCheck.CheckType;
import io.github.mzmine.gui.WindowLocation;
import io.github.mzmine.gui.mainwindow.introductiontab.MZmineIntroductionTab;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineRunnableModule;
import io.github.mzmine.modules.batchmode.ModuleQuickSelectDialog;
import io.github.mzmine.modules.io.projectload.ProjectOpeningTask;
import io.github.mzmine.modules.tools.batchwizard.BatchWizardModule;
import io.github.mzmine.modules.visualization.projectmetadata.ProjectMetadataTab;
import io.github.mzmine.modules.visualization.spectra.msn_tree.MSnTreeVisualizerModule;
import io.github.mzmine.modules.visualization.spectra.simplespectra.mirrorspectra.MirrorScanWindowFXML;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.logging.Logger;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import org.apache.commons.io.FileUtils;

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
        FileUtils.getUserDirectory() + File.separator + "mzmine_0_0.log");

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

  public void setSampleMetadata(Event event) {
    MZmineCore.getDesktop().addTab(new ProjectMetadataTab());
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
    // show setup dialog and run
    MZmineCore.setupAndRunModule(moduleJavaClass);
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

  public void setTaskViewerBottom(ActionEvent e) {
    MZmineGUI.handleTaskManagerLocationChange(WindowLocation.MAIN);
  }

  public void setTaskViewerTab(ActionEvent e) {
    MZmineGUI.handleTaskManagerLocationChange(WindowLocation.TAB);
  }

  public void setTaskViewerExternal(ActionEvent e) {
    MZmineGUI.handleTaskManagerLocationChange(WindowLocation.EXTERNAL);
  }

  public void hideTaskViewer(ActionEvent e) {
    MZmineGUI.handleTaskManagerLocationChange(WindowLocation.HIDDEN);
  }

  public void showSpectralMirrorDialog(ActionEvent event) {
    MirrorScanWindowFXML window = new MirrorScanWindowFXML();
    window.show();
  }

  public void openQuickSearch(final ActionEvent e) {
    ModuleQuickSelectDialog.openQuickSearch();
  }
}


