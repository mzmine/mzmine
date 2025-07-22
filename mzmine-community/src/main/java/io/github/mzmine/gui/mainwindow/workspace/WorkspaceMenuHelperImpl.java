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

package io.github.mzmine.gui.mainwindow.workspace;

import io.github.mzmine.gui.MZmineDesktop;
import io.github.mzmine.gui.NewVersionCheck;
import io.github.mzmine.gui.NewVersionCheck.CheckType;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.main.MZmineConfiguration;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.io.projectload.ProjectOpeningTask;
import io.github.mzmine.taskcontrol.SimpleRunnableTask;
import io.github.mzmine.taskcontrol.TaskService;
import io.github.mzmine.util.files.FileAndPathUtil;
import io.mzio.mzmine.gui.workspace.WorkspaceMenuHelper;
import io.mzio.users.client.UserAuthStore;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.stage.FileChooser;

/**
 * Contains static utility methods for workspace related main-menus
 */
class WorkspaceMenuHelperImpl extends WorkspaceMenuHelper {

  private static final Logger logger = Logger.getLogger(WorkspaceMenuHelperImpl.class.getName());

  @Override
  public void openUsersDirectory() {
    if (!Desktop.isDesktopSupported()) {
      return;
    }
    try {
      Desktop desktop = Desktop.getDesktop();
      desktop.open(UserAuthStore.getUserPath());
    } catch (IOException e) {
    }
  }

  @Override
  public void fillRecentProjects(Menu recentProjectsMenu) {
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

  @Override
  public void saveConfiguration() {
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

  @Override
  public void loadConfiguration() {
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

  @Override
  public void handleShowLogFile() {
    final File logFile = ConfigService.getConfiguration().getLogFile();
    try {
      MZmineDesktop gui = MZmineCore.getDesktop();
      gui.openWebPage(logFile.toPath().toUri().toURL());
    } catch (MalformedURLException e) {
      logger.log(Level.WARNING, "Opening log file failed: " + logFile.getAbsolutePath(), e);
    }
  }

  @Override
  public void versionCheck() {
    // Check for new version of MZmine
    logger.info("Checking for new MZmine version");
    NewVersionCheck NVC = new NewVersionCheck(CheckType.MENU);
    TaskService.getController().addTask(new SimpleRunnableTask("New version check", NVC));
  }

}
