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

package net.sf.mzmine.util;

import java.util.Comparator;

import net.sf.mzmine.datamodel.DataPoint;

/**
 * This class implements Comparator class to provide a comparison between two
 * DataPoints.
 * 
 */
public class DataPointSorter implements Comparator<DataPoint> {

    private SortingProperty property;
    private SortingDirection direction;

    public DataPointSorter(SortingProperty property, SortingDirection direction) {
	this.property = property;
	this.direction = direction;
    }

    public int compare(DataPoint dp1, DataPoint dp2) {

	int result;

	switch (property) {
	case MZ:

	    result = Double.compare(dp1.getMZ(), dp2.getMZ());

	    // If the data points have same m/z, we do a second comparison of
	    // intensity, to ensure that this comparator is consistent with
	    // equality: (compare(x, y)==0) == (x.equals(y)),
	    if (result == 0)
		result = Double.compare(dp1.getIntensity(), dp2.getIntensity());

	    if (direction == SortingDirection.Ascending)
		return result;
	    else
		return -result;

	case Intensity:
	    result = Double.compare(dp1.getIntensity(), dp2.getIntensity());

	    // If the data points have same intensity, we do a second comparison
	    // of m/z, to ensure that this comparator is consistent with
	    // equality: (compare(x, y)==0) == (x.equals(y)),
	    if (result == 0)
		result = Double.compare(dp1.getMZ(), dp2.getMZ());

	    if (direction == SortingDirection.Ascending)
		return result;
	    else
		return -result;
	default:
	    // We should never get here, so throw an exception
	    throw (new IllegalStateException());
	}

    }
}
