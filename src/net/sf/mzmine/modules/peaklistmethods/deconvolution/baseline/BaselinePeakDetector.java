/*
 * Copyright 2006-2010 The MZmine 2 Development Team
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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.peaklistmethods.deconvolution.baseline;

import java.util.Vector;

import net.sf.mzmine.data.ChromatographicPeak;
import net.sf.mzmine.data.DataPoint;
import net.sf.mzmine.modules.peaklistmethods.deconvolution.PeakResolver;
import net.sf.mzmine.modules.peaklistmethods.deconvolution.ResolvedPeak;

/**
 * This class implements a simple peak builder. This takes all collected MzPeaks
 * in one chromatogram and try to find all possible peaks. This detection
 * follows the concept of baseline in a chromatogram to set a peak (threshold
 * level).
 * 
 */
public class BaselinePeakDetector implements PeakResolver {

	private double minimumPeakHeight, minimumPeakDuration, baselineLevel;

	public BaselinePeakDetector(BaselinePeakDetectorParameters parameters) {
		minimumPeakHeight = (Double) parameters
				.getParameterValue(BaselinePeakDetectorParameters.minimumPeakHeight);
		minimumPeakDuration = (Double) parameters
				.getParameterValue(BaselinePeakDetectorParameters.minimumPeakDuration);
		baselineLevel = (Double) parameters
				.getParameterValue(BaselinePeakDetectorParameters.baselineLevel);
	}

	/**
     * 
     */
	public ChromatographicPeak[] resolvePeaks(ChromatographicPeak chromatogram,
			int scanNumbers[], double retentionTimes[], double intensities[]) {

		Vector<ResolvedPeak> resolvedPeaks = new Vector<ResolvedPeak>();

		// Current region is a region of consecutive scans which all have
		// intensity above baseline level
		int currentRegionStart = 0, currentRegionEnd;
		double currentRegionHeight;

		while (currentRegionStart < scanNumbers.length) {

			// Find a start of the region
			DataPoint startPeak = chromatogram
					.getDataPoint(scanNumbers[currentRegionStart]);
			if ((startPeak == null)
					|| (startPeak.getIntensity() < baselineLevel)) {
				currentRegionStart++;
				continue;
			}

			currentRegionHeight = startPeak.getIntensity();

			// Search for end of the region
			currentRegionEnd = currentRegionStart + 1;
			while (currentRegionEnd < scanNumbers.length) {
				DataPoint endPeak = chromatogram
						.getDataPoint(scanNumbers[currentRegionEnd]);
				if ((endPeak == null)
						|| (endPeak.getIntensity() < baselineLevel)) {
					break;
				}
				if (endPeak.getIntensity() > currentRegionHeight)
					currentRegionHeight = endPeak.getIntensity();
				currentRegionEnd++;
			}

			// Substract one index, so the end index points at the last data
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

		return resolvedPeaks.toArray(new ResolvedPeak[0]);
	}
}
