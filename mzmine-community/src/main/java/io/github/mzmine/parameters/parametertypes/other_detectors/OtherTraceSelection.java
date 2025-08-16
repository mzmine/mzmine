/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

import static io.github.mzmine.util.StringUtils.inQuotes;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.otherdetectors.OtherDataFile;
import io.github.mzmine.datamodel.otherdetectors.OtherFeature;
import io.github.mzmine.datamodel.otherdetectors.OtherTimeSeriesData;
import io.github.mzmine.modules.io.import_rawdata_mzml.msdk.data.ChromatogramType;
import io.github.mzmine.util.ParsingUtils;
import io.github.mzmine.util.TextUtils;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;

public record OtherTraceSelection(@Nullable ChromatogramType chromatogramType,
                                  @Nullable String rangeUnitFilter,
                                  @Nullable String rangeLabelFilter,
                                  @Nullable String descriptionFilter,
                                  @NotNull OtherRawOrProcessed rawOrProcessed) {

  private static final Logger logger = Logger.getLogger(OtherTraceSelection.class.getName());

  private static final String XML_CHROM_TYPE_ATTR = "chromatogramType";
  private static final String XML_RANGE_UNIT_FILTER_ATTR = "rangeUnitFilter";
  private static final String XML_RANGE_LABEL_FILTER_ATTR = "rangeLabelFilter";
  private static final String XML_DESCRIPTION_FILTER_ATTR = "descriptionFilter";
  private static final String XML_RAW_OR_PROCESSED_ATTR = "rawOrProcessed";

  public OtherTraceSelection(@Nullable ChromatogramType chromatogramType,
      @Nullable String rangeUnitFilter, @Nullable String rangeLabelFilter,
      @Nullable String descriptionFilter, @NotNull OtherRawOrProcessed rawOrProcessed) {
    this.chromatogramType = chromatogramType;
    this.rangeUnitFilter =
        rangeUnitFilter != null ? TextUtils.createRegexFromWildcards(rangeUnitFilter) : null;
    this.rangeLabelFilter =
        rangeLabelFilter != null ? TextUtils.createRegexFromWildcards(rangeLabelFilter) : null;
    this.descriptionFilter =
        descriptionFilter != null ? TextUtils.createRegexFromWildcards(descriptionFilter) : null;
    this.rawOrProcessed = rawOrProcessed;
  }

  public static OtherTraceSelection rawUv() {
    return new OtherTraceSelection(ChromatogramType.ABSORPTION, null, null, null,
        OtherRawOrProcessed.RAW);
  }

  public static OtherTraceSelection featureUv() {
    return new OtherTraceSelection(ChromatogramType.ABSORPTION, null, null, null,
        OtherRawOrProcessed.FEATURES);
  }

  public static OtherTraceSelection preprocessedUv() {
    return new OtherTraceSelection(ChromatogramType.ABSORPTION, null, null, null,
        OtherRawOrProcessed.PREPROCESSED);
  }

  public static OtherTraceSelection loadFromXml(Element element) {
    try {
      final var chromType =
          ParsingUtils.readNullableString(element.getAttribute(XML_CHROM_TYPE_ATTR)) != null
              ? ChromatogramType.valueOf(element.getAttribute(XML_CHROM_TYPE_ATTR)) : null;

      final String rangeUnitFilter = ParsingUtils.readNullableString(
          element.getAttribute(XML_RANGE_UNIT_FILTER_ATTR));

      final String rangeLabelFilter = ParsingUtils.readNullableString(
          element.getAttribute(XML_RANGE_LABEL_FILTER_ATTR));

      final String descriptionFilter = ParsingUtils.readNullableString(
          element.getAttribute(XML_DESCRIPTION_FILTER_ATTR));

      final OtherRawOrProcessed rawOrProcessed = OtherRawOrProcessed.valueOf(
          element.getAttribute(XML_RAW_OR_PROCESSED_ATTR));

      return new OtherTraceSelection(chromType, rangeUnitFilter, rangeLabelFilter,
          descriptionFilter, rawOrProcessed);
    } catch (Exception e) {
      logger.log(Level.SEVERE,
          "Cannot load OtherTraceSelection from xml. Defaulting to only raw UV.", e);
      return rawUv();
    }
  }

  public Stream<OtherFeature> streamMatchingTraces(OtherTimeSeriesData otherTimeSeriesData) {
    // make sure everything matches. if not, the stream will simply be empty.
    if(!matchesTimeSeriesData(otherTimeSeriesData)) {
      return Stream.empty();
    }
    return rawOrProcessed.streamMatching(otherTimeSeriesData);
  }

  public List<OtherFeature> getMatchingTraces(OtherTimeSeriesData otherTimeSeriesData) {
    return streamMatchingTraces(otherTimeSeriesData).toList();
  }

  public List<OtherFeature> getMatchingTraces(Collection<RawDataFile> msFiles) {
    return streamMatchingTraces(msFiles).toList();
  }

  public Stream<OtherFeature> streamMatchingTraces(Collection<RawDataFile> msFiles) {
    return streamMatchingTimeSeriesData(msFiles) //
        .flatMap(rawOrProcessed::streamMatching);
  }

  public List<OtherTimeSeriesData> getMatchingTimeSeriesData(Collection<RawDataFile> msFiles) {
    return streamMatchingTimeSeriesData(msFiles).toList(); //
  }

  public Stream<OtherTimeSeriesData> streamMatchingTimeSeriesData(Collection<RawDataFile> msFiles) {
    return msFiles.stream().flatMap(f -> f.getOtherDataFiles().stream())
        .filter(OtherDataFile::hasTimeSeries).map(OtherDataFile::getOtherTimeSeriesData)
        .filter(this::matchesTimeSeriesData); //
  }

  private boolean matchesTimeSeriesData(OtherTimeSeriesData obj) {
    return Objects.nonNull(obj) && (chromatogramType == null
        || obj.getChromatogramType() == chromatogramType) && (rangeUnitFilter == null
        || obj.getTimeSeriesRangeUnit().matches(rangeUnitFilter)) && (rangeLabelFilter == null
        || obj.getTimeSeriesRangeLabel().matches(rangeLabelFilter)) && (descriptionFilter == null
        || obj.getOtherDataFile().getDescription().matches(descriptionFilter));
  }

  /**
   * @param element The element to write the data into.
   */
  public void saveToXml(Element element) {
    element.setAttribute(XML_CHROM_TYPE_ATTR, ParsingUtils.parseNullableString(
        chromatogramType != null ? chromatogramType.name() : null));
    element.setAttribute(XML_RANGE_UNIT_FILTER_ATTR,
        ParsingUtils.parseNullableString(rangeUnitFilter));
    element.setAttribute(XML_RANGE_LABEL_FILTER_ATTR,
        ParsingUtils.parseNullableString(rangeLabelFilter));
    element.setAttribute(XML_DESCRIPTION_FILTER_ATTR,
        ParsingUtils.parseNullableString(descriptionFilter));
    element.setAttribute(XML_RAW_OR_PROCESSED_ATTR, rawOrProcessed.name());
  }

  @Override
  public String toString() {
    final StringBuilder b = new StringBuilder();
    b.append("only ");
    b.append(rawOrProcessed);
    b.append(", ");
    if (chromatogramType != null) {
      b.append(" of type ");
      b.append(chromatogramType.getDescription());
      b.append("(s), ");
    }
    if (rangeUnitFilter != null) {
      b.append("with range unit: ");
      b.append(inQuotes(rangeUnitFilter)).append(", ");
    }
    if (rangeUnitFilter != null) {
      b.append("with range label: ");
      b.append(inQuotes(rangeUnitFilter)).append(", ");
    }
    if (descriptionFilter != null) {
      b.append("with description: ");
      b.append(inQuotes(descriptionFilter)).append(", ");
    }

    return b.toString();
  }

  public OtherTraceSelection copy() {
    return new OtherTraceSelection(chromatogramType, rangeUnitFilter, rangeLabelFilter,
        descriptionFilter, rawOrProcessed);
  }

  public OtherTraceSelection withChromatogramType(@Nullable ChromatogramType chromatogramType) {
    return new OtherTraceSelection(chromatogramType, rangeUnitFilter, rangeLabelFilter,
        descriptionFilter, rawOrProcessed);
  }

  public OtherTraceSelection withRangeUnitFilter(@Nullable String rangeUnitFilter) {
    return new OtherTraceSelection(chromatogramType, rangeUnitFilter, rangeLabelFilter,
        descriptionFilter, rawOrProcessed);
  }

  public OtherTraceSelection withRangeLabelFilter(@Nullable String rangeLabelFilter) {
    return new OtherTraceSelection(chromatogramType, rangeUnitFilter, rangeLabelFilter,
        descriptionFilter, rawOrProcessed);
  }

  public OtherTraceSelection withDescriptionFilter(@Nullable String descriptionFilter) {
    return new OtherTraceSelection(chromatogramType, rangeUnitFilter, rangeLabelFilter,
        descriptionFilter, rawOrProcessed);
  }

  public OtherTraceSelection withRawOrProcessed(@NotNull OtherRawOrProcessed rawOrProcessed) {
    return new OtherTraceSelection(chromatogramType, rangeUnitFilter, rangeLabelFilter,
        descriptionFilter, rawOrProcessed);
  }

}
