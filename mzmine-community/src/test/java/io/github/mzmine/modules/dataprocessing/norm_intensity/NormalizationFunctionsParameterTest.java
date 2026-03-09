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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mzmine.datamodel.AbundanceMeasure;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.modules.visualization.projectmetadata.SampleType;
import io.github.mzmine.parameters.ParameterUtils;
import io.github.mzmine.parameters.parametertypes.OriginalFeatureListHandlingParameter.OriginalFeatureListOption;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelection;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelectionType;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilePlaceholder;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

class NormalizationFunctionsParameterTest {

  @TempDir
  Path tempDir;

  @Test
  void saveLoadRoundtripKeepsAllFunctionTypes() {
    final LocalDateTime factorTimestamp = LocalDateTime.of(2026, 1, 1, 10, 0);
    final LocalDateTime standardTimestamp = LocalDateTime.of(2026, 1, 1, 10, 10);
    final LocalDateTime interpolatedTimestamp = LocalDateTime.of(2026, 1, 1, 10, 5);

    final FactorNormalizationFunction factorFunction = new FactorNormalizationFunction(
        new RawDataFilePlaceholder("factor_file", tempDir.resolve("factor.mzML").toString(), 11),
        factorTimestamp, 2d);
    final StandardCompoundNormalizationFunction standardFunction = new StandardCompoundNormalizationFunction(
        new RawDataFilePlaceholder("standard_file", tempDir.resolve("standard.mzML").toString(),
            12), standardTimestamp, StandardUsageType.Nearest, 1d,
        List.of(new StandardCompoundReferencePoint(100d, 5f, 200d)));
    final InterpolatedNormalizationFunction interpolatedFunction = new InterpolatedNormalizationFunction(
        new RawDataFilePlaceholder("target_file", tempDir.resolve("target.mzML").toString(), 13),
        interpolatedTimestamp, factorFunction, 0.25d, standardFunction, 0.75d);

    final NormalizationFunctionsParameter parameter = new NormalizationFunctionsParameter();
    parameter.setValue(List.of(factorFunction, standardFunction, interpolatedFunction));

    final String xml = ParameterUtils.saveParameterToXMLString(parameter);
    final NormalizationFunctionsParameter loadedParameter = new NormalizationFunctionsParameter();
    ParameterUtils.loadParameterFromString(loadedParameter, xml);

    final List<NormalizationFunction> loadedFunctions = loadedParameter.getValue();
    assertEquals(3, loadedFunctions.size());

    final FactorNormalizationFunction loadedFactor = assertInstanceOf(
        FactorNormalizationFunction.class, loadedFunctions.get(0));
    assertEquals("factor_file", loadedFactor.rawDataFilePlaceholder().getName());
    assertEquals(factorTimestamp, loadedFactor.acquisitionTimestamp());
    assertEquals(2d, loadedFactor.getNormalizationFactor(0d, 0f), 1e-12);

    final StandardCompoundNormalizationFunction loadedStandard = assertInstanceOf(
        StandardCompoundNormalizationFunction.class, loadedFunctions.get(1));
    assertEquals("standard_file", loadedStandard.rawDataFilePlaceholder().getName());
    assertEquals(standardTimestamp, loadedStandard.acquisitionTimestamp());
    assertEquals(StandardUsageType.Nearest, loadedStandard.usageType());
    assertEquals(1, loadedStandard.referencePoints().size());
    assertEquals(0.005d, loadedStandard.getNormalizationFactor(100d, 5f), 1e-12);

    final InterpolatedNormalizationFunction loadedInterpolated = assertInstanceOf(
        InterpolatedNormalizationFunction.class, loadedFunctions.get(2));
    assertEquals("target_file", loadedInterpolated.rawDataFilePlaceholder().getName());
    assertEquals(interpolatedTimestamp, loadedInterpolated.acquisitionTimestamp());
    assertEquals(0.25d, loadedInterpolated.previousWeight(), 1e-12);
    assertEquals(0.75d, loadedInterpolated.nextWeight(), 1e-12);
    assertEquals(0.50375d, loadedInterpolated.getNormalizationFactor(100d, 5f), 1e-12);
  }

  @Test
  void loadValueFromXMLSkipsInvalidFunctionEntries() throws Exception {
    final LocalDateTime timestamp = LocalDateTime.of(2026, 1, 1, 10, 0);
    final FactorNormalizationFunction validFunction = new FactorNormalizationFunction(
        new RawDataFilePlaceholder("valid_file", tempDir.resolve("valid.mzML").toString(), 7),
        timestamp, 2d);

    final Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        .newDocument();
    final Element root = document.createElement("normalizationFunctions");
    document.appendChild(root);

    NormalizationFunction.appendFunctionElement(root, validFunction);

    final Element invalidFunction = document.createElement(
        NormalizationFunction.XML_FUNCTION_ELEMENT);
    invalidFunction.setAttribute(NormalizationFunction.XML_FUNCTION_TYPE_ATTR, "unknown_type");
    root.appendChild(invalidFunction);

    final NormalizationFunctionsParameter parameter = new NormalizationFunctionsParameter();
    parameter.loadValueFromXML(root);

    final List<NormalizationFunction> loadedFunctions = parameter.getValue();
    assertEquals(1, loadedFunctions.size());
    final FactorNormalizationFunction loadedValid = assertInstanceOf(
        FactorNormalizationFunction.class, loadedFunctions.getFirst());
    assertEquals("valid_file", loadedValid.rawDataFilePlaceholder().getName());
    assertEquals(2d, loadedValid.getNormalizationFactor(0d, 0f), 1e-12);
  }

  @Test
  void hiddenParameterInNormalizerParameterSetPersistsFunctions() throws Exception {
    final LocalDateTime timestamp = LocalDateTime.of(2026, 1, 2, 10, 0);
    final List<NormalizationFunction> functions = List.of(new FactorNormalizationFunction(
        new RawDataFilePlaceholder("file_a", tempDir.resolve("a.mzML").toString(), 42), timestamp,
        1.25d));
    final IntensityNormalizerParameters parameters = createIntensityParameters("hidden_save",
        AbundanceMeasure.Height, OriginalFeatureListOption.KEEP, functions);

    final String xml = ParameterUtils.saveValuesToXMLString(parameters);
    final IntensityNormalizerParameters loaded = createIntensityParameters("placeholder",
        AbundanceMeasure.Area, OriginalFeatureListOption.REMOVE, List.of());
    ParameterUtils.loadValuesFromXMLString(loaded, xml);

    final List<NormalizationFunction> loadedFunctions = loaded.getValue(
        IntensityNormalizerParameters.normalizationFunctions);
    assertEquals(1, loadedFunctions.size());
    final FactorNormalizationFunction loadedFactor = assertInstanceOf(
        FactorNormalizationFunction.class, loadedFunctions.getFirst());
    assertEquals("file_a", loadedFactor.rawDataFilePlaceholder().getName());
    assertEquals(timestamp, loadedFactor.acquisitionTimestamp());
    assertEquals(1.25d, loadedFactor.getNormalizationFactor(0d, 0f), 1e-12);
  }

  @Test
  void extractionUsesLatestIntensityNormalizerAppliedMethod() {
    final RawDataFile rawDataFile = RawDataFile.createDummyFile();
    final ModularFeatureList featureList = new ModularFeatureList("flist", null, rawDataFile);

    final IntensityNormalizerParameters olderParameters = createIntensityParameters("older",
        AbundanceMeasure.Height, OriginalFeatureListOption.KEEP, List.of(
        new FactorNormalizationFunction(new RawDataFilePlaceholder(rawDataFile),
            LocalDateTime.of(2026, 1, 1, 9, 0), 2d)));
    featureList.addDescriptionOfAppliedTask(
        new SimpleFeatureListAppliedMethod(IntensityNormalizerModule.class, olderParameters,
            Instant.parse("2026-01-01T09:30:00Z")));

    final IntensityNormalizerParameters latestParameters = createIntensityParameters("latest",
        AbundanceMeasure.Height, OriginalFeatureListOption.KEEP, List.of(
        new FactorNormalizationFunction(new RawDataFilePlaceholder(rawDataFile),
            LocalDateTime.of(2026, 1, 1, 11, 0), 3d)));
    featureList.addDescriptionOfAppliedTask(
        new SimpleFeatureListAppliedMethod(IntensityNormalizerModule.class, latestParameters,
            Instant.parse("2026-01-01T11:30:00Z")));

    final List<NormalizationFunction> latestFunctions = IntensityNormalizerModule.getNormalizationFunctionsOfLatestCall(
        featureList);
    assertEquals(1, latestFunctions.size());
    assertEquals(3d, latestFunctions.getFirst().getNormalizationFactor(100d, 5f), 1e-12);

    final NormalizationFunction latestForFile = IntensityNormalizerModule.getNormalizationFunctionOfLatestCallForFile(
        featureList, rawDataFile);
    assertNotNull(latestForFile);
    assertEquals(3d, latestForFile.getNormalizationFactor(100d, 5f), 1e-12);

    final NormalizationFunction missingFile = IntensityNormalizerModule.getNormalizationFunctionOfLatestCallForFile(
        featureList,
        new RawDataFilePlaceholder("unknown", tempDir.resolve("unknown.mzML").toString(), 999));
    assertNull(missingFile);
  }

  @Test
  void valueEqualsIsTrueForEquivalentRecordValues() {
    final NormalizationFunctionsParameter firstParameter = new NormalizationFunctionsParameter();
    firstParameter.setValue(createFunctions(2d));

    final NormalizationFunctionsParameter secondParameter = new NormalizationFunctionsParameter();
    secondParameter.setValue(createFunctions(2d));

    assertTrue(firstParameter.valueEquals(secondParameter));
    assertTrue(secondParameter.valueEquals(firstParameter));
  }

  @Test
  void valueEqualsIsFalseForDifferentRecordValues() {
    final NormalizationFunctionsParameter firstParameter = new NormalizationFunctionsParameter();
    firstParameter.setValue(createFunctions(2d));

    final NormalizationFunctionsParameter secondParameter = new NormalizationFunctionsParameter();
    secondParameter.setValue(createFunctions(3d));

    assertFalse(firstParameter.valueEquals(secondParameter));
    assertFalse(secondParameter.valueEquals(firstParameter));
  }

  private @NotNull List<NormalizationFunction> createFunctions(final double factorValue) {
    final FactorNormalizationFunction factorFunction = new FactorNormalizationFunction(
        new RawDataFilePlaceholder("factor_file", tempDir.resolve("factor.mzML").toString(), 11),
        LocalDateTime.of(2026, 1, 1, 10, 0), factorValue);
    final StandardCompoundNormalizationFunction standardFunction = new StandardCompoundNormalizationFunction(
        new RawDataFilePlaceholder("standard_file", tempDir.resolve("standard.mzML").toString(),
            12), LocalDateTime.of(2026, 1, 1, 10, 10), StandardUsageType.Nearest, 1d,
        List.of(new StandardCompoundReferencePoint(100d, 5f, 200d)));
    final InterpolatedNormalizationFunction interpolatedFunction = new InterpolatedNormalizationFunction(
        new RawDataFilePlaceholder("target_file", tempDir.resolve("target.mzML").toString(), 13),
        LocalDateTime.of(2026, 1, 1, 10, 5), factorFunction, 0.25d, standardFunction, 0.75d);
    return List.of(factorFunction, standardFunction, interpolatedFunction);
  }

  private @NotNull IntensityNormalizerParameters createIntensityParameters(
      final @NotNull String suffix, final @NotNull AbundanceMeasure abundanceMeasure,
      final @NotNull OriginalFeatureListOption handleOriginal,
      final @NotNull List<NormalizationFunction> normalizationFunctions) {
    return IntensityNormalizerParameters.create(
        new FeatureListsSelection(FeatureListsSelectionType.ALL_FEATURELISTS), suffix,
        NormalizationType.TotalRawSignal,
        FactorNormalizationModuleParameters.create(List.of(SampleType.QC)), abundanceMeasure,
        handleOriginal, normalizationFunctions);
  }
}
