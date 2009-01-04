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

package net.sf.mzmine.modules.peakpicking.peakrecognition;

import java.util.Hashtable;

import net.sf.mzmine.data.ChromatographicPeak;
import net.sf.mzmine.data.MzDataPoint;
import net.sf.mzmine.data.MzPeak;
import net.sf.mzmine.data.PeakStatus;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.util.MathUtils;
import net.sf.mzmine.util.PeakUtils;
import net.sf.mzmine.util.Range;

/**
 * ResolvedPeak
 * 
 */
public class ResolvedPeak implements ChromatographicPeak {

	// Data file of this chromatogram
	private RawDataFile dataFile;

	// Chromatogram m/z, RT, height, area
	private double mz, rt, height, area;

	// Scan numbers
	private int scanNumbers[];

	private Hashtable<Integer, MzPeak> mzPeaksMap;

	// Top intensity scan, fragment scan
	private int representativeScan, fragmentScan;

	// Ranges of raw data points
	private Range rawDataPointsIntensityRange, rawDataPointsMZRange,
			rawDataPointsRTRange;

	/**
	 * Initializes this peak
	 */
	public ResolvedPeak(RawDataFile dataFile, int scanNumbers[],
			MzPeak mzPeaks[]) {
		
		this.dataFile = dataFile;
		this.scanNumbers = scanNumbers;

		mzPeaksMap = new Hashtable<Integer, MzPeak>();

		// Calculate median m/z
		double allMzValues[] = new double[mzPeaks.length];
		for (int i = 0; i < mzPeaks.length; i++) {
			allMzValues[i] = mzPeaks[i].getMZ();
		}
		mz = MathUtils.calcQuantile(allMzValues, 0.5f);

		// Set raw data point ranges, height, rt and representative scan
		height = Double.MIN_VALUE;
		for (int i = 0; i < mzPeaks.length; i++) {

			if (i == 0) {
				rawDataPointsIntensityRange = new Range(mzPeaks[i]
						.getIntensity());
				rawDataPointsMZRange = new Range(mzPeaks[i].getMZ());
				rawDataPointsRTRange = new Range(dataFile.getScan(
						scanNumbers[i]).getRetentionTime());
			} else {
				rawDataPointsRTRange.extendRange(dataFile.getScan(
						scanNumbers[i]).getRetentionTime());
			}
			for (MzDataPoint dp : mzPeaks[i].getRawDataPoints()) {
				rawDataPointsIntensityRange.extendRange(dp.getIntensity());
				rawDataPointsMZRange.extendRange(dp.getMZ());
			}

			if (height < mzPeaks[i].getIntensity()) {
				height = mzPeaks[i].getIntensity();
				rt = dataFile.getScan(scanNumbers[i]).getRetentionTime();
				representativeScan = scanNumbers[i];
			}
		}

		// Update area
		area = 0;
		for (int i = 1; i < scanNumbers.length; i++) {
			double previousRT = dataFile.getScan(scanNumbers[i - 1])
					.getRetentionTime();
			double currentRT = dataFile.getScan(scanNumbers[i])
					.getRetentionTime();
			double previousHeight = mzPeaks[i - 1].getIntensity();
			double currentHeight = mzPeaks[i].getIntensity();
			area += (currentRT - previousRT) * (currentHeight + previousHeight)
					/ 2;
		}

		// Update fragment scan
		fragmentScan = -1;
		double topBasePeak = 0;
		int[] fragmentScanNumbers = dataFile.getScanNumbers(2,
				rawDataPointsRTRange);
		for (int number : fragmentScanNumbers) {
			Scan scan = dataFile.getScan(number);
			if (rawDataPointsMZRange.contains(scan.getPrecursorMZ())) {
				if ((fragmentScan == -1)
						|| (scan.getBasePeak().getIntensity() > topBasePeak)) {
					fragmentScan = number;
					topBasePeak = scan.getBasePeak().getIntensity();
				}
			}
		}

	}

	public MzPeak getMzPeak(int scanNumber) {
		return mzPeaksMap.get(scanNumber);
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
		return PeakUtils.peakToString(this);
	}

	public double getArea() {
		return area;
	}

	public double getHeight() {
		return height;
	}

	public int getMostIntenseFragmentScanNumber() {
		return fragmentScan;
	}

	public PeakStatus getPeakStatus() {
		return PeakStatus.DETECTED;
	}

	public double getRT() {
		return rt;
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
		return scanNumbers;
	}

	public RawDataFile getDataFile() {
		return dataFile;
	}

}
