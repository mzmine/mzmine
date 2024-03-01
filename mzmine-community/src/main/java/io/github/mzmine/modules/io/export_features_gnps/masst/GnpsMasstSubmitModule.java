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