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

package net.sf.mzmine.modules.peakpicking.recursivethreshold;

import java.util.ArrayList;
import java.util.TreeMap;

import net.sf.mzmine.data.DataPoint;
import net.sf.mzmine.data.Peak;
import net.sf.mzmine.data.PeakStatus;
import net.sf.mzmine.data.impl.SimpleDataPoint;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.util.CollectionUtils;
import net.sf.mzmine.util.MathUtils;
import net.sf.mzmine.util.PeakUtils;
import net.sf.mzmine.util.Range;

/**
 * This class is an implementation of the peak interface for peak picking
 * methods.
 */
class RecursivePeak implements Peak {

    private PeakStatus peakStatus;

    // This table maps a scanNumber to an array of m/z and intensity pairs
    private TreeMap<Integer, DataPoint> datapointsMap;

    private RawDataFile dataFile;

    // Raw M/Z, RT, Height and Area
    private float mz, rt, height, area;

    // Boundaries of the peak
    private Range rtRange, mzRange, intensityRange;

    // These are used for constructing the peak
    private boolean precalcRequiredMZ;
    private boolean precalcRequiredRT;
    private boolean precalcRequiredMins;
    private boolean precalcRequiredArea;
    private ArrayList<Float> datapointsMZs;
    private ArrayList<Float> datapointsRTs;
    private ArrayList<Float> datapointsIntensities;

    private boolean growing;

    /**
     * Initializes empty peak for adding data points to
     */
    RecursivePeak(RawDataFile dataFile) {
        this.dataFile = dataFile;
        intializeAddingDatapoints();
    }

    /**
     * This method returns the status of the peak
     */
    public PeakStatus getPeakStatus() {
        return peakStatus;
    }

    public void setPeakStatus(PeakStatus peakStatus) {
        this.peakStatus = peakStatus;
    }

    /*
     * Get methods for basic properties of the peak as defined by the peak
     * picking method
     */

    /**
     * This method returns M/Z value of the peak
     */
    public float getMZ() {
        if (precalcRequiredMZ)
            precalculateMZ();
        return mz;
    }

    /**
     * This method returns retention time of the peak
     */
    public float getRT() {
        if (precalcRequiredRT)
            precalculateRT();
        return rt;
    }

    /**
     * This method returns the raw height of the peak
     */
    public float getHeight() {
        if (precalcRequiredRT)
            precalculateRT();
        return height;
    }

    /**
     * This method returns the raw area of the peak
     */
    public float getArea() {
        if (precalcRequiredArea)
            precalculateArea();
        return area;
    }

    /**
     * This method returns numbers of scans that contain this peak
     */
    public int[] getScanNumbers() {
        return CollectionUtils.toIntArray(datapointsMap.keySet());
    }

    /**
     * This method returns a representative datapoint of this peak in a given
     * scan
     */
    public DataPoint getDataPoint(int scanNumber) {
        return datapointsMap.get(scanNumber);
    }

    /**
     * This method returns a representative datapoint of this peak in a given
     * scan
     */
    public DataPoint[] getRawDataPoints(int scanNumber) {
        return new DataPoint[] { datapointsMap.get(scanNumber) };
    }

    public Range getRawDataPointsIntensityRange() {
        return intensityRange;
    }

    public Range getRawDataPointsMZRange() {
        return mzRange;
    }

    public Range getRawDataPointsRTRange() {
        return rtRange;
    }

    private void intializeAddingDatapoints() {

        datapointsMap = new TreeMap<Integer, DataPoint>();

        precalcRequiredMZ = true;
        precalcRequiredRT = true;
        precalcRequiredMins = true;
        precalcRequiredArea = true;

        growing = false;

        datapointsMZs = new ArrayList<Float>();
        datapointsRTs = new ArrayList<Float>();
        datapointsIntensities = new ArrayList<Float>();

    }

    private void precalculateMZ() {
        // Calculate median MZ
        mz = MathUtils.calcQuantile(
                CollectionUtils.toFloatArray(datapointsMZs), 0.5f);
        precalcRequiredMZ = false;
    }

    private void precalculateRT() {
        // Find maximum intensity datapoint and use its RT
        float maxIntensity = 0.0f;
        for (int ind = 0; ind < datapointsIntensities.size(); ind++) {
            if (maxIntensity <= datapointsIntensities.get(ind)) {
                maxIntensity = datapointsIntensities.get(ind);
                rt = datapointsRTs.get(ind);
                height = maxIntensity;
            }
        }
        precalcRequiredRT = false;
    }

    private void precalculateArea() {

        float sum = 0.0f;

        // process all datapoints
        for (int i = 0; i < (datapointsIntensities.size() - 1); i++) {

            // X axis interval length
            final float rtDifference = datapointsRTs.get(i + 1)
                    - datapointsRTs.get(i);

            // intensity at the beginning of the interval
            final float intensityStart = datapointsIntensities.get(i);

            // intensity at the end of the interval
            final float intensityEnd = datapointsIntensities.get(i + 1);

            // calculate area of the interval
            sum += (rtDifference * (intensityStart + intensityEnd) / 2);

        }

        area = sum;

        precalcRequiredArea = false;
    }

    private void precalculateMins() {

        for (int ind = 0; ind < datapointsMZs.size(); ind++) {
            if (ind == 0) {
                rtRange = new Range(datapointsRTs.get(ind));
                mzRange = new Range(datapointsMZs.get(ind));
                intensityRange = new Range(datapointsIntensities.get(ind));
            } else {
                rtRange.extendRange(datapointsRTs.get(ind));
                mzRange.extendRange(datapointsMZs.get(ind));
                intensityRange.extendRange(datapointsIntensities.get(ind));
            }
        }
        precalcRequiredMins = false;
    }

    public void addDatapoint(int scanNumber, float mz, float rt, float intensity) {

        growing = true;
        precalcRequiredMZ = true;
        precalcRequiredRT = true;
        precalcRequiredMins = true;
        precalcRequiredArea = true;

        // Add datapoint
        DataPoint datapoint = new SimpleDataPoint(mz, intensity);

        datapointsMap.put(scanNumber, datapoint);

        // Update construction time variables
        datapointsMZs.add(mz);
        datapointsRTs.add(rt);
        datapointsIntensities.add(intensity);

    }

    public boolean isGrowing() {
        return growing;
    }

    public void resetGrowingState() {
        growing = false;
    }

    public void finalizedAddingDatapoints(PeakStatus peakStatus) {

        this.peakStatus = peakStatus;

        if (precalcRequiredMZ)
            precalculateMZ();
        if (precalcRequiredRT)
            precalculateRT();
        if (precalcRequiredMins)
            precalculateMins();
        if (precalcRequiredArea)
            precalculateArea();

        datapointsMZs = null;
        datapointsRTs = null;
        datapointsIntensities = null;

    }

    public ArrayList<Float> getConstructionIntensities() {
        return datapointsIntensities;
    }

    /**
     * @see net.sf.mzmine.data.Peak#getDataFile()
     */
    public RawDataFile getDataFile() {
        return dataFile;
    }

    public String toString() {
        return PeakUtils.peakToString(this);
    }

}
