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

package net.sf.mzmine.modules.peakpicking.anothercentroid;

import java.util.Comparator;

import net.sf.mzmine.data.DataPoint;

/**
 * This is a helper class required for sorting datapoints in order of decreasing
 * intensity.
 */
public class DataPointSorter implements Comparator<DataPoint> {

	public enum SortingProperty {
		MZ, INTENSITY
	};

	public enum SortingDirection {
		ASCENDING, DESCENDING
	}

	private SortingProperty property;
	private SortingDirection direction;

	public DataPointSorter() {
		this.property = SortingProperty.INTENSITY;
		this.direction = SortingDirection.DESCENDING;
	}

	public DataPointSorter(SortingProperty property, SortingDirection direction) {
		this.property = property;
		this.direction = direction;
	}

	public int compare(DataPoint d1, DataPoint d2) {

		Float d1Value, d2Value;

		switch (property) {
		case MZ:
			d1Value = d1.getMZ();
			d2Value = d2.getMZ();
			break;

		case INTENSITY:
		default:
			d1Value = d1.getIntensity();
			d2Value = d2.getIntensity();
			break;

		}

		int compResult = d1Value.compareTo(d2Value);

		switch (direction) {
		case ASCENDING:
			return compResult;
		case DESCENDING:
		default:
			return -compResult;
		}

	}

}