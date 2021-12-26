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

package io.github.mzmine.modules.dataprocessing.id_ccscalc.reference;

import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.dataprocessing.id_ccscalc.CCSCalculator;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.FormulaUtils;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ReferenceCCSCalcModule implements MZmineModule, CCSCalculator {

  @Override
  public @NotNull String getName() {
    return "Reference CCS calculation";
  }

  @Override
  public @Nullable Class<? extends ParameterSet> getParameterSetClass() {
    return ReferenceCCSCalcParameters.class;
  }

  @Override
  public @Nullable Float calcCCS(@Nullable Double mz, @Nullable Float mobility, int charge,
      @Nullable SimpleRegression calibration, @Nullable final MobilityType mobilityType) {
    if (mz == null || mobility == null || mz.isNaN() || mz.isInfinite() || mobility.isNaN()
        || mobility.isInfinite()) {
      return null;
    }

    if (calibration == null) {
      throw new IllegalStateException("No calibration function found.");
    }

    return (float) calibration.predict(
        mz * Math.abs(charge) + Math.abs(charge) * FormulaUtils.electronMass);
  }
}
