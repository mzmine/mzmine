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

package net.sf.mzmine.modules.peakpicking.twostep.massdetection;

import net.sf.mzmine.data.DataPoint;
import net.sf.mzmine.data.Peak;
import net.sf.mzmine.data.PeakStatus;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.util.Range;

/**
 * 
 * This class is only used for visualization terms by MassDetectorSetupDialog.
 * 
 */
class FakePeak implements Peak {

	private DataPoint datapoint;
	private Range mzRange, intensityRange;
	private int scanNumber;

	/**
	 * 
	 * @param scanNumber
	 * @param datapoint
	 */
	public FakePeak(int scanNumber, DataPoint datapoint) {
		super();
		this.datapoint = datapoint;
		this.scanNumber = scanNumber;
		mzRange = new Range(datapoint.getMZ());
		intensityRange = new Range(datapoint.getIntensity());
	}

	public float getArea() {
		return 0;
	}

	public RawDataFile getDataFile() {
		return null;
	}

	public DataPoint getDataPoint(int scanNumber) {
		return datapoint;
	}

	public float getHeight() {
		return 0;
	}

	public float getMZ() {
		return 0;
	}

	public PeakStatus getPeakStatus() {
		return null;
	}

	public float getRT() {
		return 0;
	}

	public DataPoint[] getRawDataPoints(int scanNumber) {
		return null;
	}

	public Range getRawDataPointsIntensityRange() {
		return intensityRange;
	}

	public Range getRawDataPointsMZRange() {
		return mzRange;
	}

	public Range getRawDataPointsRTRange() {
		return null;
	}

	public int[] getScanNumbers() {
		int scanNumbers[] = { scanNumber };
		return scanNumbers;
	}

	public void setMZ(float mz) {
		// TODO Auto-generated method stub
		
	}

}
