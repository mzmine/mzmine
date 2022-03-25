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

package io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.minimumsearch;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.FeatureResolver;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.FeatureResolverSetupDialog;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.GeneralResolverParameters;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.Resolver;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.PercentParameter;
import io.github.mzmine.parameters.parametertypes.ranges.DoubleRangeParameter;
import io.github.mzmine.util.ExitCode;
import java.text.DecimalFormat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MinimumSearchFeatureResolverParameters extends GeneralResolverParameters {

  public static final PercentParameter CHROMATOGRAPHIC_THRESHOLD_LEVEL = new PercentParameter(
      "Chromatographic threshold", "Percentile threshold for removing noise.\n"
      + "The algorithm will remove the lowest abundant X % data points from a chromatogram and only consider\n"
      + "the remaining (highest) values. Important filter for noisy chromatograms.",
      0.85d, 0d, 1d);

  public static final DoubleParameter SEARCH_RT_RANGE = new DoubleParameter(
      "Minimum search range RT/Mobility (absolute)",
      "If a local minimum is minimal in this range of retention time or mobility, it will be considered a border between two peaks.\n"
          + "Start optimising with a value close to the FWHM of a peak.",
      new DecimalFormat("0.000"), 0.05);

  public static final PercentParameter MIN_RELATIVE_HEIGHT = new PercentParameter(
      "Minimum relative height",
      "Minimum height of a peak relative to the chromatogram top data point", 0d);

  public static final DoubleParameter MIN_ABSOLUTE_HEIGHT = new DoubleParameter(
      "Minimum absolute height", "Minimum absolute height of a peak to be recognized",
      MZmineCore.getConfiguration().getIntensityFormat(), 1E3);

  public static final DoubleParameter MIN_RATIO = new DoubleParameter("Min ratio of peak top/edge",
      "Minimum ratio between peak's top intensity and side (lowest) data points."
          + "\nThis parameter helps to reduce detection of false peaks in case the chromatogram is not smooth.",
      new DecimalFormat("0.00"), 1.7d);

  public static final DoubleRangeParameter PEAK_DURATION = new DoubleRangeParameter(
      "Peak duration range (min/mobility)", "Range of acceptable peak lengths",
      MZmineCore.getConfiguration().getRTFormat(), Range.closed(0.0, 10.0));

  public MinimumSearchFeatureResolverParameters() {
    super(new Parameter[]{PEAK_LISTS, SUFFIX, handleOriginal, groupMS2Parameters, dimension,
        CHROMATOGRAPHIC_THRESHOLD_LEVEL, SEARCH_RT_RANGE, MIN_RELATIVE_HEIGHT, MIN_ABSOLUTE_HEIGHT,
        MIN_RATIO, PEAK_DURATION, MIN_NUMBER_OF_DATAPOINTS});
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
    throw new UnsupportedOperationException("Legacy resolver method. Unsupported in local min.");
  }

  @Nullable
  @Override
  public Resolver getResolver(ParameterSet parameters, ModularFeatureList flist) {
    return new MinimumSearchFeatureResolver(parameters, flist);
  }

  @NotNull
  @Override
  public IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.SUPPORTED;
  }
}
