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

import net.sf.mzmine.data.Peak;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.io.OpenedRawDataFile;

/**
 * Simple implementation of the PeakList interface.
 */
public class SimplePeakList implements PeakList {

    private String name;
    private Vector<OpenedRawDataFile> rawDataFiles;
    private ArrayList<PeakListRow> peakListRows;
    private double maxDataPointIntensity = 0;

    public SimplePeakList() {
        this(null);
    }

    public SimplePeakList(String name) {
        this.name = name;
        rawDataFiles = new Vector<OpenedRawDataFile>();
        peakListRows = new ArrayList<PeakListRow>();
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
        return peakListRows.size();
    }

    /**
     * Returns the peak of a given raw data file on a give row of the alignment
     * result
     * 
     * @param row Row of the alignment result
     * @param rawDataFile Raw data file where the peak is detected/estimated
     */
    public Peak getPeak(int row, OpenedRawDataFile rawDataFile) {
        return peakListRows.get(row).getPeak(rawDataFile);
    }

    /**
     * Returns all peaks for a raw data file
     */
    public Peak[] getPeaks(OpenedRawDataFile rawDataFile) {
        Peak[] peaks = new Peak[getNumberOfRows()];
        for (int row = 0; row < getNumberOfRows(); row++) {
            peaks[row] = peakListRows.get(row).getPeak(rawDataFile);
        }
        return peaks;
    }

    /**
     * Returns all peaks on one row
     */
    public PeakListRow getRow(int row) {
        return peakListRows.get(row);
    }

    public PeakListRow[] getRows() {
        return peakListRows.toArray(new PeakListRow[0]);
    }

    public void addRow(PeakListRow row) {
        peakListRows.add(row);
        if (row.getDataPointMaxIntensity() > maxDataPointIntensity) {
            maxDataPointIntensity = row.getDataPointMaxIntensity();
        }
    }

    /**
     * Adds a new opened raw data to the alignment result
     */
    public void addOpenedRawDataFile(OpenedRawDataFile openedRawDataFile) {
        rawDataFiles.add(openedRawDataFile);
    }

    /**
     * Returns all peaks overlapping with a retention time range
     * 
     * @param startRT Start of the retention time range
     * @param endRT End of the retention time range
     * @return
     */
    public Peak[] getPeaksInsideScanRange(OpenedRawDataFile file,
            double startRT, double endRT) {
        return getPeaksInsideScanAndMZRange(file, startRT, endRT,
                Double.MIN_VALUE, Double.MAX_VALUE);
    }

    /**
     * @see net.sf.mzmine.data.PeakList#getPeaksInsideMZRange(double, double)
     */
    public Peak[] getPeaksInsideMZRange(OpenedRawDataFile file, double startMZ,
            double endMZ) {
        return getPeaksInsideScanAndMZRange(file, Double.MIN_VALUE,
                Double.MAX_VALUE, startMZ, endMZ);
    }

    /**
     * @see net.sf.mzmine.data.PeakList#getPeaksInsideScanAndMZRange(double,
     *      double, double, double)
     */
    public Peak[] getPeaksInsideScanAndMZRange(OpenedRawDataFile file,
            double startRT, double endRT, double startMZ, double endMZ) {
        Vector<Peak> peaksInside = new Vector<Peak>();

        Peak[] peaks = getPeaks(file);
        for (Peak p : peaks) {
            if ((p.getDataPointMinRT() <= endRT)
                    && (p.getDataPointMaxRT() >= startRT)
                    && (p.getDataPointMinMZ() <= endMZ)
                    && (p.getDataPointMaxMZ() >= startMZ))
                peaksInside.add(p);
        }

        return peaksInside.toArray(new Peak[peaksInside.size()]);
    }

    /**
     * @see net.sf.mzmine.data.PeakList#removeRow(net.sf.mzmine.data.PeakListRow)
     */
    public void removeRow(PeakListRow row) {
        peakListRows.remove(row);
        updateMaxIntensity();
    }

    /**
     * @see net.sf.mzmine.data.PeakList#removeRow(net.sf.mzmine.data.PeakListRow)
     */
    public void removeRow(int row) {
        peakListRows.remove(row);
        updateMaxIntensity();
    }

    private void updateMaxIntensity() {
        maxDataPointIntensity = 0;
        for (PeakListRow peakListRow: peakListRows) {
            if (peakListRow.getDataPointMaxIntensity() > maxDataPointIntensity) maxDataPointIntensity = peakListRow.getDataPointMaxIntensity();
        }
    }
    
    /**
     * @see net.sf.mzmine.data.PeakList#getPeakRow(net.sf.mzmine.data.Peak)
     */
    public int getPeakRow(Peak peak) {

        PeakListRow rows[] = getRows();

        for (int i = 0; i < rows.length; i++) {
            Peak rowPeaks[] = rows[i].getPeaks();
            for (Peak p : rowPeaks) {
                if (p == peak)
                    return i;
            }
        }

        return -1;
    }

    /**
     * @see net.sf.mzmine.data.PeakList#getDataPointMaxIntensity()
     */
    public double getDataPointMaxIntensity() {
        return maxDataPointIntensity;
    }

}
