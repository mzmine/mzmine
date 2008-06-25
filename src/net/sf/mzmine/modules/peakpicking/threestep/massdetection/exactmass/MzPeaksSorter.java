/*
 * Copyright 2006-2007 The MZmine Development Team
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

package net.sf.mzmine.modules.peakpicking.threestep.massdetection.exactmass;

import java.util.Comparator;

import net.sf.mzmine.modules.peakpicking.twostep.massdetection.MzPeak;

/**
 * This class implements Comparator class to provide a comparison between to
 * MzPeak. This comparison could be by MZ or intensity values, and also could be
 * less than or bigger than. This behavior is defined by the two booleans in the
 * constructor.
 * 
 */
public class MzPeaksSorter implements Comparator<MzPeak> {

	private boolean sortByMZ, ascending;

	/**
	 * This constructor receives two booleans to define the behavior of this
	 * comparator. Comparison based on MZ or intensity values, and "less than"
	 * or "bigger than"
	 * 
	 * @param sortByMZ
	 * @param ascending
	 */
	MzPeaksSorter(boolean sortByMZ, boolean ascending) {
		this.sortByMZ = sortByMZ;
		this.ascending = ascending;
	}

	public int compare(MzPeak dp1, MzPeak dp2) {
		Float mz1 = 0.0f, mz2 = 0.0f;

		if (sortByMZ) {
			mz1 = dp1.getMZ();
			mz2 = dp2.getMZ();
		} else {
			mz1 = dp1.getIntensity();
			mz2 = dp2.getIntensity();
		}

		if (ascending)
			return mz1.compareTo(mz2);
		else
			return mz2.compareTo(mz1);
	}
}
