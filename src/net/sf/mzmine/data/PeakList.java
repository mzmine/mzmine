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
 * WARRANTY; without even the im plied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.data;

import net.sf.mzmine.io.OpenedRawDataFile;


/**
 * 
 */
public interface PeakList {

    /**
     * @return Short descriptive name for the alignment result
     */
    public String toString();

    /**
     * Returns number of raw data files participating in the alignment
     */
    public int getNumberOfRawDataFiles();

    /**
     * Returns all raw data files participating in the alignment
     */
    public OpenedRawDataFile[] getRawDataFiles();

    /**
     * Returns a raw data file
     * @param position  Position of the raw data file in the matrix (running numbering from left 0,1,2,...)
     */
    public OpenedRawDataFile getRawDataFile(int position);

    /**
     * Returns number of rows in the alignment result
     */
    public int getNumberOfRows();

    /**
     * Returns the peak of a given raw data file on a give row of the alignment result
     * @param   row Row of the alignment result
     * @param   rawDataFile Raw data file where the peak is detected/estimated
     */
    public Peak getPeak(int row, OpenedRawDataFile rawDataFile);

    /**
     * Returns all peaks for a raw data file
     */
    public Peak[] getPeaks(OpenedRawDataFile rawDataFile);

    /**
     * Returns all peaks on one row
     */
    public PeakListRow getRow(int row);

    /**
     * Returns all alignment result rows
     */
    public PeakListRow[] getRows();

    
    /**
     * Returns all peaks overlapping with a retention time range
     * 
     * @param startRT Start of the retention time range
     * @param endRT End of the retention time range
     */
    public Peak[] getPeaksInsideScanRange(OpenedRawDataFile file, double startRT, double endRT);

    /**
     * Returns all peaks in a given m/z range
     * 
     * @param startMZ Start of the m/z range
     * @param endMZ End of the m/z range
     */
    public Peak[] getPeaksInsideMZRange(OpenedRawDataFile file, double startMZ, double endMZ);

    /**
     * Returns all peaks in a given m/z & retention time ranges
     * 
     * @param startRT Start of the retention time range
     * @param endRT End of the retention time range
     * @param startMZ Start of the m/z range
     * @param endMZ End of the m/z range
     */
    public Peak[] getPeaksInsideScanAndMZRange(OpenedRawDataFile file, double startRT, double endRT,
            double startMZ, double endMZ);

    /**
     * Removes a row from this peak list
     * 
     */
    public void removeRow(PeakListRow row);

}
