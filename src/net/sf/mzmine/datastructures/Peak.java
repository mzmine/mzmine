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

package net.sf.mzmine.datastructures;


import net.sf.mzmine.alignmentresultmethods.*;
import net.sf.mzmine.alignmentresultvisualizers.*;
import net.sf.mzmine.datastructures.*;
import net.sf.mzmine.obsoletedistributionframework.*;
import net.sf.mzmine.peaklistmethods.*;
import net.sf.mzmine.rawdatamethods.*;
import net.sf.mzmine.rawdatavisualizers.*;
import net.sf.mzmine.userinterface.*;
import net.sf.mzmine.util.*;

import java.io.Serializable;

public class Peak implements Serializable {

	// ID
	private int peakID;					// Unique ID for every peak

	// Basic properties of peak
	private double mz;
	private double rt;
	private double height;
	private double area;
	int isotopePatternID = -1;			// Running numbering for isotope patterns (must be unique for each pattern)
	int isotopePeakNumber = 0;			// Isotope peak number, 0=monoisotopic (or unassigned), 1,2,3,= following peaks after monoisotopic
	int chargeState = -1;					// Charge of the peak (-1 = unassigned, unknown), 0=unused!, 1=+1, 2=+2, ...

	// Datapoints that form the peak
	private double[] mzDatapoints;
	private int[] scanNumberDatapoints;
	private double[] intensityDatapoints;

	// These fields are precalculated from datapoints
	private double stdevMZ;				// Stdev, min and max of MZ datapoints
	private double minMZ;
	private double maxMZ;
	private double medianIntensity;
	private int scanNumberMaxIntensity;	// Scan number of max intensity



	/**
	 * Constructor: copies necessary data from an under-construction peak
	 */
/*
	public Peak(PeakConstruction peakConstruction) {

		// Get data points
		mzDatapoints = peakConstruction.getCentroidMZs();
		scanNumberDatapoints = peakConstruction.getScanNums();
		intensityDatapoints = peakConstruction.getCentroidIntensities();

		// Precalculate some fields
		maxIntensity			= peakConstruction.getMaxIntensity();
		medianIntensity			= peakConstruction.getMedianIntensity();
		area					= peakConstruction.getArea();
		mzMedian				= peakConstruction.getCentroidMZMedian();
		mzStdev 				= peakConstruction.getCentroidMZStdev();
		rtMaxIntensity 			= peakConstruction.getMaxIntensityTime();
		scanNumberMaxIntensity 	= peakConstruction.getMaxIntensityScanNum();

	}
*/

	/**
	 * Constructor
	 */
	public Peak(double _mz, double _rt, double _height, double _area, double[] _mzDatapoints, int[] _scanNumberDatapoints, double[] _intensityDatapoints) {

		mz = _mz;
		rt = _rt;
		height = _height;
		area = _area;

		mzDatapoints = _mzDatapoints;
		scanNumberDatapoints = _scanNumberDatapoints;
		intensityDatapoints = _intensityDatapoints;

		precalculateFields();
	}




	// Methods for Peak ID
	// -------------------

	/**
	 * Sets peak ID
	 */
	public void setPeakID(int _peakID) { peakID = _peakID; }

	/**
	 * Returns peak ID
	 */
	public int getPeakID() { return peakID; }



	// Method for basic properties
	// ---------------------------

	/**
	 * Returns M/Z of the peak
	 */
	public double getMZ() { return mz; }

	/**
	 * Sets M/Z of the peak
	 */
	public void setMZ(double _mz) { mz = _mz; }



	/**
	 * Returns RT of the peak
	 */
	public double getRT() { return rt; }

	/**
	 * Sets RT of the peak
	 */
	public void setRT(double _rt) { rt = _rt; }


	/**
	 * Returns height of the peak
	 */
	public double getHeight() { return height; }

	/**
	 * Sets height of the peak
	 */
	public void setHeight(double _height) { height = _height; }


	/**
	 * Returns area of the peak
	 */
	public double getArea() { return area; }

	/**
	 * Sets area of the peak
	 */
	public void setArea(double _area) { area = _area; }


	/**
	 * Returns isotope pattern ID
	 */
	public int getIsotopePatternID() { return isotopePatternID; }

	/**
	 * Sets isotope pattern ID
	 */
	public void setIsotopePatternID(int _isotopePatternID) { isotopePatternID = _isotopePatternID; }


	/**
	 * Returns isotope peak number
	 */
	public int getIsotopePeakNumber() { return isotopePeakNumber; }

	/**
	 * Sets isotope peak number
	 */
	public void setIsotopePeakNumber(int _isotopePeakNumber) { isotopePeakNumber = _isotopePeakNumber; }


	/**
	 * Returns charge state
	 */
	public int getChargeState() { return chargeState; }

	/**
	 * Sets charge state
	 */
	public void setChargeState(int _chargeState) { chargeState = _chargeState; }




	// Method for data points
	// ----------------------

	/**
	 * Returns M/Z datapoints
	 */
	public double[] getMZDatapoints() { return mzDatapoints; }

	/**
	 * Sets M/Z datapoints and updates precalculated fields
	 */
	public void setMZDatapoints(double[] _mzDatapoints) {
		mzDatapoints = _mzDatapoints;
		precalculateFields();
	}

	/**
	 * Returns scan numbers
	 */
	public int[] getScanNumberDatapoints() { return scanNumberDatapoints; }

	/**
	 * Sets scan numbers and updates precalculated fields
	 */
	public void setScanNumberDatapoints(int[] _scanNumberDatapoints) {
		scanNumberDatapoints = _scanNumberDatapoints;
		precalculateFields();
	}

	/**
	 * Returns intensity datapoints
	 */
	public double[] getIntensityDatapoints() { return intensityDatapoints; }

	/**
	 * Sets intensity datapoints and updates precalculated fields
	 */
	public void setIntensityDatapoints(double[] _intensityDatapoints) {
		intensityDatapoints = _intensityDatapoints;
		precalculateFields();
	}




	// Methods for precalculated fields
	// --------------------------------

	/**
	 * This method precalculates some values using datapoint arrays.
	 * Precalculating is done for faster access to these properties
	 */
	private void precalculateFields() {


		if ( (mzDatapoints!=null) && (mzDatapoints.length>0) ) {
			// Find min and max M/Z
			minMZ = Double.MAX_VALUE;
			maxMZ = 0.0;
			for (int i=0; i<mzDatapoints.length; i++) {
				if (minMZ>mzDatapoints[i]) minMZ = mzDatapoints[i];
				if (maxMZ<mzDatapoints[i]) maxMZ = mzDatapoints[i];
			}
			// Calculate stdev of M/Z values
			stdevMZ = MyMath.calcStd(mzDatapoints);
		} else {
			minMZ = 0.0;
			maxMZ = 0.0;
			stdevMZ = 0.0;
		}



		if ( (intensityDatapoints!=null) && (intensityDatapoints.length>0) ) {
			// Calculate median intensity
			medianIntensity = MyMath.calcQuantile(intensityDatapoints, 0.5);
		} else {
			medianIntensity = 0.0;
		}


		if (	(intensityDatapoints!=null) && (scanNumberDatapoints!=null) &&
				(intensityDatapoints.length>0) && (scanNumberDatapoints.length>0) ) {
			// Find maximum intensity scan
			scanNumberMaxIntensity = scanNumberDatapoints[0];
			double maxIntensity = 0.0;
			for (int i=0; i<scanNumberDatapoints.length; i++) {
				if (intensityDatapoints[i]>=maxIntensity) {
					maxIntensity = intensityDatapoints[i];
					scanNumberMaxIntensity = scanNumberDatapoints[i];
				}
			}
		}

	}

	/**
	 * Returns standard deviation of MZ datapoints
	 */
	public double getMZStdev() { return stdevMZ; }

	/**
	 * Returns smallest MZ value in this peak
	 */
	public double getMZMinimum() { return minMZ; }

	/**
	 * Returns biggest MZ value in this peak
	 */
	public double getMZMaximum() { return maxMZ; }

	/**
	 * Returns median intensity
	 */
	public double getMedianIntensity() { return medianIntensity; }

	/**
	 * Returns scan number of the maximum intensity
	 */
	public int getMaxIntensityScanNumber() { return scanNumberMaxIntensity; }

	/**
	 * Returns first scan number of the peak
	 */
	public int getStartScanNumber() { return scanNumberDatapoints[0]; }

	/**
	 * Returns last scan number of the peak
	 */
	public int getStopScanNumber() { return scanNumberDatapoints[scanNumberDatapoints.length-1]; }




	// Methods that require some functionality
	// ---------------------------------------

	/**
	 * Returns MZ value at given scan
	 */
	public double getMZAtScan(int scanNumber) {
		for (int i=0; i<scanNumberDatapoints.length; i++) {
			if (scanNumberDatapoints[i] == scanNumber) {
				return mzDatapoints[i];
			}
		}
		return -1;
	}

	/**
	 * Returns intensity value at given scan
	 */
	public double getIntensityAtScan(int scanNumber) {
		for (int i=0; i<scanNumberDatapoints.length; i++) {
			if (scanNumberDatapoints[i] == scanNumber) {
				return intensityDatapoints[i];
			}
		}
		return -1;
	}




}