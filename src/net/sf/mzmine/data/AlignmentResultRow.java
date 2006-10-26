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

import net.sf.mzmine.io.OpenedRawDataFile;


/**
 *
 */
public interface AlignmentResultRow extends DataUnit {

	/*
	 * Return isotope pattern assigned to this row
	 */
	public IsotopePattern getIsotopePattern();

	/*
	 * Return raw datas with peaks on this row
	 */
	public OpenedRawDataFile[] getOpenedRawDataFiles();
	
	/*
	 * Return peaks assigned to this row
	 */
	public Peak[] getPeaks();

	/*
	 * Returns peak for given raw data file
	 */
	public Peak getPeak(OpenedRawDataFile rawData);

	/*
	 * Returns average normalized M/Z for peaks on this row
	 */
	public double getAverageMZ();

	/*
	 * Returns average normalized RT for peaks on this row
	 */
	public double getAverageRT();

	/*
	 * Returns number of peaks assigned to this row
	 */
	public int getNumberOfPeaks();

	/**
	 * Returns all identification results assigned to a single row of the alignment result
	 * One row can have zero, one or any number of identifications.
	 */
	public CompoundIdentity[] getIdentificationResults(int row);

}
