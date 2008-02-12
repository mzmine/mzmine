/*
 * Copyright 2006-2008 The MZmine Development Team
 * 
 * This file is part of MZmine.
 * 
 * MZmine is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.peakpicking.anothercentroid;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.TreeSet;

import net.sf.mzmine.data.DataPoint;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.modules.peakpicking.anothercentroid.DataPointSorter.SortingDirection;
import net.sf.mzmine.modules.peakpicking.anothercentroid.DataPointSorter.SortingProperty;

class ConstructionIsotopePattern {

	private TreeSet<DataPoint> addedDataPoints;
	private int chargeState;
	private float mzTolerance;
	
	private float[] XIC;

	ConstructionIsotopePattern(int chargeState, float mzTolerance) {
		this.chargeState = chargeState;
		this.mzTolerance = mzTolerance;
		
		addedDataPoints = new TreeSet<DataPoint>(new DataPointSorter(
				SortingProperty.MZ, SortingDirection.ASCENDING));
		
	}

	protected void addDataPoint(DataPoint dataPoint) {
		addedDataPoints.add(dataPoint);

	}
	
	protected void addDataPoints(DataPoint[] dataPoints) {
		for (DataPoint dataPoint : dataPoints) {
			addDataPoint(dataPoint);
		}
	}
	
	protected void removeDataPoints() {
		addedDataPoints.clear();
	}

	protected DataPoint[] getDataPoints() {
		return addedDataPoints.toArray(new DataPoint[0]);
	}

	protected DataPoint getMonoisotopicDataPoint() {
		return addedDataPoints.first();
	}

	protected int getNumberOfDataPoints() {
		return addedDataPoints.size();
	}

	protected int getChargeState() {
		return chargeState;
	}

	protected boolean isSimilar(ConstructionIsotopePattern anotherPattern) {

		// Criteria for similarity: matching monoisotopic m/z and charge state

		if (this.getChargeState() != anotherPattern.getChargeState())
			return false;

		if (Math.abs(this.getMonoisotopicDataPoint().getMZ() - anotherPattern
				.getMonoisotopicDataPoint().getMZ()) > mzTolerance)
			return false;

		return true;

	}
	
	protected Bin[] initializeXIC(int numberOfScans) {
		XIC = new float[numberOfScans];
		
		ArrayList<Bin> bins = new ArrayList<Bin>();
	
		Iterator<DataPoint> dataPointsIter = addedDataPoints.iterator();
		while (dataPointsIter.hasNext()) {
			DataPoint dataPoint = dataPointsIter.next();
			bins.add(new Bin(this, dataPoint.getMZ()-mzTolerance, dataPoint.getMZ()+mzTolerance));
		}
		
		return bins.toArray(new Bin[0]);
		
	}
	
	protected void addIntensity(int scanCount, float intensity) {
			XIC[scanCount] += intensity;
	}
	
	protected float[] getXIC() {
		return XIC;
	}

}
