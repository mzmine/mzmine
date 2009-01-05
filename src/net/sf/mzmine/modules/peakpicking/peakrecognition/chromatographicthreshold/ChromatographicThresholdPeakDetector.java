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

package net.sf.mzmine.modules.peakpicking.peakrecognition.chromatographicthreshold;

import java.util.Vector;

import net.sf.mzmine.data.ChromatographicPeak;
import net.sf.mzmine.data.MzPeak;
import net.sf.mzmine.modules.peakpicking.peakrecognition.PeakResolver;
import net.sf.mzmine.modules.peakpicking.peakrecognition.ResolvedPeak;
import net.sf.mzmine.util.MathUtils;

/**
 * This class implements a chromatographic threshold peak builder.
 * 
 */
public class ChromatographicThresholdPeakDetector implements PeakResolver {

	private double chromatographicThresholdLevel, minimumPeakHeight,
			minimumPeakDuration;

	public ChromatographicThresholdPeakDetector(
			ChromatographicThresholdPeakDetectorParameters parameters) {

		minimumPeakHeight = (Double) parameters
				.getParameterValue(ChromatographicThresholdPeakDetectorParameters.minimumPeakHeight);
		minimumPeakDuration = (Double) parameters
				.getParameterValue(ChromatographicThresholdPeakDetectorParameters.minimumPeakDuration);
		chromatographicThresholdLevel = (Double) parameters
				.getParameterValue(ChromatographicThresholdPeakDetectorParameters.chromatographicThresholdLevel);

	}

	/**
	 */
	public ChromatographicPeak[] resolvePeaks(ChromatographicPeak chromatogram,
			int scanNumbers[], double retentionTimes[], double intensities[]) {

		Vector<ResolvedPeak> resolvedPeaks = new Vector<ResolvedPeak>();

		double thresholdLevel = MathUtils.calcQuantile(intensities,
				chromatographicThresholdLevel);

		// Current region is a region of consecutive scans which all have
		// intensity above chromatographic threshold level
		int currentRegionStart = 0, currentRegionEnd;
		double currentRegionHeight;

		while (currentRegionStart < scanNumbers.length) {

			// Find a start of the region
			MzPeak startPeak = chromatogram
					.getMzPeak(scanNumbers[currentRegionStart]);
			if ((startPeak == null)
					|| (startPeak.getIntensity() < thresholdLevel)) {
				currentRegionStart++;
				continue;
			}

			currentRegionHeight = startPeak.getIntensity();

			// Search for end of the region
			currentRegionEnd = currentRegionStart + 1;
			while (currentRegionEnd < scanNumbers.length) {
				MzPeak endPeak = chromatogram
						.getMzPeak(scanNumbers[currentRegionEnd]);
				if ((endPeak == null)
						|| (endPeak.getIntensity() < thresholdLevel)) {
					break;
				}
				if (endPeak.getIntensity() > currentRegionHeight)
					currentRegionHeight = endPeak.getIntensity();
				currentRegionEnd++;
			}

			// Subtract one index, so the end index points at the last data
			// point of current region
			currentRegionEnd--;

			// Check current region, if it makes a good peak
			if ((retentionTimes[currentRegionEnd]
					- retentionTimes[currentRegionStart] >= minimumPeakDuration)
					&& (currentRegionHeight >= minimumPeakHeight)) {

				// Create a new ResolvedPeak and add it
				ResolvedPeak newPeak = new ResolvedPeak(chromatogram,
						currentRegionStart, currentRegionEnd);
				resolvedPeaks.add(newPeak);
			}

			// Find next peak region, starting from next data point
			currentRegionStart = currentRegionEnd + 1;

		}

		return resolvedPeaks.toArray(new ChromatographicPeak[0]);
	}

}
