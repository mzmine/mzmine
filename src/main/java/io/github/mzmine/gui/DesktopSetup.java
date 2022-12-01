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

import java.awt.Desktop;
import java.awt.Taskbar;
import java.util.logging.Logger;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.javafx.FxIconUtil;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

/**
 *
 */
public class DesktopSetup implements Runnable {

  private final Logger logger = Logger.getLogger(this.getClass().getName());

  private static final Image mzMineIcon = FxIconUtil.loadImageFromResources("MZmineIcon.png");

  @Override
  public void run() {

    logger.finest("Configuring desktop settings");

    // Set basic desktop handlers
    final Desktop awtDesktop = Desktop.getDesktop();
    if (awtDesktop != null) {

      // Setup About handler
      if (awtDesktop.isSupported(Desktop.Action.APP_ABOUT)) {
        awtDesktop.setAboutHandler(e -> {
          MZmineGUI.showAboutWindow();
        });
      }

      // Setup Quit handler
      if (awtDesktop.isSupported(Desktop.Action.APP_QUIT_HANDLER)) {
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
      if ((mzMineIcon != null) && taskBar.isSupported(Taskbar.Feature.ICON_IMAGE)) {
        final java.awt.Image mzMineIconAWT = SwingFXUtils.fromFXImage(mzMineIcon, null);
        taskBar.setIconImage(mzMineIconAWT);
      }

      // Add a task controller listener to show task progress
      MZmineCore.getTaskController().addTaskControlListener((numOfWaitingTasks, percentDone) -> {
        if (numOfWaitingTasks > 0) {
          if (taskBar.isSupported(Taskbar.Feature.ICON_BADGE_NUMBER)) {
            String badge = String.valueOf(numOfWaitingTasks);
            taskBar.setIconBadge(badge);
          }

          if (taskBar.isSupported(Taskbar.Feature.PROGRESS_VALUE))
            taskBar.setProgressValue(percentDone);

        } else {

          if (taskBar.isSupported(Taskbar.Feature.ICON_BADGE_NUMBER))
            taskBar.setIconBadge(null);
          /*
           * if (taskBar.isSupported( Taskbar.FeatureOld.PROGRESS_STATE_WINDOW))
           * taskBar.setWindowProgressState( MZmineCore.getDesktop().getMainWindow(),
           * Taskbar.State.OFF);
           */
          if (taskBar.isSupported(Taskbar.Feature.PROGRESS_VALUE))
            taskBar.setProgressValue(-1);
          /*
           * if (taskBar.isSupported( Taskbar.FeatureOld.PROGRESS_VALUE_WINDOW))
           * taskBar.setWindowProgressValue( MZmineCore.getDesktop().getMainWindow(), -1);
           */
        }
      });

    }

    // Let the OS decide the location of new windows. Otherwise, all windows
    // would appear at the top left corner by default.
    // TODO: investigate if this applies to JavaFX windows
    System.setProperty("java.awt.Window.locationByPlatform", "true");

  }
}
