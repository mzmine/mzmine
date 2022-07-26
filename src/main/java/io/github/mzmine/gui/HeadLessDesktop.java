/*
 * Copyright 2006-2022 The MZmine Development Team
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
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.taskcontrol.impl.WrappedTask;
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
import javafx.scene.control.TableView;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.jetbrains.annotations.NotNull;


public class HeadLessDesktop implements Desktop {

  private static final String MODULE_NAME = "Desktop";
  private static final Logger logger = Logger.getLogger(HeadLessDesktop.class.getName());

  @Override
  public Stage getMainWindow() {
    return null;
  }

  @Override
  public void setStatusBarText(String text) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setStatusBarText(String text, Color textColor) {
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
  public void displayErrorMessage(String msg) {
    logger.severe(msg);
  }


  @Override
  public void displayException(Exception e) {
    logger.log(Level.SEVERE, e.toString(), e);
    e.printStackTrace();
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
  public @NotNull Class<? extends ParameterSet> getParameterSetClass() {
    return SimpleParameterSet.class;
  }

  @Override
  public @NotNull String getName() {
    return MODULE_NAME;
  }

  @Override
  public @NotNull ExitCode exitMZmine() {
    System.exit(0);
    return ExitCode.OK;
  }

  @Override
  public TableView<WrappedTask> getTasksView() {
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
  public ButtonType displayConfirmation(String msg, ButtonType... buttonTypes) {
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

}
