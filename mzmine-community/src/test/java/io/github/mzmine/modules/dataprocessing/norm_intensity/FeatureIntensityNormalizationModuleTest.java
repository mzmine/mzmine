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

import static io.github.mzmine.modules.dataprocessing.norm_intensity.NormIntensityTestUtils.addRow;
import static io.github.mzmine.modules.dataprocessing.norm_intensity.NormIntensityTestUtils.createFeatureIntensityParameters;
import static io.github.mzmine.modules.dataprocessing.norm_intensity.NormIntensityTestUtils.createMainParameters;
import static io.github.mzmine.modules.dataprocessing.norm_intensity.NormIntensityTestUtils.createRawFile;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.mzmine.datamodel.AbundanceMeasure;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.modules.visualization.projectmetadata.table.MetadataTable;
import io.github.mzmine.project.impl.RawDataFileImpl;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class FeatureIntensityNormalizationModuleTest {

  @Test
  void createReferenceFunctionsUsesMedianFeatureIntensity() {
    final FeatureIntensityNormalizationModule module = new FeatureIntensityNormalizationModule();
    final RawDataFileImpl fileA = createRawFile("file_a", LocalDateTime.of(2026, 1, 1, 10, 0));
    final RawDataFileImpl fileB = createRawFile("file_b", LocalDateTime.of(2026, 1, 1, 10, 5));

    final ModularFeatureList featureList = new ModularFeatureList("flist", null, fileA, fileB);
    addRow(featureList, 1, fileA, 1f, fileB, 1f);
    addRow(featureList, 2, fileA, 2f, fileB, 1f);
    addRow(featureList, 3, fileA, 3f, fileB, 1f);
    addRow(featureList, 4, fileA, 100f, fileB, 1f);

    final IntensityNormalizationSearchableSummary summary = new IntensityNormalizationSearchableSummary(
        featureList.getNumberOfRawDataFiles());
    final Map<RawDataFile, NormalizationFunction> functions = module.createReferenceFunctions(
        summary,
        List.of(fileA, fileB), featureList, new SamplesBatch(featureList.getRawDataFiles(), null), new MetadataTable(false),
        createMainParameters(AbundanceMeasure.Height), createFeatureIntensityParameters(
            FeatureIntensityNormalizationMode.MEDIAN));

    final FactorNormalizationFunction functionA = assertInstanceOf(
        FactorNormalizationFunction.class, functions.get(fileA));
    final FactorNormalizationFunction functionB = assertInstanceOf(
        FactorNormalizationFunction.class, functions.get(fileB));

    // Median(file_a)=2.5 and Median(file_b)=1.0 => median=3.5/2
    assertEquals(3.5/2d/2.5, functionA.getNormalizationFactor(0d, 0f), 1e-12);
    assertEquals(3.5/2d/1d, functionB.getNormalizationFactor(0d, 0f), 1e-12);

    final RawFileNormalizationFunction rawFileFuncA = summary.getRawFileFunction(fileA);
    final RawFileNormalizationFunction rawFileFuncB = summary.getRawFileFunction(fileB);
    assertNotNull(rawFileFuncA);
    assertNotNull(rawFileFuncB);
    assertEquals(fileA.getStartTimeStamp(), rawFileFuncA.acquisitionTimestamp());
    assertEquals(fileB.getStartTimeStamp(), rawFileFuncB.acquisitionTimestamp());
  }

  @Test
  void createReferenceFunctionsThrowsIfNoFeaturesFound() {
    final FeatureIntensityNormalizationModule module = new FeatureIntensityNormalizationModule();
    final RawDataFileImpl file = createRawFile("empty_file", LocalDateTime.of(2026, 1, 1, 10, 0));
    final ModularFeatureList featureList = new ModularFeatureList("flist", null, file);

    final IntensityNormalizationSearchableSummary summary = new IntensityNormalizationSearchableSummary(
        featureList.getNumberOfRawDataFiles());
    final IllegalStateException exception = assertThrows(IllegalStateException.class,
        () -> module.createReferenceFunctions(summary,
            List.of(file), featureList, new SamplesBatch(featureList.getRawDataFiles(), null), new MetadataTable(false),
            createMainParameters(AbundanceMeasure.Height), createFeatureIntensityParameters(
                FeatureIntensityNormalizationMode.MEDIAN)));

    assertEquals("No feature abundances found for file empty_file in feature list flist.",
        exception.getMessage());
  }

  @Test
  void createReferenceFunctionsThrowsIfAllAbundancesAreZero() {
    final FeatureIntensityNormalizationModule module = new FeatureIntensityNormalizationModule();
    final RawDataFileImpl file = createRawFile("zero_file", LocalDateTime.of(2026, 1, 1, 10, 0));
    final ModularFeatureList featureList = new ModularFeatureList("flist", null, file);
    addRow(featureList, 1, file, 0f, null, null);
    addRow(featureList, 2, file, 0f, null, null);

    final IntensityNormalizationSearchableSummary summary = new IntensityNormalizationSearchableSummary(
        featureList.getNumberOfRawDataFiles());
    final IllegalStateException exception = assertThrows(IllegalStateException.class,
        () -> module.createReferenceFunctions(summary,
            List.of(file), featureList, new SamplesBatch(featureList.getRawDataFiles(), null), new MetadataTable(false),
            createMainParameters(AbundanceMeasure.Height), createFeatureIntensityParameters(
                FeatureIntensityNormalizationMode.MEDIAN)));

    assertEquals("No features found or Median (default) of feature intensities is 0 for file: zero_file",
        exception.getMessage());
  }

  @Test
  void createReferenceFunctionsUsesAreaMeasure() {
    final FeatureIntensityNormalizationModule module = new FeatureIntensityNormalizationModule();
    final RawDataFileImpl fileA = createRawFile("file_a", LocalDateTime.of(2026, 1, 1, 10, 0));
    final RawDataFileImpl fileB = createRawFile("file_b", LocalDateTime.of(2026, 1, 1, 10, 5));

    final ModularFeatureList featureList = new ModularFeatureList("flist", null, fileA, fileB);
    // Heights are identical for both files; only areas differ.
    addRow(featureList, 1, fileA, 1f, 2f, fileB, 1f, 1f);
    addRow(featureList, 2, fileA, 1f, 4f, fileB, 1f, 1f);

    final IntensityNormalizationSearchableSummary summary = new IntensityNormalizationSearchableSummary(
        featureList.getNumberOfRawDataFiles());
    final Map<RawDataFile, NormalizationFunction> functions = module.createReferenceFunctions(
        summary,
        List.of(fileA, fileB), featureList, new SamplesBatch(featureList.getRawDataFiles(), null), new MetadataTable(false),
        createMainParameters(AbundanceMeasure.Area), createFeatureIntensityParameters(
            FeatureIntensityNormalizationMode.MEDIAN));

    final FactorNormalizationFunction functionA = assertInstanceOf(
        FactorNormalizationFunction.class, functions.get(fileA));
    final FactorNormalizationFunction functionB = assertInstanceOf(
        FactorNormalizationFunction.class, functions.get(fileB));

    // Median area(file_a)=3.0 and Median area(file_b)=1.0 => median=2.0.
    assertEquals(2/3d, functionA.getNormalizationFactor(0d, 0f), 1e-12);
    assertEquals(2d, functionB.getNormalizationFactor(0d, 0f), 1e-12);
  }

  @Test
  void createReferenceFunctionsWorksWithoutRunDate() {
    final FeatureIntensityNormalizationModule module = new FeatureIntensityNormalizationModule();
    final RawDataFileImpl fileA = createRawFile("file_a", null);
    final RawDataFileImpl fileB = createRawFile("file_b", null);

    final ModularFeatureList featureList = new ModularFeatureList("flist", null, fileA, fileB);
    addRow(featureList, 1, fileA, 1f, fileB, 8f);
    addRow(featureList, 2, fileA, 3f, fileB, 8f);

    final IntensityNormalizationSearchableSummary summary = new IntensityNormalizationSearchableSummary(
        featureList.getNumberOfRawDataFiles());
    final Map<RawDataFile, NormalizationFunction> functions = module.createReferenceFunctions(
        summary,
        List.of(fileA, fileB), featureList, new SamplesBatch(featureList.getRawDataFiles(), null), new MetadataTable(false),
        createMainParameters(AbundanceMeasure.Height), createFeatureIntensityParameters(
            FeatureIntensityNormalizationMode.MEDIAN));

    final FactorNormalizationFunction functionA = assertInstanceOf(
        FactorNormalizationFunction.class, functions.get(fileA));
    final FactorNormalizationFunction functionB = assertInstanceOf(
        FactorNormalizationFunction.class, functions.get(fileB));

    final RawFileNormalizationFunction rawFileFuncA = summary.getRawFileFunction(fileA);
    final RawFileNormalizationFunction rawFileFuncB = summary.getRawFileFunction(fileB);
    assertNotNull(rawFileFuncA);
    assertNotNull(rawFileFuncB);
    assertNull(rawFileFuncA.acquisitionTimestamp());
    assertNull(rawFileFuncB.acquisitionTimestamp());
    // median a=2 median b=8 total median is 5
    assertEquals(5d/2d, functionA.getNormalizationFactor(0d, 0f), 1e-12);
    assertEquals(5d/8d, functionB.getNormalizationFactor(0d, 0f), 1e-12);
  }
}
