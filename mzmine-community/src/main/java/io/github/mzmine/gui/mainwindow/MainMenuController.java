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

package io.github.mzmine.gui.mainwindow;

import io.github.mzmine.gui.DesktopService;
import io.github.mzmine.gui.MZmineDesktop;
import io.github.mzmine.gui.MZmineGUI;
import io.github.mzmine.gui.NewVersionCheck;
import io.github.mzmine.gui.NewVersionCheck.CheckType;
import io.github.mzmine.gui.WindowLocation;
import io.github.mzmine.gui.mainwindow.dependenciestab.DependenciesTab;
import io.github.mzmine.gui.mainwindow.introductiontab.MZmineIntroductionTab;
import io.github.mzmine.javafx.concurrent.threading.FxThread;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.main.MZmineConfiguration;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineRunnableModule;
import io.github.mzmine.modules.batchmode.ModuleQuickSelectDialog;
import io.github.mzmine.modules.io.projectload.ProjectOpeningTask;
import io.github.mzmine.modules.tools.batchwizard.BatchWizardModule;
import io.github.mzmine.modules.visualization.projectmetadata.ProjectMetadataTab;
import io.github.mzmine.modules.visualization.spectra.msn_tree.MSnTreeVisualizerModule;
import io.github.mzmine.modules.visualization.spectra.simplespectra.mirrorspectra.MirrorScanWindowFXML;
import io.github.mzmine.util.files.FileAndPathUtil;
import io.mzio.links.MzioMZmineLinks;
import io.mzio.users.client.UserAuthStore;
import io.mzio.users.gui.fx.UsersViewState;
import io.mzio.users.user.CurrentUserService;
import io.mzio.users.user.MZmineUser;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.stage.FileChooser;

/**
 * The controller class for MainMenu.fxml
 */
public class MainMenuController {

  private static final Logger logger = Logger.getLogger(MainMenuController.class.getName());

  public ObjectProperty<MZmineUser> currentUser = new SimpleObjectProperty<>();
  public MenuItem itemRemoveUser;

  @FXML
  private Menu recentProjectsMenu;

  @FXML
  public void initialize() {
    fillRecentProjects();
    // disable project
    recentProjectsMenu.setDisable(true);

    CurrentUserService.subscribe(user -> currentUser.set(user));
    itemRemoveUser.disableProperty().bind(currentUser.map(Objects::isNull));
    itemRemoveUser.textProperty().bind(
        currentUser.map(user -> "Remove user %s from local system".formatted(user.getNickname()))
            .orElse("Remove user"));
  }

  public void closeProject(Event event) {
    MZmineGUI.requestCloseProject();
  }

  public void exitApplication(Event event) {
    MZmineGUI.requestQuit();
  }


  public void handleShowLogFile(Event event) {
    final File logFile = ConfigService.getConfiguration().getLogFile();
    try {
      MZmineDesktop gui = MZmineCore.getDesktop();
      gui.openWebPage(logFile.toPath().toUri().toURL());
    } catch (MalformedURLException e) {
      logger.log(Level.WARNING, "Opening log file failed: " + logFile.getAbsolutePath(), e);
    }
  }

  public void openLink(Event event) {

    assert event.getSource() instanceof MenuItem;
    final MenuItem menuItem = (MenuItem) event.getSource();
    assert menuItem.getUserData() instanceof String;
    try {
      final URL linkURL = new URL((String) menuItem.getUserData());
      // Open link in browser
      MZmineDesktop gui = MZmineCore.getDesktop();
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

  public void showUsersTab(final ActionEvent e) {
    FxThread.runLater(UsersTab::showTab);
  }

  public void showUserSignUp(final ActionEvent e) {
    UsersTab.showTab(UsersViewState.LOGIN);
  }

  public void removeLocalUser(final ActionEvent e) {
    UserAuthStore.removeUserFile(CurrentUserService.getUser());
  }

  public void showDependencyTab(ActionEvent actionEvent) {
    MZmineCore.getDesktop().addTab(new DependenciesTab());
  }

  public void saveConfiguration(final ActionEvent event) {
    try {
      var chooser = new FileChooser();
      chooser.setInitialDirectory(MZmineConfiguration.CONFIG_FILE.getParentFile());
      File file = chooser.showSaveDialog(null);
      if (file == null) {
        return;
      }
      file = FileAndPathUtil.getRealFilePath(file, MZmineConfiguration.CONFIG_EXTENSION);
      ConfigService.getConfiguration().saveConfiguration(file);
    } catch (Exception ex) {
      logger.log(Level.WARNING, "Cannot save config", ex);
    }
  }

  public void loadConfiguration(final ActionEvent event) {
    try {
      var chooser = new FileChooser();
      chooser.setInitialDirectory(MZmineConfiguration.CONFIG_FILE.getParentFile());
      File file = chooser.showOpenDialog(null);
      if (file == null) {
        return;
      }
      ConfigService.getConfiguration().loadConfiguration(file, true);

    } catch (Exception ex) {
      logger.log(Level.WARNING, "Cannot save config", ex);
    }
  }

  public void openUsersDirectory(final ActionEvent event) {
    if (!Desktop.isDesktopSupported()) {
      return;
    }
    try {
      Desktop desktop = Desktop.getDesktop();
      desktop.open(UserAuthStore.getUserPath());
    } catch (IOException e) {
    }
  }

  public void openUserAccountConsole(final ActionEvent e) {
    DesktopService.getDesktop().openWebPage(MzioMZmineLinks.USER_CONSOLE.getUrl());
  }
}


