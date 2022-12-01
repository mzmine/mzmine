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

package io.github.mzmine.modules.dataprocessing.id_ccscalibration.external;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.modules.dataprocessing.id_ccscalibration.CCSCalculator;
import io.github.mzmine.modules.dataprocessing.id_ccscalibration.CCSCalibration;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import java.io.File;
import java.time.Instant;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Reads an externally generated CCS calibration (Agilent OverrideImsCal.xml or Waters
 * mob_cal.csv) and sets it to a raw data file.
 */
public class ExternalCCSCalibrationModule implements MZmineProcessingModule, CCSCalculator {

  @Override
  public @NotNull String getName() {
    return "External CCS calibration";
  }

  @Override
  public @Nullable Class<? extends ParameterSet> getParameterSetClass() {
    return ExternalCCSCalibrationParameters.class;
  }

  @Override
  public CCSCalibration getCalibration(@Nullable ModularFeatureList flist,
      @NotNull ParameterSet ccsCalculatorParameters) {

    final File calibrationFile = ccsCalculatorParameters.getValue(
        ExternalCCSCalibrationParameters.calibrationFile);

    if (calibrationFile.getName().endsWith(".xml")) {
      return AgilentImsCalibrationReader.readCalibrationFile(calibrationFile);
    } else if (calibrationFile.getName().endsWith(".csv")) {
      return WatersImsCalibrationReader.readCalibrationFile(calibrationFile);
    }
    throw new IllegalArgumentException("Invalid calibration file.");
  }

  @Override
  public @NotNull String getDescription() {
    return "Uses an externally calculated CCS calibration to calculate CCS values.";
  }

  @Override
  public @NotNull ExitCode runModule(@NotNull MZmineProject project,
      @NotNull ParameterSet parameters, @NotNull Collection<Task> tasks,
      @NotNull Instant moduleCallDate) {
    tasks.add(new ExternalCCSCalibrationTask(null, moduleCallDate, parameters));
    return ExitCode.OK;
  }

  @Override
  public @NotNull MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.ANNOTATION;
  }
}
