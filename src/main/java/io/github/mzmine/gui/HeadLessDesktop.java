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

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import io.github.mzmine.datamodel.PeakList;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.taskcontrol.impl.WrappedTask;
import io.github.mzmine.util.ExitCode;
import javafx.scene.control.TableView;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class HeadLessDesktop implements Desktop {

  private static final String MODULE_NAME = "Desktop";

  private Logger logger = Logger.getLogger(this.getClass().getName());

  @Override
  public Stage getMainWindow() {
    return null;
  }

  @Override
  public void setStatusBarText(String text) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setStatusBarText(String text, Color textColor) {}

  @Override
  public void displayMessage(Stage window, String msg) {
    logger.info(msg);
  }

  @Override
  public void displayMessage(Stage window, String title, String msg) {
    logger.info(msg);
  }

  @Override
  public void displayErrorMessage(Stage window, String msg) {
    logger.severe(msg);
  }

  @Override
  public void displayErrorMessage(Stage window, String title, String msg) {
    logger.severe(msg);
  }

  @Override
  public void displayException(Stage window, Exception e) {
    logger.log(Level.SEVERE, e.toString(), e);
    e.printStackTrace();
  }

  @Override
  public RawDataFile[] getSelectedDataFiles() {
    throw new UnsupportedOperationException();
  }

  @Override
  public PeakList[] getSelectedPeakLists() {
    throw new UnsupportedOperationException();
  }

  @Override
  public @Nonnull Class<? extends ParameterSet> getParameterSetClass() {
    return SimpleParameterSet.class;
  }

  @Override
  public @Nonnull String getName() {
    return MODULE_NAME;
  }

  @Override
  public @Nonnull ExitCode exitMZmine() {
    System.exit(0);
    return ExitCode.OK;
  }

  @Override
  public TableView<WrappedTask> getTasksView() {
    return null;
  }

}
