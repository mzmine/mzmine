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

package net.sf.mzmine.interfaces;

import java.util.Hashtable;

/**
 * This interface defines the properties of a detected peak
 */
public interface Peak {

    /**
     * DETECTED - peak was found in primary peak picking
     * ESTIMATED - peak was estimated in secondary peak picking (after alignment)
     * MISSING - peak is not found or estimated
     *
     */
    public static enum PeakStatus {
        DETECTED, ESTIMATED, MISSING
    };

	/**
	 * This method returns the status of the peak
	 */
	public PeakStatus getPeakStatus();


	/* Get methods for basic properties of the peak as defined by the peak picking method */

	/**
	 * This method returns M/Z value of the peak
	 */
	public double getMZ();

	/**
	 * This method returns retention time of the peak
	 */
	public double getRT();

	/**
	 * This method returns the raw height of the peak
	 */
	public double getRawHeight();

	/**
	 * This method returns the raw area of the peak
	 */
	public double getRawArea();



	/* Get methods for accessing the raw datapoints that construct the peak */

	/**
	 * This method returns a hashtable of scan numbers and indices of datapoints
	 * within the scans.
	 *
	 * @return Hashtable maps scan number to index of datapoint within the scan
	 */
	public Hashtable<Integer, Integer> getRawDatapoints();

	/**
	 * Returns the first scan number of all datapoints
	 */
	public int getFirstScanNumber();

	/**
	 * Returns the last scan number of all datapoints
	 */
	public int getLastScanNumber();

	/**
	 * Returns minimum M/Z value of all datapoints
	 */
	public double getMinMZ();

	/**
	 * Returns maximum M/Z value of all datapoints
	 */
	public double getMaxMZ();



	/* Set/get methods for handling normalized heights and areas */

	/**
	 * This method sets the normalized height of the peak
	 */
	public void setNormalizedHeight(double normalizedHeight);

	/**
	 * This method returns the normalized height of the peak, or raw height if normalized height is not set.
	 */
	public double getNormalizedHeight();

	/**
	 * This method sets the normalized area of the peak
	 */
	public void setNormalizedArea(double normalizedArea);

	/**
	 * This method returns the normalized area of the peak, or raw area if normalized area is not set.
	 */
	public double getNormalizedArea();



	/* 	Set/get methods for isotope pattern of the peak */

	/**
	 * This method returns the isotope pattern where this peak is assigned
	 *
	 * @return isotope pattern or null if peak is not assigned to any pattern.
	 */
	public IsotopePattern getIsotopePattern();

	/**
	 * This method sets the isotope pattern of the peak
	 */
	public void setIsotopePattern(IsotopePattern isotopePattern);




}
