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

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.modules.dataprocessing.id_ccscalibration.CCSCalibration;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelection;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelectionType;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import java.time.Instant;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Sets a CCSCalibration to a raw data file.
 */
public class SetCCSCalibrationModule implements MZmineProcessingModule {

  public static void setCalibrationToFiles(RawDataFile[] files, CCSCalibration calibration) {
    ParameterSet parameters = MZmineCore.getConfiguration()
        .getModuleParameters(SetCCSCalibrationModule.class).cloneParameterSet();

    RawDataFilesSelection selection = new RawDataFilesSelection(RawDataFilesSelectionType.SPECIFIC_FILES);
    selection.setSpecificFiles(files);
    parameters.setParameter(SetCCSCalibrationParameters.files, selection);
    parameters.setParameter(SetCCSCalibrationParameters.ccsCal, calibration);

    MZmineCore.runMZmineModule(SetCCSCalibrationModule.class, parameters);
  }

  @Override
  public @NotNull String getName() {
    return "Set CCS calibration";
  }

  @Override
  public @Nullable Class<? extends ParameterSet> getParameterSetClass() {
    return SetCCSCalibrationParameters.class;
  }

  @Override
  public @NotNull String getDescription() {
    return "Sets a CCS calibration to a raw data file.";
  }

  @Override
  public @NotNull ExitCode runModule(@NotNull MZmineProject project,
      @NotNull ParameterSet parameters, @NotNull Collection<Task> tasks,
      @NotNull Instant moduleCallDate) {
    tasks.add(new SetCCSCalibrationTask(parameters, null, moduleCallDate));
    return ExitCode.OK;
  }

  @Override
  public @NotNull MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.ANNOTATION;
  }
}
