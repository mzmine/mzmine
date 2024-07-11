/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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


package io.github.mzmine.modules.dataprocessing.norm_rtcalibration;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.OriginalFeatureListHandlingParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance.Unit;
import io.github.mzmine.parameters.parametertypes.tolerances.RTToleranceParameter;

public class RTCorrectionParameters extends SimpleParameterSet {

  public static final FeatureListsParameter featureLists = new FeatureListsParameter(2);

  public static final StringParameter suffix = new StringParameter("Name suffix",
      "Suffix to be added to feature list name", "RT_corrected");

  public static final MZToleranceParameter MZTolerance = new MZToleranceParameter(0.005, 5);

  public static final RTToleranceParameter RTTolerance = new RTToleranceParameter(
      "Retention time tolerance", "Maximum allowed difference between two retention time values",
      new RTTolerance(0.01f, Unit.MINUTES));

  public static final DoubleParameter minHeight = new DoubleParameter("Minimum standard intensity",
      "Minimum height of a feature to be selected as standard for RT correction",
      MZmineCore.getConfiguration().getIntensityFormat());

  public static final OriginalFeatureListHandlingParameter handleOriginal =
      new OriginalFeatureListHandlingParameter("Original feature list",
          "Defines the processing.\nKEEP is to keep the original feature list and create a new"
              + "processed list.\nREMOVE saves memory.", false);

  public RTCorrectionParameters() {
    super(
        new Parameter[]{featureLists, suffix, MZTolerance, RTTolerance, minHeight, handleOriginal},
        "https://mzmine.github.io/mzmine_documentation/module_docs/norm_rt_calibration/norm_rt_calibration.html");
  }

}
