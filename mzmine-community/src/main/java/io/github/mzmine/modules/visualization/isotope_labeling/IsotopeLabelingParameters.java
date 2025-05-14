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

package io.github.mzmine.modules.visualization.isotope_labeling;

import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;

/**
 * Parameters for isotope labeling visualization
 */
public class IsotopeLabelingParameters extends SimpleParameterSet {

  public static final FeatureListsParameter featureLists = new FeatureListsParameter();

  public static final ComboParameter<String> visualizationType = new ComboParameter<>(
      "Visualization type", "Type of visualization to use for isotope patterns",
      new String[]{"Relative intensities", "Absolute intensities"}, "Relative intensities");

  public static final IntegerParameter maxIsotopologues = new IntegerParameter(
      "Maximum isotopologues", "Maximum number of isotopologues to display in a chart", 10);

  public static final BooleanParameter stackBars = new BooleanParameter("Stack bars",
      "Stack bars by sample group in the chart", true);

  public static final BooleanParameter showErrorBars = new BooleanParameter("Show error bars",
      "Show error bars representing standard deviation in the chart", true);

  public IsotopeLabelingParameters() {
    super(featureLists, visualizationType, maxIsotopologues, stackBars, showErrorBars);
  }
}