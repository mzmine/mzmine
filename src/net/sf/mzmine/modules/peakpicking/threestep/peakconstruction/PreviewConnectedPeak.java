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

import net.sf.mzmine.data.DataPoint;
import net.sf.mzmine.data.Peak;
import net.sf.mzmine.data.PeakStatus;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.util.Range;

public class PreviewConnectedPeak implements Peak {
	
	private Peak peak;

	public PreviewConnectedPeak(Peak originalPeak){
		this.peak = originalPeak;
	}
	
	/**
	 * This method returns a string with the basic information that defines this
	 * peak
	 * 
	 * @return String information
	 */
	public String toString() {
		StringBuffer buf = new StringBuffer();
		Format timeFormat = MZmineCore.getRTFormat();
		buf.append(" peak@");
		buf.append(timeFormat.format(this.getRT()));
		return buf.toString();
	}

	public float getArea() {
		return peak.getArea();
	}

	public RawDataFile getDataFile() {
		return peak.getDataFile();
	}

	public DataPoint getDataPoint(int scanNumber) {
		return peak.getDataPoint(scanNumber);
	}

	public float getHeight() {
		return peak.getHeight();
	}

	public float getMZ() {
		return peak.getMZ();
	}

	public PeakStatus getPeakStatus() {
		return peak.getPeakStatus();
	}

	public float getRT() {
		return peak.getRT();
	}

	public DataPoint[] getRawDataPoints(int scanNumber) {
		return peak.getRawDataPoints(scanNumber);
	}

	public Range getRawDataPointsIntensityRange() {
		return peak.getRawDataPointsIntensityRange();
	}

	public Range getRawDataPointsMZRange() {
		return peak.getRawDataPointsMZRange();
	}

	public Range getRawDataPointsRTRange() {
		return peak.getRawDataPointsRTRange();
	}

	public int[] getScanNumbers() {
		return peak.getScanNumbers();
	}

}
