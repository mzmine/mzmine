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
public class RawDataVisualizerSpectrumCalc {

	// These are needed for calculating spectrum plot
	private RawDataVisualizerRefreshRequest refreshRequest;

	// These are the results of specturm plot calculation
	private double[] intensities;
	private double[] mzValues;
	private double minMZValue;
	private double maxMZValue;
	private double maxIntensity;

	// These are temp variables
	private int scanIndex;


	/**
	 * Constructor for total ion chromatogram calculator
	 */
    public RawDataVisualizerSpectrumCalc() {
    }

	/**
	 *	Prepares calculator for receiving required scans
	 */
	public void refreshInitialize(RawDataVisualizerRefreshRequest _refreshRequest) {

		refreshRequest = _refreshRequest;


		minMZValue = refreshRequest.spectrumStartMZ;
		maxMZValue = refreshRequest.spectrumStopMZ;

		// If raw data is needed
		if (refreshRequest.spectrumNeedsRawData) {

			// Combination of multiple spectra
			if (refreshRequest.spectrumMode == RawDataVisualizerRefreshRequest.MODE_COMBINEDSPECTRA) {
				// Allocate space for storing and combining the binned spectra
				intensities = new double[refreshRequest.spectrumXResolution];
				mzValues = new double[refreshRequest.spectrumXResolution];

				for (int mri=0; mri<refreshRequest.spectrumXResolution; mri++) { mzValues[mri] = refreshRequest.spectrumStartMZ + (double)(mri) * (double)(refreshRequest.spectrumStopMZ-refreshRequest.spectrumStartMZ)/(double)refreshRequest.spectrumXResolution; }
				maxIntensity = Integer.MIN_VALUE;

			// Single spectrum
			} else {
				intensities = null;
				mzValues = null;

				maxIntensity = Integer.MIN_VALUE;
			}
		}
	}

	/**
	 *	Offers next scan
	 */
	public void refreshHaveOneScan(Scan s) {

		if (refreshRequest.spectrumMode == RawDataVisualizerRefreshRequest.MODE_SINGLESPECTRUM) {

			// Get mass and intensity measurements
			double[] tmpMZs = s.getMZValues();
			double[] tmpInts = s.getIntensityValues();

			// Find first index with MZ value within specified range
			int ind = 0;
			while ( (ind<tmpMZs.length) && (tmpMZs[ind]<refreshRequest.spectrumStartMZ)) { ind++; }
			int startInd = ind - 1;
			if (startInd<0) { startInd = 0; }

			// Find last index with MZ value within specified range
			ind = tmpMZs.length-1;
			while ((ind>=0) && (tmpMZs[ind]>refreshRequest.spectrumStopMZ)) { ind--; }
			int stopInd = ind + 1;
			if (stopInd>(tmpMZs.length-1)) { stopInd = tmpMZs.length-1; }

			// Put intensities and mzvalues to arrays
			mzValues = new double[stopInd-startInd+1];
			intensities = new double[stopInd-startInd+1];
			int ind2=0;
			for (ind = startInd; ind<=stopInd; ind++) {
				mzValues[ind2] = tmpMZs[ind];
				double tmpInt = tmpInts[ind];
				if (tmpInt>maxIntensity) { maxIntensity = tmpInt; }
				intensities[ind2] = tmpInt;

				ind2++;
			}

			// Clean up
			tmpMZs = null;
			tmpInts = null;

		}

		if (refreshRequest.spectrumMode == RawDataVisualizerRefreshRequest.MODE_COMBINEDSPECTRA) {
			double[] griddedOneScanInts = s.getBinnedIntensities(refreshRequest.spectrumStartMZ, refreshRequest.spectrumStopMZ, refreshRequest.spectrumXResolution, true);
			for (int gri=0; gri<refreshRequest.spectrumXResolution; gri++) { intensities[gri] += griddedOneScanInts[gri]; }
		}

	}

	/**
	 * Does final things and cleans up after no more scans
	 */
	public void refreshFinalize() {

		// Find maximum intensity value (for single spectrum this is done already in "have one scan" step)
		if (refreshRequest.spectrumMode == RawDataVisualizerRefreshRequest.MODE_COMBINEDSPECTRA) {
			for (double tmpInt : intensities) {
				if (tmpInt>maxIntensity) { maxIntensity = tmpInt; }
			}
		}
		refreshRequest = null;

	}


	/**
	 * Returns calculated total intensities
	 */
	public double[] getIntensities() {
		return intensities;
	}

	/**
	 * Returns calculated m/z values
	 */
	public double[] getMZValues() {
		return mzValues;
	}

	/**
	 * Returns minimum MZ value
	 */
	public double getMinMZValue() {
		return minMZValue;
	}

	/**
	 * Returns maximum MZ value
	 */
	public double getMaxMZValue() {
		return maxMZValue;
	}

	/**
	 * Returns maximum intensity
	 */
	public double getMaxIntensity() {
		return maxIntensity;
	}


}



