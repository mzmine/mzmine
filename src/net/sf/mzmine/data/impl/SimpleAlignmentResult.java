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

package net.sf.mzmine.data.impl;

import java.util.ArrayList;
import java.util.Vector;

import net.sf.mzmine.data.AlignmentResult;
import net.sf.mzmine.data.AlignmentResultRow;
import net.sf.mzmine.data.Peak;
import net.sf.mzmine.io.OpenedRawDataFile;

/**
 * 
 */
public class SimpleAlignmentResult implements AlignmentResult {

    private String name;
    private Vector<OpenedRawDataFile> rawDataFiles;
    private ArrayList<AlignmentResultRow> alignmentRows;

    public SimpleAlignmentResult(String name) {
        this.name = name;
        rawDataFiles = new Vector<OpenedRawDataFile>();
        alignmentRows = new ArrayList<AlignmentResultRow>();
    }

    @Override public String toString() {
        return name;
    }

    /**
     * Returns number of raw data files participating in the alignment
     */
    public int getNumberOfRawDataFiles() {
        return rawDataFiles.size();
    }

    /**
     * Returns all raw data files participating in the alignment
     */
    public OpenedRawDataFile[] getRawDataFiles() {
        return rawDataFiles.toArray(new OpenedRawDataFile[0]);
    }

    public OpenedRawDataFile getRawDataFile(int position) {
        return rawDataFiles.get(position);
    }

    /**
     * Returns number of rows in the alignment result
     */
    public int getNumberOfRows() {
        return alignmentRows.size();
    }

    /**
     * Returns the peak of a given raw data file on a give row of the alignment
     * result
     * 
     * @param row Row of the alignment result
     * @param rawDataFile Raw data file where the peak is detected/estimated
     */
    public Peak getPeak(int row, OpenedRawDataFile rawDataFile) {
        return alignmentRows.get(row).getPeak(rawDataFile);
    }

    /**
     * Returns all peaks for a raw data file
     */
    public Peak[] getPeaks(OpenedRawDataFile rawDataFile) {
        Peak[] peaks = new Peak[getNumberOfRows()];
        for (int row = 0; row < getNumberOfRows(); row++) {
            peaks[row] = alignmentRows.get(row).getPeak(rawDataFile);
        }
        return peaks;
    }

    /**
     * Returns all peaks on one row
     */
    public AlignmentResultRow getRow(int row) {

        return alignmentRows.get(row);

    }

    public AlignmentResultRow[] getRows() {
        return alignmentRows.toArray(new AlignmentResultRow[0]);
    }

    public void addRow(AlignmentResultRow row) {
        alignmentRows.add(row);
    }

    /**
     * Adds a new opened raw data to the alignment result
     */
    public void addOpenedRawDataFile(OpenedRawDataFile openedRawDataFile) {

        rawDataFiles.add(openedRawDataFile);

    }

}
