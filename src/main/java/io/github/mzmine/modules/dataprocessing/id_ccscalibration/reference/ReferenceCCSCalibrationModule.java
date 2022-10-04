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

package io.github.mzmine.modules.dataprocessing.id_ccscalibration.reference;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.modules.dataprocessing.id_ccscalc.CCSUtils;
import io.github.mzmine.modules.dataprocessing.id_ccscalibration.CCSCalculator;
import io.github.mzmine.modules.dataprocessing.id_ccscalibration.CCSCalibration;
import io.github.mzmine.modules.dataprocessing.id_ccscalibration.DriftTubeCCSCalibration;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.mobilitytolerance.MobilityTolerance;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.RangeUtils;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Calculates a {@link CCSCalibration} based on a given list of reference compounds. The compounds
 * have to be detected as features in a feature list. The extracted calibration can be set for a
 * number of raw data files.
 * <p></p>
 * Currently implemented for linear relationships of mobility and m/z (TIMS and DTIMS).
 * @author https://github.com/SteffenHeu
 */
public class ReferenceCCSCalibrationModule implements MZmineProcessingModule, CCSCalculator {

  private static final Logger logger = Logger.getLogger(
      ReferenceCCSCalibrationModule.class.getName());

  @Override
  public @NotNull String getName() {
    return "Internal standard CCS calculation";
  }

  @Override
  public @Nullable Class<? extends ParameterSet> getParameterSetClass() {
    return ReferenceCCSCalibrationParameters.class;
  }

  @Override
  public CCSCalibration getCalibration(@Nullable ModularFeatureList flist,
      @NotNull ParameterSet ccsCalculatorParameters) {
    if (flist == null) {
      logger.warning("Cannot find calibration without a feature list.");
      return null;
    }

    final File calibrantFile = ccsCalculatorParameters.getParameter(
        ReferenceCCSCalibrationParameters.referenceList).getValue();
    if (!(calibrantFile.exists() && calibrantFile.canRead())) {
      logger.warning(() -> "Cannot read calibrant file " + calibrantFile.getAbsolutePath());
      return null;
    }

    final List<CCSCalibrant> calibrants;
    try {
      calibrants = CCSUtils.getCalibrantsFromCSV(calibrantFile);
      logger.warning(() -> "No calibrants found in " + calibrantFile.getAbsolutePath());
      if(calibrants == null) {
        return null;
      }
    } catch (IOException e) {
      logger.log(Level.WARNING,
          "Cannot parse calibrants from calibrant file " + calibrantFile.getAbsolutePath());
      return null;
    }

    final MZTolerance mzTol = ccsCalculatorParameters.getValue(
        ReferenceCCSCalibrationParameters.mzTolerance);
    final Range<Float> rtRange = RangeUtils.toFloatRange(
        ccsCalculatorParameters.getValue(ReferenceCCSCalibrationParameters.rtRange));
    final MobilityTolerance mobTol = ccsCalculatorParameters.getValue(
        ReferenceCCSCalibrationParameters.mobTolerance);
    final Double minHeight = ccsCalculatorParameters.getValue(
        ReferenceCCSCalibrationParameters.minHeight);

    final List<CCSCalibrant> detectedCalibrants = CCSUtils.findCalibrants(flist, calibrants, mzTol,
        rtRange, mobTol, minHeight);

    final SimpleRegression driftTimeMzRegression = CCSUtils.getDriftTimeMzRegression(
        detectedCalibrants);
    if (driftTimeMzRegression == null) {
      return null;
    }

    final DriftTubeCCSCalibration driftTubeCCSCalibration = new DriftTubeCCSCalibration(
        driftTimeMzRegression);
    logger.info(() -> "Found calibration " + driftTubeCCSCalibration.toString());

    return driftTubeCCSCalibration;
  }

  @Override
  public @NotNull String getDescription() {
    return "Calculates CCS values based on an reference compound list and it's detected features in a feature list.";
  }

  @Override
  public @NotNull ExitCode runModule(@NotNull MZmineProject project,
      @NotNull ParameterSet parameters, @NotNull Collection<Task> tasks,
      @NotNull Instant moduleCallDate) {
    tasks.add(new ReferenceCCSCalibrationTask(null, moduleCallDate, parameters));
    return ExitCode.OK;
  }

  @Override
  public @NotNull MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.ANNOTATION;
  }
}
