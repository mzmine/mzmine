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

package io.github.mzmine.modules.dataprocessing.norm_standardcompound;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.OriginalFeatureListHandlingParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureSelectionParameter;
import io.github.mzmine.util.FeatureMeasurementType;

public class StandardCompoundNormalizerParameters extends SimpleParameterSet {

  public static final FeatureListsParameter featureList = new FeatureListsParameter(1, 1);

  public static final StringParameter suffix = new StringParameter("Name suffix",
      "Suffix to be added to feature list name", "normalized");

  public static final ComboParameter<StandardUsageType> standardUsageType = new ComboParameter<StandardUsageType>(
      "Normalization type", "Normalize intensities using ", StandardUsageType.values());

  public static final ComboParameter<FeatureMeasurementType> featureMeasurementType = new ComboParameter<FeatureMeasurementType>(
      "Feature measurement type", "Measure features using ", FeatureMeasurementType.values());

  public static final DoubleParameter MZvsRTBalance = new DoubleParameter("m/z vs RT balance",
      "Used in distance measuring as multiplier of m/z difference");

  public static final OriginalFeatureListHandlingParameter handleOriginal = //
      new OriginalFeatureListHandlingParameter(false);

  public static final FeatureSelectionParameter standardCompounds = new FeatureSelectionParameter(
      "Standard compounds", "List of features for choosing the normalization standards", null);

  public StandardCompoundNormalizerParameters() {
    super(new Parameter[]{featureList, suffix, standardUsageType, featureMeasurementType,
        MZvsRTBalance, standardCompounds, handleOriginal});
  }

}
