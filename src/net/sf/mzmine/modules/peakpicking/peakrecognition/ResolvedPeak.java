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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.peakpicking.peakrecognition;

import java.util.Hashtable;

import net.sf.mzmine.data.ChromatographicPeak;
import net.sf.mzmine.data.MzDataPoint;
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

	private Hashtable<Integer, MzDataPoint> dataPointMap;

	// Top intensity scan, fragment scan
	private int representativeScan, fragmentScan;

	// Ranges of raw data points
	private Range rawDataPointsIntensityRange, rawDataPointsMZRange,
			rawDataPointsRTRange;

	/**
	 * Initializes this peak using data points from a given chromatogram -
	 * regionStart marks the index of the first data point (inclusive),
	 * regionEnd marks the index of the last data point (inclusive)
	 */
	public ResolvedPeak(ChromatographicPeak chromatogram, int regionStart,
			int regionEnd) {

		this.dataFile = chromatogram.getDataFile();

		// Make an array of scan numbers of this peak
		scanNumbers = new int[regionEnd - regionStart + 1];
		System.arraycopy(dataFile.getScanNumbers(1), regionStart, scanNumbers,
				0, regionEnd - regionStart + 1);

		dataPointMap = new Hashtable<Integer, MzDataPoint>();

		// We keep the m/z range specified by the chromatogram, instead of
		// determining it from the m/z data points. The reason is that in
		// continuous raw data, each m/z peak has a width. That width is
		// remembered in chromatogram.getRawDataPointsMZRange()
		rawDataPointsMZRange = chromatogram.getRawDataPointsMZRange();

		// Set raw data point ranges, height, rt and representative scan
		height = Double.MIN_VALUE;
		double allMzValues[] = new double[scanNumbers.length];
		for (int i = 0; i < scanNumbers.length; i++) {

			MzDataPoint dp = chromatogram.getDataPoint(scanNumbers[i]);
			if (dp == null)
				continue;

			dataPointMap.put(scanNumbers[i], dp);

			allMzValues[i] = dp.getMZ();

			if (rawDataPointsIntensityRange == null) {
				rawDataPointsIntensityRange = new Range(dp.getIntensity());
				rawDataPointsRTRange = new Range(dataFile.getScan(
						scanNumbers[i]).getRetentionTime());
			} else {
				rawDataPointsRTRange.extendRange(dataFile.getScan(
						scanNumbers[i]).getRetentionTime());
				rawDataPointsIntensityRange.extendRange(dp.getIntensity());
			}

			if (height < dp.getIntensity()) {
				height = dp.getIntensity();
				rt = dataFile.getScan(scanNumbers[i]).getRetentionTime();
				representativeScan = scanNumbers[i];
			}
		}

		// Calculate median m/z
		mz = MathUtils.calcQuantile(allMzValues, 0.5f);

		// Update area
		area = 0;
		for (int i = 1; i < scanNumbers.length; i++) {
			MzDataPoint previousPeak = dataPointMap.get(scanNumbers[i - 1]);
			MzDataPoint currentPeak = dataPointMap.get(scanNumbers[i]);
			double previousRT = dataFile.getScan(scanNumbers[i - 1])
					.getRetentionTime();
			double currentRT = dataFile.getScan(scanNumbers[i])
					.getRetentionTime();
			double previousHeight = previousPeak != null ? previousPeak
					.getIntensity() : 0;
			double currentHeight = currentPeak != null ? currentPeak
					.getIntensity() : 0;
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
			if (scan.getBasePeak() == null)
				continue;
			if (rawDataPointsMZRange.contains(scan.getPrecursorMZ())) {
				if ((fragmentScan == -1)
						|| (scan.getBasePeak().getIntensity() > topBasePeak)) {
					fragmentScan = number;
					topBasePeak = scan.getBasePeak().getIntensity();
				}
			}
		}

	}

	public MzDataPoint getDataPoint(int scanNumber) {
		return dataPointMap.get(scanNumber);
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
