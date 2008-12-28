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

package net.sf.mzmine.modules.peakpicking.threestep.peakconstruction.minimumsearch;

import java.util.Arrays;
import java.util.Vector;

import net.sf.mzmine.data.ChromatographicPeak;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.modules.peakpicking.threestep.peakconstruction.ConnectedPeak;
import net.sf.mzmine.modules.peakpicking.threestep.peakconstruction.PeakBuilder;
import net.sf.mzmine.modules.peakpicking.threestep.xicconstruction.Chromatogram;
import net.sf.mzmine.modules.peakpicking.threestep.xicconstruction.ConnectedMzPeak;
import net.sf.mzmine.util.Range;

/**
 * This peak recognition method searches for local minima in the chromatogram.
 * If a local minimum is a local minimum even at a given retention time range,
 * it is considered a border between two peaks.
 */
public class MinimumSearch implements PeakBuilder {

	private double searchRTRange, minRelativeHeight, minRatio;

	public MinimumSearch(MinimumSearchParameters parameters) {
		searchRTRange = (Double) parameters
				.getParameterValue(MinimumSearchParameters.searchRTRange);
		minRelativeHeight = (Double) parameters
				.getParameterValue(MinimumSearchParameters.minRelativeHeight);
		minRatio = (Double) parameters
		.getParameterValue(MinimumSearchParameters.minRatio);

	}

	/**
	 * @see net.sf.mzmine.modules.peakpicking.threestep.peakconstruction.PeakBuilder#addChromatogram(net.sf.mzmine.modules.peakpicking.threestep.xicconstruction.Chromatogram,
	 *      net.sf.mzmine.data.RawDataFile)
	 */
	public ChromatographicPeak[] addChromatogram(Chromatogram chromatogram,
			RawDataFile dataFile) {

		/*
		 * Preparation: make a copy of the chromatogram points so we can smooth
		 * it
		 */

		ConnectedMzPeak[] cMzPeaks = chromatogram.getConnectedMzPeaks();
		ChromatogramPoint points[] = new ChromatogramPoint[cMzPeaks.length];
		for (int i = 0; i < cMzPeaks.length; i++) {
			points[i] = new ChromatogramPoint(cMzPeaks[i].getScan()
					.getRetentionTime(), cMzPeaks[i].getMzPeak().getIntensity());
		}


		/*
		 * Part 2: searching for local minima
		 */

		// Current segment is a region between two minima, representing one
		// resolved peak
		int currentSegmentStart = 0;

		Vector<ConnectedPeak> resolvedPeaks = new Vector<ConnectedPeak>();

		minimumSearch: for (int i = 1; i < cMzPeaks.length; i++) {

			// Minimum duration of peak must be at least searchRTRange
			if (points[i].getRetentionTime()
					- points[currentSegmentStart].getRetentionTime() < searchRTRange)
				continue;

			// Set the RT range to check
			Range checkRange = new Range(points[i].getRetentionTime()
					- searchRTRange, points[i].getRetentionTime()
					+ searchRTRange);

			// Search on the left from current peak i
			int srch = i - 1;
			while ((srch > 0)
					&& (checkRange.contains(points[srch].getRetentionTime()))) {
				if (points[srch].getIntensity() < points[i].getIntensity())
					continue minimumSearch;
				srch--;
			}

			// Search on the right from current peak i
			srch = i + 1;
			while ((srch < cMzPeaks.length)
					&& (checkRange.contains(points[srch].getRetentionTime()))) {
				if (points[srch].getIntensity() < points[i].getIntensity())
					continue minimumSearch;
				srch++;
			}

			// Found a good minimum, so remove zero data points from the sides
			// of
			// the peak. We have to look at the original data points, not
			// smoothed ones.
			int currentPeakStart = currentSegmentStart;
			int currentPeakEnd = i;
			while ((currentPeakStart < currentPeakEnd)
					&& (cMzPeaks[currentPeakStart].getMzPeak().getIntensity() == 0))
				currentPeakStart++;
			while ((currentPeakEnd > 0)
					&& (cMzPeaks[currentPeakEnd].getMzPeak().getIntensity() == 0))
				currentPeakEnd--;

			// Create a new peak, if the retention time span is at least
			// searchRTRange
			if (points[currentPeakEnd].getRetentionTime()
					- points[currentPeakStart].getRetentionTime() > searchRTRange) {
				// Check the shape of the peak

				ConnectedPeak currentPeak = new ConnectedPeak(dataFile,
						cMzPeaks[currentSegmentStart]);
				for (int j = currentSegmentStart; j <= i; j++) {
					currentPeak.addMzPeak(cMzPeaks[j]);
				}
				double peakMax = currentPeak.getHeight();
				if ((peakMax >= minRelativeHeight * chromatogram.getIntensity()) &&
						(peakMax >= cMzPeaks[currentSegmentStart +1 ].getMzPeak().getIntensity() * minRatio) &&
						(peakMax >= cMzPeaks[i - 1].getMzPeak().getIntensity() * minRatio) 
						)
					resolvedPeaks.add(currentPeak);

			}

			// Start searching new segment
			currentSegmentStart = i;

		}

		return resolvedPeaks.toArray(new ChromatographicPeak[0]);

	}

}
