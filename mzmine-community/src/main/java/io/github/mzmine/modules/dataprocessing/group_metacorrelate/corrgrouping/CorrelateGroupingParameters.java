/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.modules.dataprocessing.group_metacorrelate.corrgrouping;


import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.group_metacorrelate.correlation.FeatureShapeCorrelationParameters;
import io.github.mzmine.modules.dataprocessing.group_metacorrelate.correlation.InterSampleHeightCorrParameters;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.AdvancedParametersParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.MinimumFeaturesFilterParameters;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.OriginalFeatureListHandlingParameter;
import io.github.mzmine.parameters.parametertypes.OriginalFeatureListHandlingParameter.OriginalFeatureListOption;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.submodules.OptionalModuleParameter;
import io.github.mzmine.parameters.parametertypes.submodules.ParameterSetParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.RTToleranceParameter;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

public class CorrelateGroupingParameters extends SimpleParameterSet {

  // General parameters
  public static final FeatureListsParameter PEAK_LISTS = new FeatureListsParameter();
  // RT-tolerance: Grouping
  public static final RTToleranceParameter RT_TOLERANCE = new RTToleranceParameter("RT tolerance",
      "Maximum allowed difference of retention time to set a relationship between peaks");

  /**
   * Filter by minimum height
   */
  public static final DoubleParameter MIN_HEIGHT = new DoubleParameter("Minimum feature height",
      "Used by min samples filter and MS annotations. Minimum height to recognize a feature (important to destinguis between real peaks and minor gap-filled).",
      MZmineCore.getConfiguration().getIntensityFormat(), 0d);

  /**
   * Filter by minimum height
   */
  public static final DoubleParameter NOISE_LEVEL = new DoubleParameter(
      "Intensity threshold for correlation",
      "This intensity threshold is used to filter data points before feature shape correlation",
      MZmineCore.getConfiguration().getIntensityFormat(), 0d);

  /**
   * Filter out by minimum number of features in all samples and/or in at least one sample group
   * features with height>=minHeight
   */
  public static final ParameterSetParameter<MinimumFeaturesFilterParameters> MIN_SAMPLES_FILTER = new ParameterSetParameter<>(
      "Min samples filter",
      "Filter out by min number of features in all samples and in sample groups",
      new MinimumFeaturesFilterParameters(true));

  // Sub parameters of correlation grouping
  public static final OptionalModuleParameter<FeatureShapeCorrelationParameters> FSHAPE_CORRELATION = new OptionalModuleParameter<>(
      "Feature shape correlation", "Grouping based on Pearson correlation of the feature shapes.",
      new FeatureShapeCorrelationParameters(true), true);

  public static final OptionalModuleParameter<InterSampleHeightCorrParameters> IMAX_CORRELATION = new OptionalModuleParameter<>(
      "Feature height correlation",
      "Feature to feature correlation of the maximum intensities across all samples.",
      new InterSampleHeightCorrParameters(true), true);

  public static final OptionalParameter<StringParameter> SUFFIX = new OptionalParameter<>(
      new StringParameter("Suffix (or auto)", "Select suffix or deselect for auto suffix"), false);

  public static final AdvancedParametersParameter<AdvancedCorrelateGroupingParameters> advanced = new AdvancedParametersParameter<>(
      new AdvancedCorrelateGroupingParameters(), true);

  // keep is the initial state that we used
  // some workflows depend on the intial list to be there
  public static final OriginalFeatureListHandlingParameter handleOriginal = new OriginalFeatureListHandlingParameter(
      true, OriginalFeatureListOption.KEEP);

  // Constructor
  public CorrelateGroupingParameters() {
    super(
        "https://mzmine.github.io/mzmine_documentation/module_docs/id_ion_networking/metacorr/metacorr.html",
        PEAK_LISTS, RT_TOLERANCE,
        // feature filter
        MIN_HEIGHT, NOISE_LEVEL, MIN_SAMPLES_FILTER,
        // feature shape correlation
        FSHAPE_CORRELATION,
        // intensity max correlation
        IMAX_CORRELATION,
        // suffix or auto suffix
        SUFFIX, handleOriginal, advanced);
  }

  @Override
  public Map<String, Parameter<?>> getNameParameterMap() {
    // parameters were renamed but stayed the same type
    var nameParameterMap = super.getNameParameterMap();
    // we use the same parameters here so no need to increment the version. Loading will work fine
    nameParameterMap.put("Correlation grouping", FSHAPE_CORRELATION);
    nameParameterMap.put("Intensity correlation threshold", NOISE_LEVEL);
    nameParameterMap.put("Min height", MIN_HEIGHT);
    return nameParameterMap;
  }


  @Override
  public @NotNull IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.SUPPORTED;
  }

  @Override
  public int getVersion() {
    return 3;
  }
}
