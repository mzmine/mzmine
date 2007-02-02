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
package net.sf.mzmine.methods.deisotoping.util;

import net.sf.mzmine.data.IsotopePattern;
import net.sf.mzmine.data.Peak;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.AlignmentResult;
import net.sf.mzmine.data.AlignmentResultRow;

import java.util.Hashtable;
import java.util.TreeSet;
import java.util.SortedSet;
import java.util.Comparator;
import java.util.Iterator;

/**
 * This helper class is used to group peaks by their isotope pattern.
 */

public class IsotopePatternUtility {

	private Hashtable<IsotopePattern, Integer> isotopePatternNumbers;
	private Hashtable<IsotopePattern, SortedSet<Peak>> isotopePatternPeaks;
	private Hashtable<IsotopePattern, SortedSet<AlignmentResultRow>> isotopePatternRows;

	/**
	 * Constructor: groups peaks of a peak list by their isotope pattern
	 */
	public IsotopePatternUtility(PeakList peakList) {

		isotopePatternNumbers = new Hashtable<IsotopePattern, Integer>();
		isotopePatternPeaks = new Hashtable<IsotopePattern, SortedSet<Peak>>();
		PeakSorter peakSorter = new PeakSorter();

		int runningNumber = 0;
		Peak[] peaks = peakList.getPeaks();
		for (Peak p : peaks) {

			if (!(p.hasData(IsotopePattern.class)))
				continue;

			IsotopePattern isotopePattern = (IsotopePattern)(p.getData(IsotopePattern.class)[0]);

			// Check if this pattern has already been assigned with a number
			Integer currentNumber = isotopePatternNumbers.get(isotopePattern);
			if (currentNumber==null) {
				runningNumber++;
				isotopePatternNumbers.put(isotopePattern, new Integer(runningNumber));
			}

			// Add this peak to the collection of peaks in this pattern
			SortedSet<Peak> peaksInPattern = isotopePatternPeaks.get(isotopePattern);
			if (peaksInPattern==null) {
				peaksInPattern = new TreeSet<Peak>(peakSorter);
				isotopePatternPeaks.put(isotopePattern, peaksInPattern);
			}
			peaksInPattern.add(p);

		}

	}

	/**
	 * Constructor: groups alignment rows of an alignment result by their isotope pattern
	 */
	public IsotopePatternUtility(AlignmentResult alignmentResult) {

		isotopePatternNumbers = new Hashtable<IsotopePattern, Integer>();
		isotopePatternRows = new Hashtable<IsotopePattern, SortedSet<AlignmentResultRow>>();
		RowSorter rowSorter = new RowSorter();

		int runningNumber = 0;
		AlignmentResultRow[] rows = alignmentResult.getRows();
		for (AlignmentResultRow row : rows) {

			//IsotopePattern isotopePattern = row.getIsotopePattern();
			IsotopePattern isotopePattern = (IsotopePattern)row.getLastData(IsotopePattern.class);
			if (isotopePattern == null) continue;

			// Check if this pattern has already been assigned with a number
			Integer currentNumber = isotopePatternNumbers.get(isotopePattern);
			if (currentNumber==null) {
				runningNumber++;
				isotopePatternNumbers.put(isotopePattern, new Integer(runningNumber));
			}

			// Add this peak to the collection of peaks in this pattern
			SortedSet<AlignmentResultRow> rowsInPattern = isotopePatternRows.get(isotopePattern);
			if (rowsInPattern==null) {
				rowsInPattern = new TreeSet<AlignmentResultRow>(rowSorter);
				isotopePatternRows.put(isotopePattern, rowsInPattern);
			}
			rowsInPattern.add(row);

		}

	}


	/**
	 * Returns running number for an isotope pattern
	 */
	public int getIsotopePatternNumber(IsotopePattern isotopePattern) {
		Integer number = isotopePatternNumbers.get(isotopePattern);
		if (number==null) {
			return -1;
		}
		return number;
	}

	/**
	 * Returns all isotope patterns that appear in the peak list
	 */
	public IsotopePattern[] getAllIsotopePatterns() {
		return isotopePatternNumbers.keySet().toArray(new IsotopePattern[0]);
	}

	/**
	 * Returns all peaks that belong to the same pattern, sorted in order of accending M/Z
	 */
	public Peak[] getPeaksInPattern(IsotopePattern isotopePattern) {
		SortedSet<Peak> peaksVector = isotopePatternPeaks.get(isotopePattern);
		if (peaksVector==null) return new Peak[0];
		return peaksVector.toArray(new Peak[0]);
	}

	public AlignmentResultRow[] getRowsInPattern(IsotopePattern isotopePattern) {
		SortedSet<AlignmentResultRow> rowsVector = isotopePatternRows.get(isotopePattern);
		if (rowsVector==null) return new AlignmentResultRow[0];
		return rowsVector.toArray(new AlignmentResultRow[0]);
	}

	/**
	 * Returns monoisotopic peak of the pattern (peak with lowest M/Z)
	 */
	public Peak getMonoisotopicPeak(IsotopePattern isotopePattern) {
		SortedSet<Peak> peaksVector = isotopePatternPeaks.get(isotopePattern);
		if (peaksVector==null) return null;
		return peaksVector.first();
	}

	public AlignmentResultRow getFirstRow(IsotopePattern isotopePattern) {
		SortedSet<AlignmentResultRow> rowsVector = isotopePatternRows.get(isotopePattern);
		if (rowsVector==null) return null;
		return rowsVector.first();
	}




	/**
	 * Returns number of the peak within pattern
	 */
	public int getPeakNumberWithinPattern(Peak p) {

		if (!(p.hasData(IsotopePattern.class))) return -1;

		SortedSet<Peak> peaksInPattern = isotopePatternPeaks.get((p.getData(IsotopePattern.class))[0]);
		if (peaksInPattern==null) return -1;

		Iterator<Peak> peakIterator = peaksInPattern.iterator();
		int number = 0;
		while (peakIterator.hasNext()) {
			Peak p2 = peakIterator.next();
			if (p==p2) return number;
			number++;
		}
		return -2;

	}

	public int getRowNumberWithinPattern(AlignmentResultRow p) {

		if (!(p.hasData(IsotopePattern.class))) return -1;

		SortedSet<AlignmentResultRow> rowsInPattern = isotopePatternRows.get((p.getData(IsotopePattern.class))[0]);
		if (rowsInPattern==null) return -1;

		Iterator<AlignmentResultRow> rowIterator = rowsInPattern.iterator();
		int number = 0;
		while (rowIterator.hasNext()) {
			AlignmentResultRow p2 = rowIterator.next();
			if (p==p2) return number;
			number++;
		}
		return -2;

	}


	private class PeakSorter implements Comparator<Peak> {
		public int compare(Peak p1, Peak p2) {
			if (p1.getNormalizedMZ() < p2.getNormalizedMZ()) return -1;
			if (p1.getNormalizedMZ() > p2.getNormalizedMZ()) return 1;

			if (p1.getNormalizedRT() <= p2.getNormalizedRT()) return -1;
			return 1;
		}

		public boolean equals(Object obj) { return false; }
	}


	private class RowSorter implements Comparator<AlignmentResultRow> {
		public int compare(AlignmentResultRow p1, AlignmentResultRow p2) {
			if (p1.getAverageMZ() < p2.getAverageMZ()) return -1;
			if (p1.getAverageMZ() > p2.getAverageMZ()) return 1;

			if (p1.getAverageRT() <= p2.getAverageRT()) return -1;
			return 1;
		}

		public boolean equals(Object obj) { return false; }
	}


}