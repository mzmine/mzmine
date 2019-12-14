/*
 * Copyright 2006-2020 The MZmine Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.gui.impl;

import java.awt.Font;
import java.awt.Image;
import java.awt.Taskbar;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.ToolTipManager;
import javax.swing.UIManager;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.components.MultiLineToolTipUI;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;

/**
 * This class has just a single method which sets desktop (Swing) properties for
 * MZmine 2
 */
public class DesktopSetup {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    public void init() {

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
                    MainWindow mainWindow = (MainWindow) MZmineCore
                            .getDesktop();
                    mainWindow.showAboutDialog();
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

            // Set the app icon
            if (taskBar.isSupported(Taskbar.Feature.ICON_IMAGE)) {
                final Image mzmineIcon = MZmineCore.getDesktop()
                        .getMZmineIcon();
                if (mzmineIcon != null)
                    taskBar.setIconImage(mzmineIcon);
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
                                    Taskbar.Feature.PROGRESS_STATE_WINDOW))
                                taskBar.setWindowProgressState(
                                        MZmineCore.getDesktop().getMainWindow(),
                                        Taskbar.State.NORMAL);
                            if (taskBar.isSupported(
                                    Taskbar.Feature.PROGRESS_VALUE))
                                taskBar.setProgressValue(percentDone);
                            if (taskBar.isSupported(
                                    Taskbar.Feature.PROGRESS_VALUE_WINDOW))
                                taskBar.setWindowProgressValue(
                                        MZmineCore.getDesktop().getMainWindow(),
                                        percentDone);

                        } else {
                            if (taskBar.isSupported(
                                    Taskbar.Feature.ICON_BADGE_NUMBER))
                                taskBar.setIconBadge(null);
                            if (taskBar.isSupported(
                                    Taskbar.Feature.PROGRESS_STATE_WINDOW))
                                taskBar.setWindowProgressState(
                                        MZmineCore.getDesktop().getMainWindow(),
                                        Taskbar.State.OFF);
                            if (taskBar.isSupported(
                                    Taskbar.Feature.PROGRESS_VALUE))
                                taskBar.setProgressValue(-1);
                            if (taskBar.isSupported(
                                    Taskbar.Feature.PROGRESS_VALUE_WINDOW))
                                taskBar.setWindowProgressValue(
                                        MZmineCore.getDesktop().getMainWindow(),
                                        -1);
                        }
                    });

        }

        // Let the OS decide the location of new windows. Otherwise, all windows
        // would appear at the top left corner by default.
        System.setProperty("java.awt.Window.locationByPlatform", "true");

        // Initialize JavaFX
        try {
            logger.finest(
                    "Initializing the JavaFX subsystem by creating a JFXPanel instance");
            @SuppressWarnings("unused")
            JFXPanel dummyPanel = new JFXPanel();
            Platform.setImplicitExit(false);
        } catch (Throwable e) {
            logger.log(Level.WARNING, "Failed to initialize JavaFX", e);
            e.printStackTrace();
        }
    }

}
