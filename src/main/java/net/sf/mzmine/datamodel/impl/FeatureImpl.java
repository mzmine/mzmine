/*
 * Copyright 2006-2014 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.datamodel.impl;

import java.util.Arrays;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.FeatureRawData;
import net.sf.mzmine.datamodel.FeatureStatus;
import net.sf.mzmine.datamodel.IsotopePattern;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.util.PeakUtils;

import com.google.common.collect.Range;

/**
 * This class is a simple implementation of the peak interface.
 */
public class FeatureImpl implements Feature {

    private FeatureStatus peakStatus;
    private RawDataFile dataFile;
    private FeatureRawData featureRawData;

    // Scan numbers
    private int scanNumbers[];

    private DataPoint dataPointsPerScan[];

    // M/Z, RT, Height and Area
    private double mz, rt, height, area;

    // Boundaries of the peak raw data points
    private Range<Double> rtRange, mzRange, intensityRange;

    // Number of representative scan
    private int representativeScan;

    // Number of most intense fragment scan
    private int fragmentScanNumber;

    // Isotope pattern. Null by default but can be set later by deisotoping
    // method.
    private IsotopePattern isotopePattern;
    private int charge = 0;

    FeatureImpl() {
    }

    /**
     * Initializes a new peak using given values
     * 
     */
    public FeatureImpl(RawDataFile dataFile, double MZ, double RT,
	    double height, double area, int[] scanNumbers,
	    DataPoint[] dataPointsPerScan, FeatureStatus peakStatus,
	    int representativeScan, int fragmentScanNumber, Range<Double> rtRange,
	    Range<Double> mzRange, Range<Double> intensityRange) {

	if (dataPointsPerScan.length == 0) {
	    throw new IllegalArgumentException(
		    "Cannot create a SimplePeak instance with no data points");
	}

	this.dataFile = dataFile;
	this.mz = MZ;
	this.rt = RT;
	this.height = height;
	this.area = area;
	this.scanNumbers = scanNumbers;
	this.peakStatus = peakStatus;
	this.representativeScan = representativeScan;
	this.fragmentScanNumber = fragmentScanNumber;
	this.rtRange = rtRange;
	this.mzRange = mzRange;
	this.intensityRange = intensityRange;
	this.dataPointsPerScan = dataPointsPerScan;

    }

    /**
     * This method returns the status of the peak
     */
    @Override
    public @Nonnull FeatureStatus getFeatureStatus() {
	return peakStatus;
    }

    /**
     * This method returns M/Z value of the peak
     */
    @Override
    public double getMZ() {
	return mz;
    }

    @Override
    public void setMZ(double mz) {
	this.mz = mz;
    }

    @Override
    public void setRT(double rt) {
	this.rt = rt;
    }

    /**
     * This method returns retention time of the peak
     */
    @Override
    public double getRT() {
	return rt;
    }

    /**
     * This method returns the raw height of the peak
     */
    @Override
    public double getHeight() {
	return height;
    }

    /**
     * @param height
     *            The height to set.
     */
    @Override
    public void setHeight(double height) {
	this.height = height;
    }

    /**
     * This method returns the raw area of the peak
     */
    @Override
    public double getArea() {
	return area;
    }

    /**
     * @param area
     *            The area to set.
     */
    @Override
    public void setArea(double area) {
	this.area = area;
    }

    /**
     * This method returns numbers of scans that contain this peak
     */
    public @Nonnull int[] getScanNumbers() {
	return scanNumbers;
    }

    /**
     * This method returns a representative datapoint of this peak in a given
     * scan
     */
    public DataPoint getDataPoint(int scanNumber) {
	int index = Arrays.binarySearch(scanNumbers, scanNumber);
	if (index < 0)
	    return null;
	return dataPointsPerScan[index];
    }

    /**
     * @see net.sf.mzmine.datamodel.Feature#getDataFile()
     */
    public @Nonnull RawDataFile getDataFile() {
	return dataFile;
    }

    /**
     * @see net.sf.mzmine.datamodel.Feature#setDataFile()
     */
    public void setDataFile(RawDataFile dataFile) {
	this.dataFile = dataFile;
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
	return PeakUtils.peakToString(this);
    }

    /**
     * @see net.sf.mzmine.datamodel.Feature#getRawDataPointsIntensityRange()
     */
    public @Nonnull Range<Double> getRawDataPointsIntensityRange() {
	return intensityRange;
    }

    /**
     * @see net.sf.mzmine.datamodel.Feature#getRawDataPointsMZRange()
     */
    public @Nonnull Range<Double> getRawDataPointsMZRange() {
	return mzRange;
    }

    /**
     * @see net.sf.mzmine.datamodel.Feature#getRawDataPointsRTRange()
     */
    public @Nonnull Range<Double> getRawDataPointsRTRange() {
	return rtRange;
    }

    /**
     * @see net.sf.mzmine.datamodel.Feature#getRepresentativeScanNumber()
     */
    public int getRepresentativeScanNumber() {
	return representativeScan;
    }

    public int getMostIntenseFragmentScanNumber() {
	return fragmentScanNumber;
    }

    @Override
    public IsotopePattern getIsotopePattern() {
	return isotopePattern;
    }

    @Override
    public void setIsotopePattern(@Nonnull IsotopePattern isotopePattern) {
	this.isotopePattern = isotopePattern;
    }

    @Override
    public int getCharge() {
	return charge;
    }

    @Override
    public void setCharge(int charge) {
	this.charge = charge;
    }

    @Override
    public void setFeatureStatus(@Nonnull FeatureStatus newStatus) {
	this.peakStatus = newStatus;
    }

    @Override
    @Nullable
    public FeatureRawData getRawData() {
	return featureRawData;
    }

    @Override
    public void setRawData(@Nullable FeatureRawData dataFile) {
	this.featureRawData = dataFile;

    }

}
