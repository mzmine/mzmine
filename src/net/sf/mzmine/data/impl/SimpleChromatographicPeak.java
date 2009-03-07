/*
 * Copyright 2006-2009 The MZmine 2 Development Team
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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.data.impl;

import java.util.Arrays;

import net.sf.mzmine.data.ChromatographicPeak;
import net.sf.mzmine.data.MzDataPoint;
import net.sf.mzmine.data.PeakStatus;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.util.PeakUtils;
import net.sf.mzmine.util.Range;

/**
 * This class is a simple implementation of the peak interface.
 */
public class SimpleChromatographicPeak implements ChromatographicPeak {

	private PeakStatus peakStatus;
	private RawDataFile dataFile;

	private int scanNumbers[];
	private MzDataPoint dataPointsPerScan[];

	// M/Z, RT, Height and Area
	private double mz, rt, height, area;

	// Boundaries of the peak raw data points
	private Range rtRange, mzRange, intensityRange;

	// Number of representative scan
	private int representativeScan;

	// Number of most intense fragment scan
	private int fragmentScanNumber;

	/**
	 * Initializes a new peak using given values
	 * 
	 */
	public SimpleChromatographicPeak(RawDataFile dataFile, double MZ,
			double RT, double height, double area, int[] scanNumbers,
			MzDataPoint[] dataPointsPerScan, PeakStatus peakStatus,
			int representativeScan, int fragmentScanNumber, Range rtRange,
			Range mzRange, Range intensityRange) {

		if (dataPointsPerScan.length == 0) {
			throw new IllegalArgumentException(
					"Cannot create a SimplePeak instance with no data points");
		}

		this.dataFile = dataFile;
		this.mz = MZ;
		this.rt = RT;
		this.height = height;
		this.area = area;
		this.scanNumbers = scanNumbers;
		this.dataPointsPerScan = dataPointsPerScan;
		this.peakStatus = peakStatus;
		this.representativeScan = representativeScan;
		this.fragmentScanNumber = fragmentScanNumber;
		this.rtRange = rtRange;
		this.mzRange = mzRange;
		this.intensityRange = intensityRange;

	}

	/**
	 * Copy constructor
	 */
	public SimpleChromatographicPeak(ChromatographicPeak p) {

		this.dataFile = p.getDataFile();

		this.mz = p.getMZ();
		this.rt = p.getRT();
		this.height = p.getHeight();
		this.area = p.getArea();

		this.rtRange = p.getRawDataPointsRTRange();
		this.mzRange = p.getRawDataPointsMZRange();
		this.intensityRange = p.getRawDataPointsIntensityRange();

		this.scanNumbers = p.getScanNumbers();
		this.dataPointsPerScan = new MzDataPoint[scanNumbers.length];

		for (int i = 0; i < scanNumbers.length; i++) {
			dataPointsPerScan[i] = p.getDataPoint(scanNumbers[i]);
		}

		this.peakStatus = p.getPeakStatus();

		this.representativeScan = p.getRepresentativeScanNumber();
		this.fragmentScanNumber = p.getMostIntenseFragmentScanNumber();

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
	public double getMZ() {
		return mz;
	}

	public void setMZ(double mz) {
		this.mz = mz;
	}

	public void setRT(double rt) {
		this.rt = rt;
	}

	/**
	 * This method returns retention time of the peak
	 */
	public double getRT() {
		return rt;
	}

	/**
	 * This method returns the raw height of the peak
	 */
	public double getHeight() {
		return height;
	}

	/**
	 * @param height
	 *            The height to set.
	 */
	public void setHeight(double height) {
		this.height = height;
	}

	/**
	 * This method returns the raw area of the peak
	 */
	public double getArea() {
		return area;
	}

	/**
	 * @param area
	 *            The area to set.
	 */
	public void setArea(double area) {
		this.area = area;
	}

	/**
	 * This method returns numbers of scans that contain this peak
	 */
	public int[] getScanNumbers() {
		return scanNumbers;
	}

	/**
	 * This method returns a representative datapoint of this peak in a given
	 * scan
	 */
	public MzDataPoint getDataPoint(int scanNumber) {
		int index = Arrays.binarySearch(scanNumbers, scanNumber);
		if (index < 0)
			return null;
		return dataPointsPerScan[index];
	}

	/**
	 * @see net.sf.mzmine.data.ChromatographicPeak#getDataFile()
	 */
	public RawDataFile getDataFile() {
		return dataFile;
	}

	/**
	 * @see net.sf.mzmine.data.ChromatographicPeak#setDataFile()
	 */
	public void setDataFile(RawDataFile dataFile) {
		this.dataFile = dataFile;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return PeakUtils.peakToString(this);
	}

	/**
	 * @see net.sf.mzmine.data.ChromatographicPeak#getRawDataPointsIntensityRange()
	 */
	public Range getRawDataPointsIntensityRange() {
		return intensityRange;
	}

	/**
	 * @see net.sf.mzmine.data.ChromatographicPeak#getRawDataPointsMZRange()
	 */
	public Range getRawDataPointsMZRange() {
		return mzRange;
	}

	/**
	 * @see net.sf.mzmine.data.ChromatographicPeak#getRawDataPointsRTRange()
	 */
	public Range getRawDataPointsRTRange() {
		return rtRange;
	}

	/**
	 * @see net.sf.mzmine.data.ChromatographicPeak#getRepresentativeScanNumber()
	 */
	public int getRepresentativeScanNumber() {
		return representativeScan;
	}

	/**
	 * 
	 */
	public int getMostIntenseFragmentScanNumber() {
		return fragmentScanNumber;
	}

}
