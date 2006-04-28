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

package net.sf.mzmine.interfaces;

import java.util.Hashtable;

/**
 * This interface defines the properties of a detected peak
 */
public interface IsotopePattern {

	/**
	 * Returns monoisotopic M/Z of the pattern
	 */
	public double getMonoisotopicMZ();

	/**
	 * Returns the common RT of the pattern
	 */
	public double getRT();

	/**
	 * Returns the charge state of peaks in the pattern
	 */
	public double getChargeState();

	/**
	 * Returns all peaks of the pattern
	 */
	public Peak[] getPeaks();

	/**
	 * Returns peak number n of the pattern
	 */
	public Peak getPeak(int n);


	/**
	 * Returns first scan included in any of the peaks of the pattern
	 */
	public int getFirstScanNumber();

	/**
	 * Returns last scan included in any of the peaks of the pattern
	 */
	public int getLastScanNumber();

	/**
	 * Returns minimum M/Z value of all datapoints in any of the peaks of the pattern
	 */
	public int getMinMZ();

	/**
	 * Returns maximum M/Z value of all datapoints in any of the peaks of the pattern
	 */
	public int getMaxMZ();

}