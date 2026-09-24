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

package io.github.mzmine.parameters.parametertypes.other_detectors;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.types.otherdectectors.OtherFeatureDataType;
import io.github.mzmine.datamodel.otherdetectors.OtherDataFileImpl;
import io.github.mzmine.datamodel.otherdetectors.OtherFeature;
import io.github.mzmine.datamodel.otherdetectors.OtherFeatureImpl;
import io.github.mzmine.datamodel.otherdetectors.OtherTimeSeries;
import io.github.mzmine.datamodel.otherdetectors.OtherTimeSeriesDataImpl;
import io.github.mzmine.datamodel.otherdetectors.SimpleOtherTimeSeries;
import io.github.mzmine.modules.io.import_rawdata_mzml.msdk.data.ChromatogramType;
import io.github.mzmine.modules.io.projectload.version_3_0.CONST;
import io.github.mzmine.project.impl.RawDataFileImpl;
import io.github.mzmine.util.TextUtils;
import io.github.mzmine.util.XMLUtils;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Tests the trace filtering of {@link OtherTraceSelection} and that a selection filters exactly the
 * same traces after it was saved to and loaded from xml (batch files and projects).
 */
class OtherTraceSelectionTest {

  private static final String UV_DESCRIPTION = "DAD1 A, Sig=254.4 Ref=off";
  private static final String FLD_DESCRIPTION = "FLD1 A, Ex=230, Em=340";
  // contains characters that were stripped by the legacy (version < 2) pattern cleaning
  private static final String SPECIAL_DESCRIPTION = "D:\\detector\\sig^1$ (x)";

  private static final float[] RTS = {0f, 1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f};
  private static final double[] INTENSITIES = {0d, .1d, .2d, .3d, .4d, .5d, .4d, .3d, .2d, .1d};
  // second ms file with a single uv detector, only used by the multi file tests
  private static final String SECOND_UV_DESCRIPTION = "DAD2 B, Sig=230.4 Ref=off";
  private RawDataFileImpl msFile;
  private List<RawDataFile> msFiles;
  private OtherTimeSeriesDataImpl uvData;
  private OtherFeature uv254;
  private OtherFeature uv280;
  private OtherFeature uv254Preprocessed;
  private OtherFeature uv280Preprocessed;
  private OtherFeature uv254Feature;
  private OtherFeature uv280Feature;
  private OtherTimeSeriesDataImpl fldData;
  private OtherFeature fld340;
  private OtherTimeSeriesDataImpl specialData;
  private OtherFeature specialTrace;
  private RawDataFileImpl secondMsFile;
  private OtherTimeSeriesDataImpl secondUvData;
  private OtherFeature secondUv230;

  private static @NotNull OtherTimeSeriesDataImpl createTimeSeriesData(
      @NotNull OtherDataFileImpl file, @NotNull ChromatogramType type, @NotNull String rangeLabel,
      @NotNull String rangeUnit) {
    final OtherTimeSeriesDataImpl data = new OtherTimeSeriesDataImpl(file);
    data.setChromatogramType(type);
    data.setTimeSeriesRangeLabel(rangeLabel);
    data.setTimeSeriesRangeUnit(rangeUnit);
    file.setOtherTimeSeriesData(data);
    return data;
  }

  /**
   * @param scale scales the intensities so traces with the same name are not equal
   */
  private static @NotNull OtherFeature createTrace(@NotNull OtherTimeSeriesDataImpl data,
      @NotNull String name, double scale) {
    final double[] intensities = Arrays.stream(INTENSITIES).map(v -> v * scale).toArray();
    return new OtherFeatureImpl(new SimpleOtherTimeSeries(null, RTS, intensities, name, data));
  }

  private static @NotNull OtherFeature addRawTrace(@NotNull OtherTimeSeriesDataImpl data,
      @NotNull String name, double scale) {
    final OtherFeature trace = createTrace(data, name, scale);
    data.addRawTrace(trace);
    return trace;
  }

  private static @NotNull OtherFeature addProcessedFeature(@NotNull OtherTimeSeriesDataImpl data,
      @NotNull OtherFeature rawTrace) {
    final OtherFeature feature = rawTrace.createSubFeature();
    final OtherTimeSeries rawSeries = rawTrace.getFeatureData();
    feature.set(OtherFeatureDataType.class,
        new SimpleOtherTimeSeries(null, new float[]{3f, 4f, 5f}, new double[]{.3d, .4d, .5d},
            rawSeries.getName(), data));
    data.addProcessedFeature(feature);
    return feature;
  }

  /**
   * Saves the selection via the parameter, writes the document to a string, parses it again and
   * loads it into a new parameter. This is the same path that batch files and projects take.
   */
  private static @NotNull OtherTraceSelection saveAndLoad(@NotNull OtherTraceSelection selection)
      throws Exception {
    final OtherTraceSelectionParameter saved = new OtherTraceSelectionParameter(selection);
    final Document document = XMLUtils.newDocument();
    final Element root = document.createElement("parameter");
    document.appendChild(root);
    saved.saveValueToXML(root);

    final String xml = XMLUtils.saveToString(document);
    final Document loadedDocument = XMLUtils.load(xml);

    final OtherTraceSelectionParameter loaded = new OtherTraceSelectionParameter(
        OtherTraceSelection.featureUv());
    loaded.loadValueFromXML(loadedDocument.getDocumentElement());
    return loaded.getValue();
  }

  /**
   * Creates the parameter xml with a selection element as older versions wrote it.
   */
  private static @NotNull OtherTraceSelection loadFromAttributes(@Nullable String version,
      @NotNull String... attributeValuePairs) throws Exception {
    final Document document = XMLUtils.newDocument();
    final Element root = document.createElement("parameter");
    document.appendChild(root);
    final Element selection = document.createElement("selection");
    root.appendChild(selection);
    if (version != null) {
      selection.setAttribute("ver", version);
    }
    for (int i = 0; i < attributeValuePairs.length; i += 2) {
      selection.setAttribute(attributeValuePairs[i], attributeValuePairs[i + 1]);
    }

    final Document loadedDocument = XMLUtils.load(XMLUtils.saveToString(document));
    final OtherTraceSelectionParameter loaded = new OtherTraceSelectionParameter(
        OtherTraceSelection.featureUv());
    loaded.loadValueFromXML(loadedDocument.getDocumentElement());
    return loaded.getValue();
  }

  private static void assertMatches(@NotNull OtherTraceSelection selection,
      @NotNull List<RawDataFile> files, @NotNull List<OtherFeature> expected) {
    Assertions.assertEquals(expected, selection.getMatchingTraces(files),
        () -> "Unexpected traces for selection " + selection);
  }

  private static void assertMatchesBeforeAndAfterSaveLoad(@NotNull OtherTraceSelection selection,
      @NotNull List<RawDataFile> files, @NotNull List<OtherFeature> expected) throws Exception {
    assertMatches(selection, files, expected);

    final OtherTraceSelection loaded = saveAndLoad(selection);
    Assertions.assertEquals(selection, loaded);
    assertMatches(loaded, files, expected);
  }

  static Stream<Arguments> selectionsForRoundTrip() {
    final OtherTraceSelection none = new OtherTraceSelection(null, null, null, null, null,
        OtherRawOrProcessed.RAW);
    return Stream.of( //
        Arguments.of(OtherTraceSelection.rawUv()), //
        Arguments.of(OtherTraceSelection.preprocessedUv()), //
        Arguments.of(OtherTraceSelection.featureUv()), //
        Arguments.of(none), //
        Arguments.of(none.withRawOrProcessed(OtherRawOrProcessed.FEATURES)), //
        Arguments.of(none.withChromatogramType(ChromatogramType.EMISSION)), //
        Arguments.of(none.withRangeUnitFilter("m*")), //
        Arguments.of(none.withRangeLabelFilter("*sion")), //
        Arguments.of(none.withDescriptionFilter("DAD1 A, Sig=254.4*")), //
        Arguments.of(
            new OtherTraceSelection(ChromatogramType.ABSORPTION, "mAU", "Absorbance", "DAD*",
                "254*", OtherRawOrProcessed.PREPROCESSED)), //
        Arguments.of(none.withDescriptionFilter(SPECIAL_DESCRIPTION)), //
        Arguments.of(
            new OtherTraceSelection(null, "mV\\s", "Signal ($)", "D:\\detector\\*", "trace ^1$",
                OtherRawOrProcessed.RAW)), //
        Arguments.of(
            new OtherTraceSelection(null, null, null, null, "*nm", OtherRawOrProcessed.FEATURES)) //
    );
  }

  static Stream<String> legacyWildcardPatterns() {
    return Stream.of("mAU", "DAD*", "*nm", "*", "*A, *", "DAD1 A, Sig=254.4*", "D:\\detector\\*",
        "trace ^1$", "Signal ($)", "a\\Eb*c\\Q", SPECIAL_DESCRIPTION);
  }

  @BeforeEach
  void init() {
    msFile = new RawDataFileImpl("testfile", null, null);
    msFiles = List.of(msFile);

    final OtherDataFileImpl uvFile = new OtherDataFileImpl(msFile);
    uvFile.setDescription(UV_DESCRIPTION);
    uvData = createTimeSeriesData(uvFile, ChromatogramType.ABSORPTION, "Absorbance", "mAU");
    uv254 = addRawTrace(uvData, "254 nm", 1d);
    uv280 = addRawTrace(uvData, "280 nm", 2d);
    uv254Preprocessed = createTrace(uvData, "254 nm", 3d);
    uv280Preprocessed = createTrace(uvData, "280 nm", 4d);
    uvData.setPreprocessedTraces(List.of(uv254Preprocessed, uv280Preprocessed));
    uv254Feature = addProcessedFeature(uvData, uv254);
    uv280Feature = addProcessedFeature(uvData, uv280);

    // no preprocessed traces, falls back to the raw traces
    final OtherDataFileImpl fldFile = new OtherDataFileImpl(msFile);
    fldFile.setDescription(FLD_DESCRIPTION);
    fldData = createTimeSeriesData(fldFile, ChromatogramType.EMISSION, "Emission", "LU");
    fld340 = addRawTrace(fldData, "Em 340", 5d);

    final OtherDataFileImpl specialFile = new OtherDataFileImpl(msFile);
    specialFile.setDescription(SPECIAL_DESCRIPTION);
    specialData = createTimeSeriesData(specialFile, ChromatogramType.UNKNOWN, "Signal ($)",
        "mV\\s");
    specialTrace = addRawTrace(specialData, "trace ^1$", 6d);

    // a file without time series data must be ignored
    final OtherDataFileImpl emptyFile = new OtherDataFileImpl(msFile);
    emptyFile.setDescription("empty");

    msFile.addOtherDataFiles(List.of(uvFile, fldFile, specialFile, emptyFile));

    secondMsFile = new RawDataFileImpl("second testfile", null, null);
    final OtherDataFileImpl secondUvFile = new OtherDataFileImpl(secondMsFile);
    secondUvFile.setDescription(SECOND_UV_DESCRIPTION);
    secondUvData = createTimeSeriesData(secondUvFile, ChromatogramType.ABSORPTION, "Absorbance",
        "mAU");
    secondUv230 = addRawTrace(secondUvData, "230 nm", 7d);
    secondMsFile.addOtherDataFiles(List.of(secondUvFile));
  }

  private void assertMatches(@NotNull OtherTraceSelection selection,
      @NotNull List<OtherFeature> expected) {
    assertMatches(selection, msFiles, expected);
  }

  /**
   * Asserts the expected traces for the selection itself and the saved and loaded selection.
   */
  private void assertMatchesBeforeAndAfterSaveLoad(@NotNull OtherTraceSelection selection,
      @NotNull List<OtherFeature> expected) throws Exception {
    assertMatchesBeforeAndAfterSaveLoad(selection, msFiles, expected);
  }

  @ParameterizedTest
  @MethodSource("selectionsForRoundTrip")
  void saveLoadKeepsSelectionAndFiltering(@NotNull OtherTraceSelection selection) throws Exception {
    final List<OtherFeature> before = selection.getMatchingTraces(msFiles);
    final OtherTraceSelection loaded = saveAndLoad(selection);

    Assertions.assertEquals(selection, loaded);
    Assertions.assertEquals(before, loaded.getMatchingTraces(msFiles));
  }

  @Test
  void rawUvSelectsOnlyRawAbsorptionTraces() throws Exception {
    assertMatchesBeforeAndAfterSaveLoad(OtherTraceSelection.rawUv(), List.of(uv254, uv280));
  }

  @Test
  void preprocessedUvSelectsOnlyPreprocessedAbsorptionTraces() throws Exception {
    assertMatchesBeforeAndAfterSaveLoad(OtherTraceSelection.preprocessedUv(),
        List.of(uv254Preprocessed, uv280Preprocessed));
  }

  @Test
  void featureUvSelectsOnlyAbsorptionFeatures() throws Exception {
    assertMatchesBeforeAndAfterSaveLoad(OtherTraceSelection.featureUv(),
        List.of(uv254Feature, uv280Feature));
  }

  @Test
  void preprocessedFallsBackToRawTracesIfNotPreprocessed() throws Exception {
    assertMatchesBeforeAndAfterSaveLoad(
        OtherTraceSelection.preprocessedUv().withChromatogramType(ChromatogramType.EMISSION),
        List.of(fld340));
  }

  @Test
  void noFiltersSelectAllTracesOfAllFilesWithTimeSeries() throws Exception {
    assertMatchesBeforeAndAfterSaveLoad(
        new OtherTraceSelection(null, null, null, null, null, OtherRawOrProcessed.RAW),
        List.of(uv254, uv280, fld340, specialTrace));
  }

  @Test
  void chromatogramTypeFilter() throws Exception {
    assertMatchesBeforeAndAfterSaveLoad(
        OtherTraceSelection.rawUv().withChromatogramType(ChromatogramType.EMISSION),
        List.of(fld340));
    assertMatchesBeforeAndAfterSaveLoad(
        OtherTraceSelection.rawUv().withChromatogramType(ChromatogramType.ION_CURRENT), List.of());
  }

  @Test
  void rangeUnitFilter() throws Exception {
    final OtherTraceSelection noType = new OtherTraceSelection(null, null, null, null, null,
        OtherRawOrProcessed.RAW);

    assertMatchesBeforeAndAfterSaveLoad(noType.withRangeUnitFilter("mAU"), List.of(uv254, uv280));
    assertMatchesBeforeAndAfterSaveLoad(noType.withRangeUnitFilter("LU"), List.of(fld340));
    assertMatchesBeforeAndAfterSaveLoad(noType.withRangeUnitFilter("*U"),
        List.of(uv254, uv280, fld340));
    // no partial matches without wildcards
    assertMatchesBeforeAndAfterSaveLoad(noType.withRangeUnitFilter("AU"), List.of());
    // all filters must match
    assertMatchesBeforeAndAfterSaveLoad(
        noType.withRangeUnitFilter("mAU").withChromatogramType(ChromatogramType.EMISSION),
        List.of());
  }

  @Test
  void rangeLabelFilter() throws Exception {
    final OtherTraceSelection noType = new OtherTraceSelection(null, null, null, null, null,
        OtherRawOrProcessed.RAW);

    assertMatchesBeforeAndAfterSaveLoad(noType.withRangeLabelFilter("Abs*"), List.of(uv254, uv280));
    assertMatchesBeforeAndAfterSaveLoad(noType.withRangeLabelFilter("Emission"), List.of(fld340));
    assertMatchesBeforeAndAfterSaveLoad(noType.withRangeLabelFilter("*o*"),
        List.of(uv254, uv280, fld340));
  }

  @Test
  void descriptionFilter() throws Exception {
    final OtherTraceSelection noType = new OtherTraceSelection(null, null, null, null, null,
        OtherRawOrProcessed.RAW);

    assertMatchesBeforeAndAfterSaveLoad(noType.withDescriptionFilter(UV_DESCRIPTION),
        List.of(uv254, uv280));
    assertMatchesBeforeAndAfterSaveLoad(noType.withDescriptionFilter("FLD*"), List.of(fld340));
    assertMatchesBeforeAndAfterSaveLoad(noType.withDescriptionFilter("*A, *"),
        List.of(uv254, uv280, fld340));
    // regex characters are matched literally, "." must not match any character
    assertMatchesBeforeAndAfterSaveLoad(noType.withDescriptionFilter("DAD1 A, Sig=254x4*"),
        List.of());
  }

  @Test
  void nameFilter() throws Exception {
    final OtherTraceSelection noType = new OtherTraceSelection(null, null, null, null, "254 nm",
        OtherRawOrProcessed.RAW);

    assertMatchesBeforeAndAfterSaveLoad(noType, List.of(uv254));
    assertMatchesBeforeAndAfterSaveLoad(noType.withRawOrProcessed(OtherRawOrProcessed.PREPROCESSED),
        List.of(uv254Preprocessed));
    assertMatchesBeforeAndAfterSaveLoad(noType.withRawOrProcessed(OtherRawOrProcessed.FEATURES),
        List.of(uv254Feature));

    assertMatchesBeforeAndAfterSaveLoad(
        new OtherTraceSelection(null, null, null, null, "*nm", OtherRawOrProcessed.RAW),
        List.of(uv254, uv280));
    assertMatchesBeforeAndAfterSaveLoad(
        new OtherTraceSelection(null, null, null, null, "Em*", OtherRawOrProcessed.RAW),
        List.of(fld340));
    // names are case-sensitive
    assertMatchesBeforeAndAfterSaveLoad(
        new OtherTraceSelection(null, null, null, null, "254 NM", OtherRawOrProcessed.RAW),
        List.of());
  }

  @Test
  void combinedFilters() throws Exception {
    assertMatchesBeforeAndAfterSaveLoad(
        new OtherTraceSelection(ChromatogramType.ABSORPTION, "mAU", "Absorbance", "DAD*", "280*",
            OtherRawOrProcessed.FEATURES), List.of(uv280Feature));
    // description of the fld file with the uv chromatogram type
    assertMatchesBeforeAndAfterSaveLoad(
        new OtherTraceSelection(ChromatogramType.ABSORPTION, null, null, FLD_DESCRIPTION, null,
            OtherRawOrProcessed.RAW), List.of());
  }

  @Test
  void specialCharactersAreKeptAfterSaveLoad() throws Exception {
    // version 2 selections are not cleaned, so regex and escape characters must survive
    final OtherTraceSelection selection = new OtherTraceSelection(null, "mV\\s", "Signal ($)",
        "D:\\detector\\*", "trace ^1$", OtherRawOrProcessed.RAW);
    assertMatchesBeforeAndAfterSaveLoad(selection, List.of(specialTrace));
    assertMatchesBeforeAndAfterSaveLoad(
        new OtherTraceSelection(null, null, null, SPECIAL_DESCRIPTION, null,
            OtherRawOrProcessed.RAW), List.of(specialTrace));
  }

  @Test
  void multipleMsFilesCollectTracesOfAllMatchingFiles() throws Exception {
    final List<RawDataFile> files = List.of(msFile, secondMsFile);

    // both ms files match, traces are returned in the order of the ms files
    assertMatchesBeforeAndAfterSaveLoad(OtherTraceSelection.rawUv(), files,
        List.of(uv254, uv280, secondUv230));
    assertMatchesBeforeAndAfterSaveLoad(OtherTraceSelection.rawUv(), List.of(secondMsFile, msFile),
        List.of(secondUv230, uv254, uv280));
    assertMatchesBeforeAndAfterSaveLoad(
        new OtherTraceSelection(null, "mAU", null, null, "*nm", OtherRawOrProcessed.RAW), files,
        List.of(uv254, uv280, secondUv230));
  }

  @Test
  void multipleMsFilesOnlyMatchingFilesContribute() throws Exception {
    final List<RawDataFile> files = List.of(msFile, secondMsFile);

    // only the second ms file matches
    assertMatchesBeforeAndAfterSaveLoad(OtherTraceSelection.rawUv().withDescriptionFilter("DAD2*"),
        files, List.of(secondUv230));
    assertMatchesBeforeAndAfterSaveLoad(
        new OtherTraceSelection(null, null, null, null, "230 nm", OtherRawOrProcessed.RAW), files,
        List.of(secondUv230));
    // only the first ms file matches
    assertMatchesBeforeAndAfterSaveLoad(
        OtherTraceSelection.rawUv().withChromatogramType(ChromatogramType.EMISSION), files,
        List.of(fld340));
    assertMatchesBeforeAndAfterSaveLoad(OtherTraceSelection.rawUv().withDescriptionFilter("DAD1*"),
        files, List.of(uv254, uv280));
    // the second ms file has no processed features
    assertMatchesBeforeAndAfterSaveLoad(OtherTraceSelection.featureUv(), files,
        List.of(uv254Feature, uv280Feature));
    // no ms file matches
    assertMatchesBeforeAndAfterSaveLoad(
        OtherTraceSelection.rawUv().withChromatogramType(ChromatogramType.PRESSURE), files,
        List.of());
  }

  @Test
  void multipleMsFilesMatchingTimeSeriesData() throws Exception {
    final List<RawDataFile> files = List.of(msFile, secondMsFile);
    final OtherTraceSelection loaded = saveAndLoad(OtherTraceSelection.rawUv());

    Assertions.assertEquals(List.of(uvData, secondUvData), loaded.getMatchingTimeSeriesData(files));
    Assertions.assertEquals(List.of(fldData),
        loaded.withChromatogramType(ChromatogramType.EMISSION).getMatchingTimeSeriesData(files));
    Assertions.assertEquals(List.of(), loaded.getMatchingTraces(List.of()));
  }

  @Test
  void streamMatchingTracesOfSingleTimeSeriesData() {
    final OtherTraceSelection selection = OtherTraceSelection.rawUv();
    Assertions.assertEquals(List.of(uv254, uv280), selection.getMatchingTraces(uvData));
    // not matching data results in an empty list, not in an exception
    Assertions.assertEquals(List.of(), selection.getMatchingTraces(fldData));
    Assertions.assertEquals(List.of(), selection.getMatchingTraces(specialData));
  }

  @Test
  void matchingTimeSeriesDataIgnoresNameAndRawOrProcessed() throws Exception {
    // name filter does not match any trace, but the time series data is still selected
    final OtherTraceSelection selection = new OtherTraceSelection(ChromatogramType.ABSORPTION, null,
        null, null, "does not exist", OtherRawOrProcessed.FEATURES);
    final OtherTraceSelection loaded = saveAndLoad(selection);

    Assertions.assertEquals(List.of(uvData), selection.getMatchingTimeSeriesData(msFiles));
    Assertions.assertEquals(List.of(uvData), loaded.getMatchingTimeSeriesData(msFiles));
    Assertions.assertEquals(List.of(), loaded.getMatchingTraces(msFiles));
  }

  @Test
  void blankFiltersAreLoadedAsNoFilter() throws Exception {
    final OtherTraceSelection blank = new OtherTraceSelection(ChromatogramType.ABSORPTION, "", " ",
        "", "  ", OtherRawOrProcessed.RAW);
    assertMatches(blank, List.of(uv254, uv280));

    final OtherTraceSelection loaded = saveAndLoad(blank);
    // blank strings are not persisted as filters
    Assertions.assertEquals(OtherTraceSelection.rawUv(), loaded);
    assertMatches(loaded, List.of(uv254, uv280));
  }

  @Test
  void savedXmlContainsVersionAndNullValues() throws Exception {
    final Document document = XMLUtils.newDocument();
    final Element element = document.createElement("selection");
    document.appendChild(element);
    OtherTraceSelection.featureUv().saveToXml(element);

    Assertions.assertEquals("2", element.getAttribute("ver"));
    Assertions.assertEquals(ChromatogramType.ABSORPTION.name(),
        element.getAttribute("chromatogramType"));
    Assertions.assertEquals(OtherRawOrProcessed.FEATURES.name(),
        element.getAttribute("rawOrProcessed"));
    Assertions.assertEquals(CONST.XML_NULL_VALUE, element.getAttribute("rangeUnitFilter"));
    Assertions.assertEquals(CONST.XML_NULL_VALUE, element.getAttribute("rangeLabelFilter"));
    Assertions.assertEquals(CONST.XML_NULL_VALUE, element.getAttribute("descriptionFilter"));
    Assertions.assertEquals(CONST.XML_NULL_VALUE, element.getAttribute("nameFilter"));
  }

  @Test
  void loadsLegacyRegexPatternsWithoutVersion() throws Exception {
    // versions < 2 saved the regex created from the wildcard pattern and had no name filter
    final OtherTraceSelection loaded = loadFromAttributes(null, //
        "chromatogramType", ChromatogramType.ABSORPTION.name(), //
        "rangeUnitFilter", "^\\QmAU\\E$", //
        "rangeLabelFilter", "^\\QAbsorbance\\E$", //
        "descriptionFilter", CONST.XML_NULL_VALUE, //
        "rawOrProcessed", OtherRawOrProcessed.RAW.name());

    Assertions.assertEquals(
        new OtherTraceSelection(ChromatogramType.ABSORPTION, "mAU", "Absorbance", null, null,
            OtherRawOrProcessed.RAW), loaded);
    assertMatches(loaded, List.of(uv254, uv280));
  }

  @Test
  void loadsLegacyRegexPatternsWithVersion1() throws Exception {
    final OtherTraceSelection loaded = loadFromAttributes("1", //
        "chromatogramType", CONST.XML_NULL_VALUE, //
        "rangeUnitFilter", CONST.XML_NULL_VALUE, //
        "rangeLabelFilter", CONST.XML_NULL_VALUE, //
        "descriptionFilter", "^\\QFLD1 A, Ex=230, Em=340\\E$", //
        "nameFilter", "^\\QEm 340\\E$", //
        "rawOrProcessed", OtherRawOrProcessed.RAW.name());

    Assertions.assertEquals(new OtherTraceSelection(null, null, null, FLD_DESCRIPTION, "Em 340",
        OtherRawOrProcessed.RAW), loaded);
    assertMatches(loaded, List.of(fld340));
  }

  @Test
  void loadsLegacyEmptyRegexAsNoFilter() throws Exception {
    // an empty filter was saved as the regex of an empty string
    final OtherTraceSelection loaded = loadFromAttributes(null, //
        "chromatogramType", ChromatogramType.ABSORPTION.name(), //
        "rangeUnitFilter", "^\\Q\\E$", //
        "rangeLabelFilter", "", //
        "descriptionFilter", CONST.XML_NULL_VALUE, //
        "rawOrProcessed", OtherRawOrProcessed.FEATURES.name());

    Assertions.assertEquals(OtherTraceSelection.featureUv(), loaded);
    assertMatches(loaded, List.of(uv254Feature, uv280Feature));
  }

  @Test
  void loadsLegacyWildcardRegex() throws Exception {
    // "DAD*" was saved as the regex ^\QDAD\E.*\Q\E$
    final OtherTraceSelection loaded = loadFromAttributes(null, //
        "chromatogramType", CONST.XML_NULL_VALUE, //
        "rangeUnitFilter", CONST.XML_NULL_VALUE, //
        "rangeLabelFilter", CONST.XML_NULL_VALUE, //
        "descriptionFilter", "^\\QDAD\\E.*\\Q\\E$", //
        "rawOrProcessed", OtherRawOrProcessed.RAW.name());

    Assertions.assertEquals("DAD*", loaded.descriptionFilter());
    assertMatches(loaded, List.of(uv254, uv280));
  }

  /**
   * Old versions saved {@link TextUtils#createRegexFromWildcards(String)} and applied it again when
   * loading, so saving a loaded selection wrapped the pattern twice.
   */
  @ParameterizedTest
  @MethodSource("legacyWildcardPatterns")
  void loadsLegacySingleAndDoubleWrappedRegex(@NotNull String wildcards) throws Exception {
    final String regex = TextUtils.createRegexFromWildcards(wildcards);
    final String doubleWrapped = TextUtils.createRegexFromWildcards(regex);

    for (final String saved : List.of(regex, doubleWrapped)) {
      final OtherTraceSelection loaded = loadFromAttributes(null, //
          "chromatogramType", CONST.XML_NULL_VALUE, //
          "rangeUnitFilter", saved, //
          "rangeLabelFilter", saved, //
          "descriptionFilter", saved, //
          "nameFilter", saved, //
          "rawOrProcessed", OtherRawOrProcessed.RAW.name());

      Assertions.assertEquals(
          new OtherTraceSelection(null, wildcards, wildcards, wildcards, wildcards,
              OtherRawOrProcessed.RAW), loaded, () -> "Legacy pattern " + saved);
    }
  }

  @Test
  void legacyWildcardSelectionFiltersLikeCurrentSelection() throws Exception {
    final OtherTraceSelection current = new OtherTraceSelection(null, "m*", "Abs*", "DAD*", "*nm",
        OtherRawOrProcessed.FEATURES);
    final OtherTraceSelection legacy = loadFromAttributes("1", //
        "chromatogramType", CONST.XML_NULL_VALUE, //
        "rangeUnitFilter", TextUtils.createRegexFromWildcards("m*"), //
        "rangeLabelFilter", TextUtils.createRegexFromWildcards("Abs*"), //
        "descriptionFilter", TextUtils.createRegexFromWildcards("DAD*"), //
        "nameFilter", TextUtils.createRegexFromWildcards("*nm"), //
        "rawOrProcessed", OtherRawOrProcessed.FEATURES.name());

    Assertions.assertEquals(current, legacy);
    assertMatches(legacy, List.of(uv254Feature, uv280Feature));
    // saving the loaded legacy selection writes the current version
    assertMatchesBeforeAndAfterSaveLoad(legacy, List.of(uv254Feature, uv280Feature));
  }

  @Test
  void legacyPatternInUnknownFormatIsStripped() throws Exception {
    final OtherTraceSelection loaded = loadFromAttributes(null, //
        "chromatogramType", CONST.XML_NULL_VALUE, //
        "rangeUnitFilter", CONST.XML_NULL_VALUE, //
        "rangeLabelFilter", CONST.XML_NULL_VALUE, //
        "descriptionFilter", CONST.XML_NULL_VALUE, //
        "nameFilter", "^254 nm$", //
        "rawOrProcessed", OtherRawOrProcessed.RAW.name());

    Assertions.assertEquals("254 nm", loaded.nameFilter());
    assertMatches(loaded, List.of(uv254));
  }

  @Test
  void invalidXmlFallsBackToRawUv() throws Exception {
    Assertions.assertEquals(OtherTraceSelection.rawUv(), loadFromAttributes("2", //
        "chromatogramType", ChromatogramType.EMISSION.name(), //
        "rawOrProcessed", "NOT_A_VALID_OPTION"));
    Assertions.assertEquals(OtherTraceSelection.rawUv(), loadFromAttributes("2", //
        "chromatogramType", "NOT_A_CHROMATOGRAM_TYPE", //
        "rawOrProcessed", OtherRawOrProcessed.FEATURES.name()));
  }

  @Test
  void cloneParameterKeepsSelection() {
    final OtherTraceSelection selection = new OtherTraceSelection(ChromatogramType.EMISSION, "LU",
        "Emission", "FLD*", "Em*", OtherRawOrProcessed.PREPROCESSED);
    final OtherTraceSelectionParameter parameter = new OtherTraceSelectionParameter(selection);
    final OtherTraceSelection cloned = parameter.cloneParameter().getValue();

    Assertions.assertEquals(selection, cloned);
    assertMatches(cloned, List.of(fld340));
  }
}
