/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.modules.dataprocessing.norm_intensity;

import io.github.mzmine.datamodel.AbundanceMeasure;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.ArrayUtils;
import io.github.mzmine.util.MathUtils;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FeatureIntensityNormalizationModule extends AbstractFactorNormalizationTypeModule {

  @Override
  public @NotNull String getName() {
    return NormalizationType.ByFeatureIntensity.toString();
  }

  @Override
  public final @NotNull Class<? extends ParameterSet> getParameterSetClass() {
    return FeatureIntensityNormalizationParameters.class;
  }

  @Override
  protected double getNormalizationMetricForFile(
      @NotNull IntensityNormalizationSearchableSummary summary, @NotNull final RawDataFile file,
      @NotNull final ModularFeatureList featureList,
      @NotNull final ParameterSet linearNormalizerParameters,
      @NotNull final ParameterSet moduleSpecificParameters) {
    final AbundanceMeasure abundanceMeasure = linearNormalizerParameters.getValue(
        IntensityNormalizerParameters.featureMeasurementType);
    if (abundanceMeasure == null) {
      throw new IllegalStateException("No feature abundance measure selected for normalization.");
    }
    final FeatureIntensityNormalizationMode mode = moduleSpecificParameters.getValue(
        FeatureIntensityNormalizationParameters.mode);

    // need to apply any previous normalization function if it is available
    // to apply the next normalization on top of the existing
    final @Nullable RawFileNormalizationFunction existingNormFunction = summary.functions().get(file);
    final double[] abundances = featureList.stream().map(r -> (ModularFeature) r.getFeature(file))
        .filter(Objects::nonNull)
        .mapToDouble(feature -> abundanceMeasure.getOrNaN(feature, existingNormFunction))
        .filter(d -> !Double.isNaN(d)).toArray();

    if (abundances.length == 0) {
      throw new IllegalStateException(
          "No feature abundances found for file %s in feature list %s.".formatted(file.getName(),
              featureList.getName()));
    }

    final double result = switch (mode) {
      case MEDIAN -> MathUtils.calcMedian(abundances);
      case AVERAGE -> MathUtils.calcAvg(abundances);
      case SUM_TIC -> ArrayUtils.sum(abundances);
      case MAX -> ArrayUtils.max(abundances).orElse(0d);
    };

    if (!Double.isFinite(result) || Double.compare(result, 0d) == 0) {
      throw new IllegalStateException(
          "No features found or %s of feature intensities is 0 for file: %s".formatted(mode,
              file.getName()));
    }

    return result;
  }
}
