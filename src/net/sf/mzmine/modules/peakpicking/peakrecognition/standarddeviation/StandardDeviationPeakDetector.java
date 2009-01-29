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

package net.sf.mzmine.modules.peakpicking.peakrecognition.standarddeviation;

import java.util.Arrays;
import java.util.Vector;

import net.sf.mzmine.data.ChromatographicPeak;
import net.sf.mzmine.modules.peakpicking.peakrecognition.PeakResolver;
import net.sf.mzmine.modules.peakpicking.peakrecognition.ResolvedPeak;

/**
 * 
 * 
 */
public class StandardDeviationPeakDetector implements PeakResolver {

	// private Logger logger = Logger.getLogger(this.getClass().getName());

	private double standardDeviationLevel, minimumPeakHeight,
			minimumPeakDuration;

	public StandardDeviationPeakDetector(
			StandardDeviationPeakDetectorParameters parameters) {

		minimumPeakHeight = (Double) parameters
				.getParameterValue(StandardDeviationPeakDetectorParameters.minimumPeakHeight);
		minimumPeakDuration = (Double) parameters
				.getParameterValue(StandardDeviationPeakDetectorParameters.minimumPeakDuration);
		standardDeviationLevel = (Double) parameters
				.getParameterValue(StandardDeviationPeakDetectorParameters.standardDeviationLevel);
	}

	/**
     */
	public ChromatographicPeak[] resolvePeaks(ChromatographicPeak chromatogram,
			int scanNumbers[], double retentionTimes[], double intensities[]) {

		Vector<ResolvedPeak> resolvedPeaks = new Vector<ResolvedPeak>();

		double standardDeviationlevelPeak;

		double maxIntensity = 0;

		double avgChromatoIntensities = 0;
		Arrays.sort(scanNumbers);

		for (int i = 0; i < scanNumbers.length; i++) {
			if (intensities[i] > maxIntensity)
				maxIntensity = intensities[i];
			avgChromatoIntensities += intensities[i];
		}

		avgChromatoIntensities /= scanNumbers.length;

		// If the current chromatogram has characteristics of background or just
		// noise
		// return an empty array.
		if ((avgChromatoIntensities) > (maxIntensity * 0.5f))
			return resolvedPeaks.toArray(new ResolvedPeak[0]);

		boolean activePeak = false;
		// Index of starting region of the current peak
		int totalNumberPoints = scanNumbers.length;
		int currentPeakStart = totalNumberPoints;

		standardDeviationlevelPeak = calcChromatogramThreshold(intensities,
				avgChromatoIntensities, standardDeviationLevel);

		for (int i = 0; i < totalNumberPoints; i++) {
			if ((intensities[i] > standardDeviationlevelPeak) && (!activePeak)) {
				currentPeakStart = i;
				activePeak = true;
			}

			if ((intensities[i] < standardDeviationlevelPeak) && (activePeak)) {
				if (i - currentPeakStart > 0) {
					ResolvedPeak peak = new ResolvedPeak(chromatogram,
							currentPeakStart, i);
					double pLength = peak.getRawDataPointsRTRange().getSize();
					double pHeight = peak.getHeight();
					if ((pLength >= minimumPeakDuration)
							&& (pHeight >= minimumPeakHeight)) {
						resolvedPeaks.add(peak);
					}
				}
				currentPeakStart = totalNumberPoints;
				activePeak = false;
			}
		}

		return resolvedPeaks.toArray(new ResolvedPeak[0]);
	}

	/**
	 * 
	 * @param chromatoIntensities
	 * @param avgIntensities
	 * @param chromatographicThresholdLevel
	 * @return
	 */
	private double calcChromatogramThreshold(double[] chromatoIntensities,
			double avgIntensities, double chromatographicThresholdLevel) {

		double standardDeviation = 0;
		double percentage = 1.0 - chromatographicThresholdLevel;

		for (int i = 0; i < chromatoIntensities.length; i++) {
			double deviation = chromatoIntensities[i] - avgIntensities;
			double deviation2 = deviation * deviation;
			standardDeviation += deviation2;
		}

		standardDeviation /= chromatoIntensities.length;
		standardDeviation = (double) Math.sqrt(standardDeviation);

		double avgDifference = 0;
		int cont = 0;

		for (int i = 0; i < chromatoIntensities.length; i++) {
			if (chromatoIntensities[i] < standardDeviation) {
				avgDifference += (standardDeviation - chromatoIntensities[i]);
				cont++;
			}
		}

		avgDifference /= cont;
		return standardDeviation - (avgDifference * percentage);
	}

}
