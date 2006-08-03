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

import java.util.Hashtable;
import java.util.Enumeration;
import java.util.ArrayList;

import net.sf.mzmine.data.AlignmentResult;
import net.sf.mzmine.data.Peak;
import net.sf.mzmine.io.OpenedRawDataFile;



/**
 *
 */
public class SimpleAlignmentResult implements AlignmentResult {

	private Hashtable<OpenedRawDataFile, ArrayList<Peak>> alignmentResultMatrix;


	/**
	 * Returns number of raw data files participating in the alignment
	 */
	public int getNumberOfRawDataFiles() {
		return alignmentResultMatrix.size();
	}

	/**
	 * Returns all raw data files participating in the alignment
	 */
	public OpenedRawDataFile[] getRawDataFiles() {
		OpenedRawDataFile[] res = new OpenedRawDataFile[getNumberOfRawDataFiles()];

		Enumeration<OpenedRawDataFile> dataFileEnum = alignmentResultMatrix.keys();
		int ind = 0;
		while (dataFileEnum.hasMoreElements()) {
			res[ind] = dataFileEnum.nextElement();
			ind++;
		}

		return res;

	}

	/**
	 * Returns number of rows in the alignment result
	 */
	public int getNumberOfRows() {
		Enumeration<ArrayList<Peak>> peakArrayEnum = alignmentResultMatrix.elements();
		if (!(peakArrayEnum.hasMoreElements())) return 0;

		ArrayList<Peak> firstArray = peakArrayEnum.nextElement();
		if (firstArray == null) return 0;

		return firstArray.size();

	}

	/**
	 * Returns the peak of a given raw data file on a give row of the alignment result
	 * @param	row	Row of the alignment result
	 * @param	rawDataFile	Raw data file where the peak is detected/estimated
	 */
	public Peak getPeak(int row, OpenedRawDataFile rawDataFile) {
		ArrayList<Peak> peakArray = alignmentResultMatrix.get(rawDataFile);

		if (peakArray==null) return null;
		return peakArray.get(row);
	}

	/**
	 * Returns all peaks for a raw data file
	 */
	public Peak[] getPeaks(OpenedRawDataFile rawDataFile) {
		ArrayList<Peak> peakArray = alignmentResultMatrix.get(rawDataFile);

		if (peakArray==null) return null;

		return peakArray.toArray(new Peak[0]);
	}


	/**
	 * Returns all peaks on one row
	 */
	public Peak[] getPeaks(int row) {


		Peak[] peaksOnRow = new Peak[getNumberOfRawDataFiles()];

		Enumeration<ArrayList<Peak>> peakArrayEnum = alignmentResultMatrix.elements();

		if (!(peakArrayEnum.hasMoreElements())) return new Peak[0];

		int ind = 0;

		while (peakArrayEnum.hasMoreElements()) {
			ArrayList<Peak> peakArray = peakArrayEnum.nextElement();
			peaksOnRow[ind] = peakArray.get(row);
			ind ++;
		}

		return null;
	}

	/**
	 * Returns all identification results assigned to a single row of the alignment result
	 * One row can have zero, one or any number of identifications.
	 */
	//public CompoundIdentity[] getIdentificationResults(int row);

}
