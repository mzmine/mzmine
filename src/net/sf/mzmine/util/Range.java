/*
 * Copyright 2006-2012 The MZmine 2 Development Team
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

package net.sf.mzmine.util;

/**
 * This class represents a range of doubles.
 */
public class Range {

	private double min, max;

	/**
	 * Create a range with only one value, representing both minimum and
	 * maximum. Such range can later be extended using extendRange().
	 * 
	 * @param minAndMax
	 *            Range minimum and maximum
	 */
	public Range(double minAndMax) {
		this(minAndMax, minAndMax);
	}

	/**
	 * Create a range from min to max.
	 * 
	 * @param min
	 *            Range minimum
	 * @param max
	 *            Range maximum
	 */
	public Range(double min, double max) {
		if (min > max) {
			throw (new IllegalArgumentException("Range minimum (" + min
					+ ") must be <= maximum (" + max + ")"));
		}
		this.min = min;
		this.max = max;
	}

	/**
	 * Copy constructor.
	 * 
	 * @param range
	 *            Range to copy
	 */
	public Range(Range range) {
		this(range.getMin(), range.getMax());
	}

	/**
	 * 
	 */
	public Range(String rangeString) {
		String vals[] = rangeString.split("~");
		if (vals.length != 2) {
			// In previous MZmine versions, Range was represented using '-'
			// character
			vals = rangeString.split("-");
			if (vals.length != 2) {
				throw new IllegalArgumentException("Invalid range value "
						+ rangeString);
			}
		}
		this.min = Double.parseDouble(vals[0]);
		this.max = Double.parseDouble(vals[1]);
		if (min > max) {
			throw (new IllegalArgumentException(
					"Range minimum must be <= maximum"));
		}
	}

	/**
	 * @return Range minimun
	 */
	public double getMin() {
		return min;
	}

	/**
	 * @return Range maximum
	 */
	public double getMax() {
		return max;
	}

	/**
	 * Returns true if this range contains given value.
	 * 
	 * @param value
	 *            Value to check
	 * @return True if range contains this value
	 */
	public boolean contains(double value) {
		return ((min <= value) && (max >= value));
	}

	/**
	 * Returns true if this range contains the whole given range as a subset.
	 * 
	 * @param checkMin
	 *            Minimum of given range
	 * @param checkMax
	 *            Maximum of given range
	 * @return True if this range contains given range
	 */
	public boolean containsRange(double checkMin, double checkMax) {
		return ((checkMin >= min) && (checkMax <= max));
	}

	/**
	 * Returns true if this range contains the whole given range as a subset.
	 * 
	 * @param checkRange
	 *            Given range
	 * @return True if this range contains given range
	 */
	public boolean containsRange(Range checkRange) {
		return containsRange(checkRange.getMin(), checkRange.getMax());
	}

	/**
	 * Returns true if this range lies within the given range.
	 * 
	 * @param checkMin
	 *            Minimum of given range
	 * @param checkMax
	 *            Maximum of given range
	 * @return True if this range lies within given range
	 */
	public boolean isWithin(double checkMin, double checkMax) {
		return ((checkMin <= min) && (checkMax >= max));
	}

	/**
	 * Returns true if this range lies within the given range.
	 * 
	 * @param checkRange
	 *            Given range
	 * @return True if this range lies within given range
	 */
	public boolean isWithin(Range checkRange) {
		return isWithin(checkRange.getMin(), checkRange.getMax());
	}

	/**
	 * Extends this range (if necessary) to include the given value
	 * 
	 * @param value
	 *            Value to extends this range
	 */
	public void extendRange(double value) {
		if (min > value)
			min = value;
		if (max < value)
			max = value;
	}

	/**
	 * Extends this range (if necessary) to include the given range
	 * 
	 * @param extension
	 *            Range to extends this range
	 */
	public void extendRange(Range extension) {
		if (min > extension.getMin())
			min = extension.getMin();
		if (max < extension.getMax())
			max = extension.getMax();
	}

	/**
	 * Returns the size of this range.
	 * 
	 * @return Size of this range
	 */
	public double getSize() {
		return (max - min);
	}

	/**
	 * Returns the average point of this range.
	 * 
	 * @return Average
	 */
	public double getAverage() {
		return ((min + max) / 2);
	}

	/**
	 * Returns the String representation. We use the '~' character for
	 * separation, not '-', to avoid ranges like 1E-1-2E-1.
	 * 
	 * @return This range as string
	 */
	public String toString() {
		return String.valueOf(min) + "~" + String.valueOf(max);
	}

	/**
	 * Compares two Ranges
	 */
	public int compareTo(Range range2) {
		Double value1 = this.getMax() - this.getMin();
		Double value2 = range2.getMax() - range2.getMin();

		return value1.compareTo(value2);
	}

	/**
	 * Splits the range in numOfBins bins and then returns the index of the bin
	 * which contains given value. Indexes are from 0 to (numOfBins - 1).
	 */
	public int binNumber(int numOfBins, double value) {
		double rangeLength = max - min;
		double valueDistanceFromStart = value - min;
		int index = (int) Math.round((valueDistanceFromStart / rangeLength)
				* (numOfBins - 1));
		return index;
	}
}
