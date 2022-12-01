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

package io.github.mzmine.modules.dataprocessing.id_ms2search;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import org.jetbrains.annotations.NotNull;

public class Ms2SearchParameters extends SimpleParameterSet {

  public static final FeatureListsParameter peakList1 = new FeatureListsParameter("Feature List 1",
      1, 1);

  public static final FeatureListsParameter peakList2 = new FeatureListsParameter("Feature List 2",
      1, 1);


  public static final MZToleranceParameter mzTolerance = new MZToleranceParameter();

  public static final DoubleParameter intensityThreshold = new DoubleParameter(
      "Minimum MS2 ion intensity", "Minimum ion intensity to consider in MS2 comparison");

  public static final IntegerParameter minimumIonsMatched =
      new IntegerParameter("Minimum ion(s) matched per MS2 comparison",
          "Minimum number of peaks between two MS2s that must match");

  public static final DoubleParameter scoreThreshold = new DoubleParameter(
      "Minimum spectral match score to report", "Minimum MS2 comparison score to report");

  public Ms2SearchParameters() {
    super(new Parameter[]{peakList1, peakList2, mzTolerance, intensityThreshold,
        minimumIonsMatched, scoreThreshold},
        "https://mzmine.github.io/mzmine_documentation/module_docs/id_ms2_similarity/ms2-similarity-search.html");
  }

  @Override
  public @NotNull IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.SUPPORTED;
  }
}
