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

package net.sf.mzmine.data.impl;

import java.util.Vector;
import java.util.HashSet;

import net.sf.mzmine.data.IsotopePattern;
import net.sf.mzmine.data.Peak;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.impl.AbstractDataUnit;

/**
 * Simple implementation of the PeakList interface.
 */
public class SimplePeakList extends AbstractDataUnit implements PeakList {

	private Vector<Peak> peaks;


	public SimplePeakList() {
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

	public Peak getPeak(int index) {
		return peaks.get(index);
	}

	public int indexOf(Peak peak) {
		return peaks.indexOf(peak);
	}

	/**
	 * Returns all peaks overlapping with a retention time range
	 * @param	startRT Start of the retention time range
	 * @param	endRT	End of the retention time range
	 * @return
	 */
	public Peak[] getPeaksInsideScanRange(double startRT, double endRT) {
        return getPeaksInsideScanAndMZRange(startRT, endRT, Double.MIN_VALUE, Double.MAX_VALUE);
	}


    /**
     * @see net.sf.mzmine.data.PeakList#getPeaksInsideMZRange(double, double)
     */
    public Peak[] getPeaksInsideMZRange(double startMZ, double endMZ) {
        return getPeaksInsideScanAndMZRange(Double.MIN_VALUE, Double.MAX_VALUE, startMZ, endMZ);
    }

    /**
     * @see net.sf.mzmine.data.PeakList#getPeaksInsideScanAndMZRange(double, double, double, double)
     */
    public Peak[] getPeaksInsideScanAndMZRange(double startRT, double endRT, double startMZ, double endMZ) {
        Vector<Peak> peaksInside = new Vector<Peak>();

        for (Peak p : peaks) {
            if ((p.getMinRT()<=endRT) &&
                (p.getMaxRT()>=startRT) &&
                (p.getMinMZ()<=endMZ) &&
                (p.getMaxMZ()>=startMZ)) peaksInside.add(p);
        }

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
				(p.getMaxRT()>=startRT)) {

				//if (p.getIsotopePattern()!=null) isotopePatternsInside .add(p.getIsotopePattern());
				if (p.hasData(IsotopePattern.class)) {
					isotopePatternsInside.add((IsotopePattern)(p.getData(IsotopePattern.class)[0]));
				}

			}

		return isotopePatternsInside.toArray(new IsotopePattern[0]);
	}


	/**
	 * Adds a new peak to peak list
	 */
	public void addPeak(Peak p) {
		peaks.add(p);
	}


}
