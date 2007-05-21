/*
 * Copyright 2006-2007 The MZmine Development Team
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

import net.sf.mzmine.io.OpenedRawDataFile;

/**
 * This interface defines the properties of a detected peak
 */
public interface Peak extends DataUnit {

    /**
     * DETECTED - peak was found in primary peak picking 
     * ESTIMATED - peak was estimated in secondary peak picking (after alignment)
     * 
     */
    public static enum PeakStatus {
        DETECTED, ESTIMATED
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
    public double getMZ();

    /**
     * This method returns raw retention time of the peak
     */
    public double getRT();

    /**
     * This method returns the raw height of the peak
     */
    public double getHeight();

    /**
     * This method returns the raw area of the peak
     */
    public double getArea();

    /**
     * Returns peak duration in seconds
     */
    public double getDuration();

    /*
     * Get methods for accessing the raw datapoints that construct the peak.
     * These datapoints should correspond to datapoints in the raw data.
     */

    /**
     * 
     */
    public OpenedRawDataFile getDataFile();

    /**
     * This method returns numbers of scans that contain this peak
     */
    public int[] getScanNumbers();

    /**
     * This method returns an array of double[2] (mz and intensity) points for a
     * given scan number
     */
    public double[][] getRawDatapoints(int scanNumber);

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

}
