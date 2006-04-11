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

// Java packages
import java.util.*;

/**
 * This class represent one spectrum of a raw data file
 */
public class Scan {

    private double[] mzValues; // mz values of the scan
    private double[] intensityValues; // Intensity values of the scan
    private int scanNumber; // Consecutive number of this scan in a run

    private double mzRangeMin;
    private double mzRangeMax;

	/**
	 * Constructor
	 */
	public Scan(double[] _mzValues, double[] _intensityValues, int _scanNumber, double _mzRangeMin, double _mzRangeMax) {
		mzValues = _mzValues;
		intensityValues = _intensityValues;
		scanNumber = _scanNumber;
		mzRangeMin = _mzRangeMin;
		mzRangeMax = _mzRangeMax;
	}


	/**
	 * Constructor
	 */
	public Scan(int _scanNumber) {
		scanNumber = _scanNumber;
	}


	/**
	 * This method returns the minimum M/Z range of this scan.
	 * The value is usually defined independently from actual data points.
	 */
	public double getMZRangeMin() { return mzRangeMin; }

	/**
	 * This method sets minimum M/Z range value
	 */
	public void setMZRangeMin(double val) { mzRangeMin = val; }

	/**
	 * This method returns the maximum M/Z range of this scan.
	 * The value is usually defined independently from actual data points.
	 */
	public double getMZRangeMax() { return mzRangeMax; }

	/**
	 * This method sets maximum M/Z range value
	 */
	public void setMZRangeMax(double val) { mzRangeMax = val; }



	/**
	 * Calculates the sum of intensities in the spectrum
	 * @return	sum of all intensities
	 */
	public double getTotalIonCurrent() {
		double intensitysum = 0;

		for (int ei=0; ei<intensityValues.length; ei++) {
			intensitysum += intensityValues[ei];
		}

		return intensitysum;

	}


	/**
	 * Calculates the sum of intensities inside the specified range of the spectrum
	 * @param minMZ		start of the subspace (-1 = unlimited)
	 * @param maxMZ		end of the subspace (-1 = unlimited)
	 * @return	sum of intensities inside the specified subspace of the spectra
	 */
	public double getExtractedIonCurrent(double minMZ, double maxMZ) {
		double intensitysum = 0;
		double mzvalue;

		for(int ei=0; ei<intensityValues.length; ei++) {
			mzvalue = mzValues[ei];
			if ((minMZ==-1) || (minMZ<=mzvalue)) {
				if ((maxMZ==-1) || (maxMZ>=mzvalue)) {
					intensitysum += intensityValues[ei];
				}
			}
		}

		return intensitysum;
	}



	/**
	 * Return intensity values gridded to given number of mz bins over the specified range
	 *
	 * @param	startMZ	Start of MZ-range for gridding
	 * @param	stopMZ	End of MZ-range for gridding
	 * @param	binnum	Number of bins inside the range
	 * @param	interpolate	When true then fill in empty bins by interpolating
	 *
	 */
	public double[] getBinnedIntensities(double startMZ, double stopMZ, int numOfBins, boolean interpolate) {

		double[] leftBorderInts = new double[numOfBins+2];
		double[] rightBorderInts = new double[numOfBins+2];
		double[] maxInts = new double[numOfBins];

		boolean[] leftBorderExists = new boolean[numOfBins+2];
		boolean[] rightBorderExists = new boolean[numOfBins+2];
		boolean[] maxExists = new boolean[numOfBins];

		double binwidth = (stopMZ-startMZ)/numOfBins;
		int bini;

		int prevbin; double prevint;
		int nextbin; double nextint;


		// Assume that mzValues are already in sorted order!

		for (bini=0; bini<(numOfBins+2); bini++) {
			leftBorderInts[bini] = 0;
			leftBorderExists[bini] = false;
			rightBorderInts[bini] = 0;
			rightBorderExists[bini] = false;
		}
		for (bini=0; bini<(numOfBins); bini++) {
			maxInts[bini] = 0;
			maxExists[bini] = false;
		}


		// Skip values smaller than startMZ and record last (mz, intensity) pair before the first bin
		int mi=0;
		rightBorderInts[0] = 0;
		rightBorderExists[0] = true;
		while (mi<mzValues.length) {
			if (mzValues[mi]<startMZ) {
				rightBorderInts[0] = intensityValues[mi];
				mi++;
			} else {
				break;
			}
		}


		double limitMZ = startMZ+binwidth;
		bini = 0;
		while (mi<mzValues.length) {
			// If this mz value fits inside current bin, increased bin's intensity with this intensity value
			if (mzValues[mi]<limitMZ) {
				if (maxInts[bini]<=intensityValues[mi]) { maxInts[bini] = intensityValues[mi]; maxExists[bini] = true;}

				if (leftBorderExists[bini+1]==false) { leftBorderInts[bini+1] = intensityValues[mi]; leftBorderExists[bini+1]=true; }
				rightBorderInts[bini+1] = intensityValues[mi]; rightBorderExists[bini+1] = true;
				mi++;
			} else {
				// Else move to next bin and check fitness during next loop
				bini++;
				if (bini==numOfBins) break;
				limitMZ += binwidth;
			}
		}


		// Find first (mz, intensity) pair after the last bin
		if (mi<mzValues.length) {
			leftBorderInts[numOfBins+1] = intensityValues[mi];
			leftBorderExists[numOfBins+1] = true;
		} else {
			leftBorderInts[numOfBins+1] = 0;
			leftBorderExists[numOfBins+1] = true;
		}



		// Interpolate intensity values for those bins which have zero measurement hits
		// 1) fix end points separately

		if (interpolate) {

			prevint = 0; prevbin=0; nextint=0; nextbin=1;
			for (mi = 0; mi<numOfBins; mi++) {
				// if this bin is empty, lets interpolate some intensity to it
				if (maxExists[mi]==false) {


					// Search for previous non-empty bin
					for (int ni=(mi-1); ni>=(0-1); ni--) {
						if (rightBorderExists[ni+1]!=false) {
							prevbin = ni;
							prevint = rightBorderInts[ni+1];
							break;
						}

					}

					// Search for next non-empty bin
					for (int ni=(mi+1); ni<=(numOfBins+1); ni++) {

						if (leftBorderExists[ni+1]!=false) {
							nextbin = ni;
							nextint = leftBorderInts[ni+1];
							break;
						}

					}
					maxInts[mi] = prevint + ( (nextint-prevint) / ( (double)nextbin-(double)prevbin ) ) *( (double)mi-(double)prevbin );
				}

			}

		}
		return maxInts;
	}



	/**
	 * Set/get methods for attributes
	 */
	public void setScanNumber(int _scanNumber) {
		scanNumber = _scanNumber;
	}

	public int getScanNumber() {
		return scanNumber;
	}

	public void setMZValues(double[] _mzValues) {
		mzValues = _mzValues;
	}

	public double[] getMZValues() {
		return mzValues;
	}

	public void setIntensityValues(double[] _intensityValues) {
		intensityValues = _intensityValues;
	}

	public double[] getIntensityValues() {
		return intensityValues;
	}

	public double getMaxIntensity() {
		double maxIntensity = Double.MIN_VALUE;
		for (double intensity : intensityValues) {
			if (intensity>maxIntensity) { maxIntensity = intensity; }
		}
		return maxIntensity;
	}


 }
