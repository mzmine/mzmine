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

package io.github.mzmine.modules.dataprocessing.norm_linear;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.OriginalFeatureListHandlingParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.util.FeatureMeasurementType;

public class LinearNormalizerParameters extends SimpleParameterSet {

  public static final FeatureListsParameter featureLists = new FeatureListsParameter();

  public static final StringParameter suffix = new StringParameter("Name suffix",
      "Suffix to be added to feature list name", "normalized");

  public static final ComboParameter<NormalizationType> normalizationType = new ComboParameter<NormalizationType>(
      "Normalization type", "Normalize intensities by...", NormalizationType.values());

  public static final ComboParameter<FeatureMeasurementType> featureMeasurementType = new ComboParameter<FeatureMeasurementType>(
      "Feature measurement type", "Measure features using", FeatureMeasurementType.values());

  public static final OriginalFeatureListHandlingParameter handleOriginal = //
      new OriginalFeatureListHandlingParameter(false);

  public LinearNormalizerParameters() {
    super(new Parameter[]{featureLists, suffix, normalizationType, featureMeasurementType,
        handleOriginal});
  }

}
