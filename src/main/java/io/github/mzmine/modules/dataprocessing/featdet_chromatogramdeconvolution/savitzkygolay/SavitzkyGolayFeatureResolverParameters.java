/*
 * Copyright 2006-2021 The MZmine Development Team
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

package io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.savitzkygolay;

import com.google.common.collect.Range;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.FeatureResolver;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.FeatureResolverSetupDialog;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.GeneralResolverParameters;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.PercentParameter;
import io.github.mzmine.parameters.parametertypes.ranges.DoubleRangeParameter;
import io.github.mzmine.util.ExitCode;

public class SavitzkyGolayFeatureResolverParameters extends GeneralResolverParameters {

  public static final DoubleParameter MIN_PEAK_HEIGHT = new DoubleParameter("Min peak height",
      "Minimum acceptable peak height (absolute intensity)",
      MZmineCore.getConfiguration().getIntensityFormat());

  public static final DoubleRangeParameter PEAK_DURATION = new DoubleRangeParameter(
      "Peak duration range (min)", "Range of acceptable peak lengths",
      MZmineCore.getConfiguration().getRTFormat(), Range.closed(0.0, 10.0));

  public static final PercentParameter DERIVATIVE_THRESHOLD_LEVEL = new PercentParameter(
      "Derivative threshold level",
      "Minimum acceptable intensity in the 2nd derivative for peak recognition");

  public SavitzkyGolayFeatureResolverParameters() {
    super(new Parameter[]{PEAK_LISTS, SUFFIX, handleOriginal, groupMS2Parameters, MIN_PEAK_HEIGHT,
        PEAK_DURATION, DERIVATIVE_THRESHOLD_LEVEL, RENGINE_TYPE, MIN_NUMBER_OF_DATAPOINTS});
  }

  @Override
  public ExitCode showSetupDialog(boolean valueCheckRequired) {
    final FeatureResolverSetupDialog dialog = new FeatureResolverSetupDialog(valueCheckRequired,
        this, null);
    dialog.showAndWait();
    return dialog.getExitCode();
  }

  @Override
  public FeatureResolver getResolver() {
    return new SavitzkyGolayFeatureResolver();
  }
}
