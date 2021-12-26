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

package io.github.mzmine.modules.dataprocessing.id_ccscalc.internal;

import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.dataprocessing.id_ccscalc.CCSCalculator;
import io.github.mzmine.modules.dataprocessing.id_ccscalc.CCSUtils;
import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.TDFUtils;
import io.github.mzmine.parameters.ParameterSet;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class InternalCCSCalcModule implements MZmineModule, CCSCalculator {
  
  private final TDFUtils tdfUtils = new TDFUtils();

  @Override
  public @NotNull String getName() {
    return "Internal CCS Calcuation Module";
  }

  @Override
  public @Nullable Class<? extends ParameterSet> getParameterSetClass() {
    return InternalCCSCalcParameters.class;
  }

  @Override
  public @Nullable Float calcCCS(@Nullable Double mz, @Nullable Float mobility, int charge,
      @Nullable SimpleRegression calibration, @Nullable final  MobilityType mobilityType) {
    if (mz == null || mobility == null || mz.isNaN() || mz.isInfinite() || mobility.isNaN()
        || mobility.isInfinite()|| mobilityType == null) {
      return null;
    }

    return switch (mobilityType) {
      case NONE, FAIMS, TRAVELING_WAVE, DRIFT_TUBE -> null;
      case TIMS -> tdfUtils.calculateCCS(mobility, charge, mz);
    };
  }
}
