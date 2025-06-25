/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.modules.tools.isotopepatternscore;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.PercentParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;

public class IsotopePatternScoreParameters extends SimpleParameterSet {

  public static final MZToleranceParameter mzTolerance = new MZToleranceParameter(
      "Isotope m/z tolerance",
      "m/z tolerance which defines what isotopes would be considered same when "
      + "comparing two isotopic patterns.\nThis tolerance needs to be "
      + "higher than general m/z precision of the data,\nbecause some "
      + "small isotopes may overlap with the sides of bigger isotopic peaks.", 0.002, 5d);

  public static final DoubleParameter isotopeNoiseLevel = new DoubleParameter(
      "Minimum absolute intensity",
      "Minimum absolute intensity of the isotopes to be compared.\nIsotopes below this intensity will be ignored.",
      MZmineCore.getConfiguration().getIntensityFormat(), 0d);

  public static final PercentParameter isotopePatternScoreThreshold = new PercentParameter(
      "Minimum score", "If the score between isotope pattern is lower, discard this match", 0d);

  public IsotopePatternScoreParameters() {
    super(new Parameter[]{mzTolerance, isotopeNoiseLevel, isotopePatternScoreThreshold});
  }

}
