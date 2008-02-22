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

package net.sf.mzmine.util;

import java.util.Iterator;

import net.sf.mzmine.data.IsotopePattern;
import net.sf.mzmine.data.Peak;
import net.sf.mzmine.data.PeakListRow;

/**
 * Methods for handling peaks and isotope patterns
 * 
 */
public class PeakUtils {

	/**
	 * Returns peak with the lowest m/z value of the isotope pattern
	 */
	public static Peak getLowestMZPeak(IsotopePattern pattern) {

		if (pattern == null)
			return null;

		Peak[] peaks = pattern.getOriginalPeaks();

		if ((peaks == null) || (peaks.length == 0))
			return null;

		Peak lowestMZPeak = peaks[0];
		for (Peak peak : peaks)
			if (peak.getMZ() < lowestMZPeak.getMZ())
				lowestMZPeak = peak;

		return lowestMZPeak;

	}

	/**
	 * Returns the most intense peak of the isotope pattern
	 */
	public static Peak getMostIntensePeak(IsotopePattern pattern) {

		if (pattern == null)
			return null;

		Peak[] peaks = pattern.getOriginalPeaks();

		if ((peaks == null) || (peaks.length == 0))
			return null;

		Peak mostIntensePeak = peaks[0];
		for (Peak peak : peaks)
			if (peak.getArea() > mostIntensePeak.getArea())
				mostIntensePeak = peak;

		return mostIntensePeak;

	}

	/**
	 * Returns average m/z of peaks and/or isotope patterns on the peak list
	 * row. For isotope patterns, uses the lowest m/z value of the pattern.
	 */
	public static float getAverageMZUsingLowestMZPeaks(PeakListRow peakListRow) {

		if (peakListRow == null)
			return 0.0f;

		Peak[] peaks = peakListRow.getPeaks();

		if ((peaks == null) || (peaks.length == 0))
			return 0.0f;

		float mzSum = 0.0f;
		for (Peak peak : peaks) {
			if (peak instanceof IsotopePattern)
				mzSum += getLowestMZPeak((IsotopePattern) peak).getMZ();
			else
				mzSum += peak.getMZ();
		}

		return mzSum / (float) peaks.length;

	}

	/**
	 * Returns average m/z of the isotope patterns and peaks on the row. For
	 * isotope patterns, uses m/z value of the most intense peak of the pattern.
	 */
	public static float getAverageMZUsingMostIntensePeaks(
			PeakListRow peakListRow) {

		if (peakListRow == null)
			return 0.0f;

		Peak[] peaks = peakListRow.getPeaks();

		if ((peaks == null) || (peaks.length == 0))
			return 0.0f;

		float mzSum = 0.0f;
		for (Peak peak : peaks) {
			if (peak instanceof IsotopePattern)
				mzSum += getMostIntensePeak((IsotopePattern) peak).getMZ();
			else
				mzSum += peak.getMZ();
		}

		return mzSum / (float) peaks.length;

	}

}
