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

package io.github.mzmine.modules.dataprocessing.filter_groupms2_refine;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.PercentParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import org.jetbrains.annotations.NotNull;

public class GroupedMs2RefinementParameters extends SimpleParameterSet {

  public static final FeatureListsParameter featureLists = new FeatureListsParameter();

  public static final PercentParameter minimumRelativeFeatureHeight = new PercentParameter(
      "Minimum relative feature height",
      "If an MS2 was assigned to multiple features, only keep the feature assignments where feature height is at least X% of the highest feature.",
      0.25, 0d, 1d);

  public static final DoubleParameter minimumAbsoluteFeatureHeight = new DoubleParameter(
      "Minimum absolute feature height",
      "Only keep MS2-feature assignments if the feature height is at least this value. (comparable to the trigger intensity)",
      MZmineCore.getConfiguration().getIntensityFormat(), 0d);


  public GroupedMs2RefinementParameters() {
    super(new Parameter[]{featureLists, minimumRelativeFeatureHeight, minimumAbsoluteFeatureHeight},
        "https://mzmine.github.io/mzmine_documentation/module_docs/featdet_ms2_scan_pairing/ms2_scan_pairing.html");
  }

  @Override
  public @NotNull IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.SUPPORTED;
  }

}
