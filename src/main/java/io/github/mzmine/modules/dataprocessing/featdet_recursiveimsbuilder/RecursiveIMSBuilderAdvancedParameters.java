/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.dataprocessing.featdet_recursiveimsbuilder;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import java.text.DecimalFormat;
import java.text.NumberFormat;

public class RecursiveIMSBuilderAdvancedParameters extends SimpleParameterSet {

  public static final double DEFAULT_TIMS_BIN_WIDTH = 0.0008;
  public static final double DEFAULT_DTIMS_BIN_WIDTH = 0.005;
  public static final double DEFAULT_TWIMS_BIN_WIDTH = 0.005;
  private static final NumberFormat binFormat = new DecimalFormat("0.00000");

  public static final OptionalParameter<DoubleParameter> timsBinningWidth = new OptionalParameter<>(
      new DoubleParameter("Override default TIMS binning width (Vs/cm²)",
          "The binning width in mobility units of the selected raw data file.\n"
              + " The default binning width is " + binFormat.format(DEFAULT_TIMS_BIN_WIDTH) + ".",
          binFormat, DEFAULT_TIMS_BIN_WIDTH, 0.00001, 1E6));

  public static final OptionalParameter<DoubleParameter> twimsBinningWidth = new OptionalParameter(
      new DoubleParameter(
          "Travelling wave binning width (ms)",
          "The binning width in mobility units of the selected raw data file."
              + "The default binning width is " + binFormat.format(DEFAULT_TWIMS_BIN_WIDTH) + ".",
          binFormat, DEFAULT_TWIMS_BIN_WIDTH, 0.00001, 1E6));

  public static final OptionalParameter<DoubleParameter> dtimsBinningWidth = new OptionalParameter<>(
      new DoubleParameter(
          "Drift tube binning width (ms)",
          "The binning width in mobility units of the selected raw data file.\n"
              + "The default binning width is " + binFormat.format(DEFAULT_TIMS_BIN_WIDTH) + ".",
          binFormat, DEFAULT_DTIMS_BIN_WIDTH, 0.00001, 1E6));

  public RecursiveIMSBuilderAdvancedParameters() {
    super(new Parameter[]{timsBinningWidth, dtimsBinningWidth, twimsBinningWidth});
  }
}

