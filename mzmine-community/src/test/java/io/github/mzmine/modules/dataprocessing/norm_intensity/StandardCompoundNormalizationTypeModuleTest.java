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

import static io.github.mzmine.modules.dataprocessing.norm_intensity.NormIntensityTestUtils.addRow;
import static io.github.mzmine.modules.dataprocessing.norm_intensity.NormIntensityTestUtils.createMainParameters;
import static io.github.mzmine.modules.dataprocessing.norm_intensity.NormIntensityTestUtils.createRawFile;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mzmine.datamodel.AbundanceMeasure;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.numbers.MobilityType;
import io.github.mzmine.datamodel.features.types.numbers.RTType;
import io.github.mzmine.modules.visualization.projectmetadata.SampleType;
import io.github.mzmine.modules.visualization.projectmetadata.table.MetadataTable;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.mobilitytolerance.MobilityTolerance;
import io.github.mzmine.project.impl.RawDataFileImpl;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.parsers.DocumentBuilderFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

class StandardCompoundNormalizationTypeModuleTest {

  @TempDir
  Path tempDir;
  private int standardsFileIndex = 0;

  @Test
  void createReferenceFunctionsThrowsIfNoStandardsSelected() throws IOException {
    final StandardCompoundNormalizationTypeModule module = new StandardCompoundNormalizationTypeModule();
    final RawDataFileImpl file = createRawFile("file_a", LocalDateTime.of(2026, 1, 1, 10, 0));
    final ModularFeatureList featureList = new ModularFeatureList("flist", null, file);

    final StandardCompoundNormalizationTypeParameters moduleParameters = createModuleParametersFromCsv(
        StandardUsageType.Nearest, StandardCompoundNormalizationMode.REQUIRE_ALL_IN_ALL_SAMPLES,
        "mz,rt,name\n500,50,missing_standard\n");

    final IntensityNormalizationSearchableSummary summary = new IntensityNormalizationSearchableSummary(
        featureList.getNumberOfRawDataFiles());
    final IllegalStateException exception = assertThrows(IllegalStateException.class,
        () -> module.createReferenceFunctions(summary, List.of(file), featureList,
            new SamplesBatch(featureList.getRawDataFiles(), null), new MetadataTable(false),
            createMainParameters(AbundanceMeasure.Height), moduleParameters));

    assertEquals("No internal standard compounds matched the feature list.",
        exception.getMessage());
  }

  @Test
  void createReferenceFunctionsThrowsIfRequiredStandardIsMissing() throws IOException {
    final StandardCompoundNormalizationTypeModule module = new StandardCompoundNormalizationTypeModule();
    final RawDataFileImpl fileA = createRawFile("file_a", LocalDateTime.of(2026, 1, 1, 10, 0));
    final RawDataFileImpl fileB = createRawFile("file_b", LocalDateTime.of(2026, 1, 1, 10, 5));

    final ModularFeatureList featureList = new ModularFeatureList("flist", null, fileA, fileB);
    final ModularFeatureListRow standardRow = addRow(featureList, 1, fileA, 200f, fileB, null);

    final StandardCompoundNormalizationTypeParameters moduleParameters = createModuleParameters(
        StandardUsageType.Nearest, StandardCompoundNormalizationMode.REQUIRE_ALL_IN_ALL_SAMPLES,
        standardRow);

    final IntensityNormalizationSearchableSummary summary = new IntensityNormalizationSearchableSummary(
        featureList.getNumberOfRawDataFiles());
    final RuntimeException exception = assertThrows(RuntimeException.class,
        () -> module.createReferenceFunctions(summary, List.of(fileA, fileB), featureList,
            new SamplesBatch(featureList.getRawDataFiles(), null), new MetadataTable(false),
            createMainParameters(AbundanceMeasure.Height), moduleParameters));

    assertTrue(exception.getMessage().contains("was not detected in file file_b"));
  }

  @Test
  void createReferenceFunctionsThrowsIfAllStandardsAreInvalidAndOptional() throws IOException {
    final StandardCompoundNormalizationTypeModule module = new StandardCompoundNormalizationTypeModule();
    final RawDataFileImpl fileA = createRawFile("file_a", LocalDateTime.of(2026, 1, 1, 10, 0));

    final ModularFeatureList featureList = new ModularFeatureList("flist", null, fileA);
    final ModularFeatureListRow standardRow = addRow(featureList, 1, fileA, 0f, null, null);

    final StandardCompoundNormalizationTypeParameters moduleParameters = createModuleParameters(
        StandardUsageType.Weighted, StandardCompoundNormalizationMode.REQUIRE_ONE_PER_SAMPLE,
        standardRow);

    final IntensityNormalizationSearchableSummary summary = new IntensityNormalizationSearchableSummary(
        featureList.getNumberOfRawDataFiles());
    final IllegalStateException exception = assertThrows(IllegalStateException.class,
        () -> module.createReferenceFunctions(summary, List.of(fileA), featureList,
            new SamplesBatch(featureList.getRawDataFiles(), null), new MetadataTable(false),
            createMainParameters(AbundanceMeasure.Height), moduleParameters));

    assertEquals("No intensity normalization standards found for file: file_a",
        exception.getMessage());
  }

  @Test
  void createReferenceFunctionsThrowsIfStandardAbundanceInvalidAndRequired() throws IOException {
    final StandardCompoundNormalizationTypeModule module = new StandardCompoundNormalizationTypeModule();
    final RawDataFileImpl fileA = createRawFile("file_a", LocalDateTime.of(2026, 1, 1, 10, 0));

    final ModularFeatureList featureList = new ModularFeatureList("flist", null, fileA);
    final ModularFeatureListRow standardRow = addRow(featureList, 1, fileA, 0f, null, null);

    final StandardCompoundNormalizationTypeParameters moduleParameters = createModuleParameters(
        StandardUsageType.Nearest, StandardCompoundNormalizationMode.REQUIRE_ALL_IN_ALL_SAMPLES,
        standardRow);

    final IntensityNormalizationSearchableSummary summary = new IntensityNormalizationSearchableSummary(
        featureList.getNumberOfRawDataFiles());
    final IllegalStateException exception = assertThrows(IllegalStateException.class,
        () -> module.createReferenceFunctions(summary, List.of(fileA), featureList,
            new SamplesBatch(featureList.getRawDataFiles(), null), new MetadataTable(false),
            createMainParameters(AbundanceMeasure.Height), moduleParameters));

    assertTrue(exception.getMessage().contains("Invalid standard abundance found for row"));
  }

  @Test
  void createReferenceFunctionsBuildsFunctionsForValidStandards() throws IOException {
    final StandardCompoundNormalizationTypeModule module = new StandardCompoundNormalizationTypeModule();
    final RawDataFileImpl fileA = createRawFile("file_a", LocalDateTime.of(2026, 1, 1, 10, 0));
    final RawDataFileImpl fileB = createRawFile("file_b", LocalDateTime.of(2026, 1, 1, 10, 5));

    final ModularFeatureList featureList = new ModularFeatureList("flist", null, fileA, fileB);
    final ModularFeatureListRow standardRow = addRow(featureList, 1, fileA, 200f, fileB, 100f);

    final StandardCompoundNormalizationTypeParameters moduleParameters = createModuleParameters(
        StandardUsageType.Nearest, StandardCompoundNormalizationMode.REQUIRE_ALL_IN_ALL_SAMPLES,
        standardRow);

    final IntensityNormalizationSearchableSummary summary = new IntensityNormalizationSearchableSummary(
        featureList.getNumberOfRawDataFiles());
    final Map<RawDataFile, NormalizationFunction> functions = module.createReferenceFunctions(
        summary, List.of(fileA, fileB), featureList,
        new SamplesBatch(featureList.getRawDataFiles(), null), new MetadataTable(false),
        createMainParameters(AbundanceMeasure.Height), moduleParameters);

    final StandardCompoundNormalizationFunction functionA = assertInstanceOf(
        StandardCompoundNormalizationFunction.class, functions.get(fileA));
    final StandardCompoundNormalizationFunction functionB = assertInstanceOf(
        StandardCompoundNormalizationFunction.class, functions.get(fileB));

    assertEquals(0.005d, functionA.getNormalizationFactor(100d, 5f), 1e-12);
    assertEquals(0.01d, functionB.getNormalizationFactor(100d, 5f), 1e-12);
  }

  private @NotNull StandardCompoundNormalizationTypeParameters createModuleParameters(
      final @NotNull StandardUsageType usageType,
      final @NotNull StandardCompoundNormalizationMode mode,
      final @NotNull ModularFeatureListRow... standardRows) throws IOException {
    return createModuleParametersFromCsv(usageType, mode, createStandardsCsv(standardRows));
  }

  @Test
  void createReferenceFunctionsSkipsInvalidStandardsWhenNotRequired() throws IOException {
    final StandardCompoundNormalizationTypeModule module = new StandardCompoundNormalizationTypeModule();
    final RawDataFileImpl fileA = createRawFile("file_a", LocalDateTime.of(2026, 1, 1, 10, 0));

    final ModularFeatureList featureList = new ModularFeatureList("flist", null, fileA);
    // row1 has valid abundance; row2 has zero abundance and should be skipped.
    final ModularFeatureListRow standardRow1 = addRow(featureList, 1, fileA, 200f, null, null);
    final ModularFeatureListRow standardRow2 = addRow(featureList, 2, fileA, 0f, null, null,
        101d, 5f, null);

    final StandardCompoundNormalizationTypeParameters moduleParameters = createModuleParameters(
        StandardUsageType.Nearest, StandardCompoundNormalizationMode.REQUIRE_ONE_PER_SAMPLE,
        standardRow1, standardRow2);

    final IntensityNormalizationSearchableSummary summary = new IntensityNormalizationSearchableSummary(
        featureList.getNumberOfRawDataFiles());
    final Map<RawDataFile, NormalizationFunction> functions = module.createReferenceFunctions(
        summary, List.of(fileA), featureList, new SamplesBatch(featureList.getRawDataFiles(), null),
        new MetadataTable(false), createMainParameters(AbundanceMeasure.Height), moduleParameters);

    final StandardCompoundNormalizationFunction functionA = assertInstanceOf(
        StandardCompoundNormalizationFunction.class, functions.get(fileA));
    // Only row1 reference point remains; row2 (zero abundance) was skipped.
    assertEquals(1, functionA.referencePoints().size());
    assertEquals(1d / 200d, functionA.getNormalizationFactor(100d, 5f), 1e-12);
  }

  @Test
  void createReferenceFunctionsUsesOnlyBestMatchForEachStandard() throws IOException {
    final StandardCompoundNormalizationTypeModule module = new StandardCompoundNormalizationTypeModule();
    final RawDataFileImpl fileA = createRawFile("file_a", LocalDateTime.of(2026, 1, 1, 10, 0));

    final ModularFeatureList featureList = new ModularFeatureList("flist", null, fileA);
    final ModularFeatureListRow weakerMatch = addRow(featureList, 1, fileA, 200f, null, null,
        100.08d, 5.08f, null);
    final ModularFeatureListRow bestMatch = addRow(featureList, 2, fileA, 400f, null, null,
        100.01d, 5.01f, null);

    final StandardCompoundNormalizationTypeParameters moduleParameters = createModuleParametersFromCsv(
        StandardUsageType.Nearest, StandardCompoundNormalizationMode.REQUIRE_ALL_IN_ALL_SAMPLES,
        "mz,rt,name\n100,5,best_only\n");

    final IntensityNormalizationSearchableSummary summary = new IntensityNormalizationSearchableSummary(
        featureList.getNumberOfRawDataFiles());
    final Map<RawDataFile, NormalizationFunction> functions = module.createReferenceFunctions(
        summary, List.of(fileA), featureList,
        new SamplesBatch(featureList.getRawDataFiles(), null), new MetadataTable(false),
        createMainParameters(AbundanceMeasure.Height), moduleParameters);

    final StandardCompoundNormalizationFunction functionA = assertInstanceOf(
        StandardCompoundNormalizationFunction.class, functions.get(fileA));

    assertEquals(1, functionA.referencePoints().size());
    assertEquals(1d / 400d, functionA.getNormalizationFactor(100d, 5f), 1e-12);
    assertTrue(weakerMatch.getCompoundAnnotations().isEmpty());
    assertEquals("best_only", bestMatch.getCompoundAnnotations().getFirst().getCompoundName());
  }

  @Test
  void createReferenceFunctionsPrefersHighestDetectionRateOverSmallestDeviation()
      throws IOException {
    final StandardCompoundNormalizationTypeModule module = new StandardCompoundNormalizationTypeModule();
    final RawDataFileImpl fileA = createRawFile("file_a", LocalDateTime.of(2026, 1, 1, 10, 0));
    final RawDataFileImpl fileB = createRawFile("file_b", LocalDateTime.of(2026, 1, 1, 10, 5));

    final ModularFeatureList featureList = new ModularFeatureList("flist", null, fileA, fileB);
    // closest to the user defined m/z and RT and much more intense, but only detected in one file
    final ModularFeatureListRow closestButRarelyDetected = addRow(featureList, 1, fileA, 1000f,
        fileB, null, 100.01d, 5.01f, null);
    // larger deviation but detected in all reference files
    final ModularFeatureListRow fullyDetected = addRow(featureList, 2, fileA, 100f, fileB, 100f,
        100.08d, 5.08f, null);

    final StandardCompoundNormalizationTypeParameters moduleParameters = createModuleParametersFromCsv(
        StandardUsageType.Nearest, StandardCompoundNormalizationMode.REQUIRE_ALL_IN_ALL_SAMPLES,
        "mz,rt,name\n100,5,fully_detected\n");

    final IntensityNormalizationSearchableSummary summary = new IntensityNormalizationSearchableSummary(
        featureList.getNumberOfRawDataFiles());
    final Map<RawDataFile, NormalizationFunction> functions = module.createReferenceFunctions(
        summary, List.of(fileA, fileB), featureList,
        new SamplesBatch(featureList.getRawDataFiles(), null), new MetadataTable(false),
        createMainParameters(AbundanceMeasure.Height), moduleParameters);

    final StandardCompoundNormalizationFunction functionA = assertInstanceOf(
        StandardCompoundNormalizationFunction.class, functions.get(fileA));
    assertEquals(1d / 100d, functionA.getNormalizationFactor(100d, 5f), 1e-12);
    assertTrue(closestButRarelyDetected.getCompoundAnnotations().isEmpty());
    assertEquals("fully_detected",
        fullyDetected.getCompoundAnnotations().getFirst().getCompoundName());
  }

  @Test
  void createReferenceFunctionsBreaksDetectionRateTieByHighestIntensity() throws IOException {
    final StandardCompoundNormalizationTypeModule module = new StandardCompoundNormalizationTypeModule();
    final RawDataFileImpl fileA = createRawFile("file_a", LocalDateTime.of(2026, 1, 1, 10, 0));
    final RawDataFileImpl fileB = createRawFile("file_b", LocalDateTime.of(2026, 1, 1, 10, 5));

    final ModularFeatureList featureList = new ModularFeatureList("flist", null, fileA, fileB);
    // both rows are detected in both reference files, so the summed intensity decides
    final ModularFeatureListRow closestButWeaker = addRow(featureList, 1, fileA, 100f, fileB, 100f,
        100.01d, 5.01f, null);
    final ModularFeatureListRow moreIntense = addRow(featureList, 2, fileA, 500f, fileB, 500f,
        100.08d, 5.08f, null);

    final StandardCompoundNormalizationTypeParameters moduleParameters = createModuleParametersFromCsv(
        StandardUsageType.Nearest, StandardCompoundNormalizationMode.REQUIRE_ALL_IN_ALL_SAMPLES,
        "mz,rt,name\n100,5,most_intense\n");

    final IntensityNormalizationSearchableSummary summary = new IntensityNormalizationSearchableSummary(
        featureList.getNumberOfRawDataFiles());
    final Map<RawDataFile, NormalizationFunction> functions = module.createReferenceFunctions(
        summary, List.of(fileA, fileB), featureList,
        new SamplesBatch(featureList.getRawDataFiles(), null), new MetadataTable(false),
        createMainParameters(AbundanceMeasure.Height), moduleParameters);

    final StandardCompoundNormalizationFunction functionA = assertInstanceOf(
        StandardCompoundNormalizationFunction.class, functions.get(fileA));
    assertEquals(1d / 500d, functionA.getNormalizationFactor(100d, 5f), 1e-12);
    assertTrue(closestButWeaker.getCompoundAnnotations().isEmpty());
    assertEquals("most_intense", moreIntense.getCompoundAnnotations().getFirst().getCompoundName());
  }

  @Test
  void createReferenceFunctionsSkipsFilesWithoutStandard() throws IOException {
    final StandardCompoundNormalizationTypeModule module = new StandardCompoundNormalizationTypeModule();
    final RawDataFileImpl fileA = createRawFile("file_a", LocalDateTime.of(2026, 1, 1, 10, 0));
    final RawDataFileImpl fileB = createRawFile("file_b", LocalDateTime.of(2026, 1, 1, 10, 5));
    final RawDataFileImpl fileC = createRawFile("file_c", LocalDateTime.of(2026, 1, 1, 10, 10));

    final ModularFeatureList featureList = new ModularFeatureList("flist", null,
        List.of(fileA, fileB, fileC));
    // standard is missing in file_b
    final ModularFeatureListRow standardRow = addRow(featureList, 1, fileA, 200f, fileC, 400f);

    final StandardCompoundNormalizationTypeParameters moduleParameters = createModuleParameters(
        StandardUsageType.Nearest, StandardCompoundNormalizationMode.SKIP_FILES_WITHOUT_STANDARD,
        standardRow);

    final IntensityNormalizationSearchableSummary summary = new IntensityNormalizationSearchableSummary(
        featureList.getNumberOfRawDataFiles());
    final Map<RawDataFile, NormalizationFunction> functions = module.createReferenceFunctions(
        summary, List.of(fileA, fileB, fileC), featureList,
        new SamplesBatch(featureList.getRawDataFiles(), null), new MetadataTable(false),
        createMainParameters(AbundanceMeasure.Height), moduleParameters);

    assertEquals(Set.of(fileA, fileC), functions.keySet());
    assertNull(summary.get(fileB));
  }

  @Test
  void createReferenceFunctionsThrowsIfNoFileHasAStandardInSkipMode() throws IOException {
    final StandardCompoundNormalizationTypeModule module = new StandardCompoundNormalizationTypeModule();
    final RawDataFileImpl fileA = createRawFile("file_a", LocalDateTime.of(2026, 1, 1, 10, 0));
    final RawDataFileImpl fileB = createRawFile("file_b", LocalDateTime.of(2026, 1, 1, 10, 5));

    final ModularFeatureList featureList = new ModularFeatureList("flist", null, fileA, fileB);
    // the row is only detected in file_a, which is not a reference file
    final ModularFeatureListRow standardRow = addRow(featureList, 1, fileA, 200f, fileB, null);

    final StandardCompoundNormalizationTypeParameters moduleParameters = createModuleParameters(
        StandardUsageType.Nearest, StandardCompoundNormalizationMode.SKIP_FILES_WITHOUT_STANDARD,
        standardRow);

    final IntensityNormalizationSearchableSummary summary = new IntensityNormalizationSearchableSummary(
        featureList.getNumberOfRawDataFiles());
    final IllegalStateException exception = assertThrows(IllegalStateException.class,
        () -> module.createReferenceFunctions(summary, List.of(fileB), featureList,
            new SamplesBatch(featureList.getRawDataFiles(), null), new MetadataTable(false),
            createMainParameters(AbundanceMeasure.Height), moduleParameters));

    assertEquals("No internal standard was detected in any of the reference samples.",
        exception.getMessage());
  }

  @Test
  void loadValuesFromXmlMapsLegacyRequireAllStandardsToMode() throws Exception {
    assertEquals(StandardCompoundNormalizationMode.REQUIRE_ALL_IN_ALL_SAMPLES,
        loadModeFromLegacyXml("true"));
    assertEquals(StandardCompoundNormalizationMode.REQUIRE_ONE_PER_SAMPLE,
        loadModeFromLegacyXml("false"));
  }

  @Test
  void loadValuesFromXmlKeepsDefaultModeWithoutLegacyParameter() throws Exception {
    final StandardCompoundNormalizationTypeParameters parameters = (StandardCompoundNormalizationTypeParameters) new StandardCompoundNormalizationTypeParameters().cloneParameterSet();
    parameters.loadValuesFromXML(createParametersElement(null, null));

    assertEquals(StandardCompoundNormalizationMode.getDefault(),
        parameters.getValue(StandardCompoundNormalizationTypeParameters.mode));
  }

  @Test
  void loadValuesFromXmlPrefersNewModeOverLegacyParameter() throws Exception {
    final StandardCompoundNormalizationTypeParameters parameters = (StandardCompoundNormalizationTypeParameters) new StandardCompoundNormalizationTypeParameters().cloneParameterSet();
    parameters.loadValuesFromXML(createParametersElement("true",
        StandardCompoundNormalizationMode.SKIP_FILES_WITHOUT_STANDARD.getUniqueID()));

    assertEquals(StandardCompoundNormalizationMode.SKIP_FILES_WITHOUT_STANDARD,
        parameters.getValue(StandardCompoundNormalizationTypeParameters.mode));
  }

  private static @NotNull StandardCompoundNormalizationMode loadModeFromLegacyXml(
      final @NotNull String legacyValue) throws Exception {
    final StandardCompoundNormalizationTypeParameters parameters = (StandardCompoundNormalizationTypeParameters) new StandardCompoundNormalizationTypeParameters().cloneParameterSet();
    parameters.loadValuesFromXML(createParametersElement(legacyValue, null));
    return parameters.getValue(StandardCompoundNormalizationTypeParameters.mode);
  }

  private static @NotNull Element createParametersElement(final @Nullable String legacyValue,
      final @Nullable String modeValue) throws Exception {
    final Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        .newDocument();
    final Element root = document.createElement("batchstep");
    document.appendChild(root);

    if (legacyValue != null) {
      appendParameter(document, root, "Require all standards", legacyValue);
    }
    if (modeValue != null) {
      appendParameter(document, root, StandardCompoundNormalizationTypeParameters.mode.getName(),
          modeValue);
    }
    return root;
  }

  private static void appendParameter(final @NotNull Document document, final @NotNull Element root,
      final @NotNull String name, final @NotNull String value) {
    final Element element = document.createElement(SimpleParameterSet.parameterElement);
    element.setAttribute(SimpleParameterSet.nameAttribute, name);
    element.setTextContent(value);
    root.appendChild(element);
  }

  @Test
  void checkParameterValuesRequiresMzAndRtTypes() throws IOException {
    final StandardCompoundNormalizationTypeParameters moduleParameters = createModuleParametersFromCsv(
        StandardUsageType.Nearest, StandardCompoundNormalizationMode.REQUIRE_ALL_IN_ALL_SAMPLES,
        "mz,rt,name\n100,5,standard\n");
    moduleParameters.getValue(StandardCompoundNormalizationTypeParameters.standardCompounds)
        .stream().filter(importType -> importType.getDataType() instanceof RTType)
        .forEach(importType -> importType.setSelected(false));

    final List<String> errors = new ArrayList<>();

    assertFalse(moduleParameters.checkParameterValues(errors, false));
    assertTrue(errors.stream().anyMatch(error -> error.contains("RT")));
  }

  @Test
  void createReferenceFunctionsAppliesMobilityToleranceOnlyWhenSelected() throws IOException {
    final StandardCompoundNormalizationTypeModule module = new StandardCompoundNormalizationTypeModule();
    final RawDataFileImpl fileA = createRawFile("file_a", LocalDateTime.of(2026, 1, 1, 10, 0));

    final ModularFeatureList featureList = new ModularFeatureList("flist", null, fileA);
    addRow(featureList, 1, fileA, 200f, null, null, 100d, 5f, 1f);

    final IntensityNormalizationSearchableSummary summary = new IntensityNormalizationSearchableSummary(
        featureList.getNumberOfRawDataFiles());
    final StandardCompoundNormalizationTypeParameters notSelectedParameters = createModuleParametersFromCsv(
        StandardUsageType.Nearest, StandardCompoundNormalizationMode.REQUIRE_ALL_IN_ALL_SAMPLES,
        "mz,rt,mobility,name\n100,5,2,standard\n");

    final Map<RawDataFile, NormalizationFunction> functions = module.createReferenceFunctions(
        summary, List.of(fileA), featureList,
        new SamplesBatch(featureList.getRawDataFiles(), null), new MetadataTable(false),
        createMainParameters(AbundanceMeasure.Height), notSelectedParameters);
    final StandardCompoundNormalizationFunction functionA = assertInstanceOf(
        StandardCompoundNormalizationFunction.class, functions.get(fileA));
    assertEquals(1d / 200d, functionA.getNormalizationFactor(100d, 5f), 1e-12);

    final StandardCompoundNormalizationTypeParameters selectedParameters = createModuleParametersFromCsv(
        StandardUsageType.Nearest, StandardCompoundNormalizationMode.REQUIRE_ALL_IN_ALL_SAMPLES,
        "mz,rt,mobility,name\n100,5,2,standard\n");
    selectedParameters.getValue(StandardCompoundNormalizationTypeParameters.standardCompounds)
        .stream().filter(importType -> importType.getDataType() instanceof MobilityType)
        .forEach(importType -> importType.setSelected(true));

    final IntensityNormalizationSearchableSummary selectedSummary = new IntensityNormalizationSearchableSummary(
        featureList.getNumberOfRawDataFiles());
    final IllegalStateException exception = assertThrows(IllegalStateException.class,
        () -> module.createReferenceFunctions(selectedSummary, List.of(fileA), featureList,
            new SamplesBatch(featureList.getRawDataFiles(), null), new MetadataTable(false),
            createMainParameters(AbundanceMeasure.Height), selectedParameters));

    assertEquals("No internal standard compounds matched the feature list.",
        exception.getMessage());
  }

  @Test
  void createInterpolatedFunctionCreatesInterpolatedNormalizationFunction() throws IOException {
    final StandardCompoundNormalizationTypeModule module = new StandardCompoundNormalizationTypeModule();
    final RawDataFileImpl prevFile = createRawFile("prev", LocalDateTime.of(2026, 1, 1, 10, 0));
    final RawDataFileImpl nextFile = createRawFile("next", LocalDateTime.of(2026, 1, 1, 10, 8));
    final RawDataFileImpl targetFile = createRawFile("target", LocalDateTime.of(2026, 1, 1, 10, 6));

    final ModularFeatureList featureList = new ModularFeatureList("flist", null,
        List.of(prevFile, targetFile, nextFile));

    final StandardCompoundNormalizationFunction prevFunction = new StandardCompoundNormalizationFunction(
        StandardUsageType.Nearest, 1.0d,
        List.of(new StandardCompoundReferencePoint(100d, 5f, 400d)));
    final StandardCompoundNormalizationFunction nextFunction = new StandardCompoundNormalizationFunction(
        StandardUsageType.Nearest, 1.0d,
        List.of(new StandardCompoundReferencePoint(100d, 5f, 200d)));

    final IntensityNormalizationSearchableSummary summary = new IntensityNormalizationSearchableSummary(
        featureList.getNumberOfRawDataFiles());
    summary.addMergeFunction(prevFile, prevFunction);
    summary.addMergeFunction(nextFile, nextFunction);

    // internally we never use summary.functions for interpolation because
    // interpolation should be based on a single steps functions not on the merged composite function
    Map<RawDataFile, NormalizationFunction> functions = Map.of(prevFile, prevFunction, nextFile,
        nextFunction);

    module.interpolateAllFunctionsToSummary(summary, featureList,
        new SamplesBatch(featureList.getRawDataFiles()), new MetadataTable(false),
        functions, createMainParameters(AbundanceMeasure.Height),
        createModuleParametersWithoutStandards(StandardUsageType.Nearest,
            StandardCompoundNormalizationMode.REQUIRE_ONE_PER_SAMPLE));

    var result = summary.get(targetFile);
    assertNotNull(result);
    final InterpolatedNormalizationFunction interpolated = assertInstanceOf(
        InterpolatedNormalizationFunction.class, result);
    assertEquals(0.25d, interpolated.previousWeight(), 1e-12);
    assertEquals(0.75d, interpolated.nextWeight(), 1e-12);
    // factor = prevFactor*previousWeight + nextFactor*nextRunWeight
    //        = 1/400*0.25 + 1/200*0.75 = 0.000625 + 0.00375 = 0.004375
    assertEquals(0.004375d, interpolated.getNormalizationFactor(100d, 5f), 1e-12);
  }

  private @NotNull StandardCompoundNormalizationTypeParameters createModuleParametersWithoutStandards(
      final @NotNull StandardUsageType usageType,
      final @NotNull StandardCompoundNormalizationMode mode) throws IOException {
    return createModuleParametersFromCsv(usageType, mode, "mz,rt,name\n500,50,unused\n");
  }

  private @NotNull StandardCompoundNormalizationTypeParameters createModuleParametersFromCsv(
      final @NotNull StandardUsageType usageType,
      final @NotNull StandardCompoundNormalizationMode mode, final @NotNull String csvContent)
      throws IOException {
    final File standardsFile = writeStandardsFile(csvContent);
    return StandardCompoundNormalizationTypeParameters.create(List.of(SampleType.values()),
        usageType, 1d, standardsFile, ",", new MZTolerance(0.25, 0d),
        new RTTolerance(0.25f, RTTolerance.Unit.MINUTES), new MobilityTolerance(0.25f), mode);
  }

  private @NotNull File writeStandardsFile(final @NotNull String csvContent) throws IOException {
    final Path file = tempDir.resolve("standards-" + standardsFileIndex++ + ".csv");
    Files.writeString(file, csvContent);
    return file.toFile();
  }

  private static @NotNull String createStandardsCsv(
      final @NotNull ModularFeatureListRow... standardRows) {
    final StringBuilder builder = new StringBuilder("mz,rt,name\n");
    for (int i = 0; i < standardRows.length; i++) {
      final ModularFeatureListRow row = standardRows[i];
      builder.append(row.getAverageMZ()).append(',').append(row.getAverageRT()).append(',')
          .append("standard_").append(i + 1).append('\n');
    }
    return builder.toString();
  }
}

