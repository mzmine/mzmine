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
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package io.github.mzmine.modules.visualization.histo_feature_correlation;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;

public class FeatureCorrelationHistogramParameters extends SimpleParameterSet {

  public static final FeatureListsParameter featureLists = new FeatureListsParameter();
  public static final DoubleParameter binWidth = new DoubleParameter("m/z bin width",
      "Binning of m/z values for feature picking ", MZmineCore.getConfiguration().getScoreFormat(),
      0.01);

  public FeatureCorrelationHistogramParameters() {
    super(new Parameter[]{featureLists, binWidth},
        "https://mzmine.github.io/mzmine_documentation/visualization_modules/processed_additional.html/correlated-features-deltamz-histogram");
  }

}
