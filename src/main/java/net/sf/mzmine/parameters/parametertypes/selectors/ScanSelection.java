/*
 * Copyright 2006-2015 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.parameters.parametertypes.selectors;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.concurrent.Immutable;

import com.google.common.base.Strings;
import com.google.common.collect.Range;
import com.google.common.primitives.Ints;

import org.apache.xpath.functions.FuncFalse;

import net.sf.mzmine.datamodel.MassSpectrumType;
import net.sf.mzmine.datamodel.PolarityType;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.util.TextUtils;

@Immutable
public class ScanSelection {

    private final Range<Integer> scanNumberRange;
    private Integer baseFilteringInteger;
    private final Range<Double> scanRTRange;
    private final PolarityType polarity;
    private final MassSpectrumType spectrumType;
    private final Integer msLevel;
    private String scanDefinition;

    public ScanSelection() {
        this(1);
    }

    public ScanSelection(int msLevel) {
        this(null, null, null, null, null, msLevel, null);
    }

    public ScanSelection(Range<Double> scanRTRange, int msLevel) {
        this(null, null, scanRTRange, null, null, msLevel, null);
    }

    public ScanSelection(Range<Integer> scanNumberRange, Integer baseFilteringInteger, Range<Double> scanRTRange,
            PolarityType polarity, MassSpectrumType spectrumType, Integer msLevel, String scanDefinition) {
        this.scanNumberRange = scanNumberRange;
        this.baseFilteringInteger = baseFilteringInteger;
        this.scanRTRange = scanRTRange;
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

    public Range<Double> getScanRTRange() {
        return scanRTRange;
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

    public Scan[] getMatchingScans(RawDataFile dataFile) {

        final List<Scan> matchingScans = new ArrayList<>();
        boolean offsetSet = false;
        int offset = 1;
        
        int scanNumbers[] = dataFile.getScanNumbers();
        for (int scanNumber : scanNumbers) {

            Scan scan = dataFile.getScan(scanNumber);

            if ((msLevel != null) && (!msLevel.equals(scan.getMSLevel())))
                continue;

            if ((polarity != null) && (!polarity.equals(scan.getPolarity())))
                continue;

            if ((spectrumType != null) && (!spectrumType.equals(scan.getSpectrumType())))
                continue;

            if ((scanNumberRange != null) && (!scanNumberRange.contains(scanNumber))) {
                continue;
            } else {
                if (!offsetSet) {
                    offsetSet = true;
                    offset = scanNumber;
                }
            }

            if ((baseFilteringInteger != null) && ((scanNumber - offset) % baseFilteringInteger != 0))
                continue;

            if ((scanRTRange != null) && (!scanRTRange.contains(scan.getRetentionTime())))
                continue;

            if (!Strings.isNullOrEmpty(scanDefinition)) {

                final String actualScanDefition = scan.getScanDefinition();

                if (Strings.isNullOrEmpty(actualScanDefition))
                    continue;

                final String regex = TextUtils.createRegexFromWildcards(scanDefinition);

                if (!actualScanDefition.matches(regex))
                    continue;

            }

            matchingScans.add(scan);
        }

        return matchingScans.toArray(new Scan[matchingScans.size()]);

    }

    public int[] getMatchingScanNumbers(RawDataFile dataFile) {

        final List<Integer> matchingScans = new ArrayList<>();

        int scanNumbers[] = dataFile.getScanNumbers();
        boolean offsetSet = false;
        int offset = 1;
        for (int scanNumber : scanNumbers) {

            Scan scan = dataFile.getScan(scanNumber);

            if ((msLevel != null) && (!msLevel.equals(scan.getMSLevel())))
                continue;

            if ((polarity != null) && (!polarity.equals(scan.getPolarity())))
                continue;

            if ((spectrumType != null) && (!spectrumType.equals(scan.getSpectrumType())))
                continue;

            if ((scanNumberRange != null) && (!scanNumberRange.contains(scanNumber))) {
                continue;
            } else {
                if (!offsetSet) {
                    offsetSet = true;
                    offset = scanNumber;
                }
            }
            if ((baseFilteringInteger != null) && ((scan.getScanNumber() - offset) % baseFilteringInteger != 0))
                continue;

            if ((scanRTRange != null) && (!scanRTRange.contains(scan.getRetentionTime())))
                continue;

            if (!Strings.isNullOrEmpty(scanDefinition)) {

                final String actualScanDefinition = scan.getScanDefinition();

                if (Strings.isNullOrEmpty(actualScanDefinition))
                    continue;

                final String regex = TextUtils.createRegexFromWildcards(scanDefinition);

                if (!actualScanDefinition.matches(regex))
                    continue;

            }

            matchingScans.add(scanNumber);
        }

        return Ints.toArray(matchingScans);

    }
}
