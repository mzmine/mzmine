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

package io.github.mzmine.modules.dataprocessing.filter_peakcomparisonrowfilter;

import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import java.text.DecimalFormat;

import com.google.common.collect.Range;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.ranges.DoubleRangeParameter;

public class PeakComparisonRowFilterParameters extends SimpleParameterSet {

  public static final FeatureListsParameter PEAK_LISTS = new FeatureListsParameter();

  public static final StringParameter SUFFIX =
      new StringParameter("Name suffix", "Suffix to be added to feature list name", "filtered");

  public static final IntegerParameter COLUMN_INDEX_1 =
      new IntegerParameter("1st peak column to compare (zero indexed)",
          "index of second column for comparison, e.g. \"0\"", 0);

  public static final IntegerParameter COLUMN_INDEX_2 =
      new IntegerParameter("2nd peak column to compare (zero indexed)",
          "index of second column for comparison,e.g. \"1\"", 1);

  public static final OptionalParameter<DoubleRangeParameter> FOLD_CHANGE =
      new OptionalParameter<>(new DoubleRangeParameter("Fold change range : log2(peak1/peak2)",
          "Return peaks with a fold change within this range", new DecimalFormat("0.0"),
          Range.closed(-5.0, 5.0)));

  public static final OptionalParameter<DoubleRangeParameter> MZ_PPM_DIFF = new OptionalParameter<>(
      new DoubleRangeParameter("m/z difference range : peak1 to peak2 (ppm)",
          "Return peaks with a m/z difference within this range", new DecimalFormat("0.0"),
          Range.closed(-5.0, 5.0)));

  public static final OptionalParameter<DoubleRangeParameter> RT_DIFF =
      new OptionalParameter<>(new DoubleRangeParameter("RT difference range : peak1 to peak2 (min)",
          "Return peaks with an RT difference within this range", new DecimalFormat("0.0"),
          Range.closed(-0.2, 0.2)));

  public static final BooleanParameter AUTO_REMOVE = new BooleanParameter(
      "Remove source feature list after filtering",
      "If checked, the original feature list will be removed leaving only the filtered version");

  public PeakComparisonRowFilterParameters() {
    super(new Parameter[] {PEAK_LISTS, SUFFIX, COLUMN_INDEX_1, COLUMN_INDEX_2, FOLD_CHANGE,
        MZ_PPM_DIFF, RT_DIFF, AUTO_REMOVE},
        "https://mzmine.github.io/mzmine_documentation/module_docs/filter_peakcomparison_row_filter/filter_peakcomparison_row_filter.html");
  }

}
