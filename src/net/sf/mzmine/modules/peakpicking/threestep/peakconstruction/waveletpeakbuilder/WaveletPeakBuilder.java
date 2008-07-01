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

package net.sf.mzmine.modules.peakpicking.threestep.peakconstruction.waveletpeakbuilder;

import java.util.Vector;

import net.sf.mzmine.data.Peak;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.impl.SimpleDataPoint;
import net.sf.mzmine.modules.peakpicking.threestep.massdetection.MzPeak;
import net.sf.mzmine.modules.peakpicking.threestep.peakconstruction.ConnectedPeak;
import net.sf.mzmine.modules.peakpicking.threestep.peakconstruction.PeakBuilder;
import net.sf.mzmine.modules.peakpicking.threestep.xicconstruction.Chromatogram;
import net.sf.mzmine.modules.peakpicking.threestep.xicconstruction.ConnectedMzPeak;

/**
 * This class implements a simple peak builder. This takes all detected MzPeaks
 * in one Scan and try to find a possible relationship between each one of these
 * with MzPeaks of the previous scan. This relationship is set by a match score
 * using MatchScore class, according with the parameters of Tolerance of MZ and
 * Intensity. Also it can apply a second search for possible peaks (threshold
 * level), over a already detected peak.
 * 
 */
public class WaveletPeakBuilder implements PeakBuilder {

	// private Logger logger = Logger.getLogger(this.getClass().getName());

	private float minimumPeakHeight, minimumPeakDuration, maxIntensity = 0;
	double[] W;

	/**
	 * Parameters of the wavelet, The WAVELET_ESL & WAVELET_ESL indicates the
	 * Effective Support boundaries
	 */
	private static final int WAVELET_ESL = -5;
	private static final int WAVELET_ESR = 5;
	private double maxWaveletIntensity = 0;

	public WaveletPeakBuilder(WaveletPeakBuilderParameters parameters) {

		minimumPeakHeight = (Float) parameters
				.getParameterValue(WaveletPeakBuilderParameters.minimumPeakHeight);
		minimumPeakDuration = (Float) parameters
				.getParameterValue(WaveletPeakBuilderParameters.minimumPeakDuration);

		preCalculateCWT(1000);

	}

	public Peak[] addChromatogram(Chromatogram chromatogram,
			RawDataFile dataFile) {
		
		maxWaveletIntensity = 0;

		Vector<Peak> detectedPeaks = new Vector<Peak>();

		int[] scanNumbers = dataFile.getScanNumbers(1);
		float[] chromatoIntensities = new float[scanNumbers.length];

		for (int i = 0; i < scanNumbers.length; i++) {
			ConnectedMzPeak mzValue = chromatogram
					.getConnectedMzPeak(scanNumbers[i]);
			if (mzValue != null)
				chromatoIntensities[i] = mzValue.getMzPeak().getIntensity();
			else
				chromatoIntensities[i] = 0;
			if (chromatoIntensities[i] > maxIntensity)
				maxIntensity = chromatoIntensities[i];
		}

		double[] waveletIntensities = performCWT(chromatoIntensities);

		Peak[] chromatographicPeaks = getWaveletPeaks(chromatogram, dataFile,
				scanNumbers, waveletIntensities);

		if (chromatographicPeaks.length != 0) {
			for (Peak p : chromatographicPeaks) {
				float pLength = p.getRawDataPointsRTRange().getSize();
				float pHeight = p.getHeight();
				if ((pLength >= minimumPeakDuration)
						&& (pHeight >= minimumPeakHeight)) {
					detectedPeaks.add(p);
				}
			}
		}
		else{
			ConnectedMzPeak[] cMzPeaks = chromatogram.getConnectedMzPeaks();
			
			//logger.finest("Number of cMzPeaks " + cMzPeaks.length);
			
			Vector<ConnectedMzPeak> regionOfMzPeaks = new Vector<ConnectedMzPeak>();

			if (cMzPeaks.length > 0) {
				
				for (ConnectedMzPeak mzPeak : cMzPeaks) {
			
					if (mzPeak.getMzPeak().getIntensity() > minimumPeakHeight) {
						regionOfMzPeaks.add(mzPeak);
					} else if (regionOfMzPeaks.size() != 0) {
						ConnectedPeak peak = new ConnectedPeak(dataFile,
								regionOfMzPeaks.get(0));
						for (int i = 0; i < regionOfMzPeaks.size(); i++) {
							peak.addMzPeak(regionOfMzPeaks.get(i));
						}
						regionOfMzPeaks.clear();
						detectedPeaks.add(peak);
					}
					
				}
				
				if (regionOfMzPeaks.size() != 0) {
					ConnectedPeak peak = new ConnectedPeak(dataFile,
							regionOfMzPeaks.get(0));
					for (int i = 0; i < regionOfMzPeaks.size(); i++) {
						peak.addMzPeak(regionOfMzPeaks.get(i));
					}
					detectedPeaks.add(peak);
				}
				
			}
		}

		return detectedPeaks.toArray(new Peak[0]);

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
	public Peak[] getWaveletPeaks(Chromatogram chromatogram,
			RawDataFile dataFile, int[] scanNumbers, double[] waveletIntensities) {

		boolean activeFirstPeak = false, activeSecondPeak = false, passThreshold = false;
		int crossZero = 0;

		Vector<ConnectedPeak> newPeaks = new Vector<ConnectedPeak>();
		Vector<ConnectedMzPeak> newMzPeaks = new Vector<ConnectedMzPeak>();
		Vector<ConnectedMzPeak> newOverlappedMzPeaks = new Vector<ConnectedMzPeak>();
		
		for (int i = 1; i < waveletIntensities.length; i++) {

			if (((waveletIntensities[i - 1] < 0.0f) && (waveletIntensities[i] > 0.0f))
					|| ((waveletIntensities[i - 1] > 0.0f) && (waveletIntensities[i] < 0.0f))) {

				if ((waveletIntensities[i - 1] > 0.0f)
						&& (waveletIntensities[i] < 0.0f)) {
					if (crossZero == 0) {
						activeFirstPeak = true;
					}
					if (crossZero == 2) {
						if (passThreshold)
							activeSecondPeak = true;
						else{
							newMzPeaks.clear();
							crossZero = 0;
						}
					}

				}

				if (crossZero == 3) {
					activeFirstPeak = false;
				}

				// Always clean
				passThreshold = false;
				
				if ((activeFirstPeak) || (activeSecondPeak)) {
					crossZero++;
				}

			}
			
			if (Math.abs(waveletIntensities[i]) > (maxWaveletIntensity * 0.02) ) {
				passThreshold = true;
			}

			if (((waveletIntensities[i - 1] < 0.0f) && (waveletIntensities[i] == 0.0f))) {
				activeFirstPeak = false;
				activeSecondPeak = false;
			}

			if ((activeFirstPeak)) {
				ConnectedMzPeak mzValue = chromatogram
						.getConnectedMzPeak(scanNumbers[i]);
				if (mzValue != null) {
					newMzPeaks.add(mzValue);
					/*ConnectedMzPeak temp = new ConnectedMzPeak(mzValue
							.getScan(), new MzPeak(
							new SimpleDataPoint(mzValue.getMzPeak().getMZ(),
									(float) waveletIntensities[i])));
					newMzPeaks.add(temp);*/
				}

			}

			if (activeSecondPeak) {
				ConnectedMzPeak mzValue = chromatogram
						.getConnectedMzPeak(scanNumbers[i]);
				if (mzValue != null) {
					newOverlappedMzPeaks.add(mzValue);
					/*ConnectedMzPeak temp = new ConnectedMzPeak(mzValue
							.getScan(), new MzPeak(
							new SimpleDataPoint(mzValue.getMzPeak().getMZ(),
									(float) waveletIntensities[i])));
					newOverlappedMzPeaks.add(temp);*/
				}
			}

			if ((newMzPeaks.size() > 0) && (!activeFirstPeak)) {
				ConnectedPeak peak = new ConnectedPeak(dataFile, newMzPeaks
						.elementAt(0));
				for (int j = 1; j < newMzPeaks.size(); j++) {
					peak.addMzPeak(newMzPeaks.elementAt(j));
				}
				newMzPeaks.clear();
				newPeaks.add(peak);

				if ((newOverlappedMzPeaks.size() > 0) && (activeSecondPeak)) {
					for (ConnectedMzPeak p : newOverlappedMzPeaks)
						newMzPeaks.add(p);
					activeSecondPeak = false;
					activeFirstPeak = true;
					crossZero = 2;
					newOverlappedMzPeaks.clear();
				} else
					crossZero = 0;
			}

		}

		return newPeaks.toArray(new ConnectedPeak[0]);
	}

	/**
	 * Perform the CWT over MzPeaks (intensity) of this peak
	 * 
	 * @param dataPoints
	 */
	public double[] performCWT(float[] chromatoIntensities) {

		int length = chromatoIntensities.length;
		int scale = 1;
		double[] waveletIntensities = new double[length];

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

		for (int k = 2; k < 5000; k += 2) {

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
				if (intensity >= maxIntensity) {
					top = true;
				}
				waveletIntensities[dx] = intensity;
				
				if (Math.abs(waveletIntensities[dx]) > maxWaveletIntensity)
					maxWaveletIntensity = Math.abs(waveletIntensities[dx]);

			}
			if (top)
				break;
			scale++;
			if (k > 50)
				k += 50;
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

		/* c = 2 / ( sqrt(3) * pi^(1/4) ) */
		double c = 0.8673250705840776;

		double waveletIndex = WAVELET_ESL, wavIndex2;

		for (int j = 0; j < points; j++) {
			wavIndex2 = waveletIndex * waveletIndex;
			W[j] = c * (1.0 - wavIndex2) * Math.exp(-wavIndex2 / 2);
			waveletIndex += wstep;
		}

	}

}
