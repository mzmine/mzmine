/*
 * Copyright 2006-2015 The MZmine 2 Development Team
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

/* Code created was by or on behalf of Syngenta and is released under the open source license in use for the
 * pre-existing code or project. Syngenta does not assert ownership or copyright any over pre-existing work.
 */

package net.sf.mzmine.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.Range;

public class RangeUtils {

    /**
     * Parses a range from String where upper and lower bounds are delimited by
     * a dash, e.g. "100.0-200.5". Note: we are dealing with doubles, so in an
     * unfortunate case the range might look like this
     * "3.402439E-36-1.310424E-2"
     * 
     */
    public static Range<Double> parseRange(String text) {
        Pattern p = Pattern.compile(
                "([\\d\\.]+(?:E\\-?\\d+)?)\\-([\\d\\.]+(?:E\\-?\\d+)?)");

        Matcher m = p.matcher(text);
        if (!m.find()) {
            throw new IllegalArgumentException(
                    "String '" + text + "' could not be parsed into a range");
        }
        double low = Double.parseDouble(m.group(1));
        double high = Double.parseDouble(m.group(2));
        Range<Double> result = Range.closed(low, high);
        return result;
    }

    /**
     * Splits the range in numOfBins bins and then returns the index of the bin
     * which contains given value. Indexes are from 0 to (numOfBins - 1).
     */
    public static int binNumber(Range<Double> range, int numOfBins,
            double value) {
        double rangeLength = range.upperEndpoint() - range.lowerEndpoint();
        double valueDistanceFromStart = value - range.lowerEndpoint();
        int index = (int) Math.round(
                (valueDistanceFromStart / rangeLength) * (numOfBins - 1));
        return index;
    }

    public static double rangeLength(Range<Double> range) {
        return range.upperEndpoint() - range.lowerEndpoint();
    }

    public static double rangeCenter(Range<Double> range) {
        return (range.upperEndpoint() + range.lowerEndpoint()) / 2.0;
    }

    public static Range<Double> fromArray(double array[]) {
        if ((array == null) || (array.length == 0))
            return Range.open(0.0, 0.0);
        double min = array[0], max = array[0];
        for (double d : array) {
            if (d > max)
                max = d;
            if (d < min)
                min = d;
        }
        return Range.closed(min, max);
    }
}
