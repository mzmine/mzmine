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

package io.github.mzmine.modules.dataprocessing.id_cliquems;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTToleranceParameter;

public class SimilarFeatureParameters extends SimpleParameterSet {

  public static final MZToleranceParameter MZ_DIFF = new MZToleranceParameter("MZ tolerance",
      "If two features' relative difference of m/z values is less than MZ tolerance, they are candidate for similar features. So, if MZ tolerance is set a (relative) value of 'x' ppm (or absolute value of 'y'), then a feature with mz value of 'm' will have all peaks with the mz in the closed range [m - m*x/10e6 , m + m*x/10e6] (or [m - y, m + y] , whichever range is larger) similar to it (if rt and intensity tolerance values are passed too).",
      0, 5);

  public static final RTToleranceParameter RT_DIFF = new RTToleranceParameter("RT tolerance",
      "If RT tolerance is set a relative value of 'x' (or absolute value 'y'), then a feature with rt value of 't' will have all peaks with rt in the closed range [t - t*x, t + t*x ]  ( or [t-y, t+y]) similar to it (if m/z and intensity tolerance values are passed too).");

  public static final DoubleParameter IN_DIFF = new DoubleParameter("Intensity tolerance (relative)",
      "If Intensity tolerance is set a value of x, then a feature with intensity value 'i' will have all peaks with the intensity range [ i - i*x , i + i*x] similar to it (Note - Tolerance is unitless)(if m/z and rt tolerance values are passed too).",
      MZmineCore.getConfiguration().getIntensityFormat(), 0.0004);

  public SimilarFeatureParameters() {
    super(new Parameter[]{MZ_DIFF, RT_DIFF, IN_DIFF});
    RT_DIFF.setValue(new RTTolerance(false, 0.0004f));

  }

}
