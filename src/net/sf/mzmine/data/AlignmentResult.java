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

import net.sf.mzmine.io.RawDataFile;


/**
 *
 */
public interface AlignmentResult {

	/**
	 * Returns number of raw data files participating in the alignment
	 */
	public int getNumberOfRawDataFiles();

	/**
	 * Returns all raw data files participating in the alignment
	 */
	public RawDataFile[] getRawDataFiles();

	/**
	 * Returns number of rows in the alignment result
	 */
	public int getNumberOfRows();

	/**
	 * Returns the peak of a given raw data file on a give row of the alignment result
	 * @param	row	Row of the alignment result
	 * @param	rawDataFile	Raw data file where the peak is detected/estimated
	 */
	public Peak getPeak(int row, RawDataFile rawDataFile);

	/**
	 * Returns all peaks for a raw data file
	 */
	public Peak[] getPeaks(RawDataFile rawDataFile);

	/**
	 * Returns all peaks on one row
	 */
	public Peak[] getPeaks(int row);

	/**
	 * Returns all identification results assigned to a single row of the alignment result
	 * One row can have zero, one or any number of identifications.
	 */
	public CompoundIdentity[] getIdentificationResults(int row);

}
