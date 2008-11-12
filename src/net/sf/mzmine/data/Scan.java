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
 * This class represent one spectrum of a raw data file.
 */
public interface Scan extends MzDataTable {

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
     * @return Retention time of this scan in seconds
     */
    public double getRetentionTime();

    /**
     * Returns the m/z range of this Scan. Never returns null.
     * 
     * @return m/z range of this Scan
     */
    public Range getMZRange();

    /**
     * Returns the top intensity data point. May return null if there are no
     * data points in this Scan.
     * 
     * @return Base peak
     */
    public MzDataPoint getBasePeak();

    /**
     * Returns the sum of intensities of all data points.
     * 
     * @return Total ion current
     */
    public double getTIC();

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
     * @return Precursor charge or 0 if this is not MSn scan or charge is
     *         unknown
     */
    public int getPrecursorCharge();

    /**
     * 
     * @return array of fragment scan numbers, or null if there are none
     */
    public int[] getFragmentScanNumbers();

}
