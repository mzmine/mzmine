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
 *
 */

package io.github.mzmine.modules.dataprocessing.id_ccscalibration.reference;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.dataprocessing.id_ccscalc.CCSUtils;
import io.github.mzmine.modules.dataprocessing.id_ccscalibration.CCSCalculator;
import io.github.mzmine.modules.dataprocessing.id_ccscalibration.CCSCalibrant;
import io.github.mzmine.modules.dataprocessing.id_ccscalibration.CCSCalibration;
import io.github.mzmine.modules.dataprocessing.id_ccscalibration.DriftTubeCCSCalibration;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.mobilitytolerance.MobilityTolerance;
import io.github.mzmine.util.RangeUtils;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ReferenceCCSCalcModule implements MZmineModule, CCSCalculator {

  private static final Logger logger = Logger.getLogger(ReferenceCCSCalcModule.class.getName());

  @Override
  public @NotNull String getName() {
    return "Internal standard CCS calculation";
  }

  @Override
  public @Nullable Class<? extends ParameterSet> getParameterSetClass() {
    return ReferenceCCSCalcParameters.class;
  }

  @Override
  public CCSCalibration getCalibration(@NotNull ModularFeatureList flist,
      @NotNull ParameterSet ccsCalculatorParameters) {
    final File calibrantFile = ccsCalculatorParameters.getParameter(
        ReferenceCCSCalcParameters.referenceList).getValue();
    if (!(calibrantFile.exists() && calibrantFile.canRead())) {
      logger.warning(() -> "Cannot read calibrant file " + calibrantFile.getAbsolutePath());
      return null;
    }

    final List<CCSCalibrant> calibrants;
    try {
      calibrants = CCSUtils.getCalibrantsFromCSV(calibrantFile);
    } catch (IOException e) {
      logger.log(Level.WARNING,
          "Cannot parse calibrants from calibrant file " + calibrantFile.getAbsolutePath());
      return null;
    }

    final MZTolerance mzTol = ccsCalculatorParameters.getValue(
        ReferenceCCSCalcParameters.mzTolerance);
    final Range<Float> rtRange = RangeUtils.toFloatRange(
        ccsCalculatorParameters.getValue(ReferenceCCSCalcParameters.rtRange));
    final MobilityTolerance mobTol = ccsCalculatorParameters.getValue(
        ReferenceCCSCalcParameters.mobTolerance);
    final Double minHeight = ccsCalculatorParameters.getValue(ReferenceCCSCalcParameters.minHeight);

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
}
