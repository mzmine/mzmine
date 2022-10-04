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

package io.github.mzmine.modules.example;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.OriginalFeatureListHandlingParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.RTToleranceParameter;

public class LearnerParameters extends SimpleParameterSet {
  /*
   * Define any parameters here (see io.github.mzmine.parameters for parameter types)
   */
  public static final FeatureListsParameter featureLists = new FeatureListsParameter();

  public static final StringParameter suffix =
      new StringParameter("Name suffix", "Suffix to be added to feature list name", "deisotoped");

  public static final MZToleranceParameter mzTolerance = new MZToleranceParameter();

  public static final RTToleranceParameter rtTolerance = new RTToleranceParameter();

  public static final IntegerParameter maximumCharge = new IntegerParameter("Maximum charge",
      "Maximum charge to consider for detecting the isotope patterns");

  public static final OriginalFeatureListHandlingParameter handleOriginal = new OriginalFeatureListHandlingParameter(
      false);

  /**
   * Create a new parameterset
   */
  public LearnerParameters() {
    /*
     * The order of the parameters is used to construct the parameter dialog automatically
     */
    super(new Parameter[]{featureLists, suffix, mzTolerance, rtTolerance, maximumCharge,
        handleOriginal});
  }

}
