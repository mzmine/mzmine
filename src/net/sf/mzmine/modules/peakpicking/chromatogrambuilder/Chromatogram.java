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

package net.sf.mzmine.modules.peakpicking.chromatogrambuilder;

import java.util.TreeMap;
import java.util.Vector;

import net.sf.mzmine.data.ChromatographicPeak;
import net.sf.mzmine.data.DataPoint;
import net.sf.mzmine.data.PeakStatus;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.impl.SimpleDataPoint;
import net.sf.mzmine.main.mzmineclient.MZmineCore;
import net.sf.mzmine.util.CollectionUtils;
import net.sf.mzmine.util.MathUtils;
import net.sf.mzmine.util.Range;
import net.sf.mzmine.util.ScanUtils;

/**
 * Chromatogram implementing ChromatographicPeak. The getScanNumbers() method
 * returns all scans in the data file MS level 1, which means the Chromatogram
 * always covers the whole retention time range.
 */
public class Chromatogram implements ChromatographicPeak {

	// Data file of this chromatogram
	private RawDataFile dataFile;

	// Data points of the chromatogram (map of scan number -> m/z peak)
	private TreeMap<Integer, DataPoint> dataPointsMap;

	// Chromatogram m/z, RT, height, area
	private double mz, rt, height, area;

	// Top intensity scan, fragment scan
	private int representativeScan = -1, fragmentScan = -1;

	// Ranges of raw data points
	private Range rawDataPointsIntensityRange, rawDataPointsMZRange;

	// A set of scan numbers of a segment which is currently being connected
	private Vector<Integer> buildingSegment;

	// Number of connected segments, which have been committed by
	// commitBuildingSegment()
	private int numOfCommittedSegments = 0;

	/**
	 * Initializes this Chromatogram
	 */
	public Chromatogram(RawDataFile dataFile) {
		this.dataFile = dataFile;
		dataPointsMap = new TreeMap<Integer, DataPoint>();
		buildingSegment = new Vector<Integer>(128);
	}

	/**
	 * This method adds a MzPeak to this Chromatogram. All values of this
	 * Chromatogram (rt, m/z, intensity and ranges) are updated on request
	 * 
	 * @param mzValue
	 */
	public void addMzPeak(int scanNumber, MzPeak mzValue) {
		dataPointsMap.put(scanNumber, mzValue);
		buildingSegment.add(scanNumber);
	}

	public DataPoint getDataPoint(int scanNumber) {
		return dataPointsMap.get(scanNumber);
	}

	/**
	 * Returns m/z value of last added data point
	 */
	public DataPoint getLastMzPeak() {
		return dataPointsMap.get(dataPointsMap.lastKey());
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
		return "Chromatogram " + MZmineCore.getMZFormat().format(mz) + " m/z";
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
		return dataFile.getDataRTRange(1);
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

	public void finishChromatogram() {

		int allScanNumbers[] = CollectionUtils.toIntArray(dataPointsMap
				.keySet());

		// Calculate median m/z
		double allMzValues[] = new double[allScanNumbers.length];
		for (int i = 0; i < allScanNumbers.length; i++) {
			allMzValues[i] = dataPointsMap.get(allScanNumbers[i]).getMZ();
		}
		mz = MathUtils.calcQuantile(allMzValues, 0.5f);

		// Update raw data point ranges, height, rt and representative scan
		height = Double.MIN_VALUE;
		for (int i = 0; i < allScanNumbers.length; i++) {

			MzPeak mzPeak = (MzPeak) dataPointsMap.get(allScanNumbers[i]);

			// Replace the MzPeak instance with an instance of SimpleDataPoint,
			// to reduce the memory usage. After we finish this Chromatogram, we
			// don't need the additional data provided by the MzPeak
			SimpleDataPoint newDataPoint = new SimpleDataPoint(mzPeak);
			dataPointsMap.put(allScanNumbers[i], newDataPoint);

			if (i == 0) {
				rawDataPointsIntensityRange = new Range(mzPeak.getIntensity());
				rawDataPointsMZRange = new Range(mzPeak.getMZ());
			}
			for (DataPoint dp : mzPeak.getRawDataPoints()) {
				rawDataPointsIntensityRange.extendRange(dp.getIntensity());
				rawDataPointsMZRange.extendRange(dp.getMZ());
			}

			if (height < mzPeak.getIntensity()) {
				height = mzPeak.getIntensity();
				rt = dataFile.getScan(allScanNumbers[i]).getRetentionTime();
				representativeScan = allScanNumbers[i];
			}
		}

		// Update area
		area = 0;
		for (int i = 1; i < allScanNumbers.length; i++) {
			double previousRT = dataFile.getScan(allScanNumbers[i - 1])
					.getRetentionTime();
			double currentRT = dataFile.getScan(allScanNumbers[i])
					.getRetentionTime();
			double previousHeight = dataPointsMap.get(allScanNumbers[i - 1])
					.getIntensity();
			double currentHeight = dataPointsMap.get(allScanNumbers[i])
					.getIntensity();
			area += (currentRT - previousRT) * (currentHeight + previousHeight)
					/ 2;
		}

		// Update fragment scan
		fragmentScan = ScanUtils.findBestFragmentScan(dataFile,
				dataFile.getDataRTRange(1), rawDataPointsMZRange);
		
		// Discard the buildingSegment
		buildingSegment = null;

	}

	public double getBuildingSegmentLength() {
		if (buildingSegment.size() < 2)
			return 0;
		int firstScan = buildingSegment.firstElement();
		int lastScan = buildingSegment.lastElement();
		double firstRT = dataFile.getScan(firstScan).getRetentionTime();
		double lastRT = dataFile.getScan(lastScan).getRetentionTime();
		return (lastRT - firstRT);
	}

	public int getNumberOfCommittedSegments() {
		return numOfCommittedSegments;
	}

	public void removeBuildingSegment() {
		for (int scanNumber : buildingSegment)
			dataPointsMap.remove(scanNumber);
		buildingSegment.clear();
	}

	public void commitBuildingSegment() {
		buildingSegment.clear();
		numOfCommittedSegments++;
	}

}
