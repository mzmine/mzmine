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

package net.sf.mzmine.modules.peakpicking.twostep.massdetection.wavelet;

import java.util.Vector;

import net.sf.mzmine.data.DataPoint;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.data.impl.SimpleDataPoint;
import net.sf.mzmine.modules.peakpicking.twostep.massdetection.MassDetector;
import net.sf.mzmine.modules.peakpicking.twostep.massdetection.MzPeak;

/*
 * This class implements the Continuous Wavelet Transform (CWT), Mexican Hat,
 * over raw datapoints of a certain spectrum. After get the spectrum in the
 * wavelet's time domain, we use the local maxima to detect possible peaks in
 * the original raw datapoints.
 */

public class WaveletMassDetector implements MassDetector {

	// parameter value
	private int scaleLevel;

	private SimpleDataPoint[] dataPoints, waveletDataPoints;
	float[] mzValues, intensityValues;

	/*
	 * Parameters of the wavelet, NPOINTS is the number of wavelet values to use
	 * The WAVELET_ESL & WAVELET_ESL indicates the Effective Support boundaries
	 */
	private static final double NPOINTS = 60000;
	private static final int WAVELET_ESL = -5;
	private static final int WAVELET_ESR = 5;

	public WaveletMassDetector(WaveletMassDetectorParameters parameters) {
		scaleLevel = (Integer) parameters
				.getParameterValue(WaveletMassDetectorParameters.scaleLevel);
	}

	public MzPeak[] getMassValues(Scan scan) {
		Scan sc = scan;
		DataPoint originalDataPoints[] = new DataPoint[0];
		originalDataPoints = sc.getDataPoints();

		// Insert necessary values (spaces) between datapoints to perform the
		// CWT
		dataPoints = insertEdge(originalDataPoints);
		waveletDataPoints = performCWT(dataPoints);

		mzValues = new float[originalDataPoints.length];
		intensityValues = new float[originalDataPoints.length];
		for (int dp = 0; dp < originalDataPoints.length; dp++) {
			mzValues[dp] = originalDataPoints[dp].getMZ();
			intensityValues[dp] = originalDataPoints[dp].getIntensity();
		}

		Vector<MzPeak> mzPeaks = new Vector<MzPeak>();

		// Find MzPeaks

		float[] waveletMzValues = new float[waveletDataPoints.length];
		float[] waveletIntensityValues = new float[waveletDataPoints.length];
		for (int dp = 0; dp < waveletDataPoints.length; dp++) {
			waveletMzValues[dp] = waveletDataPoints[dp].getMZ();
			waveletIntensityValues[dp] = waveletDataPoints[dp].getIntensity();
		}

		Vector<Integer> mzPeakInds = new Vector<Integer>();

		getAllPeakLocalMaxima(waveletIntensityValues, mzPeakInds);

		for (Integer j : mzPeakInds) {
			for (int oDPindex = 0; oDPindex < originalDataPoints.length - 1; oDPindex++) {
				if (waveletMzValues[j] == mzValues[oDPindex]) {
					//mzPeaks.add(new MzPeak(scan.getScanNumber(), j,
						//	mzValues[oDPindex], intensityValues[oDPindex]));
					mzPeaks.add(new MzPeak(scan.getScanNumber(), 
							mzValues[oDPindex], intensityValues[oDPindex]));
				}
			}
		}
		return mzPeaks.toArray(new MzPeak[0]);
	}

	/*
	 * This function insert datapoints with intensity zero, as a preprocess to
	 * perform the continuos wavelet transform
	 */
	private SimpleDataPoint[] insertEdge(DataPoint[] originalDataPoints) {
		Vector<SimpleDataPoint> edgeDataPoint = new Vector<SimpleDataPoint>();
		for (int dp = 1; dp < originalDataPoints.length - 1; dp++) {
			if ((originalDataPoints[dp].getIntensity() == 0)
					&& (originalDataPoints[dp - 1].getIntensity() > 0)
					&& (originalDataPoints[dp + 1].getIntensity() == 0)) {
				int i;
				for (i = 0; i < 20; i++) {
					SimpleDataPoint newDp = new SimpleDataPoint(
							((float) originalDataPoints[dp].getMZ() + (0.0001f * i)),
							0.0f);
					edgeDataPoint.add(newDp);
				}
			} else {
				SimpleDataPoint newDp = new SimpleDataPoint(
						(float) originalDataPoints[dp].getMZ(),
						(float) originalDataPoints[dp].getIntensity());
				edgeDataPoint.add(newDp);
			}
		}
		return edgeDataPoint.toArray(new SimpleDataPoint[0]);
	}

	/*
	 * Perform the CWT over raw data points in the selected scale level
	 */
	private SimpleDataPoint[] performCWT(DataPoint[] dataPoints) {
		int length = dataPoints.length;
		SimpleDataPoint[] cwtDataPoints = new SimpleDataPoint[length];
		double wstep = ((WAVELET_ESR - WAVELET_ESL) / NPOINTS);
		double[] W = new double[(int) NPOINTS];

		float waveletIndex = WAVELET_ESL;
		for (int j = 0; j < NPOINTS; j++) {
			W[j] = cwtMEXHATreal(waveletIndex, 1.0, 0.0);
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
					(float) intensity);
		}

		return cwtDataPoints;
	}

	/*
	 * This function calculates the wavelets's coefficients in Time domain
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
	private void getAllPeakLocalMaxima(float intensities[],
			Vector<Integer> CentroidInds) {

		int peakMaxInd = 0;
		int stopInd = intensities.length - 1;

		for (int ind = 0; ind <= stopInd; ind++) {

			while ((ind <= stopInd) && (intensities[ind] == 0)) {
				ind++;
			}
			peakMaxInd = ind;
			if (ind >= stopInd) {
				break;
			}

			// While peak is on
			while ((ind <= stopInd) && (intensities[ind] > 0)) {
				// Check if this is the maximum point of the peak
				if (intensities[ind] > intensities[peakMaxInd]) {
					peakMaxInd = ind;
				}
				ind++;
			}

			if (ind >= stopInd) {
				break;
			}

			CentroidInds.add(new Integer(peakMaxInd));
		}
	}

}
