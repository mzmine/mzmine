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

package net.sf.mzmine.methods.deisotoping.simplegrouper;

import java.util.Hashtable;
import java.util.ArrayList;

import net.sf.mzmine.util.MathUtils;
import net.sf.mzmine.data.IsotopePattern;
import net.sf.mzmine.data.Peak;

/**
 *
 */
public class GrouperPeak implements Peak {

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


	/**
	 * This constructor initializes a new peak with values from an old one.
	 */
	public GrouperPeak(Peak oldPeak) {
		mz = oldPeak.getRawMZ();
		rt = oldPeak.getRawRT();
		height = oldPeak.getRawHeight();
		area = oldPeak.getRawArea();

		datapointScanNumbers = oldPeak.getRawDatapointScanNumbers();
		datapointMZs = oldPeak.getRawDatapointMZs();
		datapointRTs = oldPeak.getRawDatapointRTs();
		datapointIntensities = oldPeak.getRawDatapointIntensities();

		normalizedMZ = oldPeak.getNormalizedMZ();
		normalizedRT = oldPeak.getNormalizedRT();
		normalizedHeight = oldPeak.getNormalizedHeight();
		normalizedArea = oldPeak.getNormalizedArea();
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

	public Hashtable<Integer, Double[]> getRawDatapoints() {

		Hashtable<Integer, Double[]> dpHash = new Hashtable<Integer, Double[]>();

		for (int i=0; i<datapointScanNumbers.size(); i++) {

			Double[] dps = new Double[3];
			dps[0] = datapointMZs.get(i);
			dps[1] = datapointRTs.get(i);
			dps[2] = datapointIntensities.get(i);

			dpHash.put(new Integer(i), dps);
		}

		return dpHash;
	}

	public ArrayList<Integer> getRawDatapointScanNumbers() {
		return datapointScanNumbers;
	}

	public ArrayList<Double> getRawDatapointMZs() {
		return datapointMZs;
	}

	public ArrayList<Double> getRawDatapointRTs() {
		return datapointRTs;
	}

	public ArrayList<Double> getRawDatapointIntensities() {
		return datapointIntensities;
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

	/**
	 * This method sets the isotope pattern of the peak
	 */
	public void setIsotopePattern(IsotopePattern isotopePattern) {
		this.isotopePattern = isotopePattern;
	}





}
