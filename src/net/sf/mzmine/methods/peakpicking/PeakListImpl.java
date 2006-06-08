/*
 * Copyright 2006 The MZmine Development Team
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

package net.sf.mzmine.methods.peakpicking;

import net.sf.mzmine.interfaces.Peak;
import net.sf.mzmine.interfaces.PeakList;
import net.sf.mzmine.interfaces.IsotopePattern;

/**
 *
 */
public class PeakListImpl implements PeakList {

	/**
	 * Returns number of peaks on the list
	 */
	public int getNumberOfPeaks() {
		// TODO
		return 0;
	}

	/**
	 * Returns all peaks in the peak list
	 */
	public Peak[] getPeaks() {
		// TODO´
		return null;
	}

	/**
	 * Returns all peaks overlapping the scan range
	 * @param	firstScan	First scan inside the range
	 * @param	lastScan	Last scan inside the range
	 */
	public Peak[] getPeaksInsideScanRange(int firstScan, int lastScan) {
		// TODO
		return null;
	}

	/**
	 * Returns all isotope patterns overlapping given scan range
	 * @param	firstScan	First scan inside the range
	 * @param	lastScan	Last scan inside the range
	 */
	public IsotopePattern[] getIsotopePatternsInsideScanRange(int firstScan, int lastScan) {
		// TODO
		return null;
	}


	/**
	 * Adds a new peak to peak list
	 * TODO!! this method is not in the interface...
	 */
	public void addPeak(Peak p) {
		// TODO
	}

}
