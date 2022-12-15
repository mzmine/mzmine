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
import org.jetbrains.annotations.Nullable;

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
  Stage getMainWindow();

  /**
   * Displays a given text on the application status bar in black color
   *
   * @param text Text to show
   */
  default void setStatusBarText(@Nullable String text) {
    setStatusBarText(text, null);
  }


  /**
   * Displays a given text on the application status bar in a given color
   *
   * @param text      Text to show
   * @param textColor Text color
   */
  default void setStatusBarText(@Nullable String text, @Nullable Color textColor) {
    setStatusBarText(text, textColor, null);
  }

  /**
   * Displays a given text on the application status bar in a given color
   *
   * @param text      Text to show
   * @param textColor Text color
   * @param url       to open on click
   */
  void setStatusBarText(@Nullable String text, @Nullable Color textColor, @Nullable String url);


  /**
   * Displays a message box with a given text
   *
   * @param msg Text to show
   */
  void displayMessage(String msg);


  /**
   * Displays a message box with a given text
   *
   * @param title Message box title
   * @param msg   Text to show
   */
  void displayMessage(String title, String msg);

  /**
   * Displays a message box with a given text
   *
   * @param title Message box title
   * @param msg   Text to show
   * @[param url url to open
   */
  void displayMessage(String title, String msg, String url);

  /**
   * Displays an error message box with a given text
   *
   * @param msg Text to show
   */
  void displayErrorMessage(String msg);

  /**
   * Displays an error message
   */
  void displayException(Exception e);

  /**
   * Displays a confirmation Yes/No alert. Can be called from any thread.
   */
  ButtonType displayConfirmation(String msg, ButtonType... buttonTypes);


  /**
   * Use notification pane to display a notification on the top of the screen.
   *
   * @param msg        message to display
   * @param buttonText button text
   * @param action     button action
   */
  default void displayNotification(String msg, String buttonText, Runnable action) {
    displayNotification(msg, buttonText, action, null);
  }

  /**
   * Use notification pane to display a notification on the top of the screen.
   *
   * @param msg               message to display
   * @param buttonText        button text
   * @param action            button action
   * @param hideForeverAction hide forever button was pressed handle this option to not show again
   */
  void displayNotification(String msg, String buttonText, Runnable action,
      Runnable hideForeverAction);

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
  ButtonType createAlertWithOptOut(String title, String headerText, String message,
      String optOutMessage, Consumer<Boolean> optOutAction);


  /**
   * Returns array of currently selected raw data files in GUI
   *
   * @return Array of selected raw data files
   */
  RawDataFile[] getSelectedDataFiles();

  /**
   * Returns array of currently selected feature lists in GUI
   *
   * @return Array of selected feature lists
   */
  FeatureList[] getSelectedPeakLists();

  /**
   * Returns array of currently selected spectral libraries in GUI
   *
   * @return Array of selected spectral libraries
   */
  SpectralLibrary[] getSelectedSpectralLibraries();

  @NotNull ExitCode exitMZmine();

  TableView<WrappedTask> getTasksView();

  void openWebPage(@NotNull URL url);

  void openWebPage(String url);

  /**
   * Adds a tab to the main window. Does not have to be called in a
   * {@link io.github.mzmine.main.MZmineCore#runLater(Runnable)} environment.
   *
   * @param tab The tab {@link MZmineTab}
   */
  void addTab(MZmineTab tab);

  /**
   * @return A list of the currently opened {@link MZmineWindow}s. If there are no such windows the
   * list is empty.
   */
  List<MZmineWindow> getWindows();

  /**
   * @return A list of all currently opened tabs in all windows.
   */
  @NotNull List<MZmineTab> getAllTabs();

  /**
   * @return A list of tabs in the main window.
   */
  @NotNull List<MZmineTab> getTabsInMainWindow();

}
