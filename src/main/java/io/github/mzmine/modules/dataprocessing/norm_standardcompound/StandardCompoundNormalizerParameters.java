/*
 * Copyright (c) 2004-2022 The MZmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
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
      new OriginalFeatureListHandlingParameter("Original feature list",
          "Defines the processing.\nKEEP is to keep the original feature list and create a new"
              + "processed list.\nREMOVE saves memory.", false);

  public static final FeatureSelectionParameter standardCompounds = new FeatureSelectionParameter(
      "Standard compounds", "List of features for choosing the normalization standards", null);

  public StandardCompoundNormalizerParameters() {
    super(new Parameter[]{featureList, suffix, standardUsageType, featureMeasurementType,
        MZvsRTBalance, standardCompounds, handleOriginal},
        "https://mzmine.github.io/mzmine_documentation/module_docs/norm_stand_cmpd/norm_stand_cmpd.html");
  }

}
