/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.modules.dataprocessing.filter_isotopefinder;

import io.github.mzmine.modules.dataprocessing.filter_isotopefinder.engine.RatioAggregation;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;

/**
 * Parameters for refining the isotope pattern across the scans within the feature FWHM. The pattern
 * is detected on the most intense scan and then refined per scan (not pre-merged), which stabilizes
 * relative intensities and recovers fine structure that is resolved only in some scans.
 */
public class FwhmRefineParameters extends SimpleParameterSet {

  public static final MZToleranceParameter refineMzTolerance = new MZToleranceParameter(
      "Cross-scan m/z tolerance",
      "Tolerance for matching signals across scans and clustering resolved fine structure.", 0.005,
      15);

  public static final ComboParameter<RatioAggregation> ratioAggregation = new ComboParameter<>(
      "Ratio aggregation",
      "How to aggregate the per-scan offset/base intensity ratios into a robust relative intensity.",
      RatioAggregation.values(), RatioAggregation.MEDIAN);

  public static final IntegerParameter minScansPresent = new IntegerParameter(
      "Min scans for recovered signal",
      "A signal that was absent in the detection scan must appear in at least this many FWHM scans to be added.",
      2, 1, 100_000);

  public FwhmRefineParameters() {
    super(new Parameter[]{refineMzTolerance, ratioAggregation, minScansPresent});
  }
}
