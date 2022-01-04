/*
 *  Copyright 2006-2020 The MZmine Development Team
 *
 *  This file is part of MZmine.
 *
 *  MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 *  General Public License as published by the Free Software Foundation; either version 2 of the
 *  License, or (at your option) any later version.
 *
 *  MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 *  the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 *  Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with MZmine; if not,
 *  write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 *  USA
 */

package io.github.mzmine.modules.dataprocessing.id_ccscalibration;

import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.modules.dataprocessing.id_ccscalibration.setcalibration.SetCCSCalibrationModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.MemoryMapStorage;
import java.time.Instant;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CCSCalibrationTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(CCSCalibrationTask.class.getName());

  private final CCSCalculator ccsCalculator;
  private final ParameterSet ccsCalculatorParameters;
  private final ModularFeatureList[] flists;
  private final RawDataFile[] files;
  private double progress = 0;
  private int processed = 0;

  public CCSCalibrationTask(@Nullable MemoryMapStorage storage, @NotNull Instant moduleCallDate,
      ParameterSet parameters) {
    super(storage, moduleCallDate);

    files = parameters.getValue(CCSCalibrationParameters.files).booleanValue()
        ? parameters.getParameter(CCSCalibrationParameters.files).getEmbeddedParameter().getValue()
        .getMatchingRawDataFiles() : new RawDataFile[0];

    flists = parameters.getValue(CCSCalibrationParameters.flists).getMatchingFeatureLists();

    this.ccsCalculator = (CCSCalculator) parameters.getParameter(
        CCSCalibrationParameters.calibrationType).getValue().getModule();
    this.ccsCalculatorParameters = parameters.getParameter(CCSCalibrationParameters.calibrationType)
        .getEmbeddedParameters();
  }

  @Override
  public String getTaskDescription() {
    return "CCS calibration task";
  }

  @Override
  public double getFinishedPercentage() {
    return progress;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);
    // either we have no raw data files and feature lists, or we have 1 feature list and multiple raw files.
    if (files.length != 0 && flists.length > 1) {
      setErrorMessage(
          "Invalid parameter selection. Either select feature list and >= 1 raw data file or no raw data files.");
      setStatus(TaskStatus.ERROR);
    }

    for (ModularFeatureList flist : flists) {
      logger.finest(() -> "Searching CCS Calibration for feature list " + flist.getName());

      final CCSCalibration calibration = ccsCalculator.getCalibration(flist,
          ccsCalculatorParameters);
      if (calibration == null) {
        logger.warning(() -> "No calibration found for feature list " + flist);
      }
      logger.info(
          () -> "Found ccs calibration for feature list " + flist.getName() + " " + calibration);

      if (files.length == 0) {
        flist.getRawDataFiles().stream().filter(f -> f instanceof IMSRawDataFile)
            .forEach(f -> ((IMSRawDataFile) f).setCCSCalibration(calibration));
        SetCCSCalibrationModule.setCalibrationToFiles(
            flist.getRawDataFiles().toArray(RawDataFile[]::new), calibration);
        processed++;
        progress = processed / (double) flists.length;
      } else {
        for (RawDataFile file : files) {
          if (file instanceof IMSRawDataFile imsFile) {
            imsFile.setCCSCalibration(calibration);
            processed++;
            progress = processed / (double) files.length;
          }
        }
        // for reproducibility, this might take a bit longer.
        SetCCSCalibrationModule.setCalibrationToFiles(files, calibration);
      }
    }
    setStatus(TaskStatus.FINISHED);
  }
}
