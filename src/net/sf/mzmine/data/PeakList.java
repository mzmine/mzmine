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

package net.sf.mzmine.data;

import net.sf.mzmine.data.DataUnit;


/**
 *
 */
public interface PeakList extends DataUnit {

	/**
	 * Returns number of peaks on the list
	 */
	public int getNumberOfPeaks();

	/**
	 * Returns all peaks in the peak list
	 */
	public Peak[] getPeaks();

	/**
	 * Returns peak at defined position
	 * @param index	Position of peak
	 */
	public Peak getPeak(int index);

	/**
	 * Returns the index of a peak on the peak list
	 * @param peak	Peak to be searched
	 */
	public int indexOf(Peak peak);

	/**
	 * Returns all peaks overlapping with a retention time range
	 * @param	startRT Start of the retention time range
	 * @param	endRT	End of the retention time range
	 */
	public Peak[] getPeaksInsideScanRange(double startRT, double endRT);

    /**
     * Returns all peaks in a given m/z range
     * @param   startMZ Start of the m/z range
     * @param   endMZ   End of the m/z range
     */
    public Peak[] getPeaksInsideMZRange(double startMZ, double endMZ);

    /**
     * Returns all peaks in a given m/z & retention time ranges
     * @param   startRT Start of the retention time range
     * @param   endRT   End of the retention time range
     * @param   startMZ Start of the m/z range
     * @param   endMZ   End of the m/z range
     */
    public Peak[] getPeaksInsideScanAndMZRange(double startRT, double endRT, double startMZ, double endMZ);

	/**
	 * Returns all isotope patterns overlapping with a retention time range
	 * @param	startRT Start of the retention time range
	 * @param	endRT	End of the retention time range
	 */
	public IsotopePattern[] getIsotopePatternsInsideScanRange(double startRT, double endRT);

}
