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

package io.github.mzmine.gui.mainwindow.workspaces;

import io.github.mzmine.main.ConfigService;
import io.github.mzmine.main.MZmineConfiguration;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineRunnableModule;
import io.github.mzmine.modules.io.projectload.ProjectOpeningTask;
import io.github.mzmine.util.files.FileAndPathUtil;
import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.stage.FileChooser;

/**
 * Contains static utility methods for workspace related main-menus
 */
public class WorkspaceMenuUtils {

  private static final Logger logger = Logger.getLogger(WorkspaceMenuUtils.class.getName());

  public static void fillRecentProjects(Menu recentProjectsMenu) {
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

  /**
   * Extracts all modules from a menu. May include duplicates.
   * @param menu
   * @return
   */
  public static List<Class<? extends MZmineRunnableModule>> extractModules(Menu menu) {
    List<Class<? extends MZmineRunnableModule>> modules = new ArrayList<>();

    for (MenuItem item : menu.getItems()) {
      if(item instanceof ModuleMenuItem mmi) {
        modules.add(mmi.getModuleClass());
      }
      else if(item instanceof Menu) {
        var mods = extractModules((Menu) item);
        modules.addAll(mods);
      }
    }

    return modules;
  }
}
