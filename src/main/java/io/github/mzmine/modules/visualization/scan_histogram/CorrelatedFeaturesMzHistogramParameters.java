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

package io.github.mzmine.modules.visualization.scan_histogram;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileSelectionType;
import io.github.mzmine.parameters.parametertypes.ranges.MZRangeParameter;
import io.github.mzmine.parameters.parametertypes.ranges.RTRangeParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import java.text.DecimalFormat;
import java.util.List;
import javafx.stage.FileChooser.ExtensionFilter;

public class CorrelatedFeaturesMzHistogramParameters extends SimpleParameterSet {

  private static final List<ExtensionFilter> extensions = List.of( //
      new ExtensionFilter("comma-separated values", "*.csv") //
  );

  public static final FeatureListsParameter featureLists = new FeatureListsParameter();
  public static final MZRangeParameter mzRange = new MZRangeParameter(true);
  public static final OptionalParameter<RTRangeParameter> rtRange = new OptionalParameter<>(
      new RTRangeParameter(false));
  public static final DoubleParameter binWidth = new DoubleParameter("m/z bin width",
      "Binning of m/z values for feature picking ", MZmineCore.getConfiguration().getMZFormat(),
      0.001);

  public static final DoubleParameter minCorr = new DoubleParameter("Minimum Pearson correlation",
      "Minimum Pearson correlation of feature shapes ", new DecimalFormat("0.000"), 0.85);

  public static final BooleanParameter limitToDoubleMz = new BooleanParameter("Limit delta to m/z",
      "Maximum m/z delta is the m/z of the smaller ion (feature list row)", true);

  public static final OptionalParameter<FileNameParameter> saveToFile = new OptionalParameter<>(
      new FileNameParameter("Append to file",
          "Append the correlated features delta m/z to a csv file", extensions,
          FileSelectionType.SAVE), false);

  public CorrelatedFeaturesMzHistogramParameters() {
    super(new Parameter[]{featureLists, mzRange, rtRange, minCorr, limitToDoubleMz, binWidth,
        saveToFile},
        "https://mzmine.github.io/mzmine_documentation/visualization_modules/processed_additional/processed_additional.html#correlated-features-deltamz-histogram");
  }

}
