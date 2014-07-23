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

package net.sf.mzmine.modules.peaklistmethods.peakpicking.shapemodeler.peakmodels;

import java.util.TreeMap;

import javax.annotation.Nonnull;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.IsotopePattern;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.impl.SimpleDataPoint;
import net.sf.mzmine.util.PeakUtils;
import net.sf.mzmine.util.Range;

public class TrianglePeakModel implements Feature {

    // Model information
    private double rtRight = -1, rtLeft = -1;
    private double alpha, beta;

    // Peak information
    private double rt, height, mz, area;
    private int[] scanNumbers;
    private RawDataFile rawDataFile;
    private FeatureStatus status;
    private int representativeScan = -1, fragmentScan = -1;
    private Range rawDataPointsIntensityRange, rawDataPointsMZRange,
	    rawDataPointsRTRange;
    private TreeMap<Integer, DataPoint> dataPointsMap;

    // Isotope pattern. Null by default but can be set later by deisotoping
    // method.
    private IsotopePattern isotopePattern;
    private int charge = 0;

    public double getArea() {
	return area;
    }

    public @Nonnull
    RawDataFile getDataFile() {
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

    public @Nonnull
    FeatureStatus getFeatureStatus() {
	return status;
    }

    public double getRT() {
	return rt;
    }

    public @Nonnull
    Range getRawDataPointsIntensityRange() {
	return rawDataPointsIntensityRange;
    }

    public @Nonnull
    Range getRawDataPointsMZRange() {
	return rawDataPointsMZRange;
    }

    public @Nonnull
    Range getRawDataPointsRTRange() {
	return rawDataPointsRTRange;
    }

    public int getRepresentativeScanNumber() {
	return representativeScan;
    }

    public @Nonnull
    int[] getScanNumbers() {
	return scanNumbers;
    }

    public String getName() {
	return "Triangle peak " + PeakUtils.peakToString(this);
    }

    public IsotopePattern getIsotopePattern() {
	return isotopePattern;
    }

    public void setIsotopePattern(@Nonnull IsotopePattern isotopePattern) {
	this.isotopePattern = isotopePattern;
    }

    public TrianglePeakModel(Feature originalDetectedShape,
	    int[] scanNumbers, double[] intensities, double[] retentionTimes,
	    double resolution) {

	height = originalDetectedShape.getHeight();
	rt = originalDetectedShape.getRT();
	mz = originalDetectedShape.getMZ();
	this.scanNumbers = scanNumbers;
	rawDataFile = originalDetectedShape.getDataFile();

	// Create a copy of the mutable properties
	rawDataPointsIntensityRange = new Range(
		originalDetectedShape.getRawDataPointsIntensityRange());
	rawDataPointsMZRange = new Range(
		originalDetectedShape.getRawDataPointsMZRange());
	rawDataPointsRTRange = new Range(
		originalDetectedShape.getRawDataPointsRTRange());

	dataPointsMap = new TreeMap<Integer, DataPoint>();
	status = originalDetectedShape.getFeatureStatus();

	rtRight = retentionTimes[retentionTimes.length - 1];
	rtLeft = retentionTimes[0];

	alpha = (double) Math.atan(height / (rt - rtLeft));
	beta = (double) Math.atan(height / (rtRight - rt));

	// Calculate intensity of each point in the shape.
	double shapeHeight, currentRT, previousRT, previousHeight;

	previousHeight = calculateIntensity(retentionTimes[0]);
	previousRT = retentionTimes[0] * 60d;

	for (int i = 0; i < retentionTimes.length; i++) {

	    currentRT = retentionTimes[i] * 60d;

	    shapeHeight = calculateIntensity(currentRT);
	    SimpleDataPoint mzPeak = new SimpleDataPoint(mz, shapeHeight);
	    dataPointsMap.put(scanNumbers[i], mzPeak);

	    area += (currentRT - previousRT) * (shapeHeight + previousHeight)
		    / 2;
	    previousRT = currentRT;
	    previousHeight = shapeHeight;
	}

    }

    private double calculateIntensity(double retentionTime) {

	double intensity = 0;
	if ((retentionTime > rtLeft) && (retentionTime < rtRight)) {
	    if (retentionTime <= rt) {
		intensity = (double) Math.tan(alpha) * (retentionTime - rtLeft);
	    }
	    if (retentionTime > rt) {
		intensity = (double) Math.tan(beta) * (rtRight - retentionTime);
	    }
	}

	return intensity;
    }

    public int getCharge() {
	return charge;
    }

    public void setCharge(int charge) {
	this.charge = charge;
    }

}
