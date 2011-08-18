/*
 * Copyright 2006-2011 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution.minimumsearch;

import java.util.Vector;

import net.sf.mzmine.data.ChromatographicPeak;
import net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution.PeakResolver;
import net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution.ResolvedPeak;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.util.MathUtils;
import net.sf.mzmine.util.Range;

/**
 * This peak recognition method searches for local minima in the chromatogram.
 * If a local minimum is a local minimum even at a given retention time range,
 * it is considered a border between two peaks.
 */
public class MinimumSearchPeakDetector implements PeakResolver {

	private ParameterSet parameters = new MinimumSearchPeakDetectorParameters(
			this);

	public String toString() {
		return "Local minimum search";
	}

	/**
     */
	public ChromatographicPeak[] resolvePeaks(ChromatographicPeak chromatogram,
			int[] scanNumbers, double[] retentionTimes, double[] intensities) {

		assert scanNumbers.length > 0;

		double chromatographicThreshold = parameters
				.getParameter(
						MinimumSearchPeakDetectorParameters.chromatographicThresholdLevel)
				.getValue();
		double searchRTRange = parameters.getParameter(
				MinimumSearchPeakDetectorParameters.searchRTRange).getDouble();
		double minRelativeHeight = parameters.getParameter(
				MinimumSearchPeakDetectorParameters.minRelativeHeight)
				.getValue();
		double minAbsoluteHeight = parameters.getParameter(
				MinimumSearchPeakDetectorParameters.minAbsoluteHeight)
				.getDouble();
		double minRatio = parameters.getParameter(
				MinimumSearchPeakDetectorParameters.minRatio).getDouble();

		Vector<ResolvedPeak> resolvedPeaks = new Vector<ResolvedPeak>();

		// First, remove all data points below chromatographic threshold
		double chromatographicThresholdLevel = MathUtils.calcQuantile(
				intensities, chromatographicThreshold);
		for (int i = 0; i < intensities.length; i++) {
			if (intensities[i] < chromatographicThresholdLevel)
				intensities[i] = 0;
		}

		// Current region is a region between two minima, representing a
		// candidate for a resolved peak

		startSearch: for (int currentRegionStart = 0; currentRegionStart < scanNumbers.length - 1; currentRegionStart++) {

			// Find the first non-zero data point
			if (intensities[currentRegionStart] == 0)
				continue;

			double currentRegionHeight = intensities[currentRegionStart];

			endSearch: for (int currentRegionEnd = currentRegionStart + 1; currentRegionEnd < scanNumbers.length; currentRegionEnd++) {

				// Update height of current region
				if (currentRegionHeight < intensities[currentRegionEnd])
					currentRegionHeight = intensities[currentRegionEnd];

				// If the intensity is 0, we have to stop here
				if (intensities[currentRegionEnd] == 0) {

					// Find the intensity at the sides (lowest data points)
					double peakMinLeft = intensities[currentRegionStart];
					double peakMinRight = intensities[currentRegionEnd];

					// Check the shape of the peak
					if ((currentRegionHeight >= minRelativeHeight
							* chromatogram.getHeight())
							&& (currentRegionHeight >= minAbsoluteHeight)
							&& (currentRegionHeight >= peakMinLeft * minRatio)
							&& (currentRegionHeight >= peakMinRight * minRatio)) {

						ResolvedPeak newPeak = new ResolvedPeak(chromatogram,
								currentRegionStart, currentRegionEnd);
						resolvedPeaks.add(newPeak);
					}

					currentRegionStart = currentRegionEnd + 1;
					continue startSearch;
				}

				// Minimum duration of peak must be at least searchRTRange
				if (retentionTimes[currentRegionEnd]
						- retentionTimes[currentRegionStart] < searchRTRange)
					continue endSearch;

				// Set the RT range to check
				Range checkRange = new Range(retentionTimes[currentRegionEnd]
						- searchRTRange, retentionTimes[currentRegionEnd]
						+ searchRTRange);

				// Search if there is lower data point on the left from current
				// peak
				// i
				int srch = currentRegionEnd - 1;
				while ((srch > 0)
						&& (checkRange.contains(retentionTimes[srch]))) {
					if (intensities[srch] < intensities[currentRegionEnd])
						continue endSearch;
					srch--;
				}

				// Search on the right from current peak i
				srch = currentRegionEnd + 1;
				while ((srch < scanNumbers.length)
						&& (checkRange.contains(retentionTimes[srch]))) {
					if (intensities[srch] < intensities[currentRegionEnd])
						continue endSearch;
					srch++;
				}

				// Find the intensity at the sides (lowest data points)
				double peakMinLeft = intensities[currentRegionStart];
				double peakMinRight = intensities[currentRegionEnd];

				// If we have reached a minimum which is non-zero, but the peak
				// shape would not fulfill the ratio condition, continue
				// searching
				// for next minimum
				if (currentRegionHeight < peakMinRight * minRatio)
					continue endSearch;

				// Check the shape of the peak
				if ((currentRegionHeight >= minRelativeHeight
						* chromatogram.getHeight())
						&& (currentRegionHeight >= minAbsoluteHeight)
						&& (currentRegionHeight >= peakMinLeft * minRatio)
						&& (currentRegionHeight >= peakMinRight * minRatio)) {

					ResolvedPeak newPeak = new ResolvedPeak(chromatogram,
							currentRegionStart, currentRegionEnd);
					resolvedPeaks.add(newPeak);
				}

				// Start searching new region
				currentRegionStart = currentRegionEnd;

			}

		}

		return resolvedPeaks.toArray(new ChromatographicPeak[0]);

	}

	public ParameterSet getParameterSet() {
		return parameters;
	}

}
