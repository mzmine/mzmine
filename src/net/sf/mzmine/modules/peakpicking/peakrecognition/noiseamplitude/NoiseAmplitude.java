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

package net.sf.mzmine.modules.peakpicking.peakrecognition.noiseamplitude;

import java.util.Iterator;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;

import net.sf.mzmine.data.ChromatographicPeak;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.modules.peakpicking.chromatogrambuilder.massconnection.Chromatogram;
import net.sf.mzmine.modules.peakpicking.peakrecognition.PeakResolver;
import net.sf.mzmine.modules.peakpicking.peakrecognition.ResolvedPeak;

/**
 * This class implements a simple peak builder. This takes all detected MzPeaks
 * in one Scan and try to find a possible relationship between each one of these
 * with MzPeaks of the previous scan. This relationship is set by a match score
 * using MatchScore class, according with the parameters of Tolerance of MZ and
 * Intensity. Also it can apply a second search for possible peaks (threshold
 * level), over a already detected peak.
 * 
 */
public class NoiseAmplitude implements PeakResolver {

	// private Logger logger = Logger.getLogger(this.getClass().getName());

	private double amplitudeOfNoise;
	private double minimumPeakHeight, minimumPeakDuration;

	public NoiseAmplitude(NoiseAmplitudeParameters parameters) {

		minimumPeakDuration = (Double) parameters
				.getParameterValue(NoiseAmplitudeParameters.minimumPeakDuration);
		minimumPeakHeight = (Double) parameters
				.getParameterValue(NoiseAmplitudeParameters.minimumPeakHeight);
		amplitudeOfNoise = (Double) parameters
				.getParameterValue(NoiseAmplitudeParameters.amplitudeOfNoise);

	}

	/**
	 */
    public ChromatographicPeak[] resolvePeaks(ChromatographicPeak chromatogram,
            int scanNumbers[], double retentionTimes[], double intensities[]) {

        Vector<ResolvedPeak> resolvedPeaks = new Vector<ResolvedPeak>();
        /*
		// This treeMap stores the score of frequency of intensity ranges
		TreeMap<Integer, Integer> binsFrequency = new TreeMap<Integer, Integer>();
		double maxIntensity = 0;
		
		
		int[] scanNumbers = dataFile.getScanNumbers(1);
		double[] chromatoIntensities = new double[scanNumbers.length];

		for (int i = 0; i < scanNumbers.length; i++) {

			ConnectedMzPeak mzValue = chromatogram
					.getConnectedMzPeak(scanNumbers[i]);
			if (mzValue != null)
				chromatoIntensities[i] = mzValue.getMzPeak().getIntensity();
			else
				chromatoIntensities[i] = 0;

			addNewIntensity(chromatoIntensities[i], binsFrequency);
			if (chromatoIntensities[i] > maxIntensity)
				maxIntensity = chromatoIntensities[i];

		}

		double noiseThreshold = getNoiseThreshold(binsFrequency, maxIntensity);

		ChromatographicPeak[] chromatographicPeaks = noiseThresholdPeaksSearch(chromatogram,
				dataFile, scanNumbers, noiseThreshold);

		if (chromatographicPeaks.length != 0) {
			for (ChromatographicPeak p : chromatographicPeaks) {
				double pLength = p.getRawDataPointsRTRange().getSize();
				double pHeight = p.getHeight();
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
		}*/

		return resolvedPeaks.toArray(new ResolvedPeak[0]);
	}

	/**
	 * 
	 * Verify a detected peak using the criteria of auto-defined noise threshold
	 * level. If some regions of the peak has an intensity below of this
	 * auto-defined level, are excluded. And besides if there are more than one
	 * region over this auto-defined level, we construct a different peak for
	 * each region.
	 * 
	 * @param SimpleChromatogram
	 *            ucPeak
	 * @return Peak[]
	 */
    /*
	private ChromatographicPeak[] noiseThresholdPeaksSearch(Chromatogram chromatogram,
			RawDataFile dataFile, int[] scanNumbers, double noiseThreshold) {

		Vector<ConnectedPeak> newChromatoPeaks = new Vector<ConnectedPeak>();
		Vector<ConnectedMzPeak> newChromatoMzPeaks = new Vector<ConnectedMzPeak>();

		for (int i = 0; i < scanNumbers.length; i++) {

			// If the intensity of this MzPeak is bigger than threshold level
			// we store it in a Vector.

			ConnectedMzPeak mzValue = chromatogram
					.getConnectedMzPeak(scanNumbers[i]);

			if (mzValue != null) {

				if (mzValue.getMzPeak().getIntensity() >= noiseThreshold) {
					newChromatoMzPeaks.add(mzValue);
				}

				// If the intensity of lower than threshold level, it could mean
				// that is the ending of this new threshold level peak
				// we store it in a Vector.

				else {

					// Verify if we add some MzPeaks to the new ConnectedPeak,
					// if
					// that is true, we create a new ConnectedPeak with all
					// stored
					// MzPeaks.

					if (newChromatoMzPeaks.size() > 0) {
						ConnectedPeak chromatoPeak = new ConnectedPeak(
								dataFile, newChromatoMzPeaks.elementAt(0));
						for (int j = 1; j < newChromatoMzPeaks.size(); j++) {
							chromatoPeak.addMzPeak(newChromatoMzPeaks
									.elementAt(j));
						}
						newChromatoMzPeaks.clear();
						newChromatoPeaks.add(chromatoPeak);
					}
				}
			}
		}

		// At least we verify if there is one last threshold peak at the end,
		// and it was not detected in the for cycle, due there is not MzPeak
		// with intensity below of threshold level to define the ending

		if (newChromatoMzPeaks.size() > 0) {
			ConnectedPeak chromatoPeak = new ConnectedPeak(dataFile,
					newChromatoMzPeaks.elementAt(0));
			for (int j = 1; j < newChromatoMzPeaks.size(); j++) {
				chromatoPeak.addMzPeak(newChromatoMzPeaks.elementAt(j));
			}
			newChromatoMzPeaks.clear();
			newChromatoPeaks.add(chromatoPeak);
		}

		return newChromatoPeaks.toArray(new ConnectedPeak[0]);
	}

	/**
	 * This method put a new intensity into a treeMap and score the frequency
	 * (the number of times that is present this level of intensity)
	 * 
	 * @param intensity
	 * @param binsFrequency
	 */
	public void addNewIntensity(double intensity,
			TreeMap<Integer, Integer> binsFrequency) {
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

	}

	/**
	 * This method returns the noise threshold level. This level is calculated
	 * using the intensity with more datapoints.
	 * 
	 * 
	 * @param binsFrequency
	 * @param maxIntensity
	 * @return
	 */
	public double getNoiseThreshold(TreeMap<Integer, Integer> binsFrequency,
			double maxIntensity) {

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

		double noiseThreshold = (numberOfBin + 2) * amplitudeOfNoise;
		double percentage = noiseThreshold / maxIntensity;
		if (percentage > 0.3)
			noiseThreshold = amplitudeOfNoise;

		return noiseThreshold;
	}

}
