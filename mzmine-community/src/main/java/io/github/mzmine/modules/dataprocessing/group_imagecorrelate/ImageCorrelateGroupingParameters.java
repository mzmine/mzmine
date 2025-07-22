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

package io.github.mzmine.modules.dataprocessing.group_imagecorrelate;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.PercentParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.util.maths.similarity.SimilarityMeasure;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

public class ImageCorrelateGroupingParameters extends SimpleParameterSet {

  public static final FeatureListsParameter FEATURE_LISTS = new FeatureListsParameter();


  /**
   * Filter data points by minimum height
   */
  public static final DoubleParameter NOISE_LEVEL = new DoubleParameter(
      "Intensity threshold for co-localization",
      "This intensity threshold is used to filter data points before image co-localization",
      MZmineCore.getConfiguration().getIntensityFormat(),
      ImageCorrelateGroupingTask.NON_ZERO_INTENSITY, ImageCorrelateGroupingTask.NON_ZERO_INTENSITY,
      null);

  public static final IntegerParameter MIN_NUMBER_OF_PIXELS = new IntegerParameter(
      "Minimum number of co-located pixels",
      "Minimum number of locations (pixels) that must be co-located", 0);

  public static final OptionalParameter<DoubleParameter> QUANTILE_THRESHOLD = new OptionalParameter<>(
      new DoubleParameter("Ignore intensities below percentile",
          "Only consider intensities above the selected percentile. The percentile is applied after removing 0 intensity data points (scans without this signal).",
          MZmineCore.getConfiguration().getRTFormat(), 0.05, 0.0, 1.0), true);

  public static final OptionalParameter<DoubleParameter> HOTSPOT_REMOVAL = new OptionalParameter<>(
      new DoubleParameter("Ignore high intensity outliers",
          "Only consider values below the selected percentile, 0.99 is recommended. The percentile is applied after removing 0 intensity data points (scans without this signal).",
          MZmineCore.getConfiguration().getRTFormat(), 0.99, 0.0, 1.0), true);

  public static final ComboParameter<SimilarityMeasure> MEASURE = new ComboParameter<>(
      "Similarity measure", "Similarity measure",
      new SimilarityMeasure[]{SimilarityMeasure.PEARSON, SimilarityMeasure.COSINE_SIM,
          SimilarityMeasure.SPEARMAN, SimilarityMeasure.LOG_RATIO_VARIANCE_1,
          SimilarityMeasure.LOG_RATIO_VARIANCE_2}, SimilarityMeasure.PEARSON);

  public static final PercentParameter MIN_R = new PercentParameter("Minimum similarity",
      "Minimum percentage for image correlation in one raw file.", 0.70, 0d, 1d);

  // Constructor
  public ImageCorrelateGroupingParameters() {
    super(new Parameter[]{FEATURE_LISTS, NOISE_LEVEL, MIN_NUMBER_OF_PIXELS, QUANTILE_THRESHOLD,
            HOTSPOT_REMOVAL, MEASURE, MIN_R},
        "https://mzmine.github.io/mzmine_documentation/module_docs/group_imagecorrelate/image-colocalization.html");
  }

  @Override
  public @NotNull IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.SUPPORTED;
  }

  @Override
  public int getVersion() {
    return 2;
  }

  @Override
  public Map<String, Parameter<?>> getNameParameterMap() {
    // parameters were renamed but stayed the same type
    var nameParameterMap = super.getNameParameterMap();
    nameParameterMap.put("Ignore very high intensity outliers", getParameter(HOTSPOT_REMOVAL));
    return nameParameterMap;
  }
}
