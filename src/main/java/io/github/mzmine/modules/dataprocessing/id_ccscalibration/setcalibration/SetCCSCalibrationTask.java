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

package io.github.mzmine.modules.dataprocessing.id_ccscalibration.setcalibration;

import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.modules.dataprocessing.id_ccscalibration.CCSCalibration;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.MemoryMapStorage;
import java.time.Instant;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SetCCSCalibrationTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(SetCCSCalibrationTask.class.getName());

  private final ParameterSet parameterSet;

  protected SetCCSCalibrationTask(ParameterSet parameters, @Nullable MemoryMapStorage storage,
      @NotNull Instant moduleCallDate) {
    super(storage, moduleCallDate);
    this.parameterSet = parameters;
  }

  @Override
  public String getTaskDescription() {
    return "Setting ccs calibration to raw data file.";
  }

  @Override
  public double getFinishedPercentage() {
    return 1;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);
    final RawDataFile[] files = parameterSet.getParameter(SetCCSCalibrationParameters.files)
        .getValue().getMatchingRawDataFiles();
    final CCSCalibration calibration = parameterSet.getValue(SetCCSCalibrationParameters.ccsCal);
    for (RawDataFile file : files) {
      if (file instanceof IMSRawDataFile imsFile) {
        logger.finest(
            () -> "Setting CCS calibration " + calibration + " to file " + file.getName());
        imsFile.setCCSCalibration(calibration);
        imsFile.getAppliedMethods().add(
            new SimpleFeatureListAppliedMethod(SetCCSCalibrationModule.class, parameterSet,
                getModuleCallDate()));
      }
    }
    setStatus(TaskStatus.FINISHED);
  }
}
