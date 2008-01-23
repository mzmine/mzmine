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

package net.sf.mzmine.modules.peakpicking.accuratemass;

import java.text.NumberFormat;
import java.util.TreeMap;

import net.sf.mzmine.data.DataPoint;
import net.sf.mzmine.data.Peak;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.util.CollectionUtils;
import net.sf.mzmine.util.MathUtils;

/**
 * This class is an implementation of the peak interface for peak picking
 * methods.
 */
class AccurateMassPeak implements Peak {

    private RawDataFile dataFile;

    // Raw M/Z, RT, Height and Area
    private float mz, rt;
    private float height, area;

    // Boundaries of the peak
    private float minRT, maxRT;
    private float minMZ, maxMZ;

    private TreeMap<Integer, AccurateMassDataPoint> dataPointMap;

    /**
     * Initializes empty peak for adding data points
     */
    AccurateMassPeak(RawDataFile dataFile) {

        dataPointMap = new TreeMap<Integer, AccurateMassDataPoint>();

        this.dataFile = dataFile;

    }

    /**
     * This peak is always a result of peak detection, therefore DETECTED
     */
    public PeakStatus getPeakStatus() {
        return PeakStatus.DETECTED;
    }

    /**
     * This method returns M/Z value of the peak
     */
    public float getMZ() {
        return mz;
    }

    /**
     * This method returns retention time of the peak
     */
    public float getRT() {
        return rt;
    }

    /**
     * This method returns the raw height of the peak
     */
    public float getHeight() {
        return height;
    }

    /**
     * This method returns the raw area of the peak
     */
    public float getArea() {
        return area;
    }

    /**
     * @see net.sf.mzmine.data.Peak#getDuration()
     */
    public float getDuration() {
        return maxRT - minRT;
    }

    /**
     * This method returns numbers of scans that contain this peak
     */
    public int[] getScanNumbers() {
        return CollectionUtils.toIntArray(dataPointMap.keySet());
    }

    /**
     * This method returns a representative datapoint of this peak in a given
     * scan
     */
    public DataPoint getDataPoint(int scanNumber) {
        return dataPointMap.get(scanNumber);
    }

    /**
     * This method returns all raw data points used to build this peak in a
     * given scan
     */
    public DataPoint[] getRawDataPoints(int scanNumber) {
        return dataPointMap.get(scanNumber).getRawDataPoints();
    }

    /**
     * Returns the first scan number of all datapoints
     */
    public float getRawDataPointMinRT() {
        return minRT;
    }

    /**
     * Returns the last scan number of all datapoints
     */
    public float getRawDataPointMaxRT() {
        return maxRT;
    }

    /**
     * Returns minimum M/Z value of all datapoints
     */
    public float getRawDataPointMinMZ() {
        return minMZ;
    }

    /**
     * Returns maximum M/Z value of all datapoints
     */
    public float getRawDataPointMaxMZ() {
        return maxMZ;
    }

    /**
     * Returns maximum M/Z value of all datapoints
     */
    public float getRawDataPointMaxIntensity() {
        return height;
    }

    /**
     * @see net.sf.mzmine.data.Peak#getDataFile()
     */
    public RawDataFile getDataFile() {
        return dataFile;
    }

    /**
     * @see java.lang.Object#toString()
     */
    public String toString() {
        NumberFormat mzFormat = MZmineCore.getDesktop().getMZFormat();
        NumberFormat timeFormat = MZmineCore.getDesktop().getRTFormat();
        StringBuffer buf = new StringBuffer();
        buf.append(mzFormat.format(mz));
        buf.append(" m/z @");
        buf.append(timeFormat.format(rt));
        return buf.toString();
    }

    /**
     * Adds a new data point to this peak
     * 
     * @param scanNumber
     * @param dataPoints
     * @param rawDataPoints
     */
    void addDatapoint(int scanNumber, AccurateMassDataPoint dataPoint) {

        dataPointMap.put(scanNumber, dataPoint);

        // Update construction time variables

        float dataPointRT = dataFile.getScan(scanNumber).getRetentionTime();
        if (dataPointRT < minRT)
            minRT = dataPointRT;
        if (dataPointRT > maxRT)
            maxRT = dataPointRT;

        for (DataPoint rawDataPoint : dataPoint.getRawDataPoints()) {

            // Update m/z boundaries
            if (rawDataPoint.getMZ() < minMZ)
                minMZ = rawDataPoint.getMZ();
            if (rawDataPoint.getMZ() > maxMZ)
                minMZ = rawDataPoint.getMZ();

            // Find the data point with top intensity and use its RT and height
            if (rawDataPoint.getIntensity() > height) {
                height = rawDataPoint.getIntensity();
                this.rt = dataPointRT;
            }

        }

        // Get all scan numbers
        int allScanNumbers[] = CollectionUtils.toIntArray(dataPointMap.keySet());

        // Calculate peak area
        area = 0f;
        for (int i = 1; i < allScanNumbers.length; i++) {

            // X axis interval length
            float previousRT = dataFile.getScan(allScanNumbers[i - 1]).getRetentionTime();
            float thisRT = dataFile.getScan(allScanNumbers[i]).getRetentionTime();
            float rtDifference = thisRT - previousRT;

            // Intensity at the beginning and end of the interval
            float previousIntensity = dataPointMap.get(allScanNumbers[i - 1]).getIntensity();
            float thisIntensity = dataPointMap.get(allScanNumbers[i]).getIntensity();
            float averageIntensity = (previousIntensity + thisIntensity) / 2;

            // Calculate area of the interval
            area += (rtDifference * averageIntensity);

        }

        // Calculate median MZ
        float mzArray[] = new float[allScanNumbers.length];
        for (int i = 0; i < allScanNumbers.length; i++) {
            mzArray[i] = dataPointMap.get(allScanNumbers[i]).getMZ();
        }
        this.mz = MathUtils.calcQuantile(mzArray, 0.5f);

    }

}
