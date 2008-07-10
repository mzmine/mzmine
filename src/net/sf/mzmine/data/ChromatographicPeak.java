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

import net.sf.mzmine.util.Range;

/**
 * This interface defines the properties of a detected peak
 */
public interface ChromatographicPeak {

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
     * Returns raw data file where this peak is present
     */
    public RawDataFile getDataFile();

    /**
     * This method returns numbers of scans that contain this peak
     */
    public int[] getScanNumbers();

    /**
     * This method returns m/z and intensity of this peak in a given scan. This
     * m/z and intensity does not need to match any actual raw data point.
     */
    public MzPeak getMzPeak(int scanNumber);

    /**
     * Returns the retention time range of all raw data points used to detect
     * this peak
     */
    public Range getRawDataPointsRTRange();

    /**
     * Returns the range of m/z values of all raw data points used to detect
     * this peak
     */
    public Range getRawDataPointsMZRange();

    /**
     * Returns the range of intensity values of all raw data points used to
     * detect this peak
     */
    public Range getRawDataPointsIntensityRange();

}
