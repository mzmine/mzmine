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

package net.sf.mzmine.data.impl;

import java.util.Hashtable;
import java.util.Enumeration;
import java.util.TreeSet;
import java.util.Iterator;
import java.util.ArrayList;

import net.sf.mzmine.util.MathUtils;
import net.sf.mzmine.data.IsotopePattern;
import net.sf.mzmine.data.Peak;

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

	private ArrayList<Integer> datapointScanNumbers;
	private ArrayList<Double> datapointMZs;
	private ArrayList<Double> datapointRTs;
	private ArrayList<Double> datapointIntensities;

	private double minRT;
	private double maxRT;
	private double minMZ;
	private double maxMZ;

	private double normalizedMZ;
	private double normalizedRT;
	private double normalizedHeight;
	private double normalizedArea;

	// These are only used during construction
	private TreeSet<Double> constructionSortedMZs;
	private boolean growing=false;

	/**
	 * Initializes empty peak for adding data points to
	 */
	public SimplePeak() {
		intializeAddingDatapoints();
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


	/**
	 * This method returns numbers of scans that contain this peak
	 */
	public int[] getScanNumbers() {
		int[] res = new int[datapointScanNumbers.size()];

		int ind=0;
		for ( Iterator<Integer> scanNumberIter = datapointScanNumbers.iterator(); scanNumberIter.hasNext(); ind++)
	      res[ind] = scanNumberIter.next();

		return res;
	}

	/**
	 * This method returns an array of double[2] (mz and intensity) points for a given scan number
	 */
	public double[][] getRawDatapoints(int scanNumber) {

		int ind = datapointScanNumbers.indexOf(new Integer(scanNumber));

		double[][] res = new double[1][2];

		res[0][0] = datapointMZs.get(ind);
		res[0][1] = datapointIntensities.get(ind);

		return res;
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


	/* Following methods are not part of Peak interface implementation */


	public boolean isGrowing() {
		return growing;
	}


	public void resetGrowingState() {
		growing = false;
	}

	private void intializeAddingDatapoints() {

		constructionSortedMZs = new TreeSet<Double>();

		datapointScanNumbers = new ArrayList<Integer>();
		datapointMZs = new ArrayList<Double>();
		datapointRTs = new ArrayList<Double>();
		datapointIntensities = new ArrayList<Double>();

		minMZ = Double.MAX_VALUE;
		maxMZ = Double.MIN_VALUE;
		minRT = Double.MAX_VALUE;
		maxRT = Double.MIN_VALUE;

		mz = 0;
		rt = 0;
		height = 0.0;
		area = 0.0;

	}

	public void addDatapoint(int scanNumber, double mz, double rt, double intensity) {

		growing = true;

		datapointScanNumbers.add(scanNumber);
		datapointMZs.add(mz);
		datapointRTs.add(rt);
		datapointIntensities.add(intensity);

		if (mz<minMZ) minMZ = mz;
		if (mz>maxMZ) maxMZ = mz;

		if (rt<minRT) minRT = rt;
		if (rt>maxRT) maxRT = rt;

		if (intensity>=height) {
			this.rt = rt;
			height = intensity;
		}

		area += intensity;

		constructionSortedMZs.add(mz);
		int numofvalues = constructionSortedMZs.size();

		if ( (numofvalues % 2) != 0 ) {

			// odd
			int numofmidvalue = (int)java.lang.Math.round((double)numofvalues/2.0);
			Iterator<Double> constructionSortedMZsIter = constructionSortedMZs.iterator();
			int n=1; while (n<numofmidvalue) { constructionSortedMZsIter.next(); n++; }
			this.mz = constructionSortedMZsIter.next();

		} else {

			// Even
			int numofmidvalue = (int)java.lang.Math.round((double)numofvalues/2.0);
			Iterator<Double> constructionSortedMZsIter = constructionSortedMZs.iterator();
			int n=1; while (n<numofmidvalue) { constructionSortedMZsIter.next(); n++; }
			this.mz = ( constructionSortedMZsIter.next() + constructionSortedMZsIter.next()) / 2.0;

		}

		normalizedMZ = this.mz;
		normalizedRT = this.rt;
		normalizedHeight = height;
		normalizedArea = area;

	}

	public void finalizedAddingDatapoints() {


	}




}
