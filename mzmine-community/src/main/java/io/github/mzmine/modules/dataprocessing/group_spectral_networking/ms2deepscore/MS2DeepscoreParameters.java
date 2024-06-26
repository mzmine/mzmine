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

package io.github.mzmine.modules.dataprocessing.group_spectral_networking.ms2deepscore;

import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.PercentParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;

/**
 * Define any parameters here (see io.github.mzmine.parameters for parameter types) static is needed
 * here to use this parameter as a key to lookup values
 * <p>
 * var flists = parameters.getValue(EmptyFeatureListParameters.featureLists);
 */
public class MS2DeepscoreParameters extends SimpleParameterSet {

  public static final FeatureListsParameter featureLists = new FeatureListsParameter();

  public static final IntegerParameter minSignals = new IntegerParameter("Minimum signals",
      "The minimum number of fragments for using a spectrum (minimum = 3)", 3, 3, null);

  public static final PercentParameter minScore = new PercentParameter("Min similarity",
      "The minimum similarity score to store the MS2Deepscore prediction", 0.9, 0.0, 1.0);

  public MS2DeepscoreParameters() {
    /*
     * The order of the parameters is used to construct the parameter dialog automatically
     */
    super(featureLists, minSignals, minScore);
  }

}
