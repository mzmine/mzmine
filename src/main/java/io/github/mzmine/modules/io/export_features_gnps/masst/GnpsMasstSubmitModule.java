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
package io.github.mzmine.modules.io.export_features_gnps.masst;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineRunnableModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.scans.ScanUtils;
import java.time.Instant;
import java.util.Collection;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Directly submits a new MASST job from MZmine
 *
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 */
public class GnpsMasstSubmitModule implements MZmineRunnableModule {

  private static final Logger logger = Logger.getLogger(GnpsMasstSubmitModule.class.getName());

  private static final String MODULE_NAME = "Submit GNPS MASST search";
  private static final String MODULE_DESCRIPTION = "Submit an MS2 spectrum to GNPS MASST search, searching against public data.";

  public static ExitCode submitSingleMASSTJob(@Nullable FeatureListRow row, double precursorMZ,
      MassSpectrum spectrum) {
    return submitSingleMASSTJob(row, precursorMZ, ScanUtils.extractDataPoints(spectrum));
  }

  public static ExitCode submitSingleMASSTJob(@Nullable FeatureListRow row, double precursorMZ,
      DataPoint[] dataPoints) {
    final ParameterSet parameters = MZmineCore.getConfiguration()
        .getModuleParameters(GnpsMasstSubmitModule.class);
    if (parameters.showSetupDialog(true) == ExitCode.OK) {
      MZmineCore.getTaskController().addTask(
          new GnpsMasstSubmitTask(row, precursorMZ, dataPoints, parameters, Instant.now()));
      return ExitCode.OK;
    }
    return ExitCode.CANCEL;
  }

  @Override
  public String getDescription() {
    return MODULE_DESCRIPTION;
  }

  @Override
  @NotNull
  public ExitCode runModule(MZmineProject project, ParameterSet parameters, Collection<Task> tasks,
      @NotNull Instant moduleCallDate) {

    logger.warning(
        "The GNPS MASST submit should never be run as a module. Directly call the submit methods to supply the needed spectral data");

    return ExitCode.CANCEL;
  }

  @Override
  public MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.FEATURELISTEXPORT;
  }

  @Override
  public String getName() {
    return MODULE_NAME;
  }

  @Override
  public Class<? extends ParameterSet> getParameterSetClass() {
    return GnpsMasstSubmitParameters.class;
  }

}