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
package io.github.mzmine.modules.dataprocessing.align_adap3;

import dulab.adap.workflow.AlignmentParameters;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.RTToleranceParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.ToleranceType;
import java.text.NumberFormat;
import java.util.Map;
import javafx.collections.FXCollections;

/**
 * @author aleksandrsmirnov
 */
public class ADAP3AlignerParameters extends SimpleParameterSet {

  public static final StringParameter NEW_PEAK_LIST_NAME = new StringParameter(
      "Aligned Feature List Name", "Feature list name", "Aligned feature list");

  public static final FeatureListsParameter PEAK_LISTS = new FeatureListsParameter();
  public static final DoubleParameter SAMPLE_COUNT_RATIO = new DoubleParameter(
      "Min confidence (between 0 and 1)",
      "A fraction of the total number of samples. An aligned feature must be detected at "
          + "least in several samples.\nThis parameter determines the minimum number of samples where a "
          + "feature must be detected.", NumberFormat.getInstance(), 0.7, 0.0, 1.0);
  public static final MZToleranceParameter MZ_RANGE = new MZToleranceParameter(
      ToleranceType.SAMPLE_TO_SAMPLE);

  public static final RTToleranceParameter RET_TIME_RANGE = new RTToleranceParameter();
  public static final DoubleParameter SCORE_WEIGHT = new DoubleParameter(
      "Score weight (between 0 and 1)",
      "The weight w that is used in the similarity function. See the help file for details.",
      NumberFormat.getInstance(), 0.1, 0.0, 1.0);

  public static final DoubleParameter SCORE_TOLERANCE = new DoubleParameter(
      "Score threshold (between 0 and 1)",
      "The minimum value of the similarity function required for features to be aligned together.",
      NumberFormat.getInstance(), 0.75, 0.0, 1.0);
  private static final String[] EIC_SCORE_TYPES = new String[]{AlignmentParameters.RT_DIFFERENCE,
      AlignmentParameters.CROSS_CORRELATION};
  public static final ComboParameter<String> EIC_SCORE = new ComboParameter<>(
      "Retention time similarity",
      "Method used for calculating the retention time similarity. The retention time difference "
          + "(fast) is preferred method.", FXCollections.observableArrayList(EIC_SCORE_TYPES),
      AlignmentParameters.RT_DIFFERENCE);

  public ADAP3AlignerParameters() {
    super(new Parameter[]{PEAK_LISTS, SAMPLE_COUNT_RATIO, RET_TIME_RANGE, MZ_RANGE, SCORE_TOLERANCE,
            SCORE_WEIGHT, EIC_SCORE, NEW_PEAK_LIST_NAME},
        "https://mzmine.github.io/mzmine_documentation/module_docs/align_adap/align_adap_gc.html");
  }

  @Override
  public Map<String, Parameter<?>> getNameParameterMap() {
    // parameters were renamed but stayed the same type
    var nameParameterMap = super.getNameParameterMap();
    // we use the same parameters here so no need to increment the version. Loading will work fine
    nameParameterMap.put("m/z tolerance", MZ_RANGE);
    return nameParameterMap;
  }
}
