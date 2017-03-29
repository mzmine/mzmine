/*
 * Copyright 2006-2015 The MZmine 2 Development Team
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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.peaklistmethods.gapfilling.samerange;

import java.util.TreeMap;

import javax.annotation.Nonnull;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.IsotopePattern;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.util.MathUtils;
import net.sf.mzmine.util.PeakUtils;
import net.sf.mzmine.util.ScanUtils;

import com.google.common.collect.Range;
import com.google.common.primitives.Ints;
import net.sf.mzmine.datamodel.impl.SimplePeakInformation;

/**
 * This class represents a manually picked chromatographic peak.
 */
class SameRangePeak implements Feature {
    private SimplePeakInformation peakInfo;
    
    private RawDataFile dataFile;

    // Raw M/Z, RT, Height and Area
    private double mz, rt, height, area;
    private Double fwhm = null, tf = null, af = null;

    // Boundaries of the peak
    private Range<Double> rtRange, mzRange, intensityRange;

    // Map of scan number and data point
    private TreeMap<Integer, DataPoint> mzPeakMap;

    // Number of most intense fragment scan
    private int fragmentScan, representativeScan;

    // Isotope pattern. Null by default but can be set later by deisotoping
    // method.
    private IsotopePattern isotopePattern;
    private int charge = 0;

    /**
     * Initializes empty peak for adding data points
     */
    SameRangePeak(RawDataFile dataFile) {
	this.dataFile = dataFile;
	mzPeakMap = new TreeMap<Integer, DataPoint>();
    }

    /**
     * This peak is always a result of manual peak detection, therefore MANUAL
     */
    public @Nonnull FeatureStatus getFeatureStatus() {
	return FeatureStatus.ESTIMATED;
    }

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
     * This method returns the raw area of the peak
     */
    public double getArea() {
	return area;
    }

    /**
     * This method returns numbers of scans that contain this peak
     */
    public @Nonnull int[] getScanNumbers() {
	return Ints.toArray(mzPeakMap.keySet());
    }

    /**
     * This method returns a representative datapoint of this peak in a given
     * scan
     */
    public DataPoint getDataPoint(int scanNumber) {
	return mzPeakMap.get(scanNumber);
    }

    public @Nonnull Range<Double> getRawDataPointsIntensityRange() {
	return intensityRange;
    }

    public @Nonnull Range<Double> getRawDataPointsMZRange() {
	return mzRange;
    }

    public @Nonnull Range<Double> getRawDataPointsRTRange() {
	return rtRange;
    }

    /**
     * @see net.sf.mzmine.datamodel.Feature#getDataFile()
     */
    public @Nonnull RawDataFile getDataFile() {
	return dataFile;
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
	return PeakUtils.peakToString(this);
    }

    /**
     * Adds a new data point to this peak
     * 
     * @param scanNumber
     * @param dataPoints
     * @param rawDataPoints
     */
    void addDatapoint(int scanNumber, DataPoint dataPoint) {

	double rt = dataFile.getScan(scanNumber).getRetentionTime();

	if (mzPeakMap.isEmpty()) {
	    rtRange = Range.singleton(rt);
	    mzRange = Range.singleton(dataPoint.getMZ());
	    intensityRange = Range.singleton(dataPoint.getIntensity());
	} else {
	    rtRange = rtRange.span(Range.singleton(rt));
	    mzRange = mzRange.span(Range.singleton(dataPoint.getMZ()));
	    intensityRange = intensityRange.span(Range.singleton(dataPoint
		    .getIntensity()));
	}

	mzPeakMap.put(scanNumber, dataPoint);

    }

    void finalizePeak() {

	// Trim the zero-intensity data points from the beginning and end
	while (!mzPeakMap.isEmpty()) {
	    int scanNumber = mzPeakMap.firstKey();
	    if (mzPeakMap.get(scanNumber).getIntensity() > 0)
		break;
	    mzPeakMap.remove(scanNumber);
	}
	while (!mzPeakMap.isEmpty()) {
	    int scanNumber = mzPeakMap.lastKey();
	    if (mzPeakMap.get(scanNumber).getIntensity() > 0)
		break;
	    mzPeakMap.remove(scanNumber);
	}

	// Check if we have any data points
	if (mzPeakMap.isEmpty()) {
	    throw (new IllegalStateException(
		    "Peak can not be finalized without any data points"));
	}

	// Get all scan numbers
	int allScanNumbers[] = Ints.toArray(mzPeakMap.keySet());

	// Find the data point with top intensity and use its RT and height
	for (int i = 0; i < allScanNumbers.length; i++) {
	    DataPoint dataPoint = mzPeakMap.get(allScanNumbers[i]);
	    double rt = dataFile.getScan(allScanNumbers[i]).getRetentionTime();
	    if (dataPoint.getIntensity() > height) {
		height = dataPoint.getIntensity();
		representativeScan = allScanNumbers[i];
		this.rt = rt;
	    }
	}

	// Calculate peak area
	area = 0;
	for (int i = 1; i < allScanNumbers.length; i++) {

	    // For area calculation, we use retention time in seconds
	    double previousRT = dataFile.getScan(allScanNumbers[i - 1])
		    .getRetentionTime() * 60d;
	    double currentRT = dataFile.getScan(allScanNumbers[i])
		    .getRetentionTime() * 60d;

	    double rtDifference = currentRT - previousRT;

	    // Intensity at the beginning and end of the interval
	    double previousIntensity = mzPeakMap.get(allScanNumbers[i - 1])
		    .getIntensity();
	    double thisIntensity = mzPeakMap.get(allScanNumbers[i])
		    .getIntensity();
	    double averageIntensity = (previousIntensity + thisIntensity) / 2;

	    // Calculate area of the interval
	    area += (rtDifference * averageIntensity);

	}

	// Calculate median MZ
	double mzArray[] = new double[allScanNumbers.length];
	for (int i = 0; i < allScanNumbers.length; i++) {
	    mzArray[i] = mzPeakMap.get(allScanNumbers[i]).getMZ();
	}
	this.mz = MathUtils.calcQuantile(mzArray, 0.5f);

	fragmentScan = ScanUtils.findBestFragmentScan(dataFile, rtRange,
		mzRange);

	if (fragmentScan > 0) {
	    Scan fragmentScanObject = dataFile.getScan(fragmentScan);
	    int precursorCharge = fragmentScanObject.getPrecursorCharge();
	    if ((precursorCharge > 0) && (this.charge == 0))
		this.charge = precursorCharge;
	}

    }

    public void setMZ(double mz) {
	this.mz = mz;
    }

    public int getRepresentativeScanNumber() {
	return representativeScan;
    }

    public int getMostIntenseFragmentScanNumber() {
	return fragmentScan;
    }

    public IsotopePattern getIsotopePattern() {
	return isotopePattern;
    }

    public void setIsotopePattern(@Nonnull IsotopePattern isotopePattern) {
	this.isotopePattern = isotopePattern;
    }

    public int getCharge() {
	return charge;
    }

    public void setCharge(int charge) {
	this.charge = charge;
    }

    public Double getFWHM() {
        return fwhm;
    }

    public void setFWHM(Double fwhm) {
        this.fwhm = fwhm;
    }

    public Double getTailingFactor() {
        return tf;
    }

    public void setTailingFactor(Double tf) {
        this.tf = tf;
    }

    public Double getAsymmetryFactor() {
        return af;
    }

    public void setAsymmetryFactor(Double af) {
        this.af = af;
    }

    //dulab Edit
    public void outputChromToFile(){
        int nothing = -1;
    }
    public void setPeakInformation(SimplePeakInformation peakInfoIn){
        this.peakInfo = peakInfoIn;
    }
    public SimplePeakInformation getPeakInformation(){
        return peakInfo;
    }
    //End dulab Edit


}
