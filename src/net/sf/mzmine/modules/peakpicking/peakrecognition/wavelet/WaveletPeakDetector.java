/*
 * Copyright 2006-2009 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.peakpicking.peakrecognition.wavelet;

import java.util.Arrays;
import java.util.Vector;
import java.util.logging.Logger;

import net.sf.mzmine.data.ChromatographicPeak;
import net.sf.mzmine.modules.peakpicking.chromatogrambuilder.massconnection.Chromatogram;
import net.sf.mzmine.modules.peakpicking.peakrecognition.PeakResolver;
import net.sf.mzmine.modules.peakpicking.peakrecognition.ResolvedPeak;
import net.sf.mzmine.util.MathUtils;

/**
 * This class implements a simple peak builder. This takes all detected MzPeaks
 * in one Scan and try to find a possible relationship between each one of these
 * with MzPeaks of the previous scan. This relationship is set by a match score
 * using MatchScore class, according with the parameters of Tolerance of MZ and
 * Intensity. Also it can apply a second search for possible peaks (threshold
 * level), over a already detected peak.
 * 
 */
public class WaveletPeakDetector implements PeakResolver {

	private Logger logger = Logger.getLogger(this.getClass().getName());

	private double minimumPeakHeight, minimumPeakDuration,
			waveletThresholdLevel;
	
	/**
	 * Parameters of the wavelet, The WAVELET_ESL & WAVELET_ESL indicates the
	 * Effective Support boundaries
	 */
	private static final int WAVELET_ESL = -5;
	private static final int WAVELET_ESR = 5;
	//private double maxWaveletIntensity = 0;
	//private double maxIntensity = 0;
	double[] W;

	public WaveletPeakDetector(WaveletPeakDetectorParameters parameters) {

		minimumPeakHeight = (Double) parameters
				.getParameterValue(WaveletPeakDetectorParameters.minimumPeakHeight);
		minimumPeakDuration = (Double) parameters
				.getParameterValue(WaveletPeakDetectorParameters.minimumPeakDuration);
		waveletThresholdLevel = (Double) parameters
				.getParameterValue(WaveletPeakDetectorParameters.waveletThresholdLevel);
		int numOfSections = (Integer) parameters
		.getParameterValue(WaveletPeakDetectorParameters.numOfSections);
		// Pre-calculate coefficients of the wavelet
		preCalculateCWT(numOfSections);

	}

    public ChromatographicPeak[] resolvePeaks(ChromatographicPeak chromatogram,
            int scanNumbers[], double retentionTimes[], double intensities[]) {

        Vector<ChromatographicPeak> resolvedPeaks = new Vector<ChromatographicPeak>();

        //maxWaveletIntensity = 0;
		//maxIntensity = 0;

		double maxIntensity = 0;
		double avgChromatoIntensities = 0;
		Arrays.sort(scanNumbers);

		for (int i = 0; i < scanNumbers.length; i++) {
			if (intensities[i] > maxIntensity)
				maxIntensity = intensities[i];
			avgChromatoIntensities += intensities[i];
		}

		avgChromatoIntensities /= scanNumbers.length;

		// If the current chromatogram has characteristics of background or just noise
		// return an empty array.
		if ((avgChromatoIntensities) > (maxIntensity * 0.5f))
			return resolvedPeaks.toArray(new ResolvedPeak[0]);

		double[] waveletIntensities = performCWT(intensities);
		double noiseThreshold = calcWaveletThreshold(waveletIntensities, waveletThresholdLevel);

		ChromatographicPeak[] chromatographicPeaks = getWaveletPeaks(
				chromatogram, scanNumbers, waveletIntensities, noiseThreshold);

		if (chromatographicPeaks.length != 0) {
			for (ChromatographicPeak p : chromatographicPeaks) {
				double pLength = p.getRawDataPointsRTRange().getSize();
				double pHeight = p.getHeight();
				if ((pLength >= minimumPeakDuration)
						&& (pHeight >= minimumPeakHeight)) {
						resolvedPeaks.add(p);
				}
			}
		}

		return resolvedPeaks.toArray(new ChromatographicPeak[0]);

	}

	/**
	 * 
	 * Verify a detected peak using the criteria of auto-defined noise level
	 * threshold level. If some regions of the peak has an intensity below of
	 * this auto-defined level, are excluded. And besides if there are more than
	 * one region over this auto-defined level, we construct a different peak
	 * for each region.
	 * 
	 * @param SimpleChromatogram
	 *            ucPeak
	 * @return Peak[]
	 */
    
	public ChromatographicPeak[] getWaveletPeaks(ChromatographicPeak chromatogram,
			int[] scanNumbers, double[] waveletIntensities, double noiseThreshold) {

        Vector<ChromatographicPeak> resolvedPeaks = new Vector<ChromatographicPeak>();

		boolean activeFirstPeak = false, activeSecondPeak = false, passThreshold = false;
		int crossZero = 0;
		
		int totalNumberPoints = waveletIntensities.length;
		
		// Indexes of start and ending of the current peak and beginning of the next
		int currentPeakStart = totalNumberPoints;
		int nextPeakStart = totalNumberPoints;
		int currentPeakEnd = 0;

		Chromatogram derivativeChromatogram = new Chromatogram(chromatogram.getDataFile());

		for (int i = 1; i < waveletIntensities.length; i++) {
			
			// DEBUGGING
			//derivativeChromatogram.addMzPeak(scanNumbers[i], 
				//new SimpleMzPeak(new SimpleDataPoint(chromatogram.getMZ(), waveletIntensities[i]*10)));

			
			if (((waveletIntensities[i - 1] < 0.0f) && (waveletIntensities[i] > 0.0f))
					|| ((waveletIntensities[i - 1] > 0.0f) && (waveletIntensities[i] < 0.0f))) {

				if ((waveletIntensities[i - 1] > 0.0f)
						&& (waveletIntensities[i] < 0.0f)) {
					if (crossZero == 0) {
						currentPeakStart = i;
						activeFirstPeak = true;
					}
					if (crossZero == 2) {
						if (passThreshold){
							activeSecondPeak = true;
							nextPeakStart = i;
						}
						else {
							currentPeakStart = i;
							crossZero = 0;
							activeFirstPeak = true;
						}
					}

				}

				if (crossZero == 3) {
					activeFirstPeak = false;
					currentPeakEnd = i;
				}

				// Always clean
				passThreshold = false;

				if ((activeFirstPeak) || (activeSecondPeak)) {
					crossZero++;
				}

			}

			// Filter for noise threshold level
			if (Math.abs(waveletIntensities[i]) > noiseThreshold) {
				passThreshold = true;
			}
			
			// Start peak region
			if ((crossZero == 0) && (waveletIntensities[i] < 0) && (!activeFirstPeak)) {
				activeFirstPeak = true;
				currentPeakStart = i;
				crossZero++;
			}
			
			// Finalize the peak region in case of zero values.
			if ((waveletIntensities[i-1] == 0) && (waveletIntensities[i] == 0) &&
				(activeFirstPeak)){
				if (crossZero < 3){
					currentPeakEnd = 0;
				}
				else{
					currentPeakEnd = i;
				}
				activeFirstPeak = false;
				activeSecondPeak = false;
				crossZero = 0;
			}

			// If exists a detected area (difference between indexes) create a
			// new resolved peak for this region of the chromatogram
			if ((currentPeakEnd - currentPeakStart > 0) && (!activeFirstPeak)) {

				ResolvedPeak newPeak = new ResolvedPeak(chromatogram,
						currentPeakStart, currentPeakEnd);
				resolvedPeaks.add((ChromatographicPeak) newPeak);

				// If exists next overlapped peak, swap the indexes between next
				// and current, and clean ending index for this new current peak
				if (activeSecondPeak) {
					activeSecondPeak = false;
					activeFirstPeak = true;
					if (waveletIntensities[i] < 0)
						crossZero = 1;
					else
						crossZero = 2;
					passThreshold = false;
					currentPeakStart = nextPeakStart;
					nextPeakStart = totalNumberPoints;
				} else {
					currentPeakStart = totalNumberPoints;
					nextPeakStart = totalNumberPoints;
					crossZero = 0;
					passThreshold = false;
					currentPeakEnd = 0;
				}
				// Reset the ending variable
				currentPeakEnd = 0;
			}
			
		}
		
		
		// DEBUGGING
		//derivativeChromatogram.finishChromatogram();
		//ChromatographicPeak peak = (ChromatographicPeak) derivativeChromatogram;
		//resolvedPeaks.add(peak);
		//logger.finest("Size of resolved peak array " + resolvedPeaks.size());

		return resolvedPeaks.toArray(new ChromatographicPeak[0]);
	}

	/**
	 * Perform the CWT over MzPeaks (intensity) of this peak
	 * 
	 * @param dataPoints
	 */
	public double[] performCWT(double[] chromatoIntensities) {

		int length = chromatoIntensities.length;
		int scale = 1;
		double[] waveletIntensities = new double[length];
        double maxWaveletIntensity = 0;
		double maxIntensity = 0;


		/*
		 * We only perform Translation of the wavelet in the starting in scale 2
		 * and continue until one value of wavelet is bigger than MzPeak
		 * intensity. In this way we can determine which scale of the wavelet
		 * fits the raw data peak.
		 */

		int d = (int) W.length / (WAVELET_ESR - WAVELET_ESL);
		int a_esl, a_esr;
		double sqrtScaleLevel, intensity;
		boolean top = false;

		for (int k = 2; k < 100; k++) {

			scale = k;
			a_esl = scale * WAVELET_ESL;
			a_esr = scale * WAVELET_ESR;
			sqrtScaleLevel = Math.sqrt(scale);
			for (int dx = 0; dx < length; dx++) {

				/* Compute wavelet boundaries */
				int t1 = a_esl + dx;
				if (t1 < 0)
					t1 = 0;
				int t2 = a_esr + dx;
				if (t2 >= length)
					t2 = (length - 1);

				/* Perform convolution */
				intensity = 0.0;
				for (int i = t1; i <= t2; i++) {
					int ind = (int) (W.length / 2)
							- (((int) d * (i - dx) / scale) * (-1));
					if (ind < 0)
						ind = 0;
					if (ind >= W.length)
						ind = W.length - 1;
					intensity += chromatoIntensities[i] * W[ind];

				}
				intensity /= sqrtScaleLevel;
				if (Math.abs(intensity) >= maxIntensity) {
					top = true;
				}
				waveletIntensities[dx] = intensity;

				if (Math.abs(waveletIntensities[dx]) > maxWaveletIntensity)
					maxWaveletIntensity = Math.abs(waveletIntensities[dx]);

			}
			if (top)
				break;
		}

		return waveletIntensities;

	}

	/**
	 * This method calculates the coefficients of our wavelet (Mex Hat).
	 */
	public void preCalculateCWT(double points) {
		W = new double[(int) points];
		int WAVELET_ESL = -5;
		int WAVELET_ESR = 5;

		double wstep = ((WAVELET_ESR - WAVELET_ESL) / points);

		/* c = 2 / ( sqrt(3) pi^(1/4) ) */
		double c = 0.8673250705840776;

		double waveletIndex = WAVELET_ESL, wavIndex2;

		for (int j = 0; j < points; j++) {
			wavIndex2 = waveletIndex * waveletIndex;
			W[j] = c * (1.0 - wavIndex2) * Math.exp(-wavIndex2 / 2);
			waveletIndex += wstep;
		}

	}

	/**
	 * 
	 * @param double[] wavelet intensities
	 * @param double comparative threshold level
	 * @return double wavelet threshold value
	 */
	private static double calcWaveletThreshold(double[] waveletIntensities, double thresholdLevel) {

		double[] intensities = new double[waveletIntensities.length];
		for (int i = 0; i < waveletIntensities.length; i++) {
			intensities[i] = (double) Math.abs(waveletIntensities[i]);
		}

		return MathUtils.calcQuantile(intensities, thresholdLevel);
	}
	
}

	