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

import java.util.Vector;
import java.util.HashSet;

import net.sf.mzmine.interfaces.Peak;
import net.sf.mzmine.interfaces.PeakList;
import net.sf.mzmine.interfaces.IsotopePattern;

/**
 *
 */
public class PeakListImpl implements PeakList {

	private Vector<Peak> peaks;


	public PeakListImpl() {
		peaks = new Vector<Peak>();
	}

	/**
	 * Returns number of peaks on the list
	 */
	public int getNumberOfPeaks() {
		return peaks.size();
	}

	/**
	 * Returns all peaks in the peak list
	 */
	public Peak[] getPeaks() {
		return peaks.toArray(new Peak[peaks.size()]);
	}

	/**
	 * Returns all peaks overlapping with a retention time range
	 * @param	startRT Start of the retention time range
	 * @param	endRT	End of the retention time range
	 * @return
	 */
	public Peak[] getPeaksInsideScanRange(double startRT, double endRT) {
		Vector<Peak> peaksInside = new Vector<Peak>();

		for (Peak p : peaks)
			if ((p.getMinRT()<=endRT) &&
				(p.getMaxRT()>=startRT)) peaksInside.add(p);

		return peaksInside.toArray(new Peak[peaksInside.size()]);
	}

	/**
	 * Returns all isotope patterns overlapping with a retention time range
	 * @param	startRT Start of the retention time range
	 * @param	endRT	End of the retention time range
	 */
	public IsotopePattern[] getIsotopePatternsInsideScanRange(double startRT, double endRT) {


		HashSet<IsotopePattern> isotopePatternsInside = new HashSet<IsotopePattern>();

		for (Peak p : peaks)
			if ((p.getMinRT()<=endRT) &&
				(p.getMaxRT()>=startRT))
					if (p.getIsotopePattern()!=null) isotopePatternsInside .add(p.getIsotopePattern());


		return isotopePatternsInside.toArray(new IsotopePattern[0]);
	}


	/**
	 * Adds a new peak to peak list
	 * TODO!! this method is not in the interface...
	 */
	public void addPeak(Peak p) {
		peaks.add(p);
	}

}
