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

package io.github.mzmine.gui.mainwindow.mainmenu.impl;

import static io.github.mzmine.util.javafx.FxMenuUtil.addMenuItem;
import static io.github.mzmine.util.javafx.FxMenuUtil.addSeparator;

import io.github.mzmine.gui.MZmineGUI;
import io.github.mzmine.gui.mainwindow.mainmenu.MainMenuEntries;
import io.github.mzmine.gui.mainwindow.mainmenu.MenuBuilder;
import io.github.mzmine.gui.mainwindow.mainmenu.Workspace;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.main.MZmineConfiguration;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.batchmode.BatchModeModule;
import io.github.mzmine.modules.io.projectload.ProjectLoadModule;
import io.github.mzmine.modules.io.projectload.ProjectOpeningTask;
import io.github.mzmine.modules.io.projectsave.ProjectSaveAsModule;
import io.github.mzmine.modules.io.projectsave.ProjectSaveModule;
import io.github.mzmine.modules.visualization.projectmetadata.ProjectMetadataTab;
import io.github.mzmine.util.files.FileAndPathUtil;
import java.io.File;
import java.time.Instant;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.stage.FileChooser;

public class ProjectMenuBuilder extends MenuBuilder {

  private static final Logger logger = Logger.getLogger(ProjectMenuBuilder.class.getName());

  @Override
  public Menu build(Collection<Workspace> workspaces) {
    final Menu menu = new Menu(MainMenuEntries.PROJECT.toString());
    final Menu recentProjects = new Menu("Recent projects");

    menu.setOnShowing(_ -> fillRecentProjects(recentProjects));

    addMenuItem(menu, "Open project", ProjectLoadModule.class, KeyCode.O,
        KeyCombination.SHORTCUT_DOWN);
    addMenuItem(menu, "Save project", ProjectSaveModule.class, KeyCode.S,
        KeyCombination.SHORTCUT_DOWN);
    addMenuItem(menu, "Save project as", ProjectSaveAsModule.class, KeyCode.S,
        KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN);
    addMenuItem(menu, "Close project", MZmineGUI::requestCloseProject, KeyCode.Q,
        KeyCombination.SHORTCUT_DOWN);
    addSeparator(menu);
    addMenuItem(menu, "Batch mode", BatchModeModule.class, KeyCode.B, KeyCombination.SHORTCUT_DOWN);
    addSeparator(menu);
    addMenuItem(menu, "Sample metadata",
        () -> MZmineCore.getDesktop().addTab(new ProjectMetadataTab()), KeyCode.M,
        KeyCombination.SHORTCUT_DOWN);
    addSeparator(menu);
    addMenuItem(menu, "Set preferences",
        () -> MZmineCore.getConfiguration().getPreferences().showSetupDialog(true), KeyCode.P,
        KeyCombination.SHORTCUT_DOWN);
    addMenuItem(menu, "Save configuration", ProjectMenuBuilder::saveConfiguration, null);
    addMenuItem(menu, "Load configuration", ProjectMenuBuilder::loadConfiguration, null);

    return menu;
  }

  private static void fillRecentProjects(Menu recentProjectsMenu) {
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

  public static void saveConfiguration() {
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

  public static void loadConfiguration() {
    try {
      var chooser = new FileChooser();
      chooser.setInitialDirectory(MZmineConfiguration.CONFIG_FILE.getParentFile());
      File file = chooser.showOpenDialog(null);
      if (file == null) {
        return;
      }
      ConfigService.getConfiguration().loadConfiguration(file, true);
      ConfigService.getPreferences().applyConfig();

    } catch (Exception ex) {
      logger.log(Level.WARNING, "Cannot save config", ex);
    }
  }
}
