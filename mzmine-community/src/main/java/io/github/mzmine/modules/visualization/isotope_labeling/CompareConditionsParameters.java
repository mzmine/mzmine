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
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;

public class CompareConditionsParameters extends SimpleParameterSet {

  // Condition names
  public static final StringParameter condition1Name = new StringParameter("Condition 1 name",
      "Name of the first condition (e.g., 'control')", "control");

  public static final StringParameter condition2Name = new StringParameter("Condition 2 name",
      "Name of the second condition (e.g., 'treatment')", "treatment");

  // Feature list selectors for conditions
  public static final FeatureListsParameter condition1UnlabeledLists = new FeatureListsParameter(
      "Condition 1 unlabeled feature lists",
      "Feature lists containing unlabeled samples for condition 1", 1, Integer.MAX_VALUE);

  public static final FeatureListsParameter condition1LabeledLists = new FeatureListsParameter(
      "Condition 1 labeled feature lists",
      "Feature lists containing labeled samples for condition 1", 1, Integer.MAX_VALUE);

  public static final FeatureListsParameter condition2UnlabeledLists = new FeatureListsParameter(
      "Condition 2 unlabeled feature lists",
      "Feature lists containing unlabeled samples for condition 2", 1, Integer.MAX_VALUE);

  public static final FeatureListsParameter condition2LabeledLists = new FeatureListsParameter(
      "Condition 2 labeled feature lists",
      "Feature lists containing labeled samples for condition 2", 1, Integer.MAX_VALUE);

  // Statistical parameters
  public static final BooleanParameter assumeEqualVariance = new BooleanParameter(
      "Assume equal variance",
      "Whether to assume that relative isotopologue intensities in each condition are drawn from distributions with equal variance",
      false);

  public static final ComboParameter<String> significanceCriteria = new ComboParameter<>(
      "Significance criteria",
      "Which peaks to consider when determining significant differences between conditions",
      new String[]{"Base peak only", "Any peak"}, "Any peak");

  public CompareConditionsParameters() {
    super(condition1Name, condition2Name, condition1UnlabeledLists, condition1LabeledLists,
        condition2UnlabeledLists, condition2LabeledLists, assumeEqualVariance,
        significanceCriteria);
  }
}