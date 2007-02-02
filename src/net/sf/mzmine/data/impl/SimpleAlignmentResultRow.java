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
import java.util.Vector;

import net.sf.mzmine.data.Peak;
import net.sf.mzmine.data.AlignmentResultRow;
import net.sf.mzmine.data.CompoundIdentity;
import net.sf.mzmine.data.IsotopePattern;
import net.sf.mzmine.io.OpenedRawDataFile;



/**
 *
 */
public class SimpleAlignmentResultRow extends AbstractDataUnit implements AlignmentResultRow  {

	private IsotopePattern isotopePattern;
	private Hashtable<OpenedRawDataFile, Peak> peaks;
	private Vector<CompoundIdentity> compoundIdentities;


	public SimpleAlignmentResultRow() {
		compoundIdentities = new Vector<CompoundIdentity>();
		peaks = new Hashtable<OpenedRawDataFile, Peak>();
	}

	/*
	 * Return isotope pattern assigned to this row
	 */
	/*
	public IsotopePattern getIsotopePattern() {
		return isotopePattern;
	}
	*/
	

	/*
	 * Return peaks assigned to this row
	 */
	public Peak[] getPeaks() {
		return peaks.values().toArray(new Peak[0]);
	}

	/*
	 * Return opened raw data files with a peak on this row
	 */
	public OpenedRawDataFile[] getOpenedRawDataFiles() {
		return peaks.keySet().toArray(new OpenedRawDataFile[0]);
	}
	
	/*
	 * Returns peak for given raw data file
	 */
	public Peak getPeak(OpenedRawDataFile rawData) {
		return peaks.get(rawData);
	}

	/*
	 * Returns average normalized M/Z for peaks on this row
	 */
	public double getAverageMZ() {
		double mzSum = 0.0;
		Enumeration<Peak> peakEnum = peaks.elements();
		while (peakEnum.hasMoreElements()) {
			Peak p = peakEnum.nextElement();
			mzSum += p.getNormalizedMZ();
		}
		return mzSum / peaks.size();
	}

	/*
	 * Returns average normalized RT for peaks on this row
	 */
	public double getAverageRT() {
		double rtSum = 0.0;
		Enumeration<Peak> peakEnum = peaks.elements();
		while (peakEnum.hasMoreElements()) {
			Peak p = peakEnum.nextElement();
			rtSum += p.getNormalizedRT();
		}
		return rtSum / peaks.size();
	}

	/*
	 * Returns number of peaks assigned to this row
	 */
	public int getNumberOfPeaks() {
		return peaks.size();
	}

	/**
	 * Returns all identification results assigned to a single row of the alignment result
	 * One row can have zero, one or any number of identifications.
	 */
	public CompoundIdentity[] getIdentificationResults(int row) {
		return compoundIdentities.toArray(new CompoundIdentity[0]);
	}



	//////////////
	/*
	public void setIsotopePattern(IsotopePattern isotopePattern) {
		this.isotopePattern = isotopePattern;
	}
	*/

	public void addPeak(OpenedRawDataFile rawData, Peak p) {
		peaks.put(rawData, p);
	}

	public void addCompoundIdentity(CompoundIdentity compoundIdentity) {
		compoundIdentities.add(compoundIdentity);
	}


}
