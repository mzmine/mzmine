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

package net.sf.mzmine.modules.peakpicking.chromatogrambuilder.massconnection;

import java.util.TreeMap;

import net.sf.mzmine.data.ChromatographicPeak;
import net.sf.mzmine.data.MzDataPoint;
import net.sf.mzmine.data.MzPeak;
import net.sf.mzmine.data.PeakStatus;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.util.MathUtils;
import net.sf.mzmine.util.Range;

/**
 * Chromatogram implementing ChromatographicPeak, covering the whole retention
 * time range with single m/z value
 * 
 */
public class Chromatogram implements ChromatographicPeak {

	// These elements are used to construct the Peak.
	private TreeMap<Integer, MzPeak> dataPointsMap;

	// Top intensity scan
	private int representativeScan = -1;

	// Peak mz and area
	private double mz, area;

	// Raw data points
	private Range rawDataPointsIntensityRange, rawDataPointsMZRange,
			rawDataPointsRTRange;

	// This dataFile is used to know the complete range of the chromatogram
	private RawDataFile dataFile;

	/**
	 * Initializes this Peak with one MzPeak
	 */
	public Chromatogram(RawDataFile dataFile) {

		this.dataFile = dataFile;

		dataPointsMap = new TreeMap<Integer, MzPeak>();

	}

	/**
	 * This method adds a MzPeak to this Peak. All values of this Peak (rt, m/z,
	 * intensity and ranges) are upgraded
	 * 
	 * 
	 * @param mzValue
	 */
	public void addMzPeak(int scanNumber, MzPeak mzValue) {

		dataPointsMap.put(scanNumber, mzValue);

		// Update raw data point ranges
		if (rawDataPointsIntensityRange == null) {
			rawDataPointsIntensityRange = new Range(mzValue.getIntensity());
			rawDataPointsMZRange = new Range(mzValue.getMZ());
			rawDataPointsRTRange = new Range(dataFile.getScan(scanNumber)
					.getRetentionTime());
		} else {
			rawDataPointsRTRange.extendRange(dataFile.getScan(scanNumber)
					.getRetentionTime());
		}
		for (MzDataPoint dp : mzValue.getRawDataPoints()) {
			rawDataPointsIntensityRange.extendRange(dp.getIntensity());
			rawDataPointsMZRange.extendRange(dp.getMZ());
		}

		// Update height and RT using representative scan number
		if (getHeight() < mzValue.getIntensity()) {
			representativeScan = scanNumber;
		}

		// Update area if we have at least 2 data points
		if (dataPointsMap.size() >= 2) {
			Integer scanNumbers[] = dataPointsMap.keySet().toArray(
					new Integer[0]);
			int previousScan = scanNumbers[scanNumbers.length - 2];
			double previousRT = dataFile.getScan(previousScan)
					.getRetentionTime();
			double currentRT = dataFile.getScan(scanNumber).getRetentionTime();
			double previousHeight = dataPointsMap.get(previousScan)
					.getIntensity();
			double currentHeight = mzValue.getIntensity();
			area += (currentRT - previousRT) * (currentHeight + previousHeight)
					/ 2;
		}

		// Calculate median m/z
		MzPeak allMzPeaks[] = dataPointsMap.values().toArray(new MzPeak[0]);
		double allMzValues[] = new double[dataPointsMap.size()];
		for (int i = 0; i < dataPointsMap.size(); i++) {
			allMzValues[i] = allMzPeaks[i].getMZ();
		}
		mz = MathUtils.calcQuantile(allMzValues, 0.5f);

	}

	/**
	 * This method returns m/z value of the chromatogram
	 */
	public double getMZ() {
		return mz;
	}

	/**
	 * This method returns a string with the basic information that defines this
	 * peak
	 * 
	 * @return String information
	 */
	public String toString() {
		return "Chromatogram @ m/z " + mz;
	}

	public double getArea() {
		return area;
	}

	public double getHeight() {
		if (representativeScan == -1)
			return 0;
		return dataPointsMap.get(representativeScan).getIntensity();
	}

	public int getMostIntenseFragmentScanNumber() {
		int topScanNumber = -1;
		double topBasePeak = 0;
		int[] fragmentScanNumbers = dataFile.getScanNumbers(2);
		for (int number : fragmentScanNumbers) {
			Scan scan = dataFile.getScan(number);
			if (rawDataPointsMZRange.contains(scan.getPrecursorMZ())) {
				if ((topScanNumber == -1)
						|| (scan.getBasePeak().getIntensity() > topBasePeak))
					topScanNumber = number;
			}
		}
		return topScanNumber;
	}

	public MzPeak getMzPeak(int scanNumber) {
		return dataPointsMap.get(scanNumber);
	}

	/**
	 * Returns m/z value of last added data point
	 */
	public MzPeak getLastMzPeak() {
		return dataPointsMap.get(dataPointsMap.lastKey());
	}

	public PeakStatus getPeakStatus() {
		return PeakStatus.DETECTED;
	}

	public double getRT() {
		if (representativeScan == -1)
			return 0;
		return dataFile.getScan(representativeScan).getRetentionTime();
	}

	public Range getRawDataPointsIntensityRange() {
		return rawDataPointsIntensityRange;
	}

	public Range getRawDataPointsMZRange() {
		return rawDataPointsMZRange;
	}

	public Range getRawDataPointsRTRange() {
		return rawDataPointsRTRange;
	}

	public int getRepresentativeScanNumber() {
		return representativeScan;
	}

	public int[] getScanNumbers() {
		return dataFile.getScanNumbers(1);
	}

	public RawDataFile getDataFile() {
		return dataFile;
	}

}
