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

  public static final OriginalFeatureListHandlingParameter handleOriginal =
      new OriginalFeatureListHandlingParameter("Original feature list",
          "Defines the processing.\nKEEP is to keep the original feature list and create a new"
              + "processed list.\nREMOVE saves memory.", false);

  public LinearNormalizerParameters() {
    super(new Parameter[]{featureLists, suffix, normalizationType, featureMeasurementType,
        handleOriginal},
        "https://mzmine.github.io/mzmine_documentation/module_docs/norm_linear/norm_linear.html");
  }

}
