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
import java.util.Hashtable;

import net.sf.mzmine.data.Peak;
import net.sf.mzmine.io.OpenedRawDataFile;
import net.sf.mzmine.userinterface.mainwindow.MainWindow;
import net.sf.mzmine.util.CollectionUtils;

/**
 * This class is a simple implementation of the peak interface.
 */
public class SimplePeak implements Peak {

    private PeakStatus peakStatus;
    private OpenedRawDataFile dataFile;

    // This table maps a scanNumber to an array of m/z and intensity pairs
    private Hashtable<Integer, double[][]> datapointsMap;

    // M/Z, RT, Height and Area
    private double mz;
    private double rt;
    private double height;
    private double area;

    // Boundaries of the peak
    private double minRT = Double.MAX_VALUE;
    private double maxRT = Double.MIN_VALUE;
    private double minMZ = Double.MAX_VALUE;
    private double maxMZ = Double.MIN_VALUE;
    private double maxIntensity = Double.MIN_VALUE;

    /**
     * Initializes a new peak using given values
     * 
     * @param datapointsPerScan first index equals index in scanNumbers, second
     *            index is the number of datapoint in scan, length of last
     *            dimension is 2: index value 0=M/Z, 1=Intensity of data point
     */
    public SimplePeak(OpenedRawDataFile dataFile, double MZ, double RT,
            double height, double area, 
            int[] scanNumbers,
            double[][][] datapointsPerScan, PeakStatus peakStatus) {

        this.dataFile = dataFile;

        this.mz = MZ;
        this.rt = RT;
        this.height = height;
        this.area = area;

        datapointsMap = new Hashtable<Integer, double[][]>();
        for (int ind = 0; ind < scanNumbers.length; ind++) {
            
            double dataPointRT = dataFile.getCurrentFile().getRetentionTime(scanNumbers[ind]);
            if (dataPointRT < minRT) minRT = dataPointRT;
            if (dataPointRT > maxRT) maxRT = dataPointRT;
            
            // update boundaries
            for (int dataPointInd = 0; dataPointInd < datapointsPerScan[ind].length; dataPointInd++) {
                double dataPointMZ = datapointsPerScan[ind][dataPointInd][0];
                double dataPointIntensity = datapointsPerScan[ind][dataPointInd][1];
                if (dataPointMZ < minMZ) minMZ = dataPointMZ;
                if (dataPointMZ > maxMZ) maxMZ = dataPointMZ;
                if (dataPointIntensity > maxIntensity) maxIntensity = dataPointIntensity;
            }
            
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

        datapointsMap = new Hashtable<Integer, double[][]>();
        for (int scanNumber : p.getScanNumbers())
            datapointsMap.put(scanNumber, p.getRawDatapoints(scanNumber));

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
    public double getMZ() {
        return mz;
    }

    /**
     * This method returns retention time of the peak
     */
    public double getRT() {
        return rt;
    }

    /**
     * This method returns the raw height of the peak
     */
    public double getHeight() {
        return height;
    }

    /**
     * @param height The height to set.
     */
    public void setHeight(double height) {
        this.height = height;
    }

    /**
     * This method returns the raw area of the peak
     */
    public double getArea() {
        return area;
    }

    /**
     * @param area The area to set.
     */
    public void setArea(double area) {
        this.area = area;
    }

    /**
     * This method returns numbers of scans that contain this peak
     */
    public int[] getScanNumbers() {
        return CollectionUtils.toIntArray(datapointsMap.keySet());
    }

    /**
     * This method returns an array of double[2] (mz and intensity) points for a
     * given scan number
     */
    public double[][] getRawDatapoints(int scanNumber) {

        double[][] datapoints = datapointsMap.get(scanNumber);

        if (datapoints == null)
            datapoints = new double[0][2];

        return datapoints;
    }

    /**
     * Returns the first scan number of all datapoints
     */
    public double getDataPointMinRT() {
        return minRT;
    }

    /**
     * Returns the last scan number of all datapoints
     */
    public double getDataPointMaxRT() {
        return maxRT;
    }

    /**
     * Returns minimum M/Z value of all datapoints
     */
    public double getDataPointMinMZ() {
        return minMZ;
    }

    /**
     * Returns maximum M/Z value of all datapoints
     */
    public double getDataPointMaxMZ() {
        return maxMZ;
    }
    
    /**
     * Returns maximum intensity value of all datapoints
     */
    public double getDataPointMaxIntensity() {
        return maxIntensity;
    }

    /**
     * @see net.sf.mzmine.data.Peak#getDataFile()
     */
    public OpenedRawDataFile getDataFile() {
        return dataFile;
    }

    /**
     * @see net.sf.mzmine.data.Peak#getDuration()
     */
    public double getDuration() {
        return maxRT - minRT;
    }
    
    public String toString() {
        StringBuffer buf = new StringBuffer();
        Format mzFormat = MainWindow.getInstance().getMZFormat();
        Format timeFormat = MainWindow.getInstance().getRTFormat();
        buf.append(mzFormat.format(getMZ()));
        buf.append(" m/z @");
        buf.append(timeFormat.format(getRT()));
        return buf.toString();
    }

}
