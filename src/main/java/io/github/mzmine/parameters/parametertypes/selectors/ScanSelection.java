/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.parameters.parametertypes.selectors;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.concurrent.Immutable;
import com.google.common.base.Strings;
import com.google.common.collect.Range;
import com.google.common.primitives.Ints;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.util.TextUtils;

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

  public Set<? extends Scan> getMachtingScans(Set<? extends Scan> scans) {
    Set<Scan> eligibleScans = new HashSet<>();
    for (Scan scan : scans) {
      if (matches(scan)) {
        eligibleScans.add(scan);
      }
    }
    return eligibleScans;
  }

  public Scan[] getMatchingScans(RawDataFile dataFile) {

    final List<Scan> matchingScans = new ArrayList<>();

    int scanNumbers[] = dataFile.getScanNumbers();
    for (int scanNumber : scanNumbers) {

      Scan scan = dataFile.getScan(scanNumber);
      if (matches(scan)) {
        matchingScans.add(scan);
      }
    }

    return matchingScans.toArray(new Scan[matchingScans.size()]);
  }

  public int[] getMatchingScanNumbers(RawDataFile dataFile) {

    final List<Integer> matchingScans = new ArrayList<>();

    int scanNumbers[] = dataFile.getScanNumbers();

    for (int scanNumber : scanNumbers) {
      Scan scan = dataFile.getScan(scanNumber);
      if (matches(scan)) {
        matchingScans.add(scanNumber);
      }
    }

    return Ints.toArray(matchingScans);
  }

  public boolean matches(Scan scan) {
    // scan offset was changed
    int offset;
    if (scanNumberRange != null) {
      offset = scanNumberRange.lowerEndpoint();
    } else {
      // first scan number
      if (scan.getDataFile() != null && scan.getDataFile().getScanNumbers().length > 0) {
        offset = scan.getDataFile().getScanNumbers()[0];
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

    if ((baseFilteringInteger != null)
        && ((scan.getScanNumber() - scanNumberOffset) % baseFilteringInteger != 0)) {
      return false;
    }

    if ((scanRTRange != null) && (!scanRTRange.contains(scan.getRetentionTime()))) {
      return false;
    }

    if (scan instanceof Frame) {
      if (scanMobilityRange != null
          && !((Frame) scan).getMobilityRange().isConnected(scanMobilityRange)) {
        return false;
      }
    } else {
      if ((scanMobilityRange != null) && (!scanMobilityRange.contains(scan.getMobility()))) {
        return false;
      }
    }

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
}
