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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.datamodel.impl;

import java.text.Format;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.IsotopePattern;
import net.sf.mzmine.datamodel.PeakIdentity;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.util.PeakSorter;
import net.sf.mzmine.util.SortingDirection;
import net.sf.mzmine.util.SortingProperty;

/**
 * Implementation of PeakListRow
 */
public class SimplePeakListRow implements PeakListRow {

	private Hashtable<RawDataFile, Feature> peaks;
	private Vector<PeakIdentity> identities;
	private PeakIdentity preferredIdentity;
	private String comment;
	private int myID;
	private double maxDataPointIntensity = 0;

	/**
	 * These variables are used for caching the average values, so we don't need
	 * to calculate them again and again
	 */
	private double averageRT, averageMZ, averageHeight, averageArea;

	public SimplePeakListRow(int myID) {
		this.myID = myID;
		peaks = new Hashtable<RawDataFile, Feature>();
		identities = new Vector<PeakIdentity>();
	}

	/**
	 * @see net.sf.mzmine.datamodel.PeakListRow#getID()
	 */
	public int getID() {
		return myID;
	}

	/**
	 * Return peaks assigned to this row
	 */
	public Feature[] getPeaks() {
		return peaks.values().toArray(new Feature[0]);
	}

	public void removePeak(RawDataFile file) {
		this.peaks.remove(file);
		calculateAverageValues();
	}

	/**
	 * Returns opened raw data files with a peak on this row
	 */
	public RawDataFile[] getRawDataFiles() {
		return peaks.keySet().toArray(new RawDataFile[0]);
	}

	/**
	 * Returns peak for given raw data file
	 */
	public Feature getPeak(RawDataFile rawData) {
		return peaks.get(rawData);
	}

	public synchronized void addPeak(RawDataFile rawData,
			Feature peak) {

		if (peak == null)
			throw new IllegalArgumentException(
					"Cannot add null peak to a peak list row");

		peaks.put(rawData, peak);
		if (peak.getRawDataPointsIntensityRange().getMax() > maxDataPointIntensity)
			maxDataPointIntensity = peak.getRawDataPointsIntensityRange()
					.getMax();
		calculateAverageValues();

	}

	public double getAverageMZ() {
		return averageMZ;
	}

	public double getAverageRT() {
		return averageRT;
	}

	public double getAverageHeight() {
		return averageHeight;
	}

	public double getAverageArea() {
		return averageArea;
	}

	private synchronized void calculateAverageValues() {
		double rtSum = 0, mzSum = 0, heightSum = 0, areaSum = 0;
		Enumeration<Feature> peakEnum = peaks.elements();
		while (peakEnum.hasMoreElements()) {
			Feature p = peakEnum.nextElement();
			rtSum += p.getRT();
			mzSum += p.getMZ();
			heightSum += p.getHeight();
			areaSum += p.getArea();
		}
		averageRT = rtSum / peaks.size();
		averageMZ = mzSum / peaks.size();
		averageHeight = heightSum / peaks.size();
		averageArea = areaSum / peaks.size();
	}

	/**
	 * Returns number of peaks assigned to this row
	 */
	public int getNumberOfPeaks() {
		return peaks.size();
	}

	public String toString() {
		StringBuffer buf = new StringBuffer();
		Format mzFormat = MZmineCore.getConfiguration().getMZFormat();
		Format timeFormat = MZmineCore.getConfiguration().getRTFormat();
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
	 * @see net.sf.mzmine.datamodel.PeakListRow#getComment()
	 */
	public String getComment() {
		return comment;
	}

	/**
	 * @see net.sf.mzmine.datamodel.PeakListRow#setComment(java.lang.String)
	 */
	public void setComment(String comment) {
		this.comment = comment;
	}

	/**
	 * @see net.sf.mzmine.datamodel.PeakListRow#addCompoundIdentity(net.sf.mzmine.datamodel.PeakIdentity)
	 */
	public synchronized void addPeakIdentity(PeakIdentity identity,
			boolean preferred) {

		// Verify if exists already an identity with the same name
		for (PeakIdentity testId : identities) {
			if (testId.getName().equals(identity.getName())) {
				return;
			}
		}

		identities.add(identity);
		if ((preferredIdentity == null) || (preferred)) {
			setPreferredPeakIdentity(identity);
		}
	}

	/**
	 * @see net.sf.mzmine.datamodel.PeakListRow#addCompoundIdentity(net.sf.mzmine.datamodel.PeakIdentity)
	 */
	public synchronized void removePeakIdentity(PeakIdentity identity) {
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
	 * @see net.sf.mzmine.datamodel.PeakListRow#getPeakIdentities()
	 */
	public PeakIdentity[] getPeakIdentities() {
		return identities.toArray(new PeakIdentity[0]);
	}

	/**
	 * @see net.sf.mzmine.datamodel.PeakListRow#getPreferredPeakIdentity()
	 */
	public PeakIdentity getPreferredPeakIdentity() {
		return preferredIdentity;
	}

	/**
	 * @see net.sf.mzmine.datamodel.PeakListRow#setPreferredPeakIdentity(net.sf.mzmine.datamodel.PeakIdentity)
	 */
	public void setPreferredPeakIdentity(PeakIdentity identity) {

		if (identity == null)
			return;

		preferredIdentity = identity;

		if (!identities.contains(identity)) {
			identities.add(identity);
		}

	}

	/**
	 * @see net.sf.mzmine.datamodel.PeakListRow#getDataPointMaxIntensity()
	 */
	public double getDataPointMaxIntensity() {
		return maxDataPointIntensity;
	}

	public boolean hasPeak(Feature peak) {
		return peaks.containsValue(peak);
	}

	public boolean hasPeak(RawDataFile file) {
		return peaks.containsKey(file);
	}

	/**
	 * Returns the highest isotope pattern of a peak in this row
	 */
	public IsotopePattern getBestIsotopePattern() {
		Feature peaks[] = getPeaks();
		Arrays.sort(peaks, new PeakSorter(SortingProperty.Height,
				SortingDirection.Descending));

		for (Feature peak : peaks) {
			IsotopePattern ip = peak.getIsotopePattern();
			if (ip != null)
				return ip;
		}

		return null;
	}

	/**
	 * Returns the highest peak in this row
	 */
	public Feature getBestPeak() {
		Feature peaks[] = getPeaks();
		Arrays.sort(peaks, new PeakSorter(SortingProperty.Height,
				SortingDirection.Descending));
		return peaks[0];
	}

}
