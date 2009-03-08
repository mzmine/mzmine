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

package net.sf.mzmine.modules.peakpicking.chromatogrambuilder.massdetection;

import net.sf.mzmine.data.ChromatographicPeak;
import net.sf.mzmine.data.DataPoint;
import net.sf.mzmine.data.PeakStatus;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.modules.peakpicking.chromatogrambuilder.MzPeak;
import net.sf.mzmine.util.Range;

/**
 * 
 * This class is only used for visualization terms by MassDetectorSetupDialog.
 * 
 */
class MassDetectorPreviewPeak implements ChromatographicPeak {

	private MzPeak mzPeak;
	private int scanNumber;

	/**
	 * 
	 * @param scanNumber
	 * @param datapoint
	 */
	public MassDetectorPreviewPeak(int scanNumber, MzPeak mzPeak) {
		this.mzPeak = mzPeak;
		this.scanNumber = scanNumber;
	}

	public double getArea() {
		return 0;
	}

	public RawDataFile getDataFile() {
		return null;
	}

	public MzPeak getDataPoint(int scanNumber) {
		return mzPeak;
	}

	public double getHeight() {
		return 0;
	}

	public double getMZ() {
		return 0;
	}

	public PeakStatus getPeakStatus() {
		return null;
	}

	public double getRT() {
		return 0;
	}

	public DataPoint[] getRawDataPoints(int scanNumber) {
		return null;
	}

	public Range getRawDataPointsIntensityRange() {
		return new Range(mzPeak.getIntensity());
	}

	public Range getRawDataPointsMZRange() {
		return new Range(mzPeak.getMZ());
	}

	public Range getRawDataPointsRTRange() {
		return null;
	}

	public int[] getScanNumbers() {
		int scanNumbers[] = { scanNumber };
		return scanNumbers;
	}

	public int getRepresentativeScanNumber() {
		return scanNumber;
	}

	public int getMostIntenseFragmentScanNumber() {
		return 0;
	}

}
