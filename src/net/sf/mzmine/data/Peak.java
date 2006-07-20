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

package net.sf.mzmine.data;

import java.util.Hashtable;

/**
 * This interface defines the properties of a detected peak
 */
public interface Peak {

    /**
     * DETECTED - peak was found in primary peak picking ESTIMATED - peak was
     * estimated in secondary peak picking (after alignment) MISSING - peak is
     * not found or estimated
     * 
     */
    public static enum PeakStatus {
        DETECTED, ESTIMATED, MISSING
    };

    /**
     * This method returns the status of the peak
     */
    public PeakStatus getPeakStatus();

    /*
     * Get methods for basic properties of the peak as defined by the peak
     * picking method. Values for these properties can be freely defined by the
     * peak picking method.
     */

    /**
     * This method returns raw M/Z value of the peak
     */
    public double getRawMZ();

    /**
     * This method returns raw retention time of the peak
     */
    public double getRawRT();

    /**
     * This method returns the raw height of the peak
     */
    public double getRawHeight();

    /**
     * This method returns the raw area of the peak
     */
    public double getRawArea();

    /*
     * Get methods for accessing the raw datapoints that construct the peak.
     * These datapoints should correspond to datapoints in the raw data.
     */

    /**
     * This method returns all datapoints of the peak
     * 
     * @return Hashtable maps scan number to triplets of M/Z, RT and Intensity
     */
    public Hashtable<Integer, Double[]> getRawDatapoints();

    /**
     * Returns the minimum RT of all datapoints
     */
    public double getMinRT();

    /**
     * Returns the maximum RT of all datapoints
     */
    public double getMaxRT();

    /**
     * Returns minimum M/Z value of all datapoints
     */
    public double getMinMZ();

    /**
     * Returns maximum M/Z value of all datapoints
     */
    public double getMaxMZ();

    /**
     * This method returns the normalized M/Z of the peak
     */
    public double getNormalizedMZ();

    /**
     * This method returns the normalized RT of the peak
     */
    public double getNormalizedRT();

    /**
     * This method returns the normalized height of the peak, or raw height if
     * normalized height is not set.
     */
    public double getNormalizedHeight();

    /**
     * This method returns the normalized area of the peak, or raw area if
     * normalized area is not set.
     */
    public double getNormalizedArea();

    /**
     * This method returns the isotope pattern where this peak is assigned
     * 
     * @return isotope pattern or null if peak is not assigned to any pattern.
     */
    public IsotopePattern getIsotopePattern();

}
