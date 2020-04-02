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
           * if (taskBar.isSupported( Taskbar.Feature.PROGRESS_STATE_WINDOW))
           * taskBar.setWindowProgressState( MZmineCore.getDesktop().getMainWindow(),
           * Taskbar.State.OFF);
           */
          if (taskBar.isSupported(Taskbar.Feature.PROGRESS_VALUE))
            taskBar.setProgressValue(-1);
          /*
           * if (taskBar.isSupported( Taskbar.Feature.PROGRESS_VALUE_WINDOW))
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
