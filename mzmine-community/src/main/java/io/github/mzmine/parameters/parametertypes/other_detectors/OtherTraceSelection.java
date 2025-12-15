/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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
                                  @Nullable String descriptionFilter, @Nullable String nameFilter,
                                  @NotNull OtherRawOrProcessed rawOrProcessed) {

  private static final Logger logger = Logger.getLogger(OtherTraceSelection.class.getName());

  private static final String XML_CHROM_TYPE_ATTR = "chromatogramType";
  private static final String XML_RANGE_UNIT_FILTER_ATTR = "rangeUnitFilter";
  private static final String XML_RANGE_LABEL_FILTER_ATTR = "rangeLabelFilter";
  private static final String XML_DESCRIPTION_FILTER_ATTR = "descriptionFilter";
  private static final String XML_NAME_FILTER_ATTR = "nameFilter";
  private static final String XML_RAW_OR_PROCESSED_ATTR = "rawOrProcessed";
  private static final String XML_VERSION_ATTR = "ver";

  public static OtherTraceSelection rawUv() {
    return new OtherTraceSelection(ChromatogramType.ABSORPTION, null, null, null, null,
        OtherRawOrProcessed.RAW);
  }

  public static OtherTraceSelection featureUv() {
    return new OtherTraceSelection(ChromatogramType.ABSORPTION, null, null, null, null,
        OtherRawOrProcessed.FEATURES);
  }

  public static OtherTraceSelection preprocessedUv() {
    return new OtherTraceSelection(ChromatogramType.ABSORPTION, null, null, null, null,
        OtherRawOrProcessed.PREPROCESSED);
  }

  public static OtherTraceSelection loadFromXml(Element element) {
    try {
      final var chromType =
          ParsingUtils.readNullableString(element.getAttribute(XML_CHROM_TYPE_ATTR)) != null
              ? ChromatogramType.valueOf(element.getAttribute(XML_CHROM_TYPE_ATTR)) : null;

      final boolean needToClean =
          !element.hasAttribute(XML_VERSION_ATTR) || (element.hasAttribute(XML_VERSION_ATTR)
              && Integer.parseInt(element.getAttribute(XML_VERSION_ATTR)) < 2);

      final String rangeUnitFilter = clean(
          ParsingUtils.readNullableString(element.getAttribute(XML_RANGE_UNIT_FILTER_ATTR)),
          needToClean);

      final String rangeLabelFilter = clean(
          ParsingUtils.readNullableString(element.getAttribute(XML_RANGE_LABEL_FILTER_ATTR)),
          needToClean);

      final String descriptionFilter = clean(
          ParsingUtils.readNullableString(element.getAttribute(XML_DESCRIPTION_FILTER_ATTR)),
          needToClean);

      final String nameFilter = clean(
          ParsingUtils.readNullableString(element.getAttribute(XML_NAME_FILTER_ATTR)), needToClean);

      final OtherRawOrProcessed rawOrProcessed = OtherRawOrProcessed.valueOf(
          element.getAttribute(XML_RAW_OR_PROCESSED_ATTR));

      return new OtherTraceSelection(chromType, rangeUnitFilter, rangeLabelFilter,
          descriptionFilter, nameFilter, rawOrProcessed);
    } catch (Exception e) {
      logger.log(Level.SEVERE,
          "Cannot load OtherTraceSelection from xml. Defaulting to only raw UV.", e);
      return rawUv();
    }
  }

  private static String clean(@Nullable String pattern, boolean needsCleaning) {
    if (!needsCleaning) {
      return pattern;
    }
    if (pattern == null) {
      return null;
    }
    pattern = pattern.replaceAll("\\^", "");
    pattern = pattern.replaceAll("\\\\Q", "");
    pattern = pattern.replaceAll("\\\\E", "");
    pattern = pattern.replaceAll("\\$", "");
    pattern = pattern.replaceAll("\\\\", "");

    return pattern;
  }

  /**
   *
   * @return Stream of all features that match all the filters. Empty stream if none matches.
   */
  public Stream<OtherFeature> streamMatchingTraces(OtherTimeSeriesData otherTimeSeriesData) {
    // make sure everything matches. if not, the stream will simply be empty.
    if (!matchesTimeSeriesData(otherTimeSeriesData)) {
      return Stream.empty();
    }
    return rawOrProcessed.streamMatching(otherTimeSeriesData) //
        .filter(this::filterName);
  }

  /**
   * @see this#streamMatchingTraces(OtherTimeSeriesData)
   */
  public List<OtherFeature> getMatchingTraces(OtherTimeSeriesData otherTimeSeriesData) {
    return streamMatchingTraces(otherTimeSeriesData).toList();
  }

  /**
   * @see this#streamMatchingTraces(OtherTimeSeriesData)
   */
  public List<OtherFeature> getMatchingTraces(Collection<RawDataFile> msFiles) {
    return streamMatchingTraces(msFiles).toList();
  }

  /**
   * @see this#streamMatchingTraces(OtherTimeSeriesData)
   */
  public Stream<OtherFeature> streamMatchingTraces(Collection<RawDataFile> msFiles) {
    return streamMatchingTimeSeriesData(msFiles) //
        .flatMap(this::streamMatchingTraces); //
  }

  /**
   *
   * @return All {@link OtherTimeSeriesData} that match the given selection. May still contain
   * individual traces that do not match the {@link this#nameFilter()} or
   * {@link this#rawOrProcessed()}. Use {@link this#streamMatchingTraces(OtherTimeSeriesData)}.
   */
  public List<OtherTimeSeriesData> getMatchingTimeSeriesData(Collection<RawDataFile> msFiles) {
    return streamMatchingTimeSeriesData(msFiles).toList(); //
  }

  /**
   *
   * @return All {@link OtherTimeSeriesData} that match the given selection. May still contain
   * individual traces that do not match the {@link this#nameFilter()} or
   * {@link this#rawOrProcessed()}. Use {@link this#streamMatchingTraces(Collection)}.
   */
  public Stream<OtherTimeSeriesData> streamMatchingTimeSeriesData(Collection<RawDataFile> msFiles) {
    return msFiles.stream().flatMap(f -> f.getOtherDataFiles().stream())
        .filter(OtherDataFile::hasTimeSeries).map(OtherDataFile::getOtherTimeSeriesData)
        .filter(this::matchesTimeSeriesData); //
  }

  private boolean matchesTimeSeriesData(OtherTimeSeriesData obj) {
    final String unitRegex =
        rangeUnitFilter != null ? TextUtils.createRegexFromWildcards(rangeUnitFilter) : null;
    final String labelRegex =
        rangeLabelFilter != null ? TextUtils.createRegexFromWildcards(rangeLabelFilter) : null;
    final String descriptionRegex =
        descriptionFilter != null ? TextUtils.createRegexFromWildcards(descriptionFilter) : null;

    return Objects.nonNull(obj) && (chromatogramType == null
        || obj.getChromatogramType() == chromatogramType) //
        && (unitRegex == null || obj.getTimeSeriesRangeUnit().matches(unitRegex)) //
        && (labelRegex == null || obj.getTimeSeriesRangeLabel().matches(labelRegex)) //
        && (descriptionRegex == null || obj.getOtherDataFile().getDescription()
        .matches(descriptionRegex));
  }

  private boolean filterName(OtherFeature f) {
    final String nameRegex =
        nameFilter != null ? TextUtils.createRegexFromWildcards(nameFilter) : null;

    return nameRegex == null || (f.getFeatureData() != null && f.getFeatureData().getName() != null
        && f.getFeatureData().getName().matches(nameRegex));
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
    element.setAttribute(XML_NAME_FILTER_ATTR, ParsingUtils.parseNullableString(nameFilter));
    element.setAttribute(XML_VERSION_ATTR, "2");
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
    if (nameFilter != null) {
      b.append("with name: ");
      b.append(inQuotes(nameFilter)).append(", ");
    }

    return b.toString();
  }

  public OtherTraceSelection copy() {
    return new OtherTraceSelection(chromatogramType, rangeUnitFilter, rangeLabelFilter,
        descriptionFilter, nameFilter, rawOrProcessed);
  }

  public OtherTraceSelection withChromatogramType(@Nullable ChromatogramType chromatogramType) {
    return new OtherTraceSelection(chromatogramType, rangeUnitFilter, rangeLabelFilter,
        descriptionFilter, nameFilter, rawOrProcessed);
  }

  public OtherTraceSelection withRangeUnitFilter(@Nullable String rangeUnitFilter) {
    return new OtherTraceSelection(chromatogramType, rangeUnitFilter, rangeLabelFilter,
        descriptionFilter, nameFilter, rawOrProcessed);
  }

  public OtherTraceSelection withRangeLabelFilter(@Nullable String rangeLabelFilter) {
    return new OtherTraceSelection(chromatogramType, rangeUnitFilter, rangeLabelFilter,
        descriptionFilter, nameFilter, rawOrProcessed);
  }

  public OtherTraceSelection withDescriptionFilter(@Nullable String descriptionFilter) {
    return new OtherTraceSelection(chromatogramType, rangeUnitFilter, rangeLabelFilter,
        descriptionFilter, nameFilter, rawOrProcessed);
  }

  public OtherTraceSelection withRawOrProcessed(@NotNull OtherRawOrProcessed rawOrProcessed) {
    return new OtherTraceSelection(chromatogramType, rangeUnitFilter, rangeLabelFilter,
        descriptionFilter, nameFilter, rawOrProcessed);
  }
}
