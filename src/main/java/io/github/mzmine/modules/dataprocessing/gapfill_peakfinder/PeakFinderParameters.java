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

package io.github.mzmine.modules.dataprocessing.gapfill_peakfinder;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.OriginalFeatureListHandlingParameter;
import io.github.mzmine.parameters.parametertypes.PercentParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.RTToleranceParameter;
import org.jetbrains.annotations.NotNull;

public class PeakFinderParameters extends SimpleParameterSet {

  public static final FeatureListsParameter peakLists = new FeatureListsParameter();

  public static final StringParameter suffix = new StringParameter("Name suffix",
      "Suffix to be added to feature list name", "gap-filled");

  public static final PercentParameter intTolerance = new PercentParameter("Intensity tolerance",
      "Maximum allowed deviation from expected /\\ shape of a peak in chromatographic direction");

  public static final MZToleranceParameter MZTolerance = new MZToleranceParameter();

  public static final RTToleranceParameter RTTolerance = new RTToleranceParameter();

  public static final BooleanParameter RTCorrection = new BooleanParameter("RT correction",
      "If checked, correction of the retention time will be applied to avoid the"
      + "\nproblems caused by the deviation of the retention time between the samples.");

  public static final BooleanParameter useParallel = new BooleanParameter(
      "Parallel (never combined with RT correction)",
      "Parallel processing of gaps (RT correction is always on a single thread)");

  public static final OriginalFeatureListHandlingParameter handleOriginal = //
      new OriginalFeatureListHandlingParameter(false);

  public PeakFinderParameters() {
    super(new Parameter[]{peakLists, suffix, intTolerance, MZTolerance, RTTolerance, RTCorrection,
        useParallel, handleOriginal});
  }

  @Override
  public @NotNull IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.UNSUPPORTED;
  }
}
