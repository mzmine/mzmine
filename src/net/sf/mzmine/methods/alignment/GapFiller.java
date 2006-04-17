/*
    Copyright 2005 VTT Biotechnology

    This file is part of MZmine.

    MZmine is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    MZmine is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with MZmine; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
*/

package net.sf.mzmine.methods.alignment;

import java.util.Hashtable;

import net.sf.mzmine.obsoletedatastructures.RawDataAtNode;

public interface GapFiller {

	/**
	 * This method show user a parameter dialog
	 */
	//public GapFillerParameters askParameters(MainWindow mainWin);

	/**
	 * This method fills in gaps for a single raw data file column and returns mz, rt, height and area values for the gaps
	 * nodeServer		Node server (caller)
	 * gapsToFill		Hashtable with row index (of alignment result) as key and average mz and average rt of the gap stored in double[]
	 * rawDataAtNode	Raw data file where the gaps should be filled
	 * parameters		Parameters for the gap filler
	 */
	public Hashtable<Integer, double[]> fillGaps(Hashtable<Integer, double[]> gapsToFill, RawDataAtNode rawData, GapFillerParameters parameters);


}