/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.modules.dataprocessing.norm_intensity;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.modules.visualization.projectmetadata.table.MetadataTable;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.MathUtils;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

/**
 * Basic implementation for normalization functions that have constant factor and do not rely on mz
 * or rt of the feature.
 */
public abstract sealed class AbstractFactorNormalizationTypeModule extends
    NormalizationTypeWithReferencesModule permits FeatureIntensityNormalizationModule,
    TotalRawSignalNormalizationTypeModule {

  private static final Logger logger = Logger.getLogger(
      AbstractFactorNormalizationTypeModule.class.getName());

  @Override
  public @NotNull List<RawDataFile> getReferenceSamples(@NotNull FeatureList flist,
      @NotNull SamplesBatch samplesBatch, @NotNull ParameterSet normalizationModuleParameters) {
    // select reference samples (like pooled QCs)
    return NormalizationFunctionUtils.getReferenceSamplesOrThrow(false, samplesBatch,
        normalizationModuleParameters.getValue(
            FeatureIntensityNormalizationParameters.sampleTypes));
  }

  @Override
  public @NotNull Map<@NotNull RawDataFile, @NotNull NormalizationFunction> createReferenceFunctions(
      @NotNull IntensityNormalizationSearchableSummary summary,
      @NotNull final List<@NotNull RawDataFile> referenceFiles,
      @NotNull final ModularFeatureList featureList, @NotNull SamplesBatch samplesBatch,
      @NotNull final MetadataTable metadata, @NotNull final ParameterSet mainParameters,
      @NotNull final ParameterSet normalizerSpecificParam) {

    final Map<@NotNull RawDataFile, @NotNull Double> referenceToNormalizationMetric = referenceFiles.stream()
        .collect(Collectors.toMap(Function.identity(),
            file -> getNormalizationMetricForFile(summary, file, featureList, mainParameters,
                normalizerSpecificParam)));

    final ReferenceFunctions<RawDataFile> referenceFunctions = calculateReferenceFunctions(
        referenceToNormalizationMetric);
    samplesBatch.setMedianReferenceNormMetric(referenceFunctions.referenceMetric());

    for (final Entry<RawDataFile, NormalizationFunction> entry : referenceFunctions.functions().entrySet()) {
      final RawDataFile file = entry.getKey();
      final NormalizationFunction function = entry.getValue();
      // add or merge function into a new instance within summary
      summary.addMergeFunction(file, function);
    }
    return referenceFunctions.functions();
  }

  /**
   * Derives the common reference metric and one constant normalization function per reference.
   * The returned functions are the unmerged functions used later for interpolation.
   *
   * @param <T> reference-sample key, such as a {@link RawDataFile} or an external sample ID;
   *            the input keys are retained in the returned function map
   */
  public static <T> @NotNull ReferenceFunctions<T> calculateReferenceFunctions(
      @NotNull final Map<T, Double> metrics) {
    if (metrics.isEmpty()) {
      return new ReferenceFunctions<>(Double.NaN, Map.of());
    }

    final double referenceMetric = MathUtils.calcMedian(
        metrics.values().stream().mapToDouble(Double::doubleValue).toArray());
    final Map<T, NormalizationFunction> functions = metrics.entrySet().stream()
        .collect(Collectors.toMap(Entry::getKey,
            entry -> new FactorNormalizationFunction(referenceMetric / entry.getValue())));
    return new ReferenceFunctions<>(referenceMetric, functions);
  }

  /**
   * Result of calculating factor-normalization reference functions.
   *
   * @param <T> reference-sample key, such as a {@link RawDataFile} or an external sample ID,
   *            matching the keys supplied to
   *            {@link AbstractFactorNormalizationTypeModule#calculateReferenceFunctions(Map)}
   */
  public record ReferenceFunctions<T>(double referenceMetric,
                                      @NotNull Map<T, NormalizationFunction> functions) {

    public ReferenceFunctions {
      functions = Map.copyOf(functions);
    }
  }

  protected abstract double getNormalizationMetricForFile(
      @NotNull IntensityNormalizationSearchableSummary summary, @NotNull RawDataFile file,
      @NotNull ModularFeatureList featureList, @NotNull ParameterSet linearNormalizerParameters,
      @NotNull ParameterSet moduleSpecificParameters);
}
