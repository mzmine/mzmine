/* Copyright 2006-2008 The MZmine Development Team
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

import java.util.Iterator;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;

import net.sf.mzmine.data.Peak;
import net.sf.mzmine.data.PeakStatus;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.modules.peakpicking.twostep.peakconstruction.ConnectedMzPeak;
import net.sf.mzmine.modules.peakpicking.twostep.peakconstruction.ConnectedPeak;

public class WaveletPeak extends ConnectedPeak {


	/**
	 * Parameters of the wavelet, The WAVELET_ESL & WAVELET_ESL indicates the
	 * Effective Support boundaries
	 */
	private static final int WAVELET_ESL = -5;
	private static final int WAVELET_ESR = 5;

	// double[] W = new double[(int) NPOINTS];
	private TreeMap<Integer, Integer> binsFrequency;
	private float amplitudeOfNoise, maxIntensity;
	private double[] waveletDataPoint;

	public WaveletPeak(RawDataFile dataFile, ConnectedMzPeak mzValue,
			float amplitudeOfNoise) {
		super(dataFile, mzValue);
		
		maxIntensity = mzValue.getMzPeak().getIntensity();

		binsFrequency = new TreeMap<Integer, Integer>();

		this.amplitudeOfNoise = amplitudeOfNoise * 2;

		int numberOfBin = (int) Math.ceil(mzValue.getMzPeak().getIntensity()
				/ amplitudeOfNoise);

		binsFrequency.put(numberOfBin, 1);
		
	}

	public void addNewIntensity(float intensity) {
		int frequencyValue = 1;
		int numberOfBin;
		if (intensity < amplitudeOfNoise)
			numberOfBin = 1;
		else
			numberOfBin = (int) Math.floor(intensity / amplitudeOfNoise);

		if (binsFrequency.containsKey(numberOfBin)) {
			frequencyValue = binsFrequency.get(numberOfBin);
			frequencyValue++;
		}
		binsFrequency.put(numberOfBin, frequencyValue);
		
		if (intensity > maxIntensity)
			maxIntensity = intensity;
	}

	/**
	 * Perform the CWT over MzPeaks (intensity) of this peak
	 * 
	 * @param dataPoints
	 */
	public void performCWT(double[] W) {

		ConnectedMzPeak[] mzValues = this.getConnectedMzPeaks();
		int length = mzValues.length;
		int scale = 1;
		waveletDataPoint = new double[length];

		/*
		 * We only perform Translation of the wavelet in the starting in scale 2
		 * and continue until one value of wavelet is bigger than MzPeak
		 * intensity. In this way we can determine which scale of the wavelet
		 * fits the raw data peak.
		 */

		int d = (int) W.length / (WAVELET_ESR - WAVELET_ESL);
		int a_esl, a_esr;
		double sqrtScaleLevel, intensity;
		boolean top= false;

		for ( int k=2; k < 5000; k+=2) {

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
					intensity += mzValues[i].getMzPeak().getIntensity() * W[ind];

				}
				intensity /= sqrtScaleLevel;
				if (intensity >= maxIntensity){
					top= true;
				}
				// Eliminate the negative part of the wavelet map
				if (intensity < 0)
					intensity = 0;
				waveletDataPoint[dx] = (float) intensity;

			}
			if(top)
				break;
			scale++;
			if (k > 50)
				k+=50;
		} 
		
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
	public Peak[] getWaveletPeaks() {

		ConnectedMzPeak[] mzValues = this.getConnectedMzPeaks();

		float noiseThreshold = this.getNoiseThreshold();

		Vector<ConnectedPeak> newWavePeaks = new Vector<ConnectedPeak>();
		Vector<ConnectedMzPeak> newMzPeaks = new Vector<ConnectedMzPeak>();

		for (int i = 0; i < mzValues.length; i++) {

			// If the intensity of this MzPeak is bigger than threshold level
			// we store it in a Vector.
			
			float intensity = mzValues[i].getMzPeak().getIntensity();
			 if ((intensity >= noiseThreshold)
					 && (waveletDataPoint[i] > 0)) {
				newMzPeaks.add(mzValues[i]);
			}

			// If the intensity is lower than threshold level, it could mean
			// that is the ending of this new threshold level peak
			// we store it in a Vector.

			else {

				// Verify if we add some MzPeaks to the new ConnectedPeak, if
				// that is true, we create a new ConnectedPeak with all stored
				// MzPeaks.

				if (newMzPeaks.size() > 0) {
					ConnectedPeak wavePeak = new ConnectedPeak(this
							.getDataFile(), newMzPeaks.elementAt(0));
					for (int j = 1; j < newMzPeaks.size(); j++) {
						wavePeak.addMzPeak(newMzPeaks.elementAt(j));
					}
					newMzPeaks.clear();
					wavePeak.finalizedAddingDatapoints(PeakStatus.DETECTED);
					newWavePeaks.add(wavePeak);
				}
			}
		}

		// At least we verify if there is one last threshold peak at the end,
		// and it was not detected in the for cycle, due there is not MzPeak
		// with intensity below of threshold level to define the ending

		if (newMzPeaks.size() > 0) {
			ConnectedPeak wavePeak = new ConnectedPeak(this.getDataFile(),
					newMzPeaks.elementAt(0));
			for (int i = 1; i < newMzPeaks.size(); i++) {
				wavePeak.addMzPeak(newMzPeaks.elementAt(i));
			}
			wavePeak.finalizedAddingDatapoints(PeakStatus.DETECTED);
			newWavePeaks.add(wavePeak);
		}

		return newWavePeaks.toArray(new ConnectedPeak[0]);

	}

	public float getNoiseThreshold() {

		int numberOfBin = 0;
		int maxFrequency = 0;

		Set<Integer> c = binsFrequency.keySet();
		Iterator<Integer> iteratorBin = c.iterator();

		while (iteratorBin.hasNext()) {
			int bin = iteratorBin.next();
			int freq = binsFrequency.get(bin);

			if (freq > maxFrequency) {
				maxFrequency = freq;
				numberOfBin = bin;
			}
		}

		float noiseThreshold = (numberOfBin + 2)  * amplitudeOfNoise;
		float percentage = noiseThreshold/maxIntensity;
		if (percentage > 0.25)
			noiseThreshold = amplitudeOfNoise;
		return noiseThreshold;
	}

}
