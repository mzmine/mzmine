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

package io.github.mzmine.gui;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.gui.mainwindow.MZmineTab;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.taskcontrol.impl.WrappedTask;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.spectraldb.entry.SpectralLibrary;
import java.net.URL;
import java.util.List;
import java.util.function.Consumer;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableView;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.jetbrains.annotations.NotNull;

/**
 * This interface represents the application GUI
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
   * @param text      Text to show
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
   * Displays a message box with a given text
   *
   * @param title Message box title
   * @param msg   Text to show
   */
  public void displayMessage(String title, String msg);

  /**
   * Displays an error message box with a given text
   *
   * @param msg Text to show
   */
  public void displayErrorMessage(String msg);

  /**
   * Displays an error message
   */
  public void displayException(Exception e);

  /**
   * Displays a confirmation Yes/No alert. Can be called from any thread.
   */
  public ButtonType displayConfirmation(String msg, ButtonType... buttonTypes);

  /**
   * Displays an opt-out yes/no dialog. Can be called from any thread.
   *
   * @param title
   * @param headerText
   * @param message
   * @param optOutMessage
   * @param optOutAction
   * @return {@link ButtonType#YES} or {@link ButtonType#NO}. In headless mode, YES is always
   * returned and the message is logged at warning level.
   */
  public ButtonType createAlertWithOptOut(String title, String headerText,
      String message, String optOutMessage, Consumer<Boolean> optOutAction);

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
  public FeatureList[] getSelectedPeakLists();

  /**
   * Returns array of currently selected spectral libraries in GUI
   *
   * @return Array of selected spectral libraries
   */
  SpectralLibrary[] getSelectedSpectralLibraries();

  @NotNull
  public ExitCode exitMZmine();

  public TableView<WrappedTask> getTasksView();

  public void openWebPage(@NotNull URL url);

  void openWebPage(String url);

  /**
   * Adds a tab to the main window. Does not have to be called in a {@link
   * io.github.mzmine.main.MZmineCore#runLater(Runnable)} environment.
   *
   * @param tab The tab {@link MZmineTab}
   */
  public void addTab(MZmineTab tab);

  /**
   * @return A list of the currently opened {@link MZmineWindow}s. If there are no such windows the
   * list is empty.
   */
  public List<MZmineWindow> getWindows();

  /**
   * @return A list of all currently opened tabs in all windows.
   */
  @NotNull
  public List<MZmineTab> getAllTabs();

  /**
   * @return A list of tabs in the main window.
   */
  @NotNull
  public List<MZmineTab> getTabsInMainWindow();

}
