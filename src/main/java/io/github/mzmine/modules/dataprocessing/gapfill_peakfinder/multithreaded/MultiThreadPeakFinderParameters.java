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

package io.github.mzmine.modules.dataprocessing.gapfill_peakfinder.multithreaded;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.OriginalFeatureListHandlingParameter;
import io.github.mzmine.parameters.parametertypes.PercentParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.RTToleranceParameter;
import org.jetbrains.annotations.NotNull;

public class MultiThreadPeakFinderParameters extends SimpleParameterSet {

  public static final FeatureListsParameter peakLists = new FeatureListsParameter();

  public static final StringParameter suffix = new StringParameter("Name suffix",
      "Suffix to be added to feature list name", "gap-filled");

  public static final PercentParameter intTolerance = new PercentParameter("Intensity tolerance",
      "Maximum allowed deviation from expected /\\ shape of a peak in chromatographic direction",
      0.2);

  public static final MZToleranceParameter MZTolerance = new MZToleranceParameter();

  public static final RTToleranceParameter RTTolerance = new RTToleranceParameter();

  public static final IntegerParameter minDataPoints = new IntegerParameter("Minimum data points",
      "Only fill gaps with features with minimum number of data points. \n Usually lower number of data points are used.",
      1, 1, Integer.MAX_VALUE);

  public static final OriginalFeatureListHandlingParameter handleOriginal = new OriginalFeatureListHandlingParameter(
      true);

  public MultiThreadPeakFinderParameters() {
    super(new Parameter[]{peakLists, suffix, intTolerance, MZTolerance, RTTolerance, minDataPoints,
        handleOriginal}, "https://mzmine.github.io/mzmine_documentation/module_docs/gapfill_peak_finder/gap-filling.html");
  }

  @Override
  public @NotNull IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.SUPPORTED;
  }
}
