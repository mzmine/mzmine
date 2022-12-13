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
import io.github.mzmine.util.RangeUtils;
import io.github.mzmine.util.TextUtils;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import javax.annotation.concurrent.Immutable;

@Immutable
public class ScanSelection {

  private final Range<Integer> scanNumberRange;
  private final Range<Double> scanMobilityRange;
  private final Range<Float> scanRTRange;
  private final PolarityType polarity;
  private final MassSpectrumType spectrumType;
  private final Integer msLevel;
  private Integer baseFilteringInteger;
  private String scanDefinition;

  public ScanSelection() {
    this(1);
  }

  public ScanSelection(int msLevel) {
    this(null, null, null, null, null, null, msLevel, null);
  }

  public ScanSelection(Range<Float> scanRTRange, int msLevel) {
    this(null, null, scanRTRange, null, null, null, msLevel, null);
  }

  public ScanSelection(Range<Integer> scanNumberRange, Integer baseFilteringInteger,
      Range<Float> scanRTRange, Range<Double> scanMobilityRange, PolarityType polarity,
      MassSpectrumType spectrumType, Integer msLevel, String scanDefinition) {
    this.scanNumberRange = scanNumberRange;
    this.baseFilteringInteger = baseFilteringInteger;
    this.scanRTRange = scanRTRange;
    this.scanMobilityRange = scanMobilityRange;
    this.polarity = polarity;
    this.spectrumType = spectrumType;
    this.msLevel = msLevel;
    this.scanDefinition = scanDefinition;
  }

  public Range<Integer> getScanNumberRange() {
    return scanNumberRange;
  }

  public Integer getBaseFilteringInteger() {
    return baseFilteringInteger;
  }

  public Range<Float> getScanRTRange() {
    return scanRTRange;
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

  public Integer getMsLevel() {
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
    if ((msLevel != null) && (!msLevel.equals(scan.getMSLevel()))) {
      return false;
    }

    if ((polarity != null) && (!polarity.equals(scan.getPolarity()))) {
      return false;
    }

    if ((spectrumType != null) && (!spectrumType.equals(scan.getSpectrumType()))) {
      return false;
    }

    if ((scanNumberRange != null) && (!scanNumberRange.contains(scan.getScanNumber()))) {
      return false;
    }

    if ((baseFilteringInteger != null) && (
        (scan.getScanNumber() - scanNumberOffset) % baseFilteringInteger != 0)) {
      return false;
    }

    if ((scanRTRange != null) && (!scanRTRange.contains(scan.getRetentionTime()))) {
      return false;
    }

    if (scan instanceof Frame) {
      if (scanMobilityRange != null && !((Frame) scan).getMobilityRange()
          .isConnected(scanMobilityRange)) {
        return false;
      }
    } /*else {
      if ((scanMobilityRange != null) && (!scanMobilityRange.contains(scan.getMobility()))) {
        return false;
      }
    }*/

    if (!Strings.isNullOrEmpty(scanDefinition)) {

      final String actualScanDefinition = scan.getScanDefinition();

      if (Strings.isNullOrEmpty(actualScanDefinition)) {
        return false;
      }

      final String regex = TextUtils.createRegexFromWildcards(scanDefinition);

      if (!actualScanDefinition.matches(regex)) {
        return false;
      }
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

  /**
   * @param scan
   * @param scanNumberOffset is used for baseFilteringInteger (filter every n-th scan)
   * @return
   */
  public boolean matches(MobilityScan scan, int scanNumberOffset) {
    if ((msLevel != null) && (!msLevel.equals(scan.getFrame().getMSLevel()))) {
      return false;
    }

    if ((polarity != null) && (!polarity.equals(scan.getFrame().getPolarity()))) {
      return false;
    }

    if ((spectrumType != null) && (!spectrumType.equals(scan.getSpectrumType()))) {
      return false;
    }

    if ((scanNumberRange != null) && (!scanNumberRange.contains(scan.getFrame().getScanNumber()))) {
      return false;
    }

    if ((baseFilteringInteger != null) && (
        (scan.getFrame().getScanNumber() - scanNumberOffset) % baseFilteringInteger != 0)) {
      return false;
    }

    if ((scanRTRange != null) && (!scanRTRange.contains(scan.getRetentionTime()))) {
      return false;
    }

    if ((scanMobilityRange != null) && (!scanMobilityRange.contains(scan.getMobility()))) {
      return false;
    }

    if (!Strings.isNullOrEmpty(scanDefinition)) {

      final String actualScanDefinition = scan.getFrame().getScanDefinition();

      if (Strings.isNullOrEmpty(actualScanDefinition)) {
        return false;
      }

      final String regex = TextUtils.createRegexFromWildcards(scanDefinition);

      if (!actualScanDefinition.matches(regex)) {
        return false;
      }
    }
    return true;
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
        && getSpectrumType() == that.getSpectrumType() && Objects.equals(getMsLevel(),
        that.getMsLevel()) && Objects.equals(getBaseFilteringInteger(),
        that.getBaseFilteringInteger()) && Objects.equals(getScanDefinition(),
        that.getScanDefinition());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getScanNumberRange(), getScanMobilityRange(), getScanRTRange(),
        getPolarity(), getSpectrumType(), getMsLevel(), getBaseFilteringInteger(),
        getScanDefinition());
  }

  @Override
  public String toString() {
    StringBuilder b = new StringBuilder();
    DecimalFormat threeDecimals = new DecimalFormat("0.000");

    if (msLevel != null) {
      b.append("MS level (").append(msLevel).append("), ");
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

  public ScanSelection cloneWithNewRtRange(Range<Float> rtRange) {
    return new ScanSelection(getScanNumberRange(), getBaseFilteringInteger(), rtRange,
        getScanMobilityRange(), getPolarity(), getSpectrumType(), getMsLevel(),
        getScanDefinition());
  }
}
