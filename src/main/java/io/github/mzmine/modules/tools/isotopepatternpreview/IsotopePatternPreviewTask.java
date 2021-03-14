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

package io.github.mzmine.modules.tools.isotopepatternpreview;

import java.util.logging.Logger;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.impl.SimpleIsotopePattern;
import io.github.mzmine.modules.tools.isotopeprediction.IsotopePatternCalculator;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import javafx.application.Platform;

public class IsotopePatternPreviewTask extends AbstractTask {

  private Logger logger = Logger.getLogger(this.getClass().getName());

  private String message;

  private boolean parametersChanged;
  private double minIntensity, mergeWidth;
  private int charge;
  private PolarityType polarity;
  private String formula;
  private boolean displayResult;

  SimpleIsotopePattern pattern;
  IsotopePatternPreviewDialog dialog;

  public IsotopePatternPreviewTask() {
    super(null);
    message = "Wating for parameters";
    parametersChanged = false;
    formula = "";
    minIntensity = 0.d;
    mergeWidth = 0.d;
    charge = 0;
    pattern = null;
  }

  public void initialise(String formula, double minIntensity, double mergeWidth, int charge,
      PolarityType polarity) {
    message = "Wating for parameters";
    parametersChanged = false;
    this.minIntensity = minIntensity;
    this.mergeWidth = mergeWidth;
    this.charge = charge;
    this.formula = formula;
    this.polarity = polarity;
    parametersChanged = true;
    pattern = null;
    displayResult = true;
  }

  public IsotopePatternPreviewTask(String formula, double minIntensity, double mergeWidth,
      int charge, PolarityType polarity, IsotopePatternPreviewDialog dialog) {
    super(null);
    parametersChanged = false;
    this.minIntensity = minIntensity;
    this.mergeWidth = mergeWidth;
    this.charge = charge;
    this.formula = formula;
    this.polarity = polarity;
    this.dialog = dialog;
    setStatus(TaskStatus.WAITING);
    parametersChanged = true;
    pattern = null;
    displayResult = true;
    message = "Calculating isotope pattern " + formula + ".";
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);
    message = "Calculating isotope pattern " + formula + ".";
    pattern = (SimpleIsotopePattern) IsotopePatternCalculator.calculateIsotopePattern(formula,
        minIntensity, mergeWidth, charge, polarity, true);
    if (pattern == null) {
      logger.warning("Isotope pattern could not be calculated.");
      setStatus(TaskStatus.FINISHED);
      return;
    }
    logger.finest("Pattern " + pattern.getDescription() + " calculated.");

    if (displayResult) {
      updateWindow();
      startNextThread();
    }
    setStatus(TaskStatus.FINISHED);
  }

  public void updateWindow() {
    Platform.runLater(() -> {
      dialog.updateChart(pattern);
      dialog.updateTable(pattern);
    });
  }

  public void startNextThread() {
    Platform.runLater(() -> dialog.startNextThread());
  }

  public void setDisplayResult(boolean val) {
    this.displayResult = val;
  }

  @Override
  public String getTaskDescription() {
    return message;
  }

  @Override
  public double getFinishedPercentage() {
    return 0;
  }
}
