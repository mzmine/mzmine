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

package net.sf.mzmine.modules.peakpicking.threestep.peakconstruction.wavelet;

import java.util.Vector;
import java.util.logging.Logger;

import net.sf.mzmine.data.ChromatographicPeak;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.impl.SimpleMzPeak;
import net.sf.mzmine.modules.peakpicking.threestep.peakconstruction.ConnectedPeak;
import net.sf.mzmine.modules.peakpicking.threestep.peakconstruction.PeakBuilder;
import net.sf.mzmine.modules.peakpicking.threestep.peakconstruction.peakfillingmodels.PeakFillingModel;
import net.sf.mzmine.modules.peakpicking.threestep.xicconstruction.Chromatogram;
import net.sf.mzmine.modules.peakpicking.threestep.xicconstruction.ConnectedMzPeak;
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
public class WaveletPeakDetector implements PeakBuilder {

	private Logger logger = Logger.getLogger(this.getClass().getName());

	private double minimumPeakHeight, minimumPeakDuration,
			waveletThresholdLevel, excessLevel;
	private boolean fillingPeaks;
	private PeakFillingModel peakModel;

	/**
	 * Parameters of the wavelet, The WAVELET_ESL & WAVELET_ESL indicates the
	 * Effective Support boundaries
	 */
	private static final int WAVELET_ESL = -5;
	private static final int WAVELET_ESR = 5;
	private double maxWaveletIntensity = 0;
	private double maxIntensity = 0;
	double[] W;

	public WaveletPeakDetector(WaveletPeakDetectorParameters parameters) {

		minimumPeakHeight = (Double) parameters
				.getParameterValue(WaveletPeakDetectorParameters.minimumPeakHeight);
		minimumPeakDuration = (Double) parameters
				.getParameterValue(WaveletPeakDetectorParameters.minimumPeakDuration);
		waveletThresholdLevel = (Double) parameters
				.getParameterValue(WaveletPeakDetectorParameters.waveletThresholdLevel);
		fillingPeaks = (Boolean) parameters
				.getParameterValue(WaveletPeakDetectorParameters.fillingPeaks);
		excessLevel = (Double) parameters
				.getParameterValue(WaveletPeakDetectorParameters.excessLevel);

		String peakModelname = (String) parameters
				.getParameterValue(WaveletPeakDetectorParameters.peakModel);

		// Create an instance of selected model class
		try {

			String peakModelClassName = null;

			for (int modelIndex = 0; modelIndex < WaveletPeakDetectorParameters.peakModelNames.length; modelIndex++) {
				if (WaveletPeakDetectorParameters.peakModelNames[modelIndex]
						.equals(peakModelname))
					peakModelClassName = WaveletPeakDetectorParameters.peakModelClasses[modelIndex];
				;
			}

			if (peakModelClassName == null)
				throw new ClassNotFoundException();

			Class peakModelClass = Class.forName(peakModelClassName);

			peakModel = (PeakFillingModel) peakModelClass.newInstance();

		} catch (Exception e) {
			logger.warning("Error trying to make an instance of peak model "
					+ peakModelname);
		}

		// Pre-calculate coefficients of the wavelet
		preCalculateCWT(1000);

	}

	public ChromatographicPeak[] addChromatogram(Chromatogram chromatogram,
			RawDataFile dataFile) {

		maxWaveletIntensity = 0;
		maxIntensity = 0;

		Vector<ChromatographicPeak> detectedPeaks = new Vector<ChromatographicPeak>();

		int[] scanNumbers = dataFile.getScanNumbers(1);
		double[] chromatoIntensities = new double[scanNumbers.length];
		double avgChromatoIntensities = 0;

		for (int i = 0; i < scanNumbers.length; i++) {
			ConnectedMzPeak mzValue = chromatogram
					.getConnectedMzPeak(scanNumbers[i]);
			if (mzValue != null)
				chromatoIntensities[i] = mzValue.getMzPeak().getIntensity();
			else
				chromatoIntensities[i] = 0;
			if (chromatoIntensities[i] > maxIntensity)
				maxIntensity = chromatoIntensities[i];
			avgChromatoIntensities += chromatoIntensities[i];
		}

		avgChromatoIntensities /= scanNumbers.length;

		// Chromatogram with characteristics of background
		if ((avgChromatoIntensities) > (maxIntensity * 0.5f))
			return detectedPeaks.toArray(new ChromatographicPeak[0]);

		double[] waveletIntensities = performCWT(chromatoIntensities);

		ChromatographicPeak[] chromatographicPeaks = getWaveletPeaks(
				chromatogram, dataFile, scanNumbers, waveletIntensities);

		if (chromatographicPeaks.length != 0) {
			for (ChromatographicPeak p : chromatographicPeaks) {
				double pLength = p.getRawDataPointsRTRange().getSize();
				double pHeight = p.getHeight();
				if ((pLength >= minimumPeakDuration)
						&& (pHeight >= minimumPeakHeight)) {
					// Apply peak filling method
					if (fillingPeaks) {
						ChromatographicPeak shapeFilledPeak = peakModel.fillingPeak(p, new double[]{excessLevel});
						pLength = shapeFilledPeak.getRawDataPointsRTRange().getSize();
						pHeight = shapeFilledPeak.getHeight();
						if ((pLength >= minimumPeakDuration)
								&& (pHeight >= minimumPeakHeight)) {
						restPeaktoChromatogram(shapeFilledPeak, chromatogram);
						detectedPeaks.add(shapeFilledPeak);
						}
						else
							detectedPeaks.add(p);
					} else
						detectedPeaks.add(p);
				}
			}
		}

		return detectedPeaks.toArray(new ChromatographicPeak[0]);

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
	public ChromatographicPeak[] getWaveletPeaks(Chromatogram chromatogram,
			RawDataFile dataFile, int[] scanNumbers, double[] waveletIntensities) {

		double waveletThresholdLevel = calcWaveletThreshold(waveletIntensities), maxLocalWaveletIntensity = 0;
		int indexMaxPoint = 0;

		boolean activeFirstPeak = false, activeSecondPeak = false, passThreshold = false;
		int crossZero = 0;

		Vector<ConnectedPeak> newPeaks = new Vector<ConnectedPeak>();
		Vector<ConnectedMzPeak> newMzPeaks = new Vector<ConnectedMzPeak>();
		Vector<ConnectedMzPeak> newOverlappedMzPeaks = new Vector<ConnectedMzPeak>();

		for (int i = 1; i < waveletIntensities.length; i++) {
			
			double absolute = (double) waveletIntensities[i]; //Math.abs(derivativeOfIntensities[i]);
			if (( absolute > maxLocalWaveletIntensity) && (crossZero == 2)) {
				maxLocalWaveletIntensity = absolute;
				indexMaxPoint = i;
			}

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
						else {
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

			if ((!passThreshold)
					&& ((Math.abs(waveletIntensities[i]) > waveletThresholdLevel))) {
				passThreshold = true;
				if ((crossZero == 0)) {
					activeFirstPeak = true;
					crossZero++;
				}
			}

			if (((waveletIntensities[i - 1] < 0.0f) && (waveletIntensities[i] == 0.0f))) {
				activeFirstPeak = false;
				activeSecondPeak = false;
			}

			// if (true){
			if ((activeFirstPeak)) {
				ConnectedMzPeak mzValue = chromatogram
						.getConnectedMzPeak(scanNumbers[i]);
				if (mzValue != null) {
					newMzPeaks.add(mzValue);
					
					  /*ConnectedMzPeak temp = new ConnectedMzPeak(mzValue
					  .getScan(), new SimpleMzPeak( new
					  SimpleDataPoint(mzValue.getMzPeak().getMZ(), (double)
					  waveletIntensities[i]))); newMzPeaks.add(temp);*/
					 
				} else if (newMzPeaks.size() > 0) {
					activeFirstPeak = false;
					crossZero = 0;
				}
			}

			if (activeSecondPeak) {
				ConnectedMzPeak mzValue = chromatogram
						.getConnectedMzPeak(scanNumbers[i]);
				if (mzValue != null) {
					newOverlappedMzPeaks.add(mzValue);
					
					  /*ConnectedMzPeak temp = new ConnectedMzPeak(mzValue
					  .getScan(), new SimpleMzPeak( new
					  SimpleDataPoint(mzValue.getMzPeak().getMZ(), (double)
					  waveletIntensities[i]))); newOverlappedMzPeaks.add(temp);*/
					 
				}
			}

			if ((newMzPeaks.size() > 0) && (!activeFirstPeak)) {
				ConnectedPeak peak = new ConnectedPeak(dataFile, newMzPeaks
						.elementAt(0));
				for (int j = 1; j < newMzPeaks.size(); j++) {
					peak.addMzPeak(newMzPeaks.elementAt(j));
				}
				
				if (fillingPeaks) {
					
					ConnectedMzPeak mzValue = chromatogram
					.getConnectedMzPeak(scanNumbers[indexMaxPoint]);
					
					if (mzValue != null) {
						double height = mzValue.getMzPeak()
								.getIntensity();
						peak.setHeight(height);

						double rt = mzValue.getScan()
								.getRetentionTime();
						peak.setRT(rt);
					}

				indexMaxPoint = 0;
				maxLocalWaveletIntensity = 0;
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
	public double[] performCWT(double[] chromatoIntensities) {

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
	 * @param chromatoIntensities
	 * @return waveletThresholdLevel
	 */
	private double calcWaveletThreshold(double[] waveletIntensities) {

		double[] intensities = new double[waveletIntensities.length];
		for (int i = 0; i < waveletIntensities.length; i++) {
			intensities[i] = (double) Math.abs(waveletIntensities[i]);
		}

		return MathUtils.calcQuantile(intensities, waveletThresholdLevel);
	}
	
	/**
	 * @param shapeFilledPeak
	 * @param chromatogram
	 */
	public void restPeaktoChromatogram(ChromatographicPeak shapeFilledPeak, Chromatogram chromatogram){
		
		ConnectedMzPeak[] listMzPeaks = ((ConnectedPeak) shapeFilledPeak)
		.getAllMzPeaks();
		int scanNumber = 0;
		double filledIntensity = 0, originalIntensity = 0, restedIntensity;
		ConnectedMzPeak mzValue = null;
		
		for (ConnectedMzPeak mzPeak: listMzPeaks){
			scanNumber = mzPeak.getScan().getScanNumber();
			filledIntensity = mzPeak.getMzPeak().getIntensity();
			mzValue = chromatogram.getConnectedMzPeak(scanNumber);
			if (mzValue != null){
				originalIntensity = mzValue.getMzPeak().getIntensity();
				restedIntensity = originalIntensity - filledIntensity;
				if (restedIntensity < 0)
					restedIntensity = 0;
				((SimpleMzPeak) mzValue.getMzPeak()).setIntensity(restedIntensity);
			}
		}
	}

}
