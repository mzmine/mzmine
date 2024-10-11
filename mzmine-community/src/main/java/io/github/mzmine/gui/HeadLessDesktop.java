/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.spectraldb.entry.SpectralLibrary;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ButtonType;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class HeadLessDesktop implements MZmineDesktop {

  private static final Logger logger = Logger.getLogger(HeadLessDesktop.class.getName());

  @Override
  public Stage getMainWindow() {
    return null;
  }

  @Override
  public boolean isGUI() {
    return false;
  }

  @Override
  public void setStatusBarText(String text, Color textColor, String url) {
    // do nothing in headless
  }

  @Override
  public void displayMessage(String msg) {
    logger.info(msg);
  }

  @Override
  public void displayMessage(String title, String msg) {
    logger.info(msg);
  }

  @Override
  public void displayMessage(final String title, final String msg, final String url) {
    logger.info(msg);
    logger.info("URL: " + url);
  }

  @Override
  public void displayErrorMessage(String msg) {
    logger.severe(msg);
  }


  @Override
  public void displayException(Exception e) {
    logger.log(Level.SEVERE, e.toString(), e);
  }

  @Override
  public RawDataFile[] getSelectedDataFiles() {
    throw new UnsupportedOperationException();
  }

  @Override
  public FeatureList[] getSelectedPeakLists() {
    throw new UnsupportedOperationException();
  }

  @Override
  public SpectralLibrary[] getSelectedSpectralLibraries() {
    throw new UnsupportedOperationException();
  }

  @Override
  public @NotNull ExitCode exit() {
    System.exit(0);
    return ExitCode.OK;
  }

  @Override
  public @Nullable TasksViewController getTasksViewController() {
    return null;
  }

  @Override
  public void openWebPage(@NotNull URL url) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void openWebPage(String url) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void addTab(MZmineTab tab) {
  }

  @Override
  public ObservableList<MZmineWindow> getWindows() {
    return FXCollections.emptyObservableList();
  }

  @Override
  @NotNull
  public List<MZmineTab> getAllTabs() {
    return Collections.emptyList();
  }

  @NotNull
  @Override
  public List<MZmineTab> getTabsInMainWindow() {
    return Collections.emptyList();
  }

  @Override
  public ButtonType displayConfirmation(final String title, String msg, ButtonType... buttonTypes) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void displayNotification(String msg, String buttonText, Runnable action,
      Runnable hideForeverAction) {
    logger.log(Level.INFO, msg);
  }

  @Override
  public ButtonType createAlertWithOptOut(String title, String headerText, String message,
      String optOutMessage, Consumer<Boolean> optOutAction) {
    logger.warning(title + "; " + headerText + "; " + message);
    return ButtonType.YES;
  }

  @Override
  public void handleShowTaskView() {
    // nothing
  }
}
