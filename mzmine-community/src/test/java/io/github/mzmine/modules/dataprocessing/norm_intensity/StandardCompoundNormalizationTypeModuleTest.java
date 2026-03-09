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
import static io.github.mzmine.modules.dataprocessing.norm_intensity.NormIntensityTestUtils.createMainParameters;
import static io.github.mzmine.modules.dataprocessing.norm_intensity.NormIntensityTestUtils.createRawFile;
import static io.github.mzmine.modules.dataprocessing.norm_intensity.NormIntensityTestUtils.toFeatureSelections;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mzmine.datamodel.AbundanceMeasure;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.modules.visualization.projectmetadata.SampleType;
import io.github.mzmine.modules.visualization.projectmetadata.table.MetadataTable;
import io.github.mzmine.project.impl.RawDataFileImpl;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

class StandardCompoundNormalizationTypeModuleTest {

  @Test
  void createReferenceFunctionsThrowsIfNoStandardsSelected() {
    final StandardCompoundNormalizationTypeModule module = new StandardCompoundNormalizationTypeModule();
    final RawDataFileImpl file = createRawFile("file_a", LocalDateTime.of(2026, 1, 1, 10, 0));
    final ModularFeatureList featureList = new ModularFeatureList("flist", null, file);

    final StandardCompoundNormalizationTypeParameters moduleParameters = createModuleParametersWithoutStandards(
        StandardUsageType.Nearest, true);

    final IllegalStateException exception = assertThrows(IllegalStateException.class,
        () -> module.createReferenceFunctions(List.of(file), featureList, new MetadataTable(false),
            createMainParameters(AbundanceMeasure.Height), moduleParameters));

    assertEquals("No internal standard features selected.", exception.getMessage());
  }

  @Test
  void createReferenceFunctionsThrowsIfRequiredStandardIsMissing() {
    final StandardCompoundNormalizationTypeModule module = new StandardCompoundNormalizationTypeModule();
    final RawDataFileImpl fileA = createRawFile("file_a", LocalDateTime.of(2026, 1, 1, 10, 0));
    final RawDataFileImpl fileB = createRawFile("file_b", LocalDateTime.of(2026, 1, 1, 10, 5));

    final ModularFeatureList featureList = new ModularFeatureList("flist", null, fileA, fileB);
    final ModularFeatureListRow standardRow = addRow(featureList, 1, fileA, 200f, fileB, null);

    final StandardCompoundNormalizationTypeParameters moduleParameters = createModuleParameters(
        StandardUsageType.Nearest, true, standardRow);

    final RuntimeException exception = assertThrows(RuntimeException.class,
        () -> module.createReferenceFunctions(List.of(fileA, fileB), featureList,
            new MetadataTable(false), createMainParameters(AbundanceMeasure.Height),
            moduleParameters));

    assertTrue(exception.getMessage().contains("was not detected in file file_b"));
  }

  @Test
  void createReferenceFunctionsThrowsIfAllStandardsAreInvalidAndOptional() {
    final StandardCompoundNormalizationTypeModule module = new StandardCompoundNormalizationTypeModule();
    final RawDataFileImpl fileA = createRawFile("file_a", LocalDateTime.of(2026, 1, 1, 10, 0));

    final ModularFeatureList featureList = new ModularFeatureList("flist", null, fileA);
    final ModularFeatureListRow standardRow = addRow(featureList, 1, fileA, 0f, null, null);

    final StandardCompoundNormalizationTypeParameters moduleParameters = createModuleParameters(
        StandardUsageType.Weighted, false, standardRow);

    final IllegalStateException exception = assertThrows(IllegalStateException.class,
        () -> module.createReferenceFunctions(List.of(fileA), featureList, new MetadataTable(false),
            createMainParameters(AbundanceMeasure.Height), moduleParameters));

    assertEquals("No intensity normalization standards found for file: file_a",
        exception.getMessage());
  }

  @Test
  void createReferenceFunctionsThrowsIfStandardAbundanceInvalidAndRequired() {
    final StandardCompoundNormalizationTypeModule module = new StandardCompoundNormalizationTypeModule();
    final RawDataFileImpl fileA = createRawFile("file_a", LocalDateTime.of(2026, 1, 1, 10, 0));

    final ModularFeatureList featureList = new ModularFeatureList("flist", null, fileA);
    final ModularFeatureListRow standardRow = addRow(featureList, 1, fileA, 0f, null, null);

    final StandardCompoundNormalizationTypeParameters moduleParameters = createModuleParameters(
        StandardUsageType.Nearest, true, standardRow);

    final IllegalStateException exception = assertThrows(IllegalStateException.class,
        () -> module.createReferenceFunctions(List.of(fileA), featureList, new MetadataTable(false),
            createMainParameters(AbundanceMeasure.Height), moduleParameters));

    assertTrue(exception.getMessage().contains("Invalid standard abundance found for row"));
  }

  @Test
  void createReferenceFunctionsBuildsFunctionsForValidStandards() {
    final StandardCompoundNormalizationTypeModule module = new StandardCompoundNormalizationTypeModule();
    final RawDataFileImpl fileA = createRawFile("file_a", LocalDateTime.of(2026, 1, 1, 10, 0));
    final RawDataFileImpl fileB = createRawFile("file_b", LocalDateTime.of(2026, 1, 1, 10, 5));

    final ModularFeatureList featureList = new ModularFeatureList("flist", null, fileA, fileB);
    final ModularFeatureListRow standardRow = addRow(featureList, 1, fileA, 200f, fileB, 100f);

    final StandardCompoundNormalizationTypeParameters moduleParameters = createModuleParameters(
        StandardUsageType.Nearest, true, standardRow);

    final Map<RawDataFile, NormalizationFunction> functions = module.createReferenceFunctions(
        List.of(fileA, fileB), featureList, new MetadataTable(false),
        createMainParameters(AbundanceMeasure.Height), moduleParameters);

    final StandardCompoundNormalizationFunction functionA = assertInstanceOf(
        StandardCompoundNormalizationFunction.class, functions.get(fileA));
    final StandardCompoundNormalizationFunction functionB = assertInstanceOf(
        StandardCompoundNormalizationFunction.class, functions.get(fileB));

    assertEquals(0.005d, functionA.getNormalizationFactor(100d, 5f), 1e-12);
    assertEquals(0.01d, functionB.getNormalizationFactor(100d, 5f), 1e-12);
  }

  private static @NotNull StandardCompoundNormalizationTypeParameters createModuleParameters(
      final @NotNull StandardUsageType usageType, final boolean requireAllStandards,
      final @NotNull ModularFeatureListRow... standardRows) {
    return StandardCompoundNormalizationTypeParameters.create(List.of(SampleType.values()),
        usageType, 1d, toFeatureSelections(standardRows), requireAllStandards);
  }

  private static @NotNull StandardCompoundNormalizationTypeParameters createModuleParametersWithoutStandards(
      final @NotNull StandardUsageType usageType, final boolean requireAllStandards) {
    return StandardCompoundNormalizationTypeParameters.create(List.of(SampleType.values()),
        usageType, 1d, List.of(), requireAllStandards);
  }
}

