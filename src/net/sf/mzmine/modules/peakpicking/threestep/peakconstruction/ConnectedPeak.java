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

package net.sf.mzmine.modules.peakpicking.threestep.peakconstruction;

import java.text.Format;
import java.util.TreeMap;
import java.util.Vector;

import net.sf.mzmine.data.DataPoint;
import net.sf.mzmine.data.Peak;
import net.sf.mzmine.data.PeakStatus;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.peakpicking.threestep.xicconstruction.scoreconnector.ConnectedMzPeak;
import net.sf.mzmine.util.CollectionUtils;
import net.sf.mzmine.util.MathUtils;
import net.sf.mzmine.util.PeakUtils;
import net.sf.mzmine.util.Range;

/**
 * This class is an implementation of the peak interface for SimpleConnector
 * Peak Builder
 * 
 */

public class ConnectedPeak implements Peak {

	private PeakStatus peakStatus = PeakStatus.DETECTED;

	// These elements are used to construct the Peak.
	private TreeMap<Integer, ConnectedMzPeak> datapointsMap;
	private Vector<Float> datapointsMZs;

	// Raw data file, M/Z, RT, Height and Area
	private RawDataFile dataFile;
	private float mz, rt, height, area, previousRetentionTime;
	private int lastValidIndex = 0;

	// Characteristics of the peak
	private Range mzRange, intensityRange, rtRange;


	/**
	 * Initializes this Peak with one MzPeak
	 */
	public ConnectedPeak(RawDataFile dataFile, ConnectedMzPeak mzValue) {
		this.dataFile = dataFile;
		datapointsMap = new TreeMap<Integer, ConnectedMzPeak>();
		datapointsMZs = new Vector<Float>();

		// We map this MzPeak with the scan number as a key due construction
		// peak purpose.
		lastValidIndex = mzValue.getScan().getScanNumber();
		datapointsMap.put(lastValidIndex, mzValue);

		// Initial characteristics of our peak with just one point (MzPeak).
		rt = mzValue.getScan().getRetentionTime();
		previousRetentionTime = rt;
		mz = mzValue.getMzPeak().getMZ();
		height = mzValue.getMzPeak().getIntensity();

		// Initial area of our peak is just one point.
		area = 0.0f;

		// Used in calculation of median MZ
		datapointsMZs.add(mz);

		// Initial ranges of our peak using all values from MzPeak
		rtRange = new Range(rt);

		DataPoint[] mzPeakRawDataPoints = mzValue.getMzPeak()
				.getRawDataPoints();

		for (DataPoint dp : mzPeakRawDataPoints) {
			if (mzRange == null)
				mzRange = new Range(dp.getMZ());
			else
				mzRange.extendRange(dp.getMZ());
			if (intensityRange == null)
				intensityRange = new Range(dp.getIntensity());
			else
				intensityRange.extendRange(dp.getIntensity());
		}

	}

	/**
	 * This method adds a MzPeak to this Peak. All values of this Peak (rt, m/z,
	 * intensity and ranges) are upgraded
	 * 
	 * 
	 * @param mzValue
	 */
	public void addMzPeak(ConnectedMzPeak mzValue) {

		// Update construction time variables
		if (height <= mzValue.getMzPeak().getIntensity()) {
			height = mzValue.getMzPeak().getIntensity();
			rt = mzValue.getScan().getRetentionTime();
		}

		// Calculate median MZ
		datapointsMZs.add(mzValue.getMzPeak().getMZ());

		mz = MathUtils.calcQuantile(
				CollectionUtils.toFloatArray(datapointsMZs), 0.5f);

		rtRange.extendRange(mzValue.getScan().getRetentionTime());

		DataPoint[] mzPeakRawDataPoints = mzValue.getMzPeak()
				.getRawDataPoints();

		for (DataPoint dp : mzPeakRawDataPoints) {
			mzRange.extendRange(dp.getMZ());
			intensityRange.extendRange(dp.getIntensity());
		}

		// Use the last added MzPeak to calculate the area of the peak.
		ConnectedMzPeak lastAddedMzPeak = datapointsMap.get(lastValidIndex);

		float rtDifference = mzValue.getScan().getRetentionTime()
				- previousRetentionTime;

		// intensity at the beginning of the interval
		float intensityStart = lastAddedMzPeak.getMzPeak().getIntensity();

		// intensity at the end of the interval
		float intensityEnd = mzValue.getMzPeak().getIntensity();

		// calculate area of the interval
		area += (rtDifference * (intensityStart + intensityEnd) / 2);

		// Add MzPeak
		lastValidIndex = mzValue.getScan().getScanNumber();
		datapointsMap.put(lastValidIndex, mzValue);
		previousRetentionTime = mzValue.getScan().getRetentionTime();
		

	}
	
	public void addMzPeak(int scanNumber) {
		datapointsMap.put(scanNumber, null);
	}


	/**
	 * This method returns the status of the peak
	 */
	public PeakStatus getPeakStatus() {
		return peakStatus;
	}

	/**
	 * This method returns M/Z value of the peak
	 */
	public float getMZ() {
		return mz;
	}

	/**
	 * This method returns retention time of the peak
	 */
	public float getRT() {
		return rt;
	}

	/**
	 * This method returns the height of the peak
	 */
	public float getHeight() {
		return height;
	}

	/**
	 * This method returns the area of the peak
	 */
	public float getArea() {
		return area;
	}

	/**
	 * This method returns numbers of scans that contain this peak
	 */
	public int[] getScanNumbers() {
		return CollectionUtils.toIntArray(datapointsMap.keySet());
	}

	/**
	 * This method returns a representative data point of this peak in a given
	 * scan
	 */
	public DataPoint getDataPoint(int scanNumber) {
        if (datapointsMap.get(scanNumber) == null) return null;
      
		return datapointsMap.get(scanNumber).getMzPeak();
	}

	/**
	 * This method returns an array of all raw data point of this peak in a
	 * given scan
	 */
	public DataPoint[] getRawDataPoints(int scanNumber) {
		return datapointsMap.get(scanNumber).getMzPeak().getRawDataPoints();
	}

	/**
	 * This method returns the intensity range of this peak
	 * 
	 */
	public Range getRawDataPointsIntensityRange() {
		return intensityRange;
	}

	/**
	 * This method returns the m/z range of this peak
	 * 
	 */
	public Range getRawDataPointsMZRange() {
		return mzRange;
	}

	/**
	 * This method returns the retention time range of this peak
	 * 
	 */
	public Range getRawDataPointsRTRange() {
		return rtRange;
	}


	/**
	 * @see net.sf.mzmine.data.Peak#getDataFile()
	 */
	public RawDataFile getDataFile() {
		return dataFile;
	}

	/**
	 * This method returns a string with the basic information that defines this
	 * peak
	 * 
	 * @return String information
	 */
	public String toString() {
        return PeakUtils.peakToString(this);
	}


}
