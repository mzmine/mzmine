/*
 * Copyright 2006-2014 The MZmine 2 Development Team
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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.datamodel;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * This class represent one spectrum of a raw data file.
 */
public interface Scan extends Spectrum {

	/**
	 * 
	 * @return RawDataFile containing this Scan
	 */
	@Nonnull
	public RawDataFile getDataFile();

	/**
	 * 
	 * @return Scan number
	 */
	public int getScanNumber();

	/**
	 * 
	 * @return MS level
	 */
	public int getMSLevel();

	/**
	 * 
	 * @return Retention time of this scan in minutes
	 */
	public double getRetentionTime();

	// TODO: public Range getScanRange(); return the actual scanning range of
	// the instrument

	/**
	 * 
	 * @return parent scan number or -1 if there is no parent scan
	 */
	public int getParentScanNumber();

	/**
	 * 
	 * @return Precursor m/z or 0 if this is not MSn scan
	 */
	public double getPrecursorMZ();

	public @Nonnull Polarity getPolarity();
	
	/**
	 * 
	 * @return Precursor charge or 0 if this is not MSn scan or charge is
	 *         unknown
	 */
	public int getPrecursorCharge();

	/**
	 * 
	 * @return array of fragment scan numbers, or null if there are none
	 */
	public int[] getFragmentScanNumbers();

	@Nonnull
	public MassList[] getMassLists();

	@Nullable
	public MassList getMassList(@Nonnull String name);

	public void addMassList(@Nonnull MassList massList);

	public void removeMassList(@Nonnull MassList massList);

}
