/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.gui;

import java.net.URL;
import javax.annotation.Nonnull;
import io.github.mzmine.datamodel.PeakList;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.gui.mainwindow.MZmineTab;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.taskcontrol.impl.WrappedTask;
import io.github.mzmine.util.ExitCode;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableView;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

/**
 * This interface represents the application GUI
 *
 */
public interface Desktop extends MZmineModule {

  /**
   * Returns a reference to main application window. May return null if MZmine is running in
   * headless (batch) mode.
   *
   * @return Main window
   */
  public Stage getMainWindow();

  /**
   * Displays a given text on the application status bar in black color
   *
   * @param text Text to show
   */
  public void setStatusBarText(String text);

  /**
   * Displays a given text on the application status bar in a given color
   *
   * @param text Text to show
   * @param textColor Text color
   */
  public void setStatusBarText(String text, Color textColor);

  /**
   * Displays a message box with a given text
   *
   * @param msg Text to show
   */
  public void displayMessage(String msg);

  /**
   * Displays a message box with a given text. Can be called from any thread.
   *
   * @param title Message box title
   * @param msg Text to show
   */
  public void displayMessage(String title, String msg);

  /**
   * Displays an error message box with a given text. Can be called from any thread.
   *
   * @param msg Text to show
   */
  public void displayErrorMessage(String msg);

  /**
   * Displays an error message. Can be called from any thread.
   *
   */
  public void displayException(Exception e);

  /**
   * Displays a confirmation Yes/No alert. Can be called from any thread.
   *
   */
  public ButtonType displayConfirmation(String msg, ButtonType... buttonTypes);

  /**
   * Returns array of currently selected raw data files in GUI
   *
   * @return Array of selected raw data files
   */
  public RawDataFile[] getSelectedDataFiles();

  /**
   * Returns array of currently selected feature lists in GUI
   *
   * @return Array of selected feature lists
   */
  public PeakList[] getSelectedPeakLists();

  @Nonnull
  public ExitCode exitMZmine();

  public TableView<WrappedTask> getTasksView();

  public void openWebPage(@Nonnull URL url);

  /**
   * Adds a tab to the main window.
   *
   * @param tab The tab {@link MZmineTab}
   * @return {@link java.util.Collection#add(Object)}
   */
  public boolean addTab(MZmineTab tab);

}
