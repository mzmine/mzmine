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

/**
 * This is a helper class required for sorting bins
 */
class BinSorter implements Comparator<Bin> {

	enum SortingProperty {
		LOWMZ, AVERAGEMZ, HIGHMZ
	};

	enum SortingDirection {
		ASCENDING, DESCENDING
	}

	private SortingProperty property;
	private SortingDirection direction;

	BinSorter() {
		this.property = SortingProperty.LOWMZ;
		this.direction = SortingDirection.ASCENDING;
	}

	public BinSorter(SortingProperty property, SortingDirection direction) {
		this.property = property;
		this.direction = direction;
	}

	public int compare(Bin b1, Bin b2) {

		Float b1Value, b2Value;

		switch (property) {
		case LOWMZ:
		default:			
			b1Value = b1.getLowMZ();
			b2Value = b2.getLowMZ();
			break;

		case AVERAGEMZ:
			b1Value = 0.5f * (b1.getLowMZ() + b1.getHighMZ());
			b2Value = 0.5f * (b2.getLowMZ() + b2.getHighMZ());
			break;
			
		case HIGHMZ:
			b1Value = b1.getHighMZ();
			b2Value = b2.getHighMZ();
			break;

		}

		int compResult = b1Value.compareTo(b2Value);

		switch (direction) {
		case ASCENDING:
			return compResult;
		case DESCENDING:
		default:
			return -compResult;
		}

	}

}