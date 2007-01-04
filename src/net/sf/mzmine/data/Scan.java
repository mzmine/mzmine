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

/**
 * This class represent one spectrum of a raw data file.
 */
public interface Scan {

    /**
     * 
     * @return Scan number
     */
    public int getScanNumber();

    /**
     * 
     * @return MS level
     */
    public int getMSLevel();

    /**
     * 
     * @return Retention time of this scan
     */
    public double getRetentionTime();

    /**
     * 
     * @return Minimum m/z of this scan
     */
    public double getMZRangeMin();

    /**
     * 
     * @return Maxmimum m/z of this scan
     */
    public double getMZRangeMax();

    /**
     * 
     * @return Base peak m/z
     */
    public double getBasePeakMZ();

    /**
     * 
     * @return Base peak intensity
     */
    public double getBasePeakIntensity();

    /**
     * 
     * @return m/z values of this scan
     */
    public double[] getMZValues();

    /**
     * 
     * @return Intensity values of this scan
     */
    public double[] getIntensityValues();

    /**
     * 
     * @return Number of m/z and intensity data points
     */
    public int getNumberOfDataPoints();

    /**
     * 
     * @return True if the scan data is centroided
     */
    public boolean isCentroided();

    /**
     * 
     * @return parent scan number or -1 if there is no parent scan
     */
    public int getParentScanNumber();

    /**
     * 
     * @return Precursor m/z or 0 if this is not MSn scan
     */
    public double getPrecursorMZ();
    
    /**
     * 
     * @return Precursor charge or 0 if this is not MSn scan or charge is unknown
     */
    public int getPrecursorCharge();

    /**
     * 
     * @return array of fragment scan numbers, or null if there are none
     */
    public int[] getFragmentScanNumbers();

}
