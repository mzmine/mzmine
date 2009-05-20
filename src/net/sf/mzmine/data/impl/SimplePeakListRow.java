/*
 * Copyright 2006-2009 The MZmine 2 Development Team
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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.data.impl;

import java.text.Format;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;

import net.sf.mzmine.data.ChromatographicPeak;
import net.sf.mzmine.data.IsotopePattern;
import net.sf.mzmine.data.PeakIdentity;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.util.PeakSorter;
import net.sf.mzmine.util.SortingDirection;
import net.sf.mzmine.util.SortingProperty;

/**
 * Implementation of PeakListRow
 */
public class SimplePeakListRow implements PeakListRow {

	private Hashtable<RawDataFile, ChromatographicPeak> peaks;
	private HashSet<PeakIdentity> identities;
	private PeakIdentity preferredIdentity;
	private String comment;
	private int myID;
	private double maxDataPointIntensity = 0;

	public SimplePeakListRow(int myID) {
		this.myID = myID;
		peaks = new Hashtable<RawDataFile, ChromatographicPeak>();
		identities = new HashSet<PeakIdentity>();
	}

	/**
	 * @see net.sf.mzmine.data.PeakListRow#getID()
	 */
	public int getID() {
		return myID;
	}

	/*
	 * Return peaks assigned to this row
	 */
	public ChromatographicPeak[] getPeaks() {
		return peaks.values().toArray(new ChromatographicPeak[0]);
	}

	/*
	 * Return opened raw data files with a peak on this row
	 */
	public RawDataFile[] getRawDataFiles() {
		return peaks.keySet().toArray(new RawDataFile[0]);
	}

	/*
	 * Returns peak for given raw data file
	 */
	public ChromatographicPeak getPeak(RawDataFile rawData) {
		return peaks.get(rawData);
	}

	public void addPeak(RawDataFile rawData, ChromatographicPeak peak) {

		if (peak == null)
			throw new IllegalArgumentException(
					"Cannot add null peak to a peak list row");

		/*
		 * convert the peak to SimpleChromatographicPeak for easy //
		 * serialization if (!((peak instanceof SimpleChromatographicPeak) ||
		 * (peak instanceof IsotopePattern))) peak = new
		 * SimpleChromatographicPeak(peak);
		 */

		peaks.put(rawData, peak);
		if (peak.getRawDataPointsIntensityRange().getMax() > maxDataPointIntensity)
			maxDataPointIntensity = peak.getRawDataPointsIntensityRange()
					.getMax();

	}

	/*
	 * Returns average normalized M/Z for peaks on this row
	 */
	public double getAverageMZ() {
		double mzSum = 0.0f;
		Enumeration<ChromatographicPeak> peakEnum = peaks.elements();
		while (peakEnum.hasMoreElements()) {
			ChromatographicPeak p = peakEnum.nextElement();
			mzSum += p.getMZ();
		}
		return mzSum / peaks.size();
	}

	/*
	 * Returns average normalized RT for peaks on this row
	 */
	public double getAverageRT() {
		double rtSum = 0.0f;
		Enumeration<ChromatographicPeak> peakEnum = peaks.elements();
		while (peakEnum.hasMoreElements()) {
			ChromatographicPeak p = peakEnum.nextElement();
			rtSum += p.getRT();
		}
		return rtSum / peaks.size();
	}

	/*
	 * Returns number of peaks assigned to this row
	 */
	public int getNumberOfPeaks() {
		return peaks.size();
	}

	public String toString() {
		StringBuffer buf = new StringBuffer();
		Format mzFormat = MZmineCore.getMZFormat();
		Format timeFormat = MZmineCore.getRTFormat();
		buf.append("#" + myID + " ");
		buf.append(mzFormat.format(getAverageMZ()));
		buf.append(" m/z @");
		buf.append(timeFormat.format(getAverageRT()));
		if (preferredIdentity != null)
			buf.append(" " + preferredIdentity.getName());
		if ((comment != null) && (comment.length() > 0))
			buf.append(" (" + comment + ")");
		return buf.toString();
	}

	/**
	 * @see net.sf.mzmine.data.PeakListRow#getComment()
	 */
	public String getComment() {
		return comment;
	}

	/**
	 * @see net.sf.mzmine.data.PeakListRow#setComment(java.lang.String)
	 */
	public void setComment(String comment) {
		this.comment = comment;
	}

	/**
	 * @see net.sf.mzmine.data.PeakListRow#addCompoundIdentity(net.sf.mzmine.data.PeakIdentity)
	 */
	public void addPeakIdentity(PeakIdentity identity, boolean preferred) {

		// Verify if exists already an identity with the same name
		PeakIdentity compoundIdentity;
		boolean exists = false;
		Iterator itr = identities.iterator();
		while (itr.hasNext()) {
			compoundIdentity = (PeakIdentity) itr.next();
			if (compoundIdentity.getName().equals(identity.getName())) {
				exists = true;
				break;
			}
		}

		if (!exists) {
			identities.add(identity);
			if ((preferredIdentity == null) || (preferred)) {
				setPreferredPeakIdentity(identity);
			}
		}
	}

	/**
	 * @see net.sf.mzmine.data.PeakListRow#addCompoundIdentity(net.sf.mzmine.data.PeakIdentity)
	 */
	public void removePeakIdentity(PeakIdentity identity) {
		identities.remove(identity);
		if (preferredIdentity == identity) {
			if (identities.size() > 0) {
				PeakIdentity[] identitiesArray = identities
						.toArray(new PeakIdentity[0]);
				setPreferredPeakIdentity(identitiesArray[0]);
			} else
				preferredIdentity = null;
		}
	}

	/**
	 * @see net.sf.mzmine.data.PeakListRow#getPeakIdentities()
	 */
	public PeakIdentity[] getPeakIdentities() {
		return identities.toArray(new PeakIdentity[0]);
	}

	/**
	 * @see net.sf.mzmine.data.PeakListRow#getPreferredPeakIdentity()
	 */
	public PeakIdentity getPreferredPeakIdentity() {
		return preferredIdentity;
	}

	/**
	 * @see net.sf.mzmine.data.PeakListRow#setPreferredPeakIdentity(net.sf.mzmine.data.PeakIdentity)
	 */
	public void setPreferredPeakIdentity(PeakIdentity identity) {
		if (identity != null) {
			preferredIdentity = identity;
			// Verify if exists already an identity with the same name
			PeakIdentity compoundIdentity;
			boolean exists = false;
			Iterator itr = identities.iterator();
			while (itr.hasNext()) {
				compoundIdentity = (PeakIdentity) itr.next();
				if (compoundIdentity.getName().equals(identity.getName())) {
					exists = true;
					break;
				}
			}

			if (!exists) {
				identities.add(identity);
			}
		}
	}

	/**
	 * @see net.sf.mzmine.data.PeakListRow#getDataPointMaxIntensity()
	 */
	public double getDataPointMaxIntensity() {
		return maxDataPointIntensity;
	}

	public boolean hasPeak(ChromatographicPeak peak) {
		return peaks.containsValue(peak);
	}

	public IsotopePattern getBestIsotopePattern()
	{
		ChromatographicPeak peaks[] = getPeaks();
		
		Arrays.sort(peaks, new PeakSorter(SortingProperty.Height,
				SortingDirection.Descending));
		
		for (ChromatographicPeak peak : peaks) {
			if (peak instanceof IsotopePattern)
				return (IsotopePattern) peak;
		}
		
		return null;
	}

	public ChromatographicPeak getBestPeak() {
		ChromatographicPeak peaks[] = getPeaks();
		Arrays.sort(peaks, new PeakSorter(SortingProperty.Height,
				SortingDirection.Descending));
		return peaks[0];
	}

}
