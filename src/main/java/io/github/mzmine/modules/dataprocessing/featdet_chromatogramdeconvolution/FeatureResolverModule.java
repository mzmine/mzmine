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

package io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.maths.CenterFunction;
import io.github.mzmine.util.maths.CenterMeasure;
import io.github.mzmine.util.maths.Weighting;
import java.util.Arrays;
import java.util.Collection;
import javax.annotation.Nonnull;

public abstract class FeatureResolverModule implements MZmineProcessingModule {

  private static final String MODULE_NAME = "Chromatogram deconvolution";
  private static final String MODULE_DESCRIPTION =
      "This module separates each detected chromatogram into individual peaks.";

  @Override
  public @Nonnull MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.FEATURELISTDETECTION;
  }

  @Override
  @Nonnull
  public ExitCode runModule(@Nonnull MZmineProject project, @Nonnull final ParameterSet parameters,
      @Nonnull final Collection<Task> tasks) {
    FeatureList[] peakLists = parameters.getParameter(GeneralResolverParameters.PEAK_LISTS).getValue()
        .getMatchingFeatureLists();

    // function to calculate center mz
    CenterFunction mzCenterFunction =
        parameters.getParameter(GeneralResolverParameters.MZ_CENTER_FUNCTION).getValue();

    // use a logger weighted, noise corrected, maximum weight capped function
    if (mzCenterFunction.getMeasure().equals(CenterMeasure.AUTO)) {
      // data point with lowest intensity
      // weight = logger(value) - logger(noise) (maxed to maxWeight)
      double noise =
          Arrays.stream(peakLists).flatMap(pkl -> pkl.getRows().stream()).map(r -> r.getFeatures().get(0))
              .mapToDouble(peak -> peak.getRawDataPointsIntensityRange().lowerEndpoint())
              .filter(v -> v != 0).min().orElse(0);

      // maxWeight 4 corresponds to a linear range of 4 orders of
      // magnitude
      // everything higher than this will be capped to this weight
      // do not overestimate influence of very high data points on mass
      // accuracy
      double maxWeight = 4;

      // use a logger weighted, noise corrected, maximum weight capped
      // function
      mzCenterFunction =
          new CenterFunction(CenterMeasure.AVG, Weighting.logger10, noise, maxWeight);
    }

    for (final FeatureList peakList : peakLists) {
      tasks.add(new FeatureResolverTask(project, peakList, parameters, mzCenterFunction));
    }

    return ExitCode.OK;
  }
}
