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

package io.github.mzmine.modules.dataprocessing.filter_duplicatefilter;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.OriginalFeatureListHandlingParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.RTToleranceParameter;

public class DuplicateFilterParameters extends SimpleParameterSet {

  public enum FilterMode {
    OLD_AVERAGE, NEW_AVERAGE, SINGLE_FEATURE;

    @Override
    public String toString() {
      return super.toString().replaceAll("_", " ");
    }
  }

  public static final FeatureListsParameter peakLists = new FeatureListsParameter();

  public static final StringParameter suffix =
      new StringParameter("Name suffix", "Suffix to be added to feature list name", "filtered");

  public static final ComboParameter<FilterMode> filterMode = new ComboParameter<>("Filter mode",
      "Old average: Only keep the row with the maximum avg area.\n New average: Create consensus row from duplicates (DETECTED>ESTIMATED>UNKNOWN).\n "
          + "Single feature: Marks rows as duplicates if they share at least one feature (in one raw data file) with the same RT and m/z. Creates a consensus row.",
      FilterMode.values(), FilterMode.NEW_AVERAGE);

  public static final MZToleranceParameter mzDifferenceMax =
      new MZToleranceParameter("m/z tolerance", "Maximum m/z difference between duplicate peaks");
  public static final RTToleranceParameter rtDifferenceMax = new RTToleranceParameter(
      "RT tolerance", "Maximum retention time difference between duplicate peaks");

  public static final BooleanParameter requireSameIdentification =
      new BooleanParameter("Require same identification",
          "If checked, duplicate peaks must have same identification(s)");

  public static final OriginalFeatureListHandlingParameter handleOriginal = new OriginalFeatureListHandlingParameter(
      true);

  public DuplicateFilterParameters() {
    super(new Parameter[]{peakLists, suffix, filterMode, mzDifferenceMax, rtDifferenceMax,
        requireSameIdentification, handleOriginal},
        "https://mzmine.github.io/mzmine_documentation/module_docs/filter_duplicate_features/duplicate_feature_filter.html");
  }

}
