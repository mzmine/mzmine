/*
 * Copyright 2006 The MZmine Development Team
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

import net.sf.mzmine.data.Scan;

/**
 * Simple implementation of the Scan interface.
 */
public class SimpleScan implements Scan {

    private int scanNumber;
    private int msLevel;
    private int parentScan;
    private int fragmentScans[];
    private double mzValues[], intensityValues[];
    private double precursorMZ;
    private int precursorCharge;
    private double retentionTime;
    private double mzRangeMin, mzRangeMax;
    private double basePeakMZ, basePeakIntensity;
    private boolean centroided;

    /**
     * Clone constructor
     */
    public SimpleScan(Scan sc) {
        this(sc.getScanNumber(), sc.getMSLevel(), sc.getRetentionTime(),
                sc.getParentScanNumber(), sc.getPrecursorMZ(),
                sc.getFragmentScanNumbers(), sc.getMZValues(),
                sc.getIntensityValues(), sc.isCentroided());
    }

    /**
     * Constructor for creating scan with given data
     */
    public SimpleScan(int scanNumber, int msLevel, double retentionTime,
            int parentScan, double precursorMZ, int fragmentScans[],
            double[] mzValues, double[] intensityValues, boolean centroided) {

        // check assumptions about proper scan data
        assert (msLevel == 1) || (parentScan > 0);

        // save scan data
        this.scanNumber = scanNumber;
        this.msLevel = msLevel;
        this.retentionTime = retentionTime;
        this.parentScan = parentScan;
        this.precursorMZ = precursorMZ;
        this.fragmentScans = fragmentScans;
        this.centroided = centroided;

        setData(mzValues, intensityValues);

    }

    /**
     * @return Returns the intensityValues.
     */
    public double[] getIntensityValues() {
        return intensityValues;
    }

    /**
     * @return Returns the mZValues.
     */
    public double[] getMZValues() {
        return mzValues;
    }

    /**
     * @param mzValues m/z values to set
     * @param intensityValues Intensity values to set
     */
    public void setData(double[] mzValues, double[] intensityValues) {

        // check assumptions
        assert mzValues.length > 0;
        assert mzValues.length == intensityValues.length;

        this.mzValues = mzValues;
        this.intensityValues = intensityValues;

        // find m/z range and base peak
        mzRangeMin = mzValues[0];
        mzRangeMax = mzValues[0];
        basePeakMZ = mzValues[0];
        basePeakIntensity = intensityValues[0];
        for (int i = 1; i < mzValues.length; i++) {
            if (mzRangeMin > mzValues[i])
                mzRangeMin = mzValues[i];
            if (mzRangeMax < mzValues[i])
                mzRangeMax = mzValues[i];
            if (basePeakIntensity < intensityValues[i]) {
                basePeakIntensity = intensityValues[i];
                basePeakMZ = mzValues[i];
            }
        }

    }

    /**
     * @see net.sf.mzmine.data.Scan#getNumberOfDataPoints()
     */
    public int getNumberOfDataPoints() {
        return mzValues.length;
    }

    /**
     * @see net.sf.mzmine.data.Scan#getScanNumber()
     */
    public int getScanNumber() {
        return scanNumber;
    }

    /**
     * @param scanNumber The scanNumber to set.
     */
    public void setScanNumber(int scanNumber) {
        this.scanNumber = scanNumber;
    }

    /**
     * @see net.sf.mzmine.data.Scan#getMSLevel()
     */
    public int getMSLevel() {
        return msLevel;
    }

    /**
     * @param msLevel The msLevel to set.
     */
    public void setMSLevel(int msLevel) {
        this.msLevel = msLevel;
    }

    /**
     * @see net.sf.mzmine.data.Scan#getPrecursorMZ()
     */
    public double getPrecursorMZ() {
        return precursorMZ;
    }

    /**
     * @param precursorMZ The precursorMZ to set.
     */
    public void setPrecursorMZ(double precursorMZ) {
        this.precursorMZ = precursorMZ;
    }

    
    /**
     * @return Returns the precursorCharge.
     */
    public int getPrecursorCharge() {
        return precursorCharge;
    }

    
    /**
     * @param precursorCharge The precursorCharge to set.
     */
    public void setPrecursorCharge(int precursorCharge) {
        this.precursorCharge = precursorCharge;
    }

    /**
     * @see net.sf.mzmine.data.Scan#getScanAcquisitionTime()
     */
    public double getRetentionTime() {
        return retentionTime;
    }

    /**
     * @param retentionTime The retentionTime to set.
     */
    public void setRetentionTime(double retentionTime) {
        this.retentionTime = retentionTime;
    }

    /**
     * @see net.sf.mzmine.data.Scan#getMZRangeMin()
     */
    public double getMZRangeMin() {
        return mzRangeMin;
    }

    /**
     * @param mzRangeMin The mzRangeMin to set.
     */
    public void setMZRangeMin(double mzRangeMin) {
        this.mzRangeMin = mzRangeMin;
    }

    /**
     * @see net.sf.mzmine.data.Scan#getMZRangeMax()
     */
    public double getMZRangeMax() {
        return mzRangeMax;
    }

    /**
     * @param mzRangeMax The mzRangeMax to set.
     */
    public void setMZRangeMax(double mzRangeMax) {
        this.mzRangeMax = mzRangeMax;
    }

    /**
     * @see net.sf.mzmine.data.Scan#getBasePeakMZ()
     */
    public double getBasePeakMZ() {
        return basePeakMZ;
    }

    /**
     * @param basePeakMZ The basePeakMZ to set.
     */
    public void setBasePeakMZ(double basePeakMZ) {
        this.basePeakMZ = basePeakMZ;
    }

    /**
     * @see net.sf.mzmine.data.Scan#getBasePeakIntensity()
     */
    public double getBasePeakIntensity() {
        return basePeakIntensity;
    }

    /**
     * @param basePeakIntensity The basePeakIntensity to set.
     */
    public void setBasePeakIntensity(double basePeakIntensity) {
        this.basePeakIntensity = basePeakIntensity;
    }

    /**
     * @see net.sf.mzmine.data.Scan#getParentScanNumber()
     */
    public int getParentScanNumber() {
        return parentScan;
    }

    /**
     * @param parentScan The parentScan to set.
     */
    public void setParentScanNumber(int parentScan) {
        this.parentScan = parentScan;
    }

    /**
     * @see net.sf.mzmine.data.Scan#getFragmentScanNumbers()
     */
    public int[] getFragmentScanNumbers() {
        return fragmentScans;
    }

    /**
     * @param fragmentScans The fragmentScans to set.
     */
    public void setFragmentScanNumbers(int[] fragmentScans) {
        this.fragmentScans = fragmentScans;
    }

    /**
     * @see net.sf.mzmine.data.Scan#isCentroided()
     */
    public boolean isCentroided() {
        return centroided;
    }

    /**
     * @param centroided The centroided to set.
     */
    public void setCentroided(boolean centroided) {
        this.centroided = centroided;
    }

    /**
     * @see net.sf.mzmine.data.Scan#getMassTolerance()
     */
    public double getMassTolerance() {
        return 0.5;
    }

}
