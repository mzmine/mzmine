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

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.modules.visualization.projectmetadata.SampleTypeFilter;
import io.github.mzmine.modules.visualization.projectmetadata.table.MetadataTable;
import io.github.mzmine.modules.visualization.projectmetadata.table.MetadataTableUtils;
import io.github.mzmine.modules.visualization.projectmetadata.table.MetadataTableUtils.InterpolationWeights;
import io.github.mzmine.parameters.ParameterSet;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

/**
 * Basic implementation for normalization functions that have constant factor and do not rely on mz
 * or rt of the feature.
 */
public abstract class AbstractFactorNormalizationTypeModule implements NormalizationTypeModule {

  @Override
  public final @NotNull Class<? extends ParameterSet> getParameterSetClass() {
    return FactorNormalizationModuleParameters.class;
  }

  @NotNull
  public List<RawDataFile> getReferenceSamples(@NotNull final FeatureList flist,
      @NotNull final ParameterSet normalizationModuleParameters) {
    final var sampleTypeFilter = new SampleTypeFilter(
        normalizationModuleParameters.getParameter(FactorNormalizationModuleParameters.sampleTypes)
            .getValue());
    return sampleTypeFilter.filterFiles(flist.getRawDataFiles());
  }

  @Override
  public @NotNull Map<@NotNull RawDataFile, @NotNull NormalizationFunction> createReferenceFunctions(
      @NotNull final List<@NotNull RawDataFile> referenceFiles,
      @NotNull final ModularFeatureList featureList, @NotNull final MetadataTable metadata,
      @NotNull final ParameterSet mainParameters,
      @NotNull final ParameterSet normalizerSpecificParam) {

    final Map<@NotNull RawDataFile, @NotNull Double> referenceToNormalizationMetric = referenceFiles.stream()
        .collect(Collectors.toMap(Function.identity(),
            file -> getNormalizationMetricForFile(file, featureList, mainParameters,
                normalizerSpecificParam)));
    final double maxNormalizationMetric = referenceToNormalizationMetric.values().stream()
        .max(Double::compare).orElse(1d);

    final Map<@NotNull RawDataFile, @NotNull NormalizationFunction> functions = new HashMap<>();
    for (final Entry<@NotNull RawDataFile, @NotNull Double> entry : referenceToNormalizationMetric.entrySet()) {
      final RawDataFile file = entry.getKey();
      final LocalDateTime runDate = MetadataTableUtils.getRunDate(metadata, file);
      final double normalizationFactor = maxNormalizationMetric / entry.getValue();
      functions.put(file, new FactorNormalizationFunction(file, runDate, normalizationFactor));
    }
    return functions;
  }

  @Override
  public @NotNull NormalizationFunction createInterpolatedFunction(
      @NotNull final RawDataFile fileToInterpolate,
      @NotNull final NormalizationFunction previousRunCalibration,
      @NotNull final NormalizationFunction nextRunCalibration,
      @NotNull final InterpolationWeights interpolationWeights,
      @NotNull final MetadataTable metadata, @NotNull final ParameterSet mainParameters,
      @NotNull final ParameterSet normalizerParameters) {
    if (!(previousRunCalibration instanceof FactorNormalizationFunction prev)
        || !(nextRunCalibration instanceof FactorNormalizationFunction next)) {
      throw new IllegalStateException("Input calibrations are no factor-based calibrations.");
    }
    final LocalDateTime runDate = MetadataTableUtils.getRunDate(metadata, fileToInterpolate);
    if (runDate == null) {
      throw new IllegalStateException(
          "No acquisition timestamp found for file: " + fileToInterpolate.getName());
    }

    final double factor = next.factor() * interpolationWeights.nextRunWeight()
        + prev.factor() * interpolationWeights.previousWeight();

    return new FactorNormalizationFunction(fileToInterpolate, runDate, factor);
  }

  protected abstract double getNormalizationMetricForFile(@NotNull RawDataFile file,
      @NotNull ModularFeatureList featureList, @NotNull ParameterSet linearNormalizerParameters,
      @NotNull ParameterSet moduleSpecificParameters);
}
