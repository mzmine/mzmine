/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.dataprocessing.masscalibration.standardslist;

import java.util.Comparator;

public class StandardsListItem {
	protected String molecularFormula;
	protected double retentionTimeSec;
	protected double mzValue;

	/*public static int compareByMZValue(StandardsListItem item1, StandardsListItem item2){

	}*/

	public static final Comparator<StandardsListItem> mzComparator = 
		Comparator.comparing(StandardsListItem::getMzValue);
	public static final Comparator<StandardsListItem> retentionTimeComparator = 
		Comparator.comparing(StandardsListItem::getRetentionTimeSec);

	public StandardsListItem(String molecularFormula, double retentionTimeSec){
		this.molecularFormula = molecularFormula;
		this.retentionTimeSec = retentionTimeSec;
	}

	public String getMolecularFormula(){
		return molecularFormula;
	}

	public double getRetentionTimeSec(){
		return retentionTimeSec;
	}

	public double getMzValue(){
		return mzValue;
	}
}
