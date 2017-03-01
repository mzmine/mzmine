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

package net.sf.mzmine.modules.rawdatamethods.peakpicking.massdetection.wavelet;

import java.util.TreeSet;
import java.util.Vector;

import javax.annotation.Nonnull;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.datamodel.impl.SimpleDataPoint;
import net.sf.mzmine.modules.rawdatamethods.peakpicking.massdetection.MassDetector;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.util.DataPointSorter;
import net.sf.mzmine.util.SortingDirection;
import net.sf.mzmine.util.SortingProperty;

/**
 * This class implements the Continuous Wavelet Transform (CWT), Mexican Hat,
 * over raw datapoints of a certain spectrum. After get the spectrum in the
 * wavelet's time domain, we use the local maxima to detect possible peaks in
 * the original raw datapoints.
 */
public class WaveletMassDetector implements MassDetector {

    /**
     * Parameters of the wavelet, NPOINTS is the number of wavelet values to use
     * The WAVELET_ESL & WAVELET_ESL indicates the Effective Support boundaries
     */
    private static final double NPOINTS = 60000;
    private static final int WAVELET_ESL = -5;
    private static final int WAVELET_ESR = 5;

    public DataPoint[] getMassValues(Scan scan, ParameterSet parameters) {
	double noiseLevel = parameters.getParameter(
		WaveletMassDetectorParameters.noiseLevel).getValue();
	int scaleLevel = parameters.getParameter(
		WaveletMassDetectorParameters.scaleLevel).getValue();
	double waveletWindow = parameters.getParameter(
		WaveletMassDetectorParameters.waveletWindow).getValue();

	DataPoint originalDataPoints[] = scan.getDataPoints();

	DataPoint waveletDataPoints[] = performCWT(originalDataPoints,
		waveletWindow, scaleLevel);

	DataPoint mzPeaks[] = getMzPeaks(noiseLevel, originalDataPoints,
		waveletDataPoints);

	return mzPeaks;
    }

    /**
     * Perform the CWT over raw data points in the selected scale level
     * 
     * @param dataPoints
     */
    private SimpleDataPoint[] performCWT(DataPoint[] dataPoints,
	    double waveletWindow, int scaleLevel) {
	int length = dataPoints.length;
	SimpleDataPoint[] cwtDataPoints = new SimpleDataPoint[length];
	double wstep = ((WAVELET_ESR - WAVELET_ESL) / NPOINTS);
	double[] W = new double[(int) NPOINTS];

	double waveletIndex = WAVELET_ESL;
	for (int j = 0; j < NPOINTS; j++) {
	    // Pre calculate the values of the wavelet
	    W[j] = cwtMEXHATreal(waveletIndex, waveletWindow, 0.0);
	    waveletIndex += wstep;
	}

	/*
	 * We only perform Translation of the wavelet in the selected scale
	 */
	int d = (int) NPOINTS / (WAVELET_ESR - WAVELET_ESL);
	int a_esl = scaleLevel * WAVELET_ESL;
	int a_esr = scaleLevel * WAVELET_ESR;
	double sqrtScaleLevel = Math.sqrt(scaleLevel);
	for (int dx = 0; dx < length; dx++) {

	    /* Compute wavelet boundaries */
	    int t1 = a_esl + dx;
	    if (t1 < 0)
		t1 = 0;
	    int t2 = a_esr + dx;
	    if (t2 >= length)
		t2 = (length - 1);

	    /* Perform convolution */
	    double intensity = 0.0;
	    for (int i = t1; i <= t2; i++) {
		int ind = (int) (NPOINTS / 2)
			- (((int) d * (i - dx) / scaleLevel) * (-1));
		if (ind < 0)
		    ind = 0;
		if (ind >= NPOINTS)
		    ind = (int) NPOINTS - 1;
		intensity += dataPoints[i].getIntensity() * W[ind];
	    }
	    intensity /= sqrtScaleLevel;
	    // Eliminate the negative part of the wavelet map
	    if (intensity < 0)
		intensity = 0;
	    cwtDataPoints[dx] = new SimpleDataPoint(dataPoints[dx].getMZ(),
		    (double) intensity);
	}

	return cwtDataPoints;
    }

    /**
     * This function calculates the wavelets's coefficients in Time domain
     * 
     * @param double x Step of the wavelet
     * @param double a Window Width of the wavelet
     * @param double b Offset from the center of the peak
     */
    private double cwtMEXHATreal(double x, double a, double b) {
	/* c = 2 / ( sqrt(3) * pi^(1/4) ) */
	double c = 0.8673250705840776;
	double TINY = 1E-200;
	double x2;

	if (a == 0.0)
	    a = TINY;
	x = (x - b) / a;
	x2 = x * x;
	return c * (1.0 - x2) * Math.exp(-x2 / 2);
    }

    /**
     * This function searches for maximums from wavelet data points
     */
    private DataPoint[] getMzPeaks(double noiseLevel,
	    DataPoint[] originalDataPoints, DataPoint[] waveletDataPoints) {

	TreeSet<DataPoint> mzPeaks = new TreeSet<DataPoint>(
		new DataPointSorter(SortingProperty.MZ,
			SortingDirection.Ascending));

	Vector<DataPoint> rawDataPoints = new Vector<DataPoint>();
	int peakMaxInd = 0;
	int stopInd = waveletDataPoints.length - 1;

	for (int ind = 0; ind <= stopInd; ind++) {

	    while ((ind <= stopInd)
		    && (waveletDataPoints[ind].getIntensity() == 0)) {
		ind++;
	    }
	    peakMaxInd = ind;
	    if (ind >= stopInd) {
		break;
	    }

	    // While peak is on
	    while ((ind <= stopInd)
		    && (waveletDataPoints[ind].getIntensity() > 0)) {
		// Check if this is the maximum point of the peak
		if (waveletDataPoints[ind].getIntensity() > waveletDataPoints[peakMaxInd]
			.getIntensity()) {
		    peakMaxInd = ind;
		}
		rawDataPoints.add(originalDataPoints[ind]);
		ind++;
	    }

	    if (ind >= stopInd) {
		break;
	    }

	    rawDataPoints.add(originalDataPoints[ind]);

	    if (originalDataPoints[peakMaxInd].getIntensity() > noiseLevel) {
		SimpleDataPoint peakDataPoint = new SimpleDataPoint(
			originalDataPoints[peakMaxInd].getMZ(),
			calcAproxIntensity(rawDataPoints));

		mzPeaks.add(peakDataPoint);

	    }
	    rawDataPoints.clear();
	}

	return mzPeaks.toArray(new DataPoint[0]);

    }

    private double calcAproxIntensity(Vector<DataPoint> rawDataPoints) {

	double aproxIntensity = 0;

	for (DataPoint d : rawDataPoints) {
	    if (d.getIntensity() > aproxIntensity)
		aproxIntensity = d.getIntensity();
	}
	return aproxIntensity;
    }

    @Override
    public @Nonnull String getName() {
	return "Wavelet transform";
    }

    @Override
    public @Nonnull Class<? extends ParameterSet> getParameterSetClass() {
	return WaveletMassDetectorParameters.class;
    }

}
