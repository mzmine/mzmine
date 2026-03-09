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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.mzmine.datamodel.AbundanceMeasure;
import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.modules.visualization.projectmetadata.table.MetadataTable;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.project.impl.RawDataFileImpl;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

class MedianFeatureIntensityNormalizationTypeModuleTest {

  @Test
  void createReferenceFunctionsUsesMedianFeatureIntensity() {
    final MedianFeatureIntensityNormalizationTypeModule module = new MedianFeatureIntensityNormalizationTypeModule();
    final RawDataFileImpl fileA = createRawFile("file_a", LocalDateTime.of(2026, 1, 1, 10, 0));
    final RawDataFileImpl fileB = createRawFile("file_b", LocalDateTime.of(2026, 1, 1, 10, 5));

    final ModularFeatureList featureList = new ModularFeatureList("flist", null, fileA, fileB);
    addRow(featureList, 1, fileA, 1f, fileB, 1f);
    addRow(featureList, 2, fileA, 2f, fileB, 1f);
    addRow(featureList, 3, fileA, 3f, fileB, 1f);
    addRow(featureList, 4, fileA, 100f, fileB, 1f);

    final Map<RawDataFile, NormalizationFunction> functions = module.createReferenceFunctions(
        List.of(fileA, fileB), featureList, new MetadataTable(false),
        createMainParameters(AbundanceMeasure.Height), new FactorNormalizationModuleParameters());

    final FactorNormalizationFunction functionA = assertInstanceOf(
        FactorNormalizationFunction.class, functions.get(fileA));
    final FactorNormalizationFunction functionB = assertInstanceOf(
        FactorNormalizationFunction.class, functions.get(fileB));

    // Median(file_a)=2.5 and Median(file_b)=1.0 => maxMetric=2.5.
    assertEquals(1d, functionA.getNormalizationFactor(0d, 0f), 1e-12);
    assertEquals(2.5d, functionB.getNormalizationFactor(0d, 0f), 1e-12);
    assertEquals(fileA.getStartTimeStamp(), functionA.acquisitionTimestamp());
    assertEquals(fileB.getStartTimeStamp(), functionB.acquisitionTimestamp());
  }

  @Test
  void createReferenceFunctionsThrowsIfNoFeaturesFound() {
    final MedianFeatureIntensityNormalizationTypeModule module = new MedianFeatureIntensityNormalizationTypeModule();
    final RawDataFileImpl file = createRawFile("empty_file", LocalDateTime.of(2026, 1, 1, 10, 0));
    final ModularFeatureList featureList = new ModularFeatureList("flist", null, file);

    final IllegalStateException exception = assertThrows(IllegalStateException.class,
        () -> module.createReferenceFunctions(List.of(file), featureList, new MetadataTable(false),
            createMainParameters(AbundanceMeasure.Height),
            new FactorNormalizationModuleParameters()));

    assertEquals("No features found or median of feature intensities is 0 for file: empty_file",
        exception.getMessage());
  }

  private static @NotNull RawDataFileImpl createRawFile(final @NotNull String name,
      final @NotNull LocalDateTime timestamp) {
    final RawDataFileImpl rawDataFile = new RawDataFileImpl(name, null, null);
    rawDataFile.setStartTimeStamp(timestamp);
    return rawDataFile;
  }

  private static @NotNull ParameterSet createMainParameters(
      final @NotNull AbundanceMeasure abundanceMeasure) {
    final IntensityNormalizerParameters parameters = new IntensityNormalizerParameters();
    parameters.setParameter(IntensityNormalizerParameters.featureMeasurementType, abundanceMeasure);
    return parameters;
  }

  private static void addRow(final @NotNull ModularFeatureList featureList, final int rowId,
      final @NotNull RawDataFile fileA, final @Nullable Float fileAHeight,
      final @NotNull RawDataFile fileB, final @Nullable Float fileBHeight) {
    final ModularFeatureListRow row = new ModularFeatureListRow(featureList, rowId);
    if (fileAHeight != null) {
      row.addFeature(fileA, createFeature(featureList, fileA, fileAHeight), false);
    }
    if (fileBHeight != null) {
      row.addFeature(fileB, createFeature(featureList, fileB, fileBHeight), false);
    }
    featureList.addRow(row);
  }

  private static @NotNull ModularFeature createFeature(
      final @NotNull ModularFeatureList featureList, final @NotNull RawDataFile rawDataFile,
      final float abundance) {
    final ModularFeature feature = new ModularFeature(featureList, rawDataFile,
        FeatureStatus.DETECTED);
    feature.setHeight(abundance);
    feature.setArea(abundance);
    feature.setMZ(100d);
    feature.setRT(5f);
    return feature;
  }
}
