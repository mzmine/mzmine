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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.util;

import java.util.Comparator;

import net.sf.mzmine.data.ChromatographicPeak;
import net.sf.mzmine.data.PeakListRow;

/**
 * Compare peak list rows either by ID, average m/z or median area of peaks
 * 
 */
public class PeakListRowSorter implements Comparator<PeakListRow> {

	public enum SortingProperty {
		MZ, ID, Height, Area
	};

	public enum SortingDirection {
		Ascending, Descending
	}

	private SortingProperty property;
	private SortingDirection direction;

	public PeakListRowSorter(SortingProperty property,
			SortingDirection direction) {
		this.property = property;
		this.direction = direction;
	}

	public int compare(PeakListRow row1, PeakListRow row2) {

		int intermediateResult;
		switch (property) {
		case MZ:
		default:
			intermediateResult = compareMZ(row1, row2);
			break;

		case ID:
			intermediateResult = compareID(row1, row2);
			break;

		case Height:
			intermediateResult = compareHeight(row1, row2);
			break;

		case Area:
			intermediateResult = compareArea(row1, row2);
			break;

		}

		if (direction == SortingDirection.Ascending)
			return intermediateResult;
		else
			return -intermediateResult;

	}

	private int compareMZ(PeakListRow row1, PeakListRow row2) {
		Double mz1 = row1.getAverageMZ();
		Double mz2 = row2.getAverageMZ();
		return mz1.compareTo(mz2);
	}

	private int compareID(PeakListRow row1, PeakListRow row2) {
		Integer id1 = row1.getID();
		Integer id2 = row2.getID();
		return id1.compareTo(id2);
	}

	/**
	 * Compares peak list rows by median height of peaks on each row
	 * 
	 */
	private int compareHeight(PeakListRow row1, PeakListRow row2) {

		ChromatographicPeak[] peaks1 = row1.getPeaks();
		double[] peakHeights1 = new double[peaks1.length];
		for (int peakInd = 0; peakInd < peakHeights1.length; peakInd++)
			peakHeights1[peakInd] = peaks1[peakInd].getHeight();
		Double medianPeakHeights1 = MathUtils.calcQuantile(peakHeights1, 0.5f);

		ChromatographicPeak[] peaks2 = row2.getPeaks();
		double[] peakHeights2 = new double[peaks2.length];
		for (int peakInd = 0; peakInd < peakHeights2.length; peakInd++)
			peakHeights2[peakInd] = peaks2[peakInd].getHeight();
		Double medianPeakHeights2 = MathUtils.calcQuantile(peakHeights2, 0.5f);

		return medianPeakHeights1.compareTo(medianPeakHeights2);

	}

	/**
	 * Compares peak list rows by median area of peaks on each row
	 * 
	 */
	private int compareArea(PeakListRow row1, PeakListRow row2) {

		ChromatographicPeak[] peaks1 = row1.getPeaks();
		double[] peakAreas1 = new double[peaks1.length];
		for (int peakInd = 0; peakInd < peakAreas1.length; peakInd++)
			peakAreas1[peakInd] = peaks1[peakInd].getArea();
		Double medianPeakAreas1 = MathUtils.calcQuantile(peakAreas1, 0.5f);

		ChromatographicPeak[] peaks2 = row2.getPeaks();
		double[] peakAreas2 = new double[peaks2.length];
		for (int peakInd = 0; peakInd < peakAreas2.length; peakInd++)
			peakAreas2[peakInd] = peaks2[peakInd].getArea();
		Double medianPeakAreas2 = MathUtils.calcQuantile(peakAreas2, 0.5f);

		return medianPeakAreas1.compareTo(medianPeakAreas2);

	}

}