/*
 * Copyright 2006-2014 The MZmine 2 Development Team
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

package net.sf.mzmine.datamodel.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.MsScan;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.util.CollectionUtils;
import net.sf.mzmine.util.ScanSorter;

import com.google.common.collect.Range;

/**
 * RawDataFile implementation.
 */
class RawDataFileImpl extends DataPointStoreImpl implements RawDataFile {

    private @Nonnull String rawDataFileName;
    private final List<MsScan> scans;

    RawDataFileImpl() {
	rawDataFileName = "New file";
	scans = Collections.synchronizedList(new ArrayList<MsScan>());
    }

    @Override
    public @Nullable MsScan getScan(int scanNumber) {
	for (MsScan scan : scans) {
	    if (scan.getScanNumber() == scanNumber)
		return scan;
	}
	return null;
    }

    @Override
    public @Nonnull MsScan[] getScans(int msLevel) {
	return getScans(msLevel, Range.all());
    }

    @Override
    public @Nonnull MsScan[] getScans(int msLevel,
	    @Nonnull Range<Double> rtRange) {
	ArrayList<MsScan> eligibleScans = new ArrayList<MsScan>();
	for (MsScan scan : scans) {
	    if ((scan.getMSLevel() == msLevel)
		    && (rtRange.contains(scan.getRetentionTime())))
		eligibleScans.add(scan);
	}
	MsScan scansArray[] = eligibleScans.toArray(new MsScan[0]);
	Arrays.sort(scansArray, new ScanSorter());
	return scansArray;
    }

    @Override
    public @Nonnull int[] getMSLevels() {
	Set<Integer> msLevelsSet = new HashSet<Integer>();
	for (MsScan scan : scans) {
	    msLevelsSet.add(scan.getMSLevel());
	}
	int[] msLevels = CollectionUtils.toIntArray(msLevelsSet);
	Arrays.sort(msLevels);
	return msLevels;
    }

    @Override
    public @Nonnull Range<Double> getDataMZRange() {
	return getDataMZRange(0);
    }

    @Override
    public @Nonnull Range<Double> getDataMZRange(int msLevel) {
	Range<Double> mzRange = null;
	for (MsScan scan : scans) {
	    if ((scan.getMSLevel() == msLevel) || (msLevel == 0)) {
		final Range<Double> scanMzRange = scan.getMZRange();
		if (mzRange == null)
		    mzRange = scanMzRange;
		else
		    mzRange = mzRange.span(scanMzRange);
	    }
	}
	if (mzRange == null)
	    mzRange = Range.singleton(0d);
	return mzRange;
    }

    @Override
    public @Nonnull Range<Double> getDataRTRange() {
	return getDataRTRange(0);
    }

    @Override
    public @Nonnull Range<Double> getDataRTRange(int msLevel) {
	Range<Double> rtRange = null;
	for (MsScan scan : scans) {
	    if ((scan.getMSLevel() == msLevel) || (msLevel == 0)) {
		final double scanRT = scan.getRetentionTime();
		final Range<Double> scanRTRange = Range.singleton(scanRT);
		if (rtRange == null)
		    rtRange = scanRTRange;
		else
		    rtRange = rtRange.span(scanRTRange);
	    }
	}
	if (rtRange == null)
	    rtRange = Range.singleton(0d);
	return rtRange;
    }

    @Override
    public @Nonnull String getName() {
	return this.rawDataFileName;
    }

    @Override
    public void setName(@Nonnull String name) {
	this.rawDataFileName = name;
    }

    @Override
    public String toString() {
	return getName();
    }

    @Override
    @Nullable
    public MsScan getMostIntenseScan(int msLevel) {
	MsScan currentHighestScan = null;
	for (MsScan scan : scans) {
	    if ((scan.getMSLevel() == msLevel) || (msLevel == 0)) {
		DataPoint newScanHighestDP = scan.getHighestDataPoint();
		if (newScanHighestDP == null)
		    continue;
		if ((currentHighestScan == null)
			|| (currentHighestScan.getHighestDataPoint() == null)) {
		    currentHighestScan = scan;
		} else {
		    if (newScanHighestDP.getIntensity() > currentHighestScan
			    .getHighestDataPoint().getIntensity()) {
			currentHighestScan = scan;
		    }
		}
	    }
	}
	return currentHighestScan;
    }

    @Override
    public Collection<MsScan> scans() {
	return scans;
    }

}