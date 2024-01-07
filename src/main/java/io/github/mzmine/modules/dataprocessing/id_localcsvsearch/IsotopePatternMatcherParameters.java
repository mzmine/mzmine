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

package io.github.mzmine.modules.dataprocessing.id_localcsvsearch;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.PercentParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;


public class IsotopePatternMatcherParameters extends SimpleParameterSet {

  public static final MZToleranceParameter isotopeMzTolerance = new MZToleranceParameter(
      "Isotope m/z tolerance",
      "Maximum allowed difference between two m/z values to be considered same.\\n"
          + "            + \"The value is specified both as absolute tolerance (in m/z) and relative tolerance (in ppm).\\n"
          + "            + \"The tolerance range is calculated using maximum of the absolute and relative tolerances.",
      0.005, 10);
  public static final PercentParameter minIntensity = new PercentParameter(
      "Minimum isotope intensity (%)",
      "Minimum intensity percentage (%) that the isotopes must have in order to apply to the isotope pattern.", 0.05);
  public static final DoubleParameter minIsotopeScore = new DoubleParameter("Minimum isotope score",
      "Minimum isotope pattern score that the detected isotope pattern must have in order to apply to the database hits",
      MZmineCore.getConfiguration().getScoreFormat(), 0.0);

  public IsotopePatternMatcherParameters() {
    super(new Parameter[]{isotopeMzTolerance, minIntensity, minIsotopeScore});
  }
}
