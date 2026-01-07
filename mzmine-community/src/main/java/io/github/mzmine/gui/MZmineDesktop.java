/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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
import io.github.mzmine.gui.mainwindow.tasksview.TasksViewController;
import io.github.mzmine.javafx.concurrent.threading.FxThread;
import io.github.mzmine.util.spectraldb.entry.SpectralLibrary;
import java.util.List;
import javafx.stage.Stage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This interface represents the application GUI or headless
 */
public interface MZmineDesktop extends Desktop {

  void handleShowTaskView();

  /**
   * Returns a reference to main application window. May return null if MZmine is running in
   * headless (batch) mode.
   *
   * @return Main window
   */
  Stage getMainWindow();

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

  /**
   * Maybe add a tasksview controller in the future for headless mode?
   *
   * @return the tasksview controller if GUI mode
   */
  @Nullable TasksViewController getTasksViewController();

  /**
   * Adds a tab to the main window. Does not have to be called in a
   * {@link FxThread#runLater(Runnable)} environment.
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
