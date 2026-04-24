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
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.modules.visualization.projectmetadata.table.MetadataTable;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.DoubleMetadataColumn;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn;
import io.github.mzmine.parameters.ParameterSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.jetbrains.annotations.NotNull;

public class MetadataColumnNormalizationTypeModule implements NormalizationTypeModule {

  @Override
  public @NotNull String getName() {
    return NormalizationType.MetadataColumn.toString();
  }

  @Override
  public @NotNull Class<? extends ParameterSet> getParameterSetClass() {
    return MetadataColumnNormalizationTypeParameters.class;
  }

  @Override
  public void createAllNormalizationFunctionsToSummary(
      @NotNull IntensityNormalizationSearchableSummary summary,
      @NotNull ModularFeatureList featureList, @NotNull SamplesBatch samplesBatch,
      @NotNull MetadataTable metadata, @NotNull ParameterSet mainParameters,
      @NotNull ParameterSet moduleSpecificParameters) {

    final MetadataNormalizationConfig metadataConfig = moduleSpecificParameters.getValue(
        MetadataColumnNormalizationTypeParameters.metadataColumn);
    final String columnName = metadataConfig.metadataColumn();
    if (columnName.isBlank()) {
      throw new IllegalStateException("No metadata column selected for normalization.");
    }

    final MetadataColumn<?> metadataColumn = metadata.getColumnByName(columnName);
    if (!(metadataColumn instanceof final DoubleMetadataColumn numericColumn)) {
      throw new IllegalStateException(
          "Selected metadata column is missing or not numeric: " + columnName);
    }

    final Map<@NotNull RawDataFile, @NotNull Double> fileToMetadataValue = HashMap.newHashMap(
        samplesBatch.size());
    for (final RawDataFile rawDataFile : samplesBatch.getRaws()) {
      final Double metadataValue = metadata.getValue(numericColumn, rawDataFile);
      // decision: allow 0 as value as "no normalization"
      if (metadataValue == null || !Double.isFinite(metadataValue) || metadataValue < 0) {
        throw new IllegalStateException(
            "Invalid metadata value (needs to be >=0) in column '%s' for file '%s': %s".formatted(
                columnName, rawDataFile.getName(), metadataValue));
      }

      // skip 0 factors and do not include them downstream
      if (Double.compare(metadataValue, 0) == 0) {
        continue;
      }

      fileToMetadataValue.put(rawDataFile, metadataValue);
    }

    if (fileToMetadataValue.isEmpty()) {
      throw new IllegalStateException(
          "No valid metadata values available in column: " + columnName);
    }

    for (final Entry<@NotNull RawDataFile, @NotNull Double> entry : fileToMetadataValue.entrySet()) {
      final RawDataFile file = entry.getKey();
      // correct sample by factor/median to keep general intensity scales
      // could also think about using the factor as is
      final double factor = entry.getValue();

      // divide or multiple by factor
      NormalizationFunction function = new FactorNormalizationFunction(
          metadataConfig.mode().isDivide() ? 1d / factor : factor);

      // add or merge function into a new instance within summary
      summary.addMergeFunction(file, function);
    }
  }

}
