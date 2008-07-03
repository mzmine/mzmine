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

package net.sf.mzmine.modules.peakpicking.threestep.xicconstruction;

import java.util.TreeMap;
import java.util.Vector;

import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.modules.peakpicking.threestep.xicconstruction.scoreconnector.ConnectedMzPeak;
import net.sf.mzmine.util.CollectionUtils;
import net.sf.mzmine.util.MathUtils;
import net.sf.mzmine.util.Range;

/**
 * This class is an implementation of the peak interface for SimpleConnector
 * Peak Builder
 * 
 */

public class Chromatogram {

	// These elements are used to construct the Peak.
	private TreeMap<Integer, ConnectedMzPeak> datapointsMap;
	private Vector<Float> datapointsMZs;

	// Raw data file, M/Z, RT, Height and Area
	private float mz, height;

	// This is used for constructing the peak
	private boolean growing = false, previousConnectedMzPeaks = false;
	
	// This dataFile is used to know the complete range of the chromatogram
	private RawDataFile dataFile;

	/**
	 * Initializes this Peak with one MzPeak
	 */
	public Chromatogram(RawDataFile dataFile, ConnectedMzPeak mzValue) {
		
		this.dataFile = dataFile;

		datapointsMap = new TreeMap<Integer, ConnectedMzPeak>();
		datapointsMZs = new Vector<Float>();

		// We map this MzPeak with the scan number as a key due construction
		// peak purpose.
		datapointsMap.put(mzValue.getScan().getScanNumber(), mzValue);

		// Initial characteristics of our chromatogram with just one point
		// (MzPeak).
		mz = mzValue.getMzPeak().getMZ();
		height = mzValue.getMzPeak().getIntensity();

		// Used in calculation of median MZ
		datapointsMZs.add(mz);

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
		}

		if (mzValue.getMzPeak().getIntensity() == 0)
			previousConnectedMzPeaks = true;

		// Calculate median MZ
		datapointsMZs.add(mzValue.getMzPeak().getMZ());

		mz = MathUtils.calcQuantile(
				CollectionUtils.toFloatArray(datapointsMZs), 0.5f);

		// Add MzPeak
		datapointsMap.put(mzValue.getScan().getScanNumber(), mzValue);
		growing = true;

	}

	/**
	 * This method returns M/Z value of the peak
	 */
	public float getMZ() {
		return mz;
	}

	/**
	 * This method returns M/Z value of the peak
	 */
	public float getIntensity() {
		return height;
	}

	
	/**
	 * This method returns the connectedMzPeak in given scan number
	 */
	public ConnectedMzPeak getConnectedMzPeak(int scanNumber) {
		return datapointsMap.get(scanNumber);
	}
	
	/**
	 * This method returns the retention time range of the last connected
	 * MzPeaks
	 * 
	 */
	public Range getLastConnectedMzPeaksRTRange() {

		ConnectedMzPeak[] lastConnectedMzPeaks = getLastConnectedMzPeaks();

		if (lastConnectedMzPeaks.length != 0) {

			Range lastRTRange = new Range(lastConnectedMzPeaks[0].getScan()
					.getRetentionTime());

			if (lastConnectedMzPeaks.length > 1) {
				float currentRT;
				for (int i = 1; i < lastConnectedMzPeaks.length; i++) {
					currentRT = lastConnectedMzPeaks[i].getScan()
							.getRetentionTime();
					lastRTRange.extendRange(currentRT);
				}
			}

			return lastRTRange;

		} else
			return new Range(0, 0);

	}

	/**
	 * This method returns the last group of MzPeaks connected in this
	 * chromatogram. The order of the array is ascend according with the number
	 * of the scans.
	 * 
	 * @return Array MzPeak
	 */
	public ConnectedMzPeak[] getLastConnectedMzPeaks() {

		ConnectedMzPeak[] allConnectedMzPeaks = datapointsMap.values().toArray(
				new ConnectedMzPeak[0]);
		Vector<ConnectedMzPeak> lastConnectedMzPeaks = new Vector<ConnectedMzPeak>();

		for (ConnectedMzPeak mzValue : allConnectedMzPeaks) {
			if (mzValue.getMzPeak().getIntensity() > 0)
				lastConnectedMzPeaks.add(mzValue);
			else
				lastConnectedMzPeaks.clear();
		}

		return lastConnectedMzPeaks.toArray(new ConnectedMzPeak[0]);

	}

	/**
	 * This method returns the last group of MzPeaks connected in this
	 * chromatogram. The order of the array is ascend according with the number
	 * of the scans.
	 * 
	 * @return Array MzPeak
	 */
	public ConnectedMzPeak[] getConnectedMzPeaks() {
		return datapointsMap.values().toArray(new ConnectedMzPeak[0]);
	}

	public RawDataFile getDataFile() {
		return dataFile;
	}
	
	public void removeLastConnectedMzPeaks() {
		ConnectedMzPeak[] lastConnectedMzPeaks = getLastConnectedMzPeaks();
		for (ConnectedMzPeak mzValue : lastConnectedMzPeaks) {
			Scan scan = mzValue.getScan();
			datapointsMap.remove(scan.getScanNumber());
		}
	}
	
	public void removeConnectedMzPeak(int scanNumber){
		datapointsMap.remove(scanNumber);
	}

	public boolean hasPreviousConnectedMzPeaks() {
		return previousConnectedMzPeaks;
	}

	/**
	 * This method returns the status of growing's flag.
	 * 
	 * @return boolean growing
	 */
	public boolean isGrowing() {
		return growing;
	}

	public boolean isLastConnectedMzPeakZero() {
		ConnectedMzPeak lastConnectedMzPeak = datapointsMap.get(datapointsMap
				.lastKey());
		if (lastConnectedMzPeak.getMzPeak().getIntensity() == 0)
			return true;
		else
			return false;
	}

	/**
	 * This method sets the growing's flag to false.
	 * 
	 */
	public void resetGrowingState() {
		growing = false;
	}

	/**
	 * This method returns a string with the basic information that defines this
	 * peak
	 * 
	 * @return String information
	 */
	public String toString() {
		return new String("Chromatogram @ MZ " + mz + " height " + height);
	}

}
