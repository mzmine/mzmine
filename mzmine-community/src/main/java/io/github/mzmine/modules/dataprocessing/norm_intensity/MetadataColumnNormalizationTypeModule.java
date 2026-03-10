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
import io.github.mzmine.modules.visualization.projectmetadata.table.MetadataTable;
import io.github.mzmine.modules.visualization.projectmetadata.table.MetadataTableUtils.InterpolationWeights;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.DoubleMetadataColumn;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn;
import io.github.mzmine.parameters.ParameterSet;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.jetbrains.annotations.NotNull;

public class MetadataColumnNormalizationTypeModule implements NormalizationTypeModule {

  @Override
  public @NotNull String getName() {
    return "Metadata column";
  }

  @Override
  public @NotNull Class<? extends ParameterSet> getParameterSetClass() {
    return MetadataColumnNormalizationTypeParameters.class;
  }

  @Override
  public @NotNull List<RawDataFile> getReferenceSamples(@NotNull final FeatureList flist,
      @NotNull final ParameterSet normalizationModuleParameters) {
    return List.copyOf(flist.getRawDataFiles());
  }

  @Override
  public @NotNull Map<@NotNull RawDataFile, @NotNull NormalizationFunction> createReferenceFunctions(
      @NotNull final List<@NotNull RawDataFile> referenceFiles,
      @NotNull final ModularFeatureList featureList, @NotNull final MetadataTable metadata,
      @NotNull final ParameterSet mainParameters,
      @NotNull final ParameterSet moduleSpecificParameters) {
    final String columnName = moduleSpecificParameters.getValue(
        MetadataColumnNormalizationTypeParameters.metadataColumn);
    if (columnName == null || columnName.isBlank()) {
      throw new IllegalStateException("No metadata column selected for normalization.");
    }

    final MetadataColumn<?> metadataColumn = metadata.getColumnByName(columnName);
    if (!(metadataColumn instanceof final DoubleMetadataColumn numericColumn)) {
      throw new IllegalStateException(
          "Selected metadata column is missing or not numeric: " + columnName);
    }

    final Map<@NotNull RawDataFile, @NotNull Double> fileToMetadataValue = new HashMap<>(
        referenceFiles.size());
    for (final RawDataFile rawDataFile : referenceFiles) {
      final Double metadataValue = metadata.getValue(numericColumn, rawDataFile);
      // decision: allow 0 as value as "no normalization"
      if (metadataValue == null || !Double.isFinite(metadataValue) || metadataValue < 0) {
        throw new IllegalStateException(
            "Invalid metadata value in column '%s' for file '%s': %s".formatted(columnName,
                rawDataFile.getName(), metadataValue));
      }
      fileToMetadataValue.put(rawDataFile, metadataValue);
    }

    final double maxMetadataValue = fileToMetadataValue.values().stream().max(Double::compare)
        .orElseThrow(() -> new IllegalStateException(
            "No valid metadata values available in column: " + columnName));

    final Map<@NotNull RawDataFile, @NotNull NormalizationFunction> functions = new HashMap<>(
        referenceFiles.size());
    for (final Entry<@NotNull RawDataFile, @NotNull Double> entry : fileToMetadataValue.entrySet()) {
      final RawDataFile file = entry.getKey();
      final double factor =
          Double.compare(entry.getValue(), 0) == 0 ? 1 : maxMetadataValue / entry.getValue();
      final LocalDateTime runDate = NormalizationTypeModule.getRunDateOrThrow(metadata, file);
      functions.put(file, new FactorNormalizationFunction(file, runDate, factor));
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

    throw new RuntimeException(
        "Interpolating a normalization is invalid for Metadata normalization. Prove a metadata value for file %s.".formatted(
            fileToInterpolate.getName()));
  }
}
