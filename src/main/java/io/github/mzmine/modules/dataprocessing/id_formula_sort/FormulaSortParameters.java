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
package io.github.mzmine.modules.dataprocessing.id_formula_sort;

import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import java.text.DecimalFormat;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;

public class FormulaSortParameters extends SimpleParameterSet {

  public static final FeatureListsParameter FEATURE_LISTS = new FeatureListsParameter();

  public static final DoubleParameter MAX_PPM_WEIGHT =
      new DoubleParameter("Max ppm distance (weight)",
          "Score is calculated as (ppm distance-ppmMax)/ppmMax", new DecimalFormat("0.0"), 10d);

  public static final DoubleParameter ISOTOPE_SCORE_WEIGHT =
      new DoubleParameter("Weight isotope pattern score", "Weight for isotope pattern score",
          new DecimalFormat("0.0"), 1d);

  public static final DoubleParameter MSMS_SCORE_WEIGHT = new DoubleParameter("Weight MS/MS score",
      "Weight for MS/MS score", new DecimalFormat("0.0"), 1d);


  public FormulaSortParameters() {
    this(false);
  }

  public FormulaSortParameters(boolean isSub) {
    super(isSub ? new Parameter[] {MAX_PPM_WEIGHT, ISOTOPE_SCORE_WEIGHT, MSMS_SCORE_WEIGHT}
        : new Parameter[] {FEATURE_LISTS, MAX_PPM_WEIGHT, ISOTOPE_SCORE_WEIGHT, MSMS_SCORE_WEIGHT});
  }
}
