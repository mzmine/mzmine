/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package io.github.mzmine.parameters.parametertypes.selectors;

import com.google.common.base.Strings;
import com.google.common.collect.Range;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.parameters.parametertypes.combowithinput.MsLevelFilter;
import io.github.mzmine.util.RangeUtils;
import io.github.mzmine.util.TextUtils;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;

public record ScanSelection(Range<Integer> scanNumberRange, Integer baseFilteringInteger,
                            Range<Double> scanRTRange, Range<Double> scanMobilityRange,
                            @NotNull PolarityType polarity, @NotNull MassSpectrumType spectrumType,
                            @NotNull MsLevelFilter msLevel, String scanDefinition) {

  /**
   * Includes all scans
   */
  public static final ScanSelection ALL_SCANS = new ScanSelection(MsLevelFilter.ALL_LEVELS);
  public static final ScanSelection MS1 = new ScanSelection(1);

  /**
   * Uses MS level 1 only
   */
  public ScanSelection() {
    this(1);
  }

  public ScanSelection(MsLevelFilter msLevelFilter) {
    this(null, null, null, null, PolarityType.ANY, MassSpectrumType.ANY, msLevelFilter, null);
  }

  public ScanSelection(Integer msLevel) {
    this(MsLevelFilter.of(msLevel));
  }

  public ScanSelection(Range<Double> scanRTRange, Integer msLevel) {
    this(null, null, scanRTRange, null, PolarityType.ANY, MassSpectrumType.ANY,
        MsLevelFilter.of(msLevel), null);
  }

  public ScanSelection(final int msLevel, final Range<Float> scanRTRange) {
    this(null, null, scanRTRange == null ? null : RangeUtils.toDoubleRange(scanRTRange), null,
        PolarityType.ANY, MassSpectrumType.ANY, MsLevelFilter.of(msLevel), null);
  }

  public Range<Integer> getScanNumberRange() {
    return scanNumberRange;
  }

  public Integer getBaseFilteringInteger() {
    return baseFilteringInteger;
  }

  public Range<Double> getScanRTRange() {
    return scanRTRange;
  }

  public Range<Float> getScanRTRangeFloat() {
    return RangeUtils.toFloatRange(scanRTRange);
  }

  public Range<Double> getScanMobilityRange() {
    return scanMobilityRange;
  }

  public PolarityType getPolarity() {
    return polarity;
  }

  public MassSpectrumType getSpectrumType() {
    return spectrumType;
  }

  public MsLevelFilter getMsLevelFilter() {
    return msLevel;
  }

  public String getScanDefinition() {
    return scanDefinition;
  }

  public <T extends Scan> List<T> getMatchingScans(Collection<T> scans) {
    List<T> eligibleScans = new ArrayList<>();
    for (T scan : scans) {
      if (matches(scan)) {
        eligibleScans.add(scan);
      }
    }
    return eligibleScans;
  }

  public Stream<Scan> streamMatchingScans(RawDataFile dataFile) {
    return dataFile.getScans().stream().filter(this::matches);
  }

  public Scan[] getMatchingScans(RawDataFile dataFile) {
    return streamMatchingScans(dataFile).toArray(Scan[]::new);
  }

  /**
   * Returns the closest scan to the given retention time matching this scan selection.
   *
   * @param file          The raw data file.
   * @param retentionTime The retention time of the wanted scan.
   * @return
   */
  public Scan getScanAtRt(RawDataFile file, float retentionTime) {

    if (retentionTime > file.getDataRTRange().upperEndpoint()) {
      return null;
    }

    Scan[] matchingScans = getMatchingScans(file);
    double minDiff = 10E6;

    for (int i = 0; i < matchingScans.length; i++) {
      Scan scan = matchingScans[i];
      double diff = Math.abs(retentionTime - scan.getRetentionTime());

      if (diff < minDiff) {
        minDiff = diff;
      } else if (diff > minDiff) {
        return matchingScans[i - 1];
      }
    }
    return null;
  }

  /**
   * This method is deprecated as MZmine now uses the scans instead of the scan numbers
   *
   * @param dataFile
   * @return
   */
  @Deprecated
  public int[] getMatchingScanNumbers(RawDataFile dataFile) {
    return streamMatchingScans(dataFile).mapToInt(Scan::getScanNumber).toArray();
  }

  public boolean matches(Scan scan) {
    // scan offset was changed
    int offset;
    if (scanNumberRange != null) {
      offset = scanNumberRange.lowerEndpoint();
    } else {
      // first scan number
      if (scan.getDataFile() != null && scan.getDataFile().getScans().size() > 0) {
        offset = scan.getDataFile().getScans().get(0).getScanNumber();
      } else {
        offset = 1;
      }
    }
    return matches(scan, offset);
  }

  /**
   * @param scan
   * @param scanNumberOffset is used for baseFilteringInteger (filter every n-th scan)
   * @return
   */
  public boolean matches(Scan scan, int scanNumberOffset) {
    if (!msLevel.accept(scan)) {
      return false;
    }

    if (polarity != PolarityType.ANY && polarity != scan.getPolarity()) {
      return false;
    }

    if (spectrumType != MassSpectrumType.ANY && spectrumType != scan.getSpectrumType()) {
      return false;
    }

    if ((scanNumberRange != null) && (!scanNumberRange.contains(scan.getScanNumber()))) {
      return false;
    }

    if ((baseFilteringInteger != null) && (
        (scan.getScanNumber() - scanNumberOffset) % baseFilteringInteger != 0)) {
      return false;
    }

    if ((scanRTRange != null) && (!scanRTRange.contains((double) scan.getRetentionTime()))) {
      return false;
    }

    if (scan instanceof MobilityScan mobScan) {
      if ((scanMobilityRange != null) && (!scanMobilityRange.contains(mobScan.getMobility()))) {
        return false;
      }
    } else if (scan instanceof Frame) {
      if (scanMobilityRange != null && !((Frame) scan).getMobilityRange()
          .isConnected(scanMobilityRange)) {
        return false;
      }
    }

    if (!Strings.isNullOrEmpty(scanDefinition)) {

      final String actualScanDefinition = scan.getScanDefinition();

      if (Strings.isNullOrEmpty(actualScanDefinition)) {
        return false;
      }

      final String regex = TextUtils.createRegexFromWildcards(scanDefinition);

      return actualScanDefinition.matches(regex);
    }
    return true;
  }


  public boolean matches(MobilityScan scan) {
    // scan offset was changed
    int offset;
    if (scanNumberRange != null) {
      offset = scanNumberRange.lowerEndpoint();
    } else {
      // first scan number
      if (scan.getFrame().getDataFile() != null
          && scan.getFrame().getDataFile().getScans().size() > 0) {
        offset = scan.getFrame().getDataFile().getScans().get(0).getScanNumber();
      } else {
        offset = 1;
      }
    }
    return matches(scan, offset);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ScanSelection that = (ScanSelection) o;
    return Objects.equals(getScanNumberRange(), that.getScanNumberRange()) && Objects.equals(
        getScanMobilityRange(), that.getScanMobilityRange()) && Objects.equals(getScanRTRange(),
        that.getScanRTRange()) && getPolarity() == that.getPolarity()
        && getSpectrumType() == that.getSpectrumType() && Objects.equals(getMsLevelFilter(),
        that.getMsLevelFilter()) && Objects.equals(getBaseFilteringInteger(),
        that.getBaseFilteringInteger()) && Objects.equals(getScanDefinition(),
        that.getScanDefinition());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getScanNumberRange(), getScanMobilityRange(), getScanRTRange(),
        getPolarity(), getSpectrumType(), getMsLevelFilter(), getBaseFilteringInteger(),
        getScanDefinition());
  }

  @Override
  public String toString() {
    StringBuilder b = new StringBuilder();
    DecimalFormat threeDecimals = new DecimalFormat("0.000");

    if (msLevel != null) {
      b.append(msLevel).append(", ");
    }
    if (scanNumberRange != null) {
      b.append("Scan (#").append(scanNumberRange.lowerEndpoint()).append(" - ")
          .append(scanNumberRange.upperEndpoint()).append("), ");
    }
    if (scanRTRange != null) {
      b.append("RT range (").append(RangeUtils.formatRange(scanRTRange, threeDecimals))
          .append("), ");
    }
    if (scanMobilityRange != null) {
      b.append("Mobility range (").append(RangeUtils.formatRange(scanMobilityRange, threeDecimals))
          .append("), ");
    }
    if (polarity != null) {
      b.append("Polarity (").append(polarity.asSingleChar()).append("), ");
    }
    if (spectrumType != null) {
      b.append("Spectrum type (").append(spectrumType).append("), ");
    }
    if (baseFilteringInteger != null) {
      b.append("Base filtering interger (").append(baseFilteringInteger).append("), ");
    }
    if (scanDefinition != null) {
      b.append("Scan definition (").append(scanDefinition).append(") ");
    }

    return b.toString();
  }


  /**
   * @return short version of filter string used in interfaces
   */
  public String toShortDescription() {
    DecimalFormat threeDecimals = new DecimalFormat("0.000");

    List<String> parts = new ArrayList<>();
    parts.add(msLevel.getFilterString());
    if (scanNumberRange != null) {
      parts.add("Scan numbers " + scanNumberRange);
    }
    if (scanRTRange != null) {
      parts.add("RT " + RangeUtils.formatRange(scanRTRange, threeDecimals, true, true));
    }
    if (scanMobilityRange != null) {
      parts.add("Mobility " + RangeUtils.formatRange(scanMobilityRange, threeDecimals, true, true));
    }
    if (polarity != PolarityType.ANY) {
      parts.add("Polarity=" + polarity.asSingleChar());
    }
    if (spectrumType != MassSpectrumType.ANY) {
      parts.add("Scan type=" + spectrumType);
    }
    if (baseFilteringInteger != null) {
      parts.add("Base filter=" + baseFilteringInteger);
    }
    if (scanDefinition != null && !scanDefinition.isBlank()) {
      parts.add("Definition contains '" + scanDefinition + "'");
    }

    return parts.stream().filter(s -> !s.isBlank()).collect(Collectors.joining(", "));
  }

  public ScanSelection cloneWithNewRtRange(Range<Double> rtRange) {
    return new ScanSelection(getScanNumberRange(), getBaseFilteringInteger(), rtRange,
        getScanMobilityRange(), getPolarity(), getSpectrumType(), getMsLevelFilter(),
        getScanDefinition());
  }
}
