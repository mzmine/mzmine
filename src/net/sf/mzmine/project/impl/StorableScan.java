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

package net.sf.mzmine.project.impl;

import java.util.Vector;

import net.sf.mzmine.data.DataPoint;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.data.impl.SimpleDataPoint;
import net.sf.mzmine.util.Range;
import net.sf.mzmine.util.ScanUtils;

/**
 * Implementation of the Scan interface which stores raw data points in a
 * temporary file, accessed by RawDataFileImpl.readFromFloatBufferFile()
 */
public class StorableScan implements Scan {

	private int scanNumber, msLevel, parentScan, fragmentScans[];
	private double precursorMZ;
	private int precursorCharge;
	private double retentionTime;
	private Range mzRange;
	private DataPoint basePeak;
	private double totalIonCurrent;
	private boolean centroided;
	private int scanFileOffset;
	private int numberOfDataPoints;
	private RawDataFileImpl rawDataFile;

	/**
	 * Constructor for creating a storable scan from a given scan
	 */
	public StorableScan(Scan originalScan, RawDataFileImpl rawDataFile,
			int scanFileOffset, int numberOfDataPoints) {

		// save scan data
		this.rawDataFile = rawDataFile;
		this.scanFileOffset = scanFileOffset;
		this.numberOfDataPoints = numberOfDataPoints;

		this.scanNumber = originalScan.getScanNumber();
		this.msLevel = originalScan.getMSLevel();
		this.retentionTime = originalScan.getRetentionTime();
		this.parentScan = originalScan.getParentScanNumber();
		this.precursorMZ = originalScan.getPrecursorMZ();
		this.precursorCharge = originalScan.getPrecursorCharge();
		this.fragmentScans = originalScan.getFragmentScanNumbers();
		this.centroided = originalScan.isCentroided();
		this.mzRange = originalScan.getMZRange();
		this.basePeak = originalScan.getBasePeak();
		this.totalIonCurrent = originalScan.getTIC();

	}

	public StorableScan(RawDataFileImpl rawDataFile, int scanFileOffset,
			int numberOfDataPoints, int scanNumber, int msLevel,
			double retentionTime, int parentScan, double precursorMZ,
			int precursorCharge, int fragmentScans[], boolean centroided) {

		this.rawDataFile = rawDataFile;
		this.scanFileOffset = scanFileOffset;
		this.numberOfDataPoints = numberOfDataPoints;

		this.scanNumber = scanNumber;
		this.msLevel = msLevel;
		this.retentionTime = retentionTime;
		this.parentScan = parentScan;
		this.precursorMZ = precursorMZ;
		this.precursorCharge = precursorCharge;
		this.fragmentScans = fragmentScans;
		this.centroided = centroided;

		DataPoint dataPoints[] = getDataPoints();

		// find m/z range and base peak
		if (dataPoints.length > 0) {

			basePeak = dataPoints[0];
			mzRange = new Range(dataPoints[0].getMZ(), dataPoints[0].getMZ());

			for (DataPoint dp : dataPoints) {

				if (dp.getIntensity() > basePeak.getIntensity())
					basePeak = dp;

				mzRange.extendRange(dp.getMZ());

				totalIonCurrent += dp.getIntensity();

			}

		} else {
			// Empty scan, so no m/z range or base peak
			mzRange = new Range(0, 0);
			basePeak = null;
			totalIonCurrent = 0;
		}

	}

	public void setParameters(int scanFileOffset, int numberOfDataPoints) {
		this.scanFileOffset = scanFileOffset;
		this.numberOfDataPoints = numberOfDataPoints;
	}

	/**
	 * @return Scan's datapoints from temporary file.
	 */
	public DataPoint[] getDataPoints() {

		float floatArray[] = rawDataFile.readFromFloatBufferFile(
				scanFileOffset, numberOfDataPoints * 2);

		DataPoint dataPoints[] = new DataPoint[numberOfDataPoints];

		for (int i = 0; i < numberOfDataPoints; i++) {
			dataPoints[i] = new SimpleDataPoint(floatArray[i * 2],
					floatArray[i * 2 + 1]);
		}

		return dataPoints;

	}

	/**
	 * @return Returns scan datapoints within a given range
	 */
	public DataPoint[] getDataPointsByMass(Range mzRange) {

		DataPoint dataPoints[] = getDataPoints();

		int startIndex, endIndex;
		for (startIndex = 0; startIndex < dataPoints.length; startIndex++) {
			if (dataPoints[startIndex].getMZ() >= mzRange.getMin()) {
				break;
			}
		}

		for (endIndex = startIndex; endIndex < dataPoints.length; endIndex++) {
			if (dataPoints[endIndex].getMZ() > mzRange.getMax()) {
				break;
			}
		}

		DataPoint pointsWithinRange[] = new DataPoint[endIndex - startIndex];

		// Copy the relevant points
		System.arraycopy(dataPoints, startIndex, pointsWithinRange, 0, endIndex
				- startIndex);

		return pointsWithinRange;
	}

	/**
	 * @return Returns scan datapoints over certain intensity
	 */
	public DataPoint[] getDataPointsOverIntensity(double intensity) {
		int index;
		Vector<DataPoint> points = new Vector<DataPoint>();
		DataPoint dataPoints[] = getDataPoints();

		for (index = 0; index < dataPoints.length; index++) {
			if (dataPoints[index].getIntensity() >= intensity) {
				points.add(dataPoints[index]);
			}
		}

		DataPoint pointsOverIntensity[] = points.toArray(new DataPoint[0]);

		return pointsOverIntensity;
	}

	public RawDataFile getDataFile() {
		return rawDataFile;
	}

	/**
	 * @see net.sf.mzmine.data.Scan#getNumberOfDataPoints()
	 */
	public int getNumberOfDataPoints() {
		return numberOfDataPoints;
	}

	/**
	 * @see net.sf.mzmine.data.Scan#getScanNumber()
	 */
	public int getScanNumber() {
		return scanNumber;
	}

	/**
	 * @see net.sf.mzmine.data.Scan#getMSLevel()
	 */
	public int getMSLevel() {
		return msLevel;
	}

	/**
	 * @see net.sf.mzmine.data.Scan#getPrecursorMZ()
	 */
	public double getPrecursorMZ() {
		return precursorMZ;
	}

	/**
	 * @return Returns the precursorCharge.
	 */
	public int getPrecursorCharge() {
		return precursorCharge;
	}

	/**
	 * @see net.sf.mzmine.data.Scan#getScanAcquisitionTime()
	 */
	public double getRetentionTime() {
		return retentionTime;
	}

	/**
	 * @see net.sf.mzmine.data.Scan#getMZRangeMax()
	 */
	public Range getMZRange() {
		return mzRange;
	}

	/**
	 * @see net.sf.mzmine.data.Scan#getBasePeakMZ()
	 */
	public DataPoint getBasePeak() {
		return basePeak;
	}

	/**
	 * @see net.sf.mzmine.data.Scan#getParentScanNumber()
	 */
	public int getParentScanNumber() {
		return parentScan;
	}

	/**
	 * @param parentScan
	 *            The parentScan to set.
	 */
	public void setParentScanNumber(int parentScan) {
		this.parentScan = parentScan;
	}

	/**
	 * @see net.sf.mzmine.data.Scan#getFragmentScanNumbers()
	 */
	public int[] getFragmentScanNumbers() {
		return fragmentScans;
	}

	/**
	 * @param fragmentScans
	 *            The fragmentScans to set.
	 */
	void setFragmentScanNumbers(int[] fragmentScans) {
		this.fragmentScans = fragmentScans;
	}

	/**
	 * @see net.sf.mzmine.data.Scan#isCentroided()
	 */
	public boolean isCentroided() {
		return centroided;
	}

	public double getTIC() {
		return totalIonCurrent;
	}

	public String toString() {
		return ScanUtils.scanToString(this);
	}

}
