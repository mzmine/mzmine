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
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.IsotopePattern;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.impl.SimpleDataPoint;
import net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution.savitzkygolay.SGDerivative;
import net.sf.mzmine.util.PeakUtils;
import net.sf.mzmine.util.RangeUtils;

import com.google.common.collect.Range;
import net.sf.mzmine.datamodel.impl.SimplePeakInformation;

public class EMGPeakModel implements Feature {
    private SimplePeakInformation peakInfo;
    private Logger logger = Logger.getLogger(this.getClass().getName());

    // EMG parameters
    private double H; // Height of EMG model
    private double M; // Time of the maximum point in EMG model
    private double Dp; // Peak width of EMG model
    private double Ap; // Asymmetry factor (skewness)
    private double C; // Excess factor of EMG model

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

    public EMGPeakModel(Feature originalDetectedShape, int[] scanNumbers,
	    double[] intensities, double[] retentionTimes, double resolution) {

	height = originalDetectedShape.getHeight();
	rt = originalDetectedShape.getRT();
	mz = originalDetectedShape.getMZ();
	this.scanNumbers = scanNumbers;

	rawDataFile = originalDetectedShape.getDataFile();

	rawDataPointsIntensityRange = originalDetectedShape
		.getRawDataPointsIntensityRange();
	rawDataPointsMZRange = originalDetectedShape.getRawDataPointsMZRange();

	dataPointsMap = new TreeMap<Integer, DataPoint>();
	status = originalDetectedShape.getFeatureStatus();

	// Initialize EMG parameters base on intensities and retention times
	initializEMGParameters(intensities, retentionTimes, rt, height);

	// Calculate intensity of each point in the shape.
	double shapeHeight, currentRT, previousRT, previousHeight;

	int allScanNumbers[] = rawDataFile.getScanNumbers(1);
	double allRetentionTimes[] = new double[allScanNumbers.length];
	for (int i = 0; i < allScanNumbers.length; i++)
	    allRetentionTimes[i] = rawDataFile.getScan(allScanNumbers[i])
		    .getRetentionTime();

	previousHeight = calculateEMGIntensity(H, M, Dp, Ap, C,
		allRetentionTimes[0]);
	previousRT = allRetentionTimes[0] * 60d;
	rawDataPointsRTRange = RangeUtils.fromArray(allRetentionTimes);

	for (int i = 0; i < allRetentionTimes.length; i++) {

	    shapeHeight = calculateEMGIntensity(H, M, Dp, Ap, C,
		    allRetentionTimes[i]);
	    if (shapeHeight > height * 0.01d) {
		SimpleDataPoint mzPeak = new SimpleDataPoint(mz, shapeHeight);
		dataPointsMap.put(allScanNumbers[i], mzPeak);
	    }

	    currentRT = allRetentionTimes[i] * 60d;
	    area += (currentRT - previousRT) * (shapeHeight + previousHeight)
		    / 2;
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

    //dulab  Edit
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
	return "EMG peak " + PeakUtils.peakToString(this);
    }

    public IsotopePattern getIsotopePattern() {
	return isotopePattern;
    }

    public void setIsotopePattern(@Nonnull IsotopePattern isotopePattern) {
	this.isotopePattern = isotopePattern;
    }

    /**
     * This method calculates the width of the chromatographic peak at half
     * intensity
     * 
     * @param listMzPeaks
     * @param height
     * @param RT
     * @return FWHM
     * 
     * 
     *         This calculation is based on work of Foley J.P., Dorsey J.G.
     *         "Equations for calculation of chromatographic figures of Merit
     *         for Ideal and Skewed Peaks", Anal. Chem. 1983, 55, 730-737.
     * */

    private void initializEMGParameters(double[] intensities,
	    double[] retentionTimes, double retentionTime, double maxIntensity) {

	// Add lateral zero values to intensities array. This allows us to
	// get better result from 2nd derivative.
	int LATERAL_OFFSET = 5;
	double[] intensitiesWithZeros = new double[intensities.length
		+ (LATERAL_OFFSET * 2)];
	// Left side zeros
	for (int i = 0; i < LATERAL_OFFSET; i++)
	    intensitiesWithZeros[i] = 0;
	// Current values
	for (int i = 0; i < intensities.length; i++)
	    intensitiesWithZeros[i + LATERAL_OFFSET] = intensities[i];
	// Right side zeros
	for (int i = intensities.length; i < intensitiesWithZeros.length; i++)
	    intensitiesWithZeros[i] = 0;

	logger.finest("Size of extended zeros = " + intensitiesWithZeros.length
		+ " original " + intensities.length);

	// First calculate the 2nd derivative of intensities in order to get the
	// inflection points of the curve in each side (left and right),
	// using a filter of level 12.
	double[] secondDerivative = SGDerivative.calculateDerivative(
		intensitiesWithZeros, false, 12);

	// Analyze the second derivative values to identify crossing zero
	// points.
	// Those positions correspond to inflection points.
	int crossZero = 0;
	int inflectionPointLeft = 0;
	int inflectionPointRight = 0;
	int index = 0;

	for (int i = 1; i < secondDerivative.length; i++) {

	    // DEBUGGING
	    // logger.finest("Second derivative [" + i+"]= " +
	    // secondDerivative[i]);

	    // Changing sign and crossing zero
	    if (((secondDerivative[i - 1] < 0.0f) && (secondDerivative[i] > 0.0f))
		    || ((secondDerivative[i - 1] > 0.0f) && (secondDerivative[i] < 0.0f))) {

		if ((secondDerivative[i - 1] < 0.0f)
			&& (secondDerivative[i] > 0.0f)) {
		    index = i - 1 - LATERAL_OFFSET;
		    if ((crossZero == 1)
			    && (retentionTimes[index] > retentionTime)) {
			inflectionPointRight = index;
			break;
		    }
		} else {
		    if ((crossZero == 0) && (i - 1 > LATERAL_OFFSET)) {
			index = i - LATERAL_OFFSET;
			if (retentionTimes[index] < retentionTime) {
			    inflectionPointLeft = index;
			    crossZero++;
			}
		    }
		}
	    }
	}

	/*
	 * The inflection point represents the tangent of the curve. We use the
	 * secant function to calculate the pendient of the tangent line.
	 * 
	 * m = f(x+h) - f(x) / h
	 * 
	 * where m is the pendient, f(x) is the intensity in inflection point
	 * and h is the time between two points defined by secant line. In our
	 * case, the identified peak's rt and height are x+h and f(x+h).
	 */
	double mLeft = (maxIntensity - intensities[inflectionPointLeft])
		/ (retentionTime - retentionTimes[inflectionPointLeft]);

	double mRight = (maxIntensity - intensities[inflectionPointRight])
		/ (retentionTimes[inflectionPointRight] - retentionTime);

	/*
	 * Then calculate peak's width at base (Wb), using tagent line. Also
	 * obtains the value of b and a (b/a = asymmetry factor). Using rect
	 * line formula
	 * 
	 * x2 = [(y2 - Y1) / m] + x1
	 * 
	 * where x2 represents the retention time value at 10% of total peak's
	 * height (y2).
	 */
	// First left side
	double a = ((maxIntensity * 0.10d) - intensities[inflectionPointLeft])
		/ mLeft;
	a += retentionTimes[inflectionPointLeft];
	// Second right side
	double b = (intensities[inflectionPointRight] - (maxIntensity * 0.10d))
		/ mRight;
	b += retentionTimes[inflectionPointRight];

	// Now assign asymmetry factor(skewness) to general variable and peak's
	// width at base
	Ap = Math.abs(b - retentionTime) / Math.abs(a - retentionTime);
	double Wb = Math.abs(b - a);

	// DEBUGGING
	// logger.finest("Value a= " +
	// MZmineCore.getConfiguration().getRTFormat().format(a) +
	// " b= " + MZmineCore.getConfiguration().getRTFormat().format(b) +
	// " Ap= " + Ap + " Wb= "
	// + Wb);

	/*
	 * Calculates the variance of asymmetric peak using the formula
	 * 
	 * Variance = Wb^2 / ([1.76 (b/a)^2] - [11.15 (b/a)] + 28)
	 * 
	 * where Wb is the peak width at base (10% of height), b is the right
	 * side of the peak and a the left side.
	 * 
	 * Sigma is calculated by
	 * 
	 * sigma = Wb / ([3.27 (b/a)] +1.2)
	 * 
	 * Now the relationship between sigma, tau and variance is defined by
	 * 
	 * Variance = (sigma)^2 + (tau)^2
	 * 
	 * so tau is obtained by
	 * 
	 * tau = sqrt( variance - sigma)
	 * 
	 * where sigma is standard deviation and tau is the time constant of
	 * exponential function.
	 */
	double variance = Math.pow(Wb, 2)
		/ (1.76d * (Math.pow(Ap, 2)) - 11.15d * (Ap) + 28.0d);
	double sigma = Wb / ((3.27d * (Ap)) + 1.2d);
	double tau = Math.sqrt(Math.abs(variance - sigma));

	// According with the relationship between standard deviation and peak's
	// width
	//
	// Wb = 4(stdDev)
	//
	// We have already Wb and sigma, so we take the average
	Dp = ((Wb / 4.0d) + sigma) / 2;

	// DEBUGGING
	// logger.finest("Value variance= " + variance + " sigma= " + sigma +
	// " tau= " + tau + " Dp= " + Dp);

	/*
	 * The relationship between skewness/excess is directly related to the
	 * proportion between tau/sigma. So excess can be calculated by
	 * 
	 * C = [sigma * (Ap)] / tau
	 * 
	 * where C is the excess value of the EMG model.
	 */
	C = (sigma * Ap) / tau;

	/*
	 * From the location of peak maximum (peak's retention time) we can
	 * calculate the center of the Gaussian part using
	 * 
	 * M = tmax - sigma[ (-0.19*(Ap)^2) + 1.16*(Ap) - 0.55 ]
	 * 
	 * where M is the retention time of the EMG model.
	 */
	M = (-0.19d * (Math.pow(Ap, 2))) + (1.16d * (Ap)) - 0.55d;
	M *= sigma;
	M += retentionTime;

	// Finally the height to use in the EMG value is approximately 20%
	// less than original peak
	H = maxIntensity * 0.80d;

	// DEBUGGING
	// logger.finest("Value C= " + C + " M= " +
	// MZmineCore.getConfiguration().getRTFormat().format(M) + " H= " + H);

    }

    /**
     * 
     * This method calculates the height of Exponential Modified Gaussian
     * function, using the mathematical model proposed by Zs. Papai and T. L.
     * Pap "Determination of chromatographic peak parameters by non-linear curve
     * fitting using statistical moments", The Analyst,
     * http://www.rsc.org/publishing/journals/AN/article.asp?doi=b111304f
     * 
     * @param H
     * @param M
     * @param Dp
     * @param Ap
     * @param C
     * @param t
     * @return intensity
     */
    private static double calculateEMGIntensity(double H, double M, double Dp,
	    double Ap, double C, double t) {
	double shapeHeight;

	double partA1 = Math.pow((t - M), 2) / (2 * Math.pow(Dp, 2));
	double partA = H * Math.exp(-1 * partA1);

	double partB1 = (t - M) / Dp;
	double partB2 = (Ap / 6.0f) * (Math.pow(partB1, 2) - (3 * partB1));
	double partB3 = (C / 24)
		* (Math.pow(partB1, 4) - (6 * Math.pow(partB1, 2)) + 3);
	double partB = 1 + partB2 + partB3;

	shapeHeight = (double) (partA * partB);

	if (shapeHeight < 0)
	    shapeHeight = 0;

	return shapeHeight;
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
