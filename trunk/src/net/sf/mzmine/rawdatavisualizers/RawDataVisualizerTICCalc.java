/*
    Copyright 2005 VTT Biotechnology

    This file is part of MZmine.

    MZmine is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    MZmine is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with MZmine; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
*/
package net.sf.mzmine.rawdatavisualizers;
import net.sf.mzmine.alignmentresultmethods.*;
import net.sf.mzmine.alignmentresultvisualizers.*;
import net.sf.mzmine.datastructures.*;
import net.sf.mzmine.distributionframework.*;
import net.sf.mzmine.miscellaneous.*;
import net.sf.mzmine.peaklistmethods.*;
import net.sf.mzmine.rawdatamethods.*;
import net.sf.mzmine.rawdatavisualizers.*;
import net.sf.mzmine.userinterface.*;


// Java packages

/**
 * This class defines the total ion chromatogram visualizer for raw data
 */
public class RawDataVisualizerTICCalc {

	// These are needed for calculating TIC
	private RawDataVisualizerRefreshRequest refreshRequest;

	// These are the results of TIC calculation
	private int[] scanNumbers;
	private double[] intensities;

	private double maxIntensity;

	// These are temp variables
	private int scanIndex;


	/**
	 * Constructor for total ion chromatogram calculator
	 */
    public RawDataVisualizerTICCalc() {
    }

	/**
	 *	Prepares calculator for receiving required scans
	 */
	public void refreshInitialize(RawDataVisualizerRefreshRequest _refreshRequest) {

		refreshRequest = _refreshRequest;

		if (refreshRequest.ticNeedsRawData) {
			scanNumbers = new int[refreshRequest.ticStopScan-refreshRequest.ticStartScan+1];
			intensities = new double[refreshRequest.ticStopScan-refreshRequest.ticStartScan+1];
			maxIntensity = Integer.MIN_VALUE;
			scanIndex = 0;
		}

	}

	/**
	 *	Offers next scan
	 */
	public void refreshHaveOneScan(Scan s) {

		// Y-coordinate is either sum of all intensities in spectrum (TIC) or sum of intensities in limited mass range (XIC)
		double tmpIntensity;
		if (refreshRequest.ticMode == RawDataVisualizerRefreshRequest.MODE_XIC) {
			tmpIntensity = s.getExtractedIonCurrent(refreshRequest.ticStartMZ, refreshRequest.ticStopMZ);
		} else {
			tmpIntensity = s.getTotalIonCurrent();
		}

		// Check if this Y-coordinate is the maximum so far.
		if (tmpIntensity>=maxIntensity) { maxIntensity= tmpIntensity; }
		intensities[scanIndex] = tmpIntensity;
		scanNumbers[scanIndex] = s.getScanNumber();

		// Move to the next coordinate
		scanIndex++;
	}

	/**
	 * Does final things and cleans up after no more scans
	 */
	public void refreshFinalize() {
	}


	/**
	 * Returns calculated total intensities
	 */
	public double[] getIntensities() {
		return intensities;
	}

	/**
	 * Returns maximum intensity
	 */
	public double getMaxIntensity() {
		return maxIntensity;
	}

	/**
	 * Returns scan numbers
	 */
	public int[] getScanNumbers() {
		return scanNumbers;
	}

}



