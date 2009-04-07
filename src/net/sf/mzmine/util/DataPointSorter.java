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

import net.sf.mzmine.data.DataPoint;

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

		Double peak1Value = getValue(dp1);
		Double peak2Value = getValue(dp2);

		if (direction == SortingDirection.Ascending)
			return peak1Value.compareTo(peak2Value);
		else
			return peak2Value.compareTo(peak1Value);

	}

	private double getValue(DataPoint dp) {
		switch (property) {
		case MZ:
			return dp.getMZ();
		case Intensity:
			return dp.getIntensity();
		}

		// We should never get here, so throw exception
		throw (new IllegalStateException());
	}
}
