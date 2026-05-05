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
import io.github.mzmine.util.XMLUtils;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

class NormalizationFunctionsParameterTest {

  final LocalDateTime factorTimestamp = LocalDateTime.of(2026, 1, 1, 10, 0);
  final LocalDateTime standardTimestamp = LocalDateTime.of(2026, 1, 1, 10, 10);
  final LocalDateTime interpolatedTimestamp = LocalDateTime.of(2026, 1, 1, 10, 5);
  final LocalDateTime compositeTimestamp = LocalDateTime.of(2026, 1, 1, 10, 15);
  final RawDataFilePlaceholder factorFile = createRawFile("factor_file");
  final RawDataFilePlaceholder standardFile = createRawFile("standard_file");
  final RawDataFilePlaceholder interpolatedFile = createRawFile("target_file");
  final RawDataFilePlaceholder compositeFile = createRawFile("composite_file");

  final double FACTOR = 2d;
  final RawFileNormalizationFunction factorFunction = createFactorFunction(factorFile,
      factorTimestamp, FACTOR);
  final RawFileNormalizationFunction standardFunction = new RawFileNormalizationFunction(
      standardFile, standardTimestamp,
      new StandardCompoundNormalizationFunction(StandardUsageType.Nearest, 1d,
          List.of(new StandardCompoundReferencePoint(100d, 5f, 200d))));
  final RawFileNormalizationFunction interpolatedFunction = new RawFileNormalizationFunction(
      interpolatedFile, interpolatedTimestamp,
      new InterpolatedNormalizationFunction(factorFunction.function(), 0.25d,
          standardFunction.function(), 0.75d));

  final RawFileNormalizationFunction compositeFunction = new RawFileNormalizationFunction(
      compositeFile, compositeTimestamp, CompositeNormalizationFunction.createComposite(
      List.of(factorFunction.function(), standardFunction.function(),
          interpolatedFunction.function())));

  @Test
  void saveLoadRoundtripKeepsAllFunctionTypes() {

    final NormalizationFunctionsParameter parameter = new NormalizationFunctionsParameter();
    final IntensityNormalizationSummary savedSummary = createSummary(factorFunction,
        standardFunction, interpolatedFunction, compositeFunction);
    parameter.setValue(savedSummary);

    final String xml = ParameterUtils.saveParameterToXMLString(parameter);
    final NormalizationFunctionsParameter loadedParameter = new NormalizationFunctionsParameter();
    ParameterUtils.loadParameterFromString(loadedParameter, xml);

    final IntensityNormalizationSummary summary = loadedParameter.getValue();
    final List<RawFileNormalizationFunction> loadedFunctions = summary.functions();
    assertEquals(savedSummary.size(), loadedFunctions.size());

    for (int i = 0; i < summary.functions().size(); i++) {
      final RawFileNormalizationFunction original = savedSummary.getRawFileFunction(i);
      final RawFileNormalizationFunction loaded = summary.getRawFileFunction(i);
      assertEquals(original.rawDataFilePlaceholder().getName(),
          loaded.rawDataFilePlaceholder().getName());
    }

    // placeholder has no acquisition date so need to check manually
    assertEquals(factorTimestamp, summary.getRawFileFunction(0).acquisitionTimestamp());
    assertEquals(standardTimestamp, summary.getRawFileFunction(1).acquisitionTimestamp());
    assertEquals(interpolatedTimestamp, summary.getRawFileFunction(2).acquisitionTimestamp());
    assertEquals(compositeTimestamp, summary.getRawFileFunction(3).acquisitionTimestamp());

    final FactorNormalizationFunction loadedFactor = assertInstanceOf(
        FactorNormalizationFunction.class, summary.get(0));
    assertEquals(2d, loadedFactor.getNormalizationFactor(0d, 0f), 1e-12);

    final StandardCompoundNormalizationFunction loadedStandard = assertInstanceOf(
        StandardCompoundNormalizationFunction.class, summary.get(1));
    assertEquals(StandardUsageType.Nearest, loadedStandard.usageType());
    assertEquals(1, loadedStandard.referencePoints().size());
    assertEquals(0.005d, loadedStandard.getNormalizationFactor(100d, 5f), 1e-12);

    final InterpolatedNormalizationFunction loadedInterpolated = assertInstanceOf(
        InterpolatedNormalizationFunction.class, summary.get(2));
    assertEquals(0.25d, loadedInterpolated.previousWeight(), 1e-12);
    assertEquals(0.75d, loadedInterpolated.nextWeight(), 1e-12);
    assertEquals(0.50375d, loadedInterpolated.getNormalizationFactor(100d, 5f), 1e-12);

    final CompositeNormalizationFunction loadedComposite = assertInstanceOf(
        CompositeNormalizationFunction.class, summary.get(3));
    assertEquals(3, loadedComposite.functions().size());
  }

  @Test
  void loadValueFromXMLResultsInEmptyFunctionsOnExceptionToRead() throws Exception {
    final LocalDateTime timestamp = LocalDateTime.of(2026, 1, 1, 10, 0);
    final RawFileNormalizationFunction validFunction = createFactorFunction(
        createRawFile("valid_file"), timestamp, 2d);

    final Document document = XMLUtils.newDocument();
    final Element root = document.createElement("normalizationFunctions");
    document.appendChild(root);

    NormalizationFunction.appendFunctionElement(root, validFunction.function());

    final Element invalidFunction = document.createElement(
        NormalizationFunction.XML_FUNCTION_ELEMENT);
    invalidFunction.setAttribute(NormalizationFunction.XML_FUNCTION_TYPE_ATTR, "unknown_type");
    root.appendChild(invalidFunction);

    final NormalizationFunctionsParameter parameter = new NormalizationFunctionsParameter();
    parameter.loadValueFromXML(root);

    final IntensityNormalizationSummary summary = parameter.getValue();
    assertEquals(0, summary.size());

//    final List<NormalizationFunction> loadedFunctions = summary.functions();
//    assertEquals(1, loadedFunctions.size());
//    final FactorNormalizationFunction loadedValid = assertInstanceOf(
//        FactorNormalizationFunction.class, loadedFunctions.getFirst());
//    assertEquals("valid_file", loadedValid.rawDataFilePlaceholder().getName());
//    assertEquals(2d, loadedValid.getNormalizationFactor(0d, 0f), 1e-12);
  }

  @Test
  void hiddenParameterInNormalizerParameterSetPersistsFunctions() throws Exception {
    final IntensityNormalizerParameters parameters = createIntensityParameters("hidden_save",
        AbundanceMeasure.Height, OriginalFeatureListOption.KEEP, List.of(factorFunction));

    final String xml = ParameterUtils.saveValuesToXMLString(parameters);
    final IntensityNormalizerParameters loaded = createIntensityParameters("placeholder",
        AbundanceMeasure.Area, OriginalFeatureListOption.REMOVE, List.of());
    ParameterUtils.loadValuesFromXMLString(loaded, xml);

    final IntensityNormalizationSummary summary = loaded.getValue(
        IntensityNormalizerParameters.hiddenNormalizationSummary);
    assertNotNull(summary);
    assertEquals(1, summary.size());
    var loadedFunctions = summary.functions();
    assertEquals(1, loadedFunctions.size());
    final RawFileNormalizationFunction rawFunc = loadedFunctions.getFirst();
    final FactorNormalizationFunction loadedFactor = assertInstanceOf(
        FactorNormalizationFunction.class, rawFunc.function());
    assertEquals(factorFile.getName(), rawFunc.rawDataFilePlaceholder().getName());
    assertEquals(factorTimestamp, rawFunc.acquisitionTimestamp());
    assertEquals(FACTOR, loadedFactor.getNormalizationFactor(0d, 0f), 1e-12);
  }

  @Test
  void extractionUsesLatestIntensityNormalizerAppliedMethod() {
    final RawDataFile rawDataFile = RawDataFile.createDummyFile();
    final ModularFeatureList featureList = new ModularFeatureList("flist", null, rawDataFile);

    final IntensityNormalizerParameters olderParameters = createIntensityParameters("older",
        AbundanceMeasure.Height, OriginalFeatureListOption.KEEP, List.of(
            createFactorFunction(new RawDataFilePlaceholder(rawDataFile),
                LocalDateTime.of(2026, 1, 1, 9, 0), 2d)));
    featureList.addDescriptionOfAppliedTask(
        new SimpleFeatureListAppliedMethod(IntensityNormalizerModule.class, olderParameters,
            Instant.parse("2026-01-01T09:30:00Z")));

    final IntensityNormalizerParameters latestParameters = createIntensityParameters("latest",
        AbundanceMeasure.Height, OriginalFeatureListOption.KEEP, List.of(
            createFactorFunction(new RawDataFilePlaceholder(rawDataFile),
                LocalDateTime.of(2026, 1, 1, 11, 0), 3d)));
    featureList.addDescriptionOfAppliedTask(
        new SimpleFeatureListAppliedMethod(IntensityNormalizerModule.class, latestParameters,
            Instant.parse("2026-01-01T11:30:00Z")));

    final IntensityNormalizationSummary summary = IntensityNormalizerModule.getNormalizationFunctionsOfLatestCall(
        featureList);
    assertNotNull(summary);
    assertEquals(1, summary.size());
    final List<RawFileNormalizationFunction> latestFunctions = summary.functions();
    assertEquals(1, latestFunctions.size());
    assertEquals(3d, latestFunctions.getFirst().function().getNormalizationFactor(100d, 5f), 1e-12);

    final NormalizationFunction latestForFile = IntensityNormalizerModule.getNormalizationFunctionsOfLatestCallForFile(
        featureList, rawDataFile).get();
    assertNotNull(latestForFile);
    assertEquals(3d, latestForFile.getNormalizationFactor(100d, 5f), 1e-12);

    final NormalizationFunction missingFile = IntensityNormalizerModule.getNormalizationFunctionsOfLatestCallForFile(
        featureList, createRawFile("unknown")).orElse(null);
    assertNull(missingFile);
  }

  @Test
  void getNormalizationFunctionsOfLatestCallReturnsEmptyIfNoAppliedMethods() {
    final RawDataFile rawDataFile = RawDataFile.createDummyFile();
    final ModularFeatureList featureList = new ModularFeatureList("flist", null, rawDataFile);

    final IntensityNormalizationSummary functions = IntensityNormalizerModule.getNormalizationFunctionsOfLatestCall(
        featureList);

    assertTrue(functions.isEmpty());
  }

  @Test
  void saveLoadRoundtripPreservesWeightedUsageType() {
    final RawFileNormalizationFunction function = new RawFileNormalizationFunction(
        createRawFile("file"), LocalDateTime.of(2026, 1, 1, 10, 0),
        new StandardCompoundNormalizationFunction(StandardUsageType.Weighted, 1d,
            List.of(new StandardCompoundReferencePoint(100d, 5f, 200d))));

    final NormalizationFunctionsParameter parameter = new NormalizationFunctionsParameter();
    parameter.setValue(createSummary(function));

    final String xml = ParameterUtils.saveParameterToXMLString(parameter);
    final NormalizationFunctionsParameter loaded = new NormalizationFunctionsParameter();
    ParameterUtils.loadParameterFromString(loaded, xml);

    final StandardCompoundNormalizationFunction loadedFunction = assertInstanceOf(
        StandardCompoundNormalizationFunction.class,
        loaded.getValue().functions().getFirst().function());
    assertEquals(StandardUsageType.Weighted, loadedFunction.usageType());
  }

  private static @NotNull IntensityNormalizationSummary createSummary(
      RawFileNormalizationFunction... functions) {
    return createSummary(List.of(functions));
  }

  private static @NotNull IntensityNormalizationSummary createSummary(
      List<RawFileNormalizationFunction> functions) {
    return new IntensityNormalizationSummary(functions);
  }

  @Test
  void saveLoadRoundtripWithEmptyList() {
    final NormalizationFunctionsParameter parameter = new NormalizationFunctionsParameter();
    parameter.setValue(IntensityNormalizationSummary.EMPTY);

    final String xml = ParameterUtils.saveParameterToXMLString(parameter);
    final NormalizationFunctionsParameter loaded = new NormalizationFunctionsParameter();
    ParameterUtils.loadParameterFromString(loaded, xml);

    assertTrue(loaded.getValue().isEmpty());
  }

  @Test
  void saveLoadRoundtripKeepsNullAcquisitionTimestampForNonInterpolatedFunctions() {
    final RawFileNormalizationFunction factorFunction = createFactorFunction(
        createRawFile("factor_file"), null, 2d);
    final RawFileNormalizationFunction standardFunction = new RawFileNormalizationFunction(
        createRawFile("standard_file"), null,
        new StandardCompoundNormalizationFunction(StandardUsageType.Nearest, 1d,
            List.of(new StandardCompoundReferencePoint(100d, 5f, 200d))));

    final NormalizationFunctionsParameter parameter = new NormalizationFunctionsParameter();
    parameter.setValue(createSummary(factorFunction, standardFunction));

    final String xml = ParameterUtils.saveParameterToXMLString(parameter);
    final NormalizationFunctionsParameter loadedParameter = new NormalizationFunctionsParameter();
    ParameterUtils.loadParameterFromString(loadedParameter, xml);

    final IntensityNormalizationSummary summary = loadedParameter.getValue();
    final List<RawFileNormalizationFunction> loadedFunctions = summary.functions();
    assertEquals(2, loadedFunctions.size());

    for (RawFileNormalizationFunction function : loadedFunctions) {
      assertNull(function.acquisitionTimestamp());
    }
  }

  @Test
  void valueEqualsIsTrueForEquivalentRecordValues() {
    final NormalizationFunctionsParameter firstParameter = new NormalizationFunctionsParameter();
    firstParameter.setValue(createSummary(createFunctions(2d)));

    final NormalizationFunctionsParameter secondParameter = new NormalizationFunctionsParameter();
    secondParameter.setValue(createSummary(createFunctions(2d)));

    assertTrue(firstParameter.valueEquals(secondParameter));
    assertTrue(secondParameter.valueEquals(firstParameter));
  }

  @Test
  void valueEqualsIsFalseForDifferentRecordValues() {
    final NormalizationFunctionsParameter firstParameter = new NormalizationFunctionsParameter();
    firstParameter.setValue(createSummary(createFunctions(2d)));

    final NormalizationFunctionsParameter secondParameter = new NormalizationFunctionsParameter();
    secondParameter.setValue(createSummary(createFunctions(3d)));

    assertFalse(firstParameter.valueEquals(secondParameter));
    assertFalse(secondParameter.valueEquals(firstParameter));
  }

  private @NotNull List<RawFileNormalizationFunction> createFunctions(final double factorValue) {
    final RawFileNormalizationFunction factorFunction = createFactorFunction(
        createRawFile("factor_file"), LocalDateTime.of(2026, 1, 1, 10, 0), factorValue);
    final RawFileNormalizationFunction standardFunction = new RawFileNormalizationFunction(
        createRawFile("standard_file"), LocalDateTime.of(2026, 1, 1, 10, 10),
        new StandardCompoundNormalizationFunction(StandardUsageType.Nearest, 1d,
            List.of(new StandardCompoundReferencePoint(100d, 5f, 200d))));
    final RawFileNormalizationFunction interpolatedFunction = new RawFileNormalizationFunction(

        createRawFile("target_file"), LocalDateTime.of(2026, 1, 1, 10, 5),
        new InterpolatedNormalizationFunction(factorFunction.function(), 0.25d,
            standardFunction.function(), 0.75d));
    return List.of(factorFunction, standardFunction, interpolatedFunction);
  }

  static RawFileNormalizationFunction createFactorFunction(@NotNull RawDataFilePlaceholder file,
      LocalDateTime date, double factor) {
    return new RawFileNormalizationFunction(file, date, new FactorNormalizationFunction(factor));
  }

  private @NotNull RawDataFilePlaceholder createRawFile(String name) {
    return new RawDataFilePlaceholder(name, name + ".mzML", 11);
  }

  private @NotNull IntensityNormalizerParameters createIntensityParameters(
      final @NotNull String suffix, final @NotNull AbundanceMeasure abundanceMeasure,
      final @NotNull OriginalFeatureListOption handleOriginal,
      final @NotNull List<RawFileNormalizationFunction> normalizationFunctions) {
    return IntensityNormalizerParameters.create(
        new FeatureListsSelection(FeatureListsSelectionType.ALL_FEATURELISTS), suffix, null,
        NormalizationType.NoNormalization, null, NormalizationType.TotalRawSignal,
        TotalRawSignalNormalizationTypeParameters.create(List.of(SampleType.QC)), null,
        abundanceMeasure, handleOriginal, createSummary(normalizationFunctions));
  }
}
