/*
 * Copyright 2006-2008 The MZmine Development Team
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

package net.sf.mzmine.util;

/**
 * This class represents a range of floats.
 */
public class Range {

    private float min, max;

    /**
     * Create a range with only one value, representing both minimum and
     * maximum. Such range can later be extended using extendRange().
     * 
     * @param minAndMax Range minimum and maximum
     */
    public Range(float minAndMax) {
        this(minAndMax, minAndMax);
    }

    /**
     * Create a range from min to max.
     * 
     * @param min Range minimum
     * @param max Range maximum
     */
    public Range(float min, float max) {
        if (min > max) {
            throw (new IllegalArgumentException(
                    "Range minimum must be <= maximum"));
        }
        this.min = min;
        this.max = max;
    }

    /**
     * Copy constructor.
     * 
     * @param range Range to copy
     */
    public Range(Range range) {
        this(range.getMin(), range.getMax());
    }

    /**
     * @return Range minimun
     */
    public float getMin() {
        return min;
    }

    /**
     * @return Range maximum
     */
    public float getMax() {
        return max;
    }

    /**
     * Returns true if this range contains given value.
     * 
     * @param value Value to check
     * @return True if range contains this value
     */
    public boolean contains(float value) {
        return ((min <= value) && (max >= value));
    }

    /**
     * Returns true if this range contains the whole given range as a subset.
     * 
     * @param checkMin Minimum of given range
     * @param checkMax Maximum of given range
     * @return True if this range contains given range
     */
    public boolean containsRange(float checkMin, float checkMax) {
        return ((checkMin >= min) && (checkMax <= max));
    }

    /**
     * Returns true if this range contains the whole given range as a subset.
     * 
     * @param checkRange Given range
     * @return True if this range contains given range
     */
    public boolean containsRange(Range checkRange) {
        return containsRange(checkRange.getMin(), checkRange.getMax());
    }

    /**
     * Returns true if this range lies within the given range.
     * 
     * @param checkMin Minimum of given range
     * @param checkMax Maximum of given range
     * @return True if this range lies within given range
     */
    public boolean isWithin(float checkMin, float checkMax) {
        return ((checkMin <= min) && (checkMax >= max));
    }

    /**
     * Returns true if this range lies within the given range.
     * 
     * @param checkRange Given range
     * @return True if this range lies within given range
     */
    public boolean isWithin(Range checkRange) {
        return isWithin(checkRange.getMin(), checkRange.getMax());
    }

    /**
     * Extends this range (if necessary) to include the given value
     * 
     * @param value Value to extends this range
     */
    public void extendRange(float value) {
        if (min > value)
            min = value;
        if (max < value)
            max = value;
    }

    /**
     * Extends this range (if necessary) to include the given range
     * 
     * @param extension Range to extends this range
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
    public float getSize() {
        return (max - min);
    }

    /**
     * Returns the average point of this range.
     * 
     * @return Average
     */
    public float getAverage() {
        return ((min + max) / 2);
    }

    /**
     * Returns the String representation
     * 
     * @return This range as string
     */
    public String toString() {
        return String.valueOf(min) + " - " + String.valueOf(max);
    }

    /**
     * Returns the String representation
     * 
     * @return This range as string
     */
    public int compareTo( Range range2) {
        Float value1 = this.getMax() - this.getMin();
        Float value2 = range2.getMax() - range2.getMin();
    	
    	return value1.compareTo(value2);
    }

}
