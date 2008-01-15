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

package net.sf.mzmine.data.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import net.sf.mzmine.data.Peak;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.io.RawDataFile;

/**
 * Simple implementation of the PeakList interface.
 */
public class SimplePeakList implements PeakList {

    private String name;
    private RawDataFile[] dataFiles;
    private ArrayList<PeakListRow> peakListRows;
    private float maxDataPointIntensity = 0;

    public SimplePeakList(String name, RawDataFile[] dataFiles) {
        this.name = name;
        this.dataFiles = dataFiles;
        peakListRows = new ArrayList<PeakListRow>();
    }

    @Override public String toString() {
        return name;
    }

    /**
     * Returns number of raw data files participating in the alignment
     */
    public int getNumberOfRawDataFiles() {
        return dataFiles.length;
    }

    /**
     * Returns all raw data files participating in the alignment
     */
    public RawDataFile[] getRawDataFiles() {
        return dataFiles;
    }

    public RawDataFile getRawDataFile(int position) {
        return dataFiles[position];
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
    public Peak getPeak(int row, RawDataFile rawDataFile) {
        return peakListRows.get(row).getPeak(rawDataFile);
    }

    /**
     * Returns all peaks for a raw data file
     */
    public Peak[] getPeaks(RawDataFile rawDataFile) {
        Vector<Peak> peakSet = new Vector<Peak>();
        for (int row = 0; row < getNumberOfRows(); row++) {
            Peak p = peakListRows.get(row).getPeak(rawDataFile);
            if (p != null)
                peakSet.add(p);
        }
        return peakSet.toArray(new Peak[0]);
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

    public PeakListRow[] getRowsInsideMZRange(float startMZ, float endMZ) {
        return getRowsInsideScanAndMZRange(Float.MIN_VALUE, Float.MAX_VALUE,
                startMZ, endMZ);
    }

    public PeakListRow[] getRowsInsideScanRange(float startRT, float endRT) {
        return getRowsInsideScanAndMZRange(startRT, endRT, Float.MIN_VALUE,
                Float.MAX_VALUE);
    }

    public PeakListRow[] getRowsInsideScanAndMZRange(float startRT,
            float endRT, float startMZ, float endMZ) {
        Vector<PeakListRow> rowsInside = new Vector<PeakListRow>();

        for (PeakListRow row : peakListRows) {
            if ((row.getAverageRT() <= endRT)
                    && (row.getAverageRT() >= startRT)
                    && (row.getAverageMZ() <= endMZ)
                    && (row.getAverageMZ() >= startMZ))
                rowsInside.add(row);
        }

        return rowsInside.toArray(new PeakListRow[0]);
    }

    public void addRow(PeakListRow row) {
        List<RawDataFile> myFiles = Arrays.asList(dataFiles);
        for (RawDataFile testFile : row.getRawDataFiles()) {
            if (!myFiles.contains(testFile))
                throw (new IllegalArgumentException("Data file " + testFile
                        + " is not in this peak list"));
        }
        peakListRows.add(row);
        if (row.getDataPointMaxIntensity() > maxDataPointIntensity) {
            maxDataPointIntensity = row.getDataPointMaxIntensity();
        }
    }

    /**
     * Returns all peaks overlapping with a retention time range
     * 
     * @param startRT Start of the retention time range
     * @param endRT End of the retention time range
     * @return
     */
    public Peak[] getPeaksInsideScanRange(RawDataFile file, float startRT,
            float endRT) {
        return getPeaksInsideScanAndMZRange(file, startRT, endRT,
                Float.MIN_VALUE, Float.MAX_VALUE);
    }

    /**
     * @see net.sf.mzmine.data.PeakList#getPeaksInsideMZRange(float, float)
     */
    public Peak[] getPeaksInsideMZRange(RawDataFile file, float startMZ,
            float endMZ) {
        return getPeaksInsideScanAndMZRange(file, Float.MIN_VALUE,
                Float.MAX_VALUE, startMZ, endMZ);
    }

    /**
     * @see net.sf.mzmine.data.PeakList#getPeaksInsideScanAndMZRange(float,
     *      float, float, float)
     */
    public Peak[] getPeaksInsideScanAndMZRange(RawDataFile file, float startRT,
            float endRT, float startMZ, float endMZ) {
        Vector<Peak> peaksInside = new Vector<Peak>();

        Peak[] peaks = getPeaks(file);
        for (Peak p : peaks) {
            if ((p.getRT() <= endRT) && (p.getRT() >= startRT)
                    && (p.getMZ() <= endMZ) && (p.getMZ() >= startMZ))
                peaksInside.add(p);
        }

        return peaksInside.toArray(new Peak[0]);
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
        for (PeakListRow peakListRow : peakListRows) {
            if (peakListRow.getDataPointMaxIntensity() > maxDataPointIntensity)
                maxDataPointIntensity = peakListRow.getDataPointMaxIntensity();
        }
    }

    /**
     * @see net.sf.mzmine.data.PeakList#getPeakRowNum(net.sf.mzmine.data.Peak)
     */
    public int getPeakRowNum(Peak peak) {

        PeakListRow rows[] = getRows();

        for (int i = 0; i < rows.length; i++) {
            if (rows[i].hasPeak(peak))
                return i;
        }

        return -1;
    }

    /**
     * @see net.sf.mzmine.data.PeakList#getDataPointMaxIntensity()
     */
    public float getDataPointMaxIntensity() {
        return maxDataPointIntensity;
    }

    public boolean hasRawDataFile(RawDataFile hasFile) {
        for (RawDataFile file : getRawDataFiles()) {
            if (file == hasFile)
                return true;
        }
        return false;
    }

    public PeakListRow getPeakRow(Peak peak) {
        PeakListRow rows[] = getRows();

        for (int i = 0; i < rows.length; i++) {
            if (rows[i].hasPeak(peak))
                return rows[i];
        }

        return null;
    }

}
