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

import java.text.Format;
import java.util.Arrays;
import java.util.Hashtable;

import net.sf.mzmine.data.Peak;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.util.CollectionUtils;

/**
 * This class is a simple implementation of the peak interface.
 */
public class SimplePeak implements Peak {

    private PeakStatus peakStatus;
    private RawDataFile dataFile;

    // This table maps a scanNumber to an array of m/z and intensity pairs
    private Hashtable<Integer, float[]> datapointsMap;

    // M/Z, RT, Height and Area
    private float mz;
    private float rt;
    private float height;
    private float area;

    // Boundaries of the peak
    private float minRT = Float.MAX_VALUE;
    private float maxRT = Float.MIN_VALUE;
    private float minMZ = Float.MAX_VALUE;
    private float maxMZ = Float.MIN_VALUE;
    private float maxIntensity = Float.MIN_VALUE;

    /**
     * Initializes a new peak using given values
     * 
     * @param datapointsPerScan first index equals index in scanNumbers, second
     *            index is the number of datapoint in scan, length of last
     *            dimension is 2: index value 0=M/Z, 1=Intensity of data point
     */
    public SimplePeak(RawDataFile dataFile, float MZ, float RT, float height,
            float area, int[] scanNumbers, float[][] datapointsPerScan,
            PeakStatus peakStatus) {

        this.dataFile = dataFile;

        this.mz = MZ;
        this.rt = RT;
        this.height = height;
        this.area = area;

        datapointsMap = new Hashtable<Integer, float[]>();
        for (int ind = 0; ind < scanNumbers.length; ind++) {

            float dataPointRT = dataFile.getScan(scanNumbers[ind]).getRetentionTime();
            if (dataPointRT < minRT)
                minRT = dataPointRT;
            if (dataPointRT > maxRT)
                maxRT = dataPointRT;

            // update boundaries
            float dataPointMZ = datapointsPerScan[ind][0];
            float dataPointIntensity = datapointsPerScan[ind][1];
            if (dataPointMZ < minMZ)
                minMZ = dataPointMZ;
            if (dataPointMZ > maxMZ)
                maxMZ = dataPointMZ;
            if (dataPointIntensity > maxIntensity)
                maxIntensity = dataPointIntensity;

            // add data point to hashtable
            datapointsMap.put(scanNumbers[ind], datapointsPerScan[ind]);
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

        datapointsMap = new Hashtable<Integer, float[]>();
        for (int scanNumber : p.getScanNumbers()) {
            datapointsMap.put(scanNumber, p.getRawDatapoint(scanNumber));
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
     * Get methods for basic properties of the peak as defined by the peak
     * picking method
     */

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
        int scanNumbers[] = CollectionUtils.toIntArray(datapointsMap.keySet());
        Arrays.sort(scanNumbers);
        return scanNumbers;
    }

    /**
     * This method returns float[2] (mz and intensity) for a given scan number
     */
    public float[] getRawDatapoint(int scanNumber) {
        return datapointsMap.get(scanNumber);
    }

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
        buf.append(mzFormat.format(getMZ()));
        buf.append(" m/z @");
        buf.append(timeFormat.format(getRT()));
        return buf.toString();
    }

}
