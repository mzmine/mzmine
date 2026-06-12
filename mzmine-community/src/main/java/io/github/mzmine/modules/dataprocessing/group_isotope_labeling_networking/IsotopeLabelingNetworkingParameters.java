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

package io.github.mzmine.modules.dataprocessing.group_isotope_labeling_networking;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.metadata.MetadataGroupingParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import java.text.NumberFormat;

public class IsotopeLabelingNetworkingParameters extends SimpleParameterSet {

  public static final FeatureListsParameter featureLists = new FeatureListsParameter(
      "Labeled feature lists", "Feature lists annotated by the Untargeted Isotope Labeling module",
      1, Integer.MAX_VALUE, false);

  public static final MetadataGroupingParameter metadataGrouping = new MetadataGroupingParameter(
      "Sample grouping", "Metadata column used to distinguish labeled from unlabeled samples "
      + "(same as in Untargeted Isotope Labeling)");

  public static final StringParameter labeledGroupValue = new StringParameter("Labeled group value",
      "Value in the selected metadata column that identifies labeled samples", "labeled");

  public static final DoubleParameter minICSScore = new DoubleParameter("Minimum ICS score",
      "Minimum Isotopologue Compatibility Score (0–1) to create a network edge.",
      NumberFormat.getNumberInstance(), 0.7, 0.0, 1.0);

  public static final ComboParameter<String> intensityMeasure = new ComboParameter<>(
      "Intensity measure",
      "Per-sample feature intensity to use when building the isotopologue distribution",
      new String[]{"Height", "Area"}, "Height");

  public IsotopeLabelingNetworkingParameters() {
    super(new Parameter[]{featureLists, metadataGrouping, labeledGroupValue, minICSScore,
        intensityMeasure});
  }
}
