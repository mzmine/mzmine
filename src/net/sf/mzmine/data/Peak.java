/*
 * Copyright 2006-2008 The MZmine Development Team
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

import net.sf.mzmine.io.RawDataFile;

/**
 * This interface defines the properties of a detected peak
 */
public interface Peak {

    /**
	 * UNKNOWN - peak was not found
	 * 
	 * DETECTED - peak was found in primary peak picking
	 * 
	 * MANUAL - peak was defined manually
	 * 
	 * ESTIMATED - peak was estimated in secondary peak picking
	 * 
	 */
    public static enum PeakStatus {
        UNKNOWN, DETECTED, MANUAL, ESTIMATED
    };

    /**
	 * This method returns the status of the peak
	 */
    public PeakStatus getPeakStatus();

    /**
	 * This method returns raw M/Z value of the peak
	 */
    public float getMZ();

    /**
	 * This method returns raw retention time of the peak
	 */
    public float getRT();

    /**
	 * This method returns the raw height of the peak
	 */
    public float getHeight();

    /**
	 * This method returns the raw area of the peak
	 */
    public float getArea();

    /**
	 * Returns peak duration in seconds
	 */
    public float getDuration();

    /**
	 * Returns raw data file where this peak is present
	 */
    public RawDataFile getDataFile();
    
    /**
	 * Set raw data file where this peak is present
	 */
    public void setDataFile(RawDataFile dataFile);
    
    /**
	 * This method returns numbers of scans that contain this peak
	 */
    public int[] getScanNumbers();

    /**
	 * This method returns m/z and intensity of this peak in a given scan. This
	 * m/z and intensity does not need to match any actual raw data point.
	 */
    public DataPoint getDataPoint(int scanNumber);

    /**
	 * This method returns all raw data points in given scan that were used to
	 * build this peak. Such data points must match actual raw data points
	 * returned by Scan.getDatapoints()
	 */
    public DataPoint[] getRawDataPoints(int scanNumber);

    /**
	 * Returns the minimum RT of all raw data points used to detect this peak
	 */
    public float getRawDataPointMinRT();

    /**
	 * Returns the maximum RT of all raw data points used to detect this peak
	 */
    public float getRawDataPointMaxRT();

    /**
	 * Returns minimum M/Z value of all raw data points used to detect this peak
	 */
    public float getRawDataPointMinMZ();

    /**
	 * Returns maximum M/Z value of all raw data points used to detect this peak
	 */
    public float getRawDataPointMaxMZ();

    /**
	 * Returns maximum intensity value of all raw data points used to detect
	 * this peak
	 */
    public float getRawDataPointMaxIntensity();

}
