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

package io.github.mzmine.modules.dataprocessing.gapfill_peakfinder.multithreaded;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.OriginalFeatureListHandlingParameter;
import io.github.mzmine.parameters.parametertypes.PercentParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.RTToleranceParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.ToleranceType;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

public class MultiThreadPeakFinderParameters extends SimpleParameterSet {

  public static final FeatureListsParameter peakLists = new FeatureListsParameter();

  public static final StringParameter suffix = new StringParameter("Name suffix",
      "Suffix to be added to feature list name", "gap-filled");

  public static final PercentParameter intTolerance = new PercentParameter("Intensity tolerance",
      "Maximum allowed deviation from expected /\\ shape of a peak in chromatographic direction",
      0.2);

  public static final MZToleranceParameter MZTolerance = new MZToleranceParameter(
      ToleranceType.SAMPLE_TO_SAMPLE);

  public static final RTToleranceParameter RTTolerance = new RTToleranceParameter();

  public static final IntegerParameter minDataPoints = new IntegerParameter(
      "Minimum scans (data points)",
      "Only fill gaps with features with minimum number of data points. \n Usually lower number of data points are used.",
      1, 1, Integer.MAX_VALUE);

  public static final OriginalFeatureListHandlingParameter handleOriginal = new OriginalFeatureListHandlingParameter(
      true);

  public MultiThreadPeakFinderParameters() {
    super(new Parameter[]{peakLists, suffix, intTolerance, MZTolerance, RTTolerance, minDataPoints,
            handleOriginal},
        "https://mzmine.github.io/mzmine_documentation/module_docs/gapfill_peak_finder/gap-filling.html");
  }

  @Override
  public @NotNull IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.SUPPORTED;
  }

  @Override
  public Map<String, Parameter<?>> getNameParameterMap() {
    // parameters were renamed but stayed the same type
    var nameParameterMap = super.getNameParameterMap();
    // we use the same parameters here so no need to increment the version. Loading will work fine
    nameParameterMap.put("m/z tolerance", MZTolerance);
    nameParameterMap.put("Minimum data points", minDataPoints);
    return nameParameterMap;
  }
}
