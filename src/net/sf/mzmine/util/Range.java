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
     * @param min Range minimum
     * @param max Range maximum
     */
    public Range(float min, float max) {
        this.min = min;
        this.max = max;
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
     * @param checkMin Minimum of checked range
     * @param checkMax Maximum of checked range
     * @return True if this range contains given range
     */
    public boolean containsRange(float checkMin, float checkMax) {
        return ((checkMin >= min) && (checkMax <= max));
    }
    
    /**
     * Returns true if this range lies within the whole given range.
     * 
     * @param checkMin Minimum of checked range
     * @param checkMax Maximum of checked range
     * @return True if this range lies within given range
     */
    public boolean isWithin(float checkMin, float checkMax) {
        return ((checkMin <= min) && (checkMax >= max));
    }

    /**
     * Extends this range (if necessary) to include the given value
     * 
     * @param value Value to extends this range
     */
    public void addValue(float value) {
        if (value < min)
            min = value;
        if (value > max)
            max = value;
    }

    /**
     * Returns the size of this range.
     * @return Size of this range
     */
    public float getSize() {
        return (max - min);
    }

}
