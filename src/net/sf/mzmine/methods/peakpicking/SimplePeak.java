/*
 * Copyright 2006 The MZmine Development Team
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

package net.sf.mzmine.methods.peakpicking;

import java.util.Hashtable;
import java.util.Enumeration;

import net.sf.mzmine.util.MyMath;
import net.sf.mzmine.interfaces.Peak;
import net.sf.mzmine.interfaces.IsotopePattern;

/**
 * This class is a simple implementation of the peak interface.
 * This implementation is used by recursive threshold, centroid and local maximum peak pickers.
 */
public class SimplePeak implements Peak {

	private PeakStatus peakStatus;

	private IsotopePattern isotopePattern;

	private double mz;
	private double rt;
	private double height;
	private double area;

	private Hashtable<Integer, Double[]> datapoints;

	private double minRT;
	private double maxRT;
	private double minMZ;
	private double maxMZ;

	private double normalizedMZ;
	private double normalizedRT;
	private double normalizedHeight;
	private double normalizedArea;

	private boolean growing=false;

	public SimplePeak() {
		datapoints = new Hashtable<Integer, Double[]>();
	}

	/**
	 * This method returns the status of the peak
	 */
	public PeakStatus getPeakStatus() {
		return peakStatus;
	}


	/* Get methods for basic properties of the peak as defined by the peak picking method */

	/**
	 * This method returns M/Z value of the peak
	 */
	public double getRawMZ() {
		return mz;
	}

	/**
	 * This method returns retention time of the peak
	 */
	public double getRawRT() {
		return rt;
	}

	/**
	 * This method returns the raw height of the peak
	 */
	public double getRawHeight() {
		return height;
	}

	/**
	 * This method returns the raw area of the peak
	 */
	public double getRawArea() {
		return area;
	}



	/* Get methods for accessing the raw datapoints that construct the peak */

	/**
	 * This method returns a hashtable mapping scan numbers to triplets of M/Z, RT and Intensity
	 *
	 * @return Hashtable maps scan number to datapoints for the scan
	 */
	public Hashtable<Integer, Double[]> getRawDatapoints() {
		return datapoints;
	}


	/**
	 * Returns the first scan number of all datapoints
	 */
	public double getMinRT() {
		return minRT;
	}

	/**
	 * Returns the last scan number of all datapoints
	 */
	public double getMaxRT() {
		return maxRT;
	}

	/**
	 * Returns minimum M/Z value of all datapoints
	 */
	public double getMinMZ() {
		return minMZ;
	}

	/**
	 * Returns maximum M/Z value of all datapoints
	 */
	public double getMaxMZ() {
		return maxMZ;
	}



	/* Set/get methods for handling normalized heights and areas */

	/**
	 * This method returns the normalized M/Z of the peak
	 */
	public double getNormalizedMZ() {
		return normalizedMZ;
	}

	/**
	 * This method returns the normalized RT of the peak
	 */
	public double getNormalizedRT() {
		return normalizedRT;
	}

	/**
	 * This method returns the normalized height of the peak, or raw height if normalized height is not set.
	 */
	public double getNormalizedHeight() {
		return normalizedHeight;
	}

	/**
	 * This method returns the normalized area of the peak, or raw area if normalized area is not set.
	 */
	public double getNormalizedArea() {
		return normalizedArea;
	}



	/* 	Set/get methods for isotope pattern of the peak */

	/**
	 * This method returns the isotope pattern where this peak is assigned
	 *
	 * @return isotope pattern or null if peak is not assigned to any pattern.
	 */
	public IsotopePattern getIsotopePattern() {
		return isotopePattern;
	}



	public void addDatapoint(int scanNumber, double mz, double rt, double intensity) {
		Double[] triplet = new Double[3];
		triplet[0] = mz;
		triplet[1] = rt;
		triplet[2] = intensity;
		datapoints.put(new Integer(scanNumber), triplet);

		growing = true;
		precalculateFromDatapoints();
	}

	public boolean isGrowing() {
		return growing;
	}

	public void resetGrowingState() {
		growing = false;
	}


	private void precalculateFromDatapoints() {

		// Basic requirements of the interface:
		// - Find minimum and maximum M/Z and RT of all datapoints
		//
		// Specific to this peak implementation
		// - Find maximum height and corresponding RT  (peak's height and RT)
		// - Calculate sum of all intensities (peak's area)
		// - Calculate median of all datapoints (defines peak's M/Z)
		//

		minMZ = Double.MAX_VALUE;
		maxMZ = Double.MIN_VALUE;
		minRT = Double.MAX_VALUE;
		maxRT = Double.MIN_VALUE;

		height = 0.0;
		area = 0.0;
		double[] allMZs = new double[datapoints.size()];

		int i=0;
		for (Enumeration<Double[]> triplets = datapoints.elements();
				triplets.hasMoreElements(); ) {

			Double[] triplet = triplets.nextElement();
			double tripletMZ = triplet[0];
			double tripletRT = triplet[1];
			double tripletIntensity = triplet[2];

			// Find minimum and maximum M/Z & RT
			if (tripletMZ<minMZ) minMZ = triplet[0];
			if (tripletMZ>maxMZ) maxMZ = triplet[0];
			if (tripletRT<minRT) minRT = triplet[1];
			if (tripletRT>maxRT) maxRT = triplet[1];

			// Find max intensity point (=> peak's height and RT)
			if (tripletIntensity>=height) {
				height = tripletIntensity;
				rt = tripletRT;
			}

			// Calc peak's area
			area += tripletIntensity;

			// Collect M/Z values
			allMZs[i] = tripletMZ;
			i++;
		}

		mz = MyMath.calcQuantile(allMZs, 0.5);

		normalizedMZ = mz;
		normalizedRT = rt;
		normalizedHeight = height;
		normalizedArea = area;

	}





}
