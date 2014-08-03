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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.MassSpectrum;
import net.sf.mzmine.util.DataPointSorter;
import net.sf.mzmine.util.ScanUtils;
import net.sf.mzmine.util.SortingDirection;
import net.sf.mzmine.util.SortingProperty;

import com.google.common.collect.Range;

/**
 * Simple implementation of the MassSpectrum interface, which stores its data in
 * a data store.
 */
abstract class SpectrumImpl implements MassSpectrum {

    private final @Nonnull DataPointStoreImpl dataPointStore;

    private int dataStoreId = -1;
    private int numberOfDataPoints;
    private Range<Double> mzRange;
    private boolean isCentroided;
    private DataPoint highestDataPoint;

    SpectrumImpl(@Nonnull DataPointStoreImpl dataPointStore) {
	this.dataPointStore = dataPointStore;
    }

    @Override
    @Nonnull
    public Range<Double> getMZRange() {
	return mzRange;
    }

    @Override
    @Nullable
    public DataPoint getHighestDataPoint() {
	return highestDataPoint;
    }

    @Override
    public boolean isCentroided() {
	return isCentroided;
    }

    @Override
    public int getNumberOfDataPoints() {
	return numberOfDataPoints;
    }

    @Override
    public synchronized @Nonnull DataPoint[] getDataPoints() throws IOException {
	DataPoint storedData[] = dataPointStore.readDataPoints(dataStoreId);
	return storedData;
    }

    @Override
    @Nonnull
    public DataPoint[] getDataPointsByMass(@Nonnull Range<Double> mzRange)
	    throws IOException {
	final DataPoint[] dataPoints = getDataPoints();
	int startIndex, endIndex;
	for (startIndex = 0; startIndex < dataPoints.length; startIndex++) {
	    if (dataPoints[startIndex].getMZ() >= mzRange.lowerEndpoint())
		break;
	}

	for (endIndex = startIndex; endIndex < dataPoints.length; endIndex++) {
	    if (dataPoints[endIndex].getMZ() > mzRange.upperEndpoint())
		break;
	}

	DataPoint pointsWithinRange[] = new DataPoint[endIndex - startIndex];

	// Copy the relevant points
	System.arraycopy(dataPoints, startIndex, pointsWithinRange, 0, endIndex
		- startIndex);

	return pointsWithinRange;
    }

    @Override
    @Nonnull
    public DataPoint[] getDataPointsOverIntensity(double intensity)
	    throws IOException {
	DataPoint[] dataPoints = getDataPoints();
	int index;
	ArrayList<DataPoint> points = new ArrayList<DataPoint>();

	for (index = 0; index < dataPoints.length; index++) {
	    if (dataPoints[index].getIntensity() >= intensity)
		points.add(dataPoints[index]);
	}

	DataPoint pointsOverIntensity[] = points.toArray(new DataPoint[0]);

	return pointsOverIntensity;
    }

    @Override
    public synchronized void setDataPoints(@Nonnull DataPoint[] newDataPoints) throws IOException {
	
	// Remove previous data, if any
	if (dataStoreId != -1) {
	    dataPointStore.removeStoredDataPoints(dataStoreId);
	}

	// Sort the data points by m/z, because getDataPoints() guarantees the
	// returned array is sorted by m/z
	Arrays.sort(newDataPoints, new DataPointSorter(SortingProperty.MZ,
		SortingDirection.Ascending));

	dataStoreId = dataPointStore.storeDataPoints(newDataPoints);
	
	numberOfDataPoints = newDataPoints.length;
	mzRange = ScanUtils.findMzRange(newDataPoints);
	highestDataPoint = ScanUtils.findTopDataPoint(newDataPoints);
	isCentroided = ScanUtils.isCentroided(newDataPoints);
	
    }

}
