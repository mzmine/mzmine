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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.peaklistmethods.peakpicking.shapemodeler.peakmodels;

import java.util.Iterator;
import java.util.TreeMap;

import javax.annotation.Nonnull;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.IsotopePattern;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.impl.SimpleDataPoint;
import net.sf.mzmine.util.PeakUtils;
import net.sf.mzmine.util.RangeUtils;

import com.google.common.collect.Range;
import net.sf.mzmine.datamodel.impl.SimplePeakInformation;

public class GaussianPeakModel implements Feature {
    private SimplePeakInformation peakInfo;
    private double FWHM, partC, part2C2;

    // Peak information
    private double rt, height, mz, area;
    private Double fwhm = null, tf = null, af = null;
    private int[] scanNumbers;
    private RawDataFile rawDataFile;
    private FeatureStatus status;
    private int representativeScan = -1, fragmentScan = -1;
    private Range<Double> rawDataPointsIntensityRange, rawDataPointsMZRange,
	    rawDataPointsRTRange;
    private TreeMap<Integer, DataPoint> dataPointsMap;

    // Isotope pattern. Null by default but can be set later by deisotoping
    // method.
    private IsotopePattern isotopePattern;
    private int charge = 0;

    private static double CONST = 2.354820045;

    public GaussianPeakModel(Feature originalDetectedShape, int[] scanNumbers,
	    double[] intensities, double[] retentionTimes, double resolution) {

	height = originalDetectedShape.getHeight();
	rt = originalDetectedShape.getRT();
	mz = originalDetectedShape.getMZ();
	rawDataFile = originalDetectedShape.getDataFile();

	rawDataPointsIntensityRange = originalDetectedShape
		.getRawDataPointsIntensityRange();
	rawDataPointsMZRange = originalDetectedShape.getRawDataPointsMZRange();

	dataPointsMap = new TreeMap<Integer, DataPoint>();
	status = originalDetectedShape.getFeatureStatus();

	// FWFM (Full Width at Half Maximum)
	FWHM = calculateWidth(intensities, retentionTimes, resolution, rt, mz,
		height);
	// FWHM = MathUtils.calcStd(intensities) * 2.355;

	partC = FWHM / CONST;
	part2C2 = 2f * (double) Math.pow(partC, 2);

	// Calculate intensity of each point in the shape.
	double shapeHeight, currentRT, previousRT, previousHeight;

	int allScanNumbers[] = rawDataFile.getScanNumbers(1);
	double allRetentionTimes[] = new double[allScanNumbers.length];
	for (int i = 0; i < allScanNumbers.length; i++)
	    allRetentionTimes[i] = rawDataFile.getScan(allScanNumbers[i])
		    .getRetentionTime();

	previousHeight = calculateIntensity(allRetentionTimes[0]);
	previousRT = allRetentionTimes[0] * 60d;
	rawDataPointsRTRange = RangeUtils.fromArray(allRetentionTimes);

	for (int i = 0; i < allRetentionTimes.length; i++) {

	    currentRT = allRetentionTimes[i] * 60d;
	    shapeHeight = calculateIntensity(currentRT);
	    if (shapeHeight > height * 0.01d) {
		SimpleDataPoint mzPeak = new SimpleDataPoint(mz, shapeHeight);
		dataPointsMap.put(allScanNumbers[i], mzPeak);
		area += ((currentRT - previousRT) * (shapeHeight + previousHeight)) / 2;
	    }

	    previousRT = currentRT;
	    previousHeight = shapeHeight;
	}

	int[] newScanNumbers = new int[dataPointsMap.keySet().size()];
	int i = 0;
	Iterator<Integer> itr = dataPointsMap.keySet().iterator();
	while (itr.hasNext()) {
	    int number = itr.next();
	    newScanNumbers[i] = number;
	    i++;
	}

	this.scanNumbers = newScanNumbers;

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

    public double getArea() {
	return area;
    }

    public @Nonnull RawDataFile getDataFile() {
	return rawDataFile;
    }

    public double getHeight() {
	return height;
    }

    public double getMZ() {
	return mz;
    }

    public int getMostIntenseFragmentScanNumber() {
	return fragmentScan;
    }

    public DataPoint getDataPoint(int scanNumber) {
	return dataPointsMap.get(scanNumber);
    }

    public @Nonnull FeatureStatus getFeatureStatus() {
	return status;
    }

    public double getRT() {
	return rt;
    }

    public @Nonnull Range<Double> getRawDataPointsIntensityRange() {
	return rawDataPointsIntensityRange;
    }

    public @Nonnull Range<Double> getRawDataPointsMZRange() {
	return rawDataPointsMZRange;
    }

    public @Nonnull Range<Double> getRawDataPointsRTRange() {
	return rawDataPointsRTRange;
    }

    public int getRepresentativeScanNumber() {
	return representativeScan;
    }

    public @Nonnull int[] getScanNumbers() {
	return scanNumbers;
    }

    public String getName() {
	return "Gaussian peak " + PeakUtils.peakToString(this);
    }

    public IsotopePattern getIsotopePattern() {
	return isotopePattern;
    }

    public void setIsotopePattern(@Nonnull IsotopePattern isotopePattern) {
	this.isotopePattern = isotopePattern;
    }

    public double calculateIntensity(double retentionTime) {

	// Using the Gaussian function we calculate the intensity at given m/z
	double diff2 = (double) Math.pow(retentionTime - rt, 2);
	double exponent = -1 * (diff2 / part2C2);
	double eX = (double) Math.exp(exponent);
	double intensity = height * eX;
	return intensity;
    }

    /**
     * This method calculates the width of the chromatographic peak at half
     * intensity
     * 
     * @param listMzPeaks
     * @param height
     * @param RT
     * @return FWHM
     */
    private static double calculateWidth(double[] intensities,
	    double[] retentionTimes, double resolution, double retentionTime,
	    double mass, double maxIntensity) {

	double halfIntensity = maxIntensity / 2, intensity = 0, intensityPlus = 0;
	double beginning = retentionTimes[0];
	double ending = retentionTimes[retentionTimes.length - 1];
	double xRight = -1;
	double xLeft = -1;

	for (int i = 0; i < intensities.length - 1; i++) {

	    intensity = intensities[i];
	    intensityPlus = intensities[i + 1];

	    if (intensity > maxIntensity)
		continue;

	    // Left side of the curve
	    if (retentionTimes[i] < retentionTime) {
		if ((intensity <= halfIntensity)
			&& (intensityPlus >= halfIntensity)) {

		    // First point with intensity just less than half of total
		    // intensity
		    double leftY1 = intensity;
		    double leftX1 = retentionTimes[i];

		    // Second point with intensity just bigger than half of
		    // total
		    // intensity
		    double leftY2 = intensityPlus;
		    double leftX2 = retentionTimes[i + 1];

		    // We calculate the slope with formula m = Y1 - Y2 / X1 - X2
		    double mLeft = (leftY1 - leftY2) / (leftX1 - leftX2);

		    // We calculate the desired point (at half intensity) with
		    // the
		    // linear equation
		    // X = X1 + [(Y - Y1) / m ], where Y = half of total
		    // intensity
		    xLeft = leftX1 + (((halfIntensity) - leftY1) / mLeft);
		    continue;
		}
	    }

	    // Right side of the curve
	    if (retentionTimes[i] > retentionTime) {
		if ((intensity >= halfIntensity)
			&& (intensityPlus <= halfIntensity)) {

		    // First point with intensity just bigger than half of total
		    // intensity
		    double rightY1 = intensity;
		    double rightX1 = retentionTimes[i];

		    // Second point with intensity just less than half of total
		    // intensity
		    double rightY2 = intensityPlus;
		    double rightX2 = retentionTimes[i + 1];

		    // We calculate the slope with formula m = Y1 - Y2 / X1 - X2
		    double mRight = (rightY1 - rightY2) / (rightX1 - rightX2);

		    // We calculate the desired point (at half intensity) with
		    // the
		    // linear equation
		    // X = X1 + [(Y - Y1) / m ], where Y = half of total
		    // intensity
		    xRight = rightX1 + (((halfIntensity) - rightY1) / mRight);
		    break;
		}
	    }
	}

	if ((xRight <= -1) && (xLeft > 0)) {
	    xRight = retentionTime + (ending - beginning) / 4.71f;
	}

	if ((xRight > 0) && (xLeft <= -1)) {
	    xLeft = retentionTime - (ending - beginning) / 4.71f;
	}

	boolean negative = (((xRight - xLeft)) < 0);

	if ((negative) || ((xRight == -1) && (xLeft == -1))) {
	    xRight = retentionTime + (ending - beginning) / 9.42f;
	    xLeft = retentionTime - (ending - beginning) / 9.42f;
	}

	double aproximatedFWHM = (xRight - xLeft);

	return aproximatedFWHM;
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
    // added for new update in feature interface
    public double getMZrangeMSMS (){
    	return 0;
    }
    public double getRTrangeMSMS (){
    	return 0;
    }

}
