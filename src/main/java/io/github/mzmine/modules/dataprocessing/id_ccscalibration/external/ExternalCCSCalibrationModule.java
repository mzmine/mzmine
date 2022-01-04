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

package io.github.mzmine.modules.dataprocessing.id_ccscalibration.external;

import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.dataprocessing.id_ccscalibration.CCSCalculator;
import io.github.mzmine.modules.dataprocessing.id_ccscalibration.CCSCalibration;
import io.github.mzmine.parameters.ParameterSet;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ExternalCCSCalibrationModule implements MZmineModule, CCSCalculator {

  private static final Logger logger = Logger.getLogger(
      ExternalCCSCalibrationModule.class.getName());

  @Override
  public @NotNull String getName() {
    return "External CCS calibration";
  }

  @Override
  public @Nullable Class<? extends ParameterSet> getParameterSetClass() {
    return ExternalCCSCalibrationParameters.class;
  }

  @Override
  public CCSCalibration getCalibration(@NotNull ModularFeatureList flist,
      @NotNull ParameterSet ccsCalculatorParameters) {

    return null;
  }
}
