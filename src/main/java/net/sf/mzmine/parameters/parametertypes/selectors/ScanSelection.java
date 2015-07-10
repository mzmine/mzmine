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

import net.sf.mzmine.datamodel.MassSpectrumType;
import net.sf.mzmine.datamodel.PolarityType;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.Scan;

import com.google.common.collect.Range;

@Immutable
public class ScanSelection {

    private final Range<Integer> scanNumberRange;
    private final Range<Double> scanRTRange;
    private final PolarityType polarity;
    private final MassSpectrumType spectrumType;
    private final Integer msLevel;

    public ScanSelection() {
        this(null, null, null, null, 1);
    }

    public ScanSelection(int msLevel) {
        this(null, null, null, null, msLevel);
    }

    public ScanSelection(Range<Double> scanRTRange, int msLevel) {
        this(null, scanRTRange, null, null, msLevel);
    }

    public ScanSelection(Range<Integer> scanNumberRange,
            Range<Double> scanRTRange, PolarityType polarity,
            MassSpectrumType spectrumType, Integer msLevel) {
        this.scanNumberRange = scanNumberRange;
        this.scanRTRange = scanRTRange;
        this.polarity = polarity;
        this.spectrumType = spectrumType;
        this.msLevel = msLevel;
    }

    public Range<Integer> getScanNumberRange() {
        return scanNumberRange;
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

    public Scan[] getMatchingScans(RawDataFile dataFile) {

        final List<Scan> matchingScans = new ArrayList<Scan>();

        int scanNumbers[] = dataFile.getScanNumbers();
        for (int scanNumber : scanNumbers) {
            Scan scan = dataFile.getScan(scanNumber);
            if ((msLevel != null) && (!msLevel.equals(scan.getMSLevel())))
                continue;
            if ((polarity != null) && (!polarity.equals(scan.getPolarity())))
                continue;
            if ((spectrumType != null)
                    && (!spectrumType.equals(scan.getSpectrumType())))
                continue;
            if ((scanNumberRange != null)
                    && (!scanNumberRange.contains(scanNumber)))
                continue;
            if ((scanRTRange != null)
                    && (!scanRTRange.contains(scan.getRetentionTime())))
                continue;
            matchingScans.add(scan);
        }

        return matchingScans.toArray(new Scan[matchingScans.size()]);

    }
}
