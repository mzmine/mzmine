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

import java.text.Format;
import java.util.Arrays;

import net.sf.mzmine.data.DataPoint;
import net.sf.mzmine.data.Peak;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.main.MZmineCore;

/**
 * This class is a simple implementation of the peak interface.
 */
public class SimplePeak implements Peak {

    private PeakStatus peakStatus;
    private RawDataFile dataFile;

    private int scanNumbers[];
    private DataPoint dataPointsPerScan[];
    private DataPoint rawDataPointsPerScan[][];

    // M/Z, RT, Height and Area
    private float mz, rt, height, area;

    // Boundaries of the peak
    private float minRT = Float.MAX_VALUE;
    private float maxRT = Float.MIN_VALUE;
    private float minMZ = Float.MAX_VALUE;
    private float maxMZ = Float.MIN_VALUE;
    private float maxIntensity = Float.MIN_VALUE;

    /**
     * Initializes a new peak using given values
     * 
     */
    public SimplePeak(RawDataFile dataFile, float MZ, float RT, float height,
            float area, int[] scanNumbers, DataPoint[] dataPointsPerScan,
            DataPoint[][] rawDataPointsPerScan, PeakStatus peakStatus) {

        this.dataFile = dataFile;

        this.mz = MZ;
        this.rt = RT;
        this.height = height;
        this.area = area;

        this.scanNumbers = scanNumbers;
        this.dataPointsPerScan = dataPointsPerScan;
        this.rawDataPointsPerScan = rawDataPointsPerScan;

        for (int ind = 0; ind < scanNumbers.length; ind++) {

            float dataPointRT = dataFile.getScan(scanNumbers[ind]).getRetentionTime();
            if (dataPointRT < minRT)
                minRT = dataPointRT;
            if (dataPointRT > maxRT)
                maxRT = dataPointRT;

            // update boundaries
            for (DataPoint dp : rawDataPointsPerScan[ind]) {
                float dataPointMZ = dp.getMZ();
                float dataPointIntensity = dp.getIntensity();
                if (dataPointMZ < minMZ)
                    minMZ = dataPointMZ;
                if (dataPointMZ > maxMZ)
                    maxMZ = dataPointMZ;
                if (dataPointIntensity > maxIntensity)
                    maxIntensity = dataPointIntensity;
            }

        }

        this.peakStatus = peakStatus;

    }

    public SimplePeak(Peak p) {

        this.dataFile = p.getDataFile();

        this.mz = p.getMZ();
        this.rt = p.getRT();
        this.height = p.getHeight();
        this.area = p.getArea();

        this.minMZ = p.getDataPointMinMZ();
        this.maxMZ = p.getDataPointMaxMZ();
        this.minRT = p.getDataPointMinRT();
        this.maxRT = p.getDataPointMaxRT();
        this.maxIntensity = p.getDataPointMaxIntensity();

        this.scanNumbers = this.getScanNumbers();
        
        this.dataPointsPerScan = new DataPoint[scanNumbers.length];
        this.rawDataPointsPerScan = new DataPoint[scanNumbers.length][];
        
        
        for (int i = 0; i < scanNumbers.length; i++) {
            dataPointsPerScan[i] = p.getDatapoint(scanNumbers[i]);
            rawDataPointsPerScan[i] = p.getRawDatapoints(scanNumbers[i]);
        }

        this.peakStatus = p.getPeakStatus();

    }

    /**
     * This method returns the status of the peak
     */
    public PeakStatus getPeakStatus() {
        return peakStatus;
    }

    /*
     * Methods for basic properties of the peak
     */

    /**
     * This method returns M/Z value of the peak
     */
    public float getMZ() {
        return mz;
    }

    public void setMZ(float mz) {
        this.mz = mz;
    }

    public void setRT(float rt) {
        this.rt = rt;
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
     * @param height The height to set.
     */
    public void setHeight(float height) {
        this.height = height;
    }

    /**
     * This method returns the raw area of the peak
     */
    public float getArea() {
        return area;
    }

    /**
     * @param area The area to set.
     */
    public void setArea(float area) {
        this.area = area;
    }

    /**
     * This method returns numbers of scans that contain this peak
     */
    public int[] getScanNumbers() {
        return scanNumbers;
    }

    /**
     * This method returns a representative datapoint of this peak in a given
     * scan
     */
    public DataPoint getDatapoint(int scanNumber) {
        int index = Arrays.binarySearch(scanNumbers, scanNumber);
        if (index < 0) return null;
        return dataPointsPerScan[index];
    }

    /**
     * This method returns a representative datapoint of this peak in a given
     * scan
     */
    public DataPoint[] getRawDatapoints(int scanNumber) {
        int index = Arrays.binarySearch(scanNumbers, scanNumber);
        if (index < 0) return null;
        return rawDataPointsPerScan[index];    }

    /**
     * Returns the first scan number of all datapoints
     */
    public float getDataPointMinRT() {
        return minRT;
    }

    /**
     * Returns the last scan number of all datapoints
     */
    public float getDataPointMaxRT() {
        return maxRT;
    }

    /**
     * Returns minimum M/Z value of all datapoints
     */
    public float getDataPointMinMZ() {
        return minMZ;
    }

    /**
     * Returns maximum M/Z value of all datapoints
     */
    public float getDataPointMaxMZ() {
        return maxMZ;
    }

    /**
     * Returns maximum intensity value of all datapoints
     */
    public float getDataPointMaxIntensity() {
        return maxIntensity;
    }

    /**
     * @see net.sf.mzmine.data.Peak#getDataFile()
     */
    public RawDataFile getDataFile() {
        return dataFile;
    }

    /**
     * @see net.sf.mzmine.data.Peak#getDuration()
     */
    public float getDuration() {
        return maxRT - minRT;
    }

    public String toString() {
        StringBuffer buf = new StringBuffer();
        Format mzFormat = MZmineCore.getDesktop().getMZFormat();
        Format timeFormat = MZmineCore.getDesktop().getRTFormat();
        buf.append(mzFormat.format(mz));
        buf.append(" m/z @");
        buf.append(timeFormat.format(rt));
        return buf.toString();
    }

}
