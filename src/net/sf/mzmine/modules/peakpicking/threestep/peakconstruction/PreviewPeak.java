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

import net.sf.mzmine.data.ChromatographicPeak;
import net.sf.mzmine.data.MzPeak;
import net.sf.mzmine.data.PeakStatus;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.util.Range;

public class PreviewPeak implements ChromatographicPeak {
	
	private ChromatographicPeak peak;

	public PreviewPeak(ChromatographicPeak originalPeak){
		this.peak = originalPeak;
	}
	
	/**
	 * This method returns a string with the basic information that defines this
	 * peak
	 * 
	 * @return String information
	 */
	public String toString() {
		Format timeFormat = MZmineCore.getRTFormat();
		return "Peak @" + timeFormat.format(this.getRT());
	}

	public double getArea() {
		return peak.getArea();
	}

	public RawDataFile getDataFile() {
		return peak.getDataFile();
	}

	public MzPeak getMzPeak(int scanNumber) {
		return peak.getMzPeak(scanNumber);
	}

	public double getHeight() {
		return peak.getHeight();
	}

	public double getMZ() {
		return peak.getMZ();
	}

	public PeakStatus getPeakStatus() {
		return peak.getPeakStatus();
	}

	public double getRT() {
		return peak.getRT();
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

	public int getRepresentativeScanNumber() {
		return peak.getRepresentativeScanNumber();
	}

}
