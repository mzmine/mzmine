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

package io.github.mzmine.modules.dataprocessing.featdet_massdetection.recursive;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;

public class RecursiveMassDetectorParameters extends SimpleParameterSet {

  public static final DoubleParameter noiseLevel = new DoubleParameter("Noise level",
      "Intensities less than this value are interpreted as noise",
      MZmineCore.getConfiguration().getIntensityFormat());

  public static final DoubleParameter minimumMZPeakWidth = new DoubleParameter("Min m/z peak width",
      "Minimum acceptable peak width in m/z", MZmineCore.getConfiguration().getMZFormat());

  public static final DoubleParameter maximumMZPeakWidth = new DoubleParameter("Max m/z peak width",
      "Maximum acceptable peak width in m/z", MZmineCore.getConfiguration().getMZFormat());

  public RecursiveMassDetectorParameters() {
    super(new UserParameter[]{noiseLevel, minimumMZPeakWidth, maximumMZPeakWidth},
        "https://mzmine.github.io/mzmine_documentation/module_docs/featdet_mass_detection/mass-detection-algorithms.html#recursive-threshold");
  }

}
