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

package net.sf.mzmine.data.impl;

import java.text.NumberFormat;
import java.util.HashSet;
import java.util.Iterator;

import net.sf.mzmine.data.GroupRelatedPeaks;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.main.mzmineclient.MZmineCore;

public class SimpleGroupRelatedPeaks implements GroupRelatedPeaks {

	public static final NumberFormat massFormater = MZmineCore.getMZFormat();
	public static final NumberFormat timeFormater = MZmineCore.getRTFormat();

	private String groupName;
	private HashSet<PeakListRow> rowsGroup;

	/**
	 * Implements an object of SimpleRelatedPeaksIdentity class, with two peaks
	 * and name.
	 * 
	 * @param groupName
	 * @param row1
	 * @param row2
	 */
	public SimpleGroupRelatedPeaks(String groupName, PeakListRow row1,
			PeakListRow row2) {

		this.groupName = groupName;
		rowsGroup = new HashSet<PeakListRow>();
		rowsGroup.add(row1);
		rowsGroup.add(row2);
	}

	/**
	 * @see net.sf.mzmine.data.GroupRelatedPeaks#setGroupName(java.lang.String)
	 */
	public void setGroupName(String name) {
		this.groupName = name;
	}

	/**
	 * @see net.sf.mzmine.data.GroupRelatedPeaks#getGroupName()
	 */
	public String getGroupName() {
		return groupName;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		String mass = "", rt = "", number = "";
		if (rowsGroup != null) {
			if (rowsGroup.size() > 0) {
				PeakListRow r = rowsGroup.toArray(new PeakListRow[0])[0];
				mass = massFormater.format(r.getAverageMZ());
				rt = timeFormater.format(r.getAverageRT());
				number = String.valueOf(rowsGroup.size());
			}
		}

		return groupName + " " + mass + "@" + rt + " (" + number + " peaks)";
	}

	/**
	 * @see net.sf.mzmine.data.GroupRelatedPeaks#addRow(net.sf.mzmine.data.PeakListRow)
	 */
	public void addRow(PeakListRow row) {
		rowsGroup.add(row);
	}

	/**
	 * @see net.sf.mzmine.data.GroupRelatedPeaks#getRows()
	 */
	public PeakListRow[] getRows() {
		return rowsGroup.toArray(new PeakListRow[0]);
	}

	/**
	 * @see net.sf.mzmine.data.GroupRelatedPeaks#containsRow(net.sf.mzmine.data.PeakListRow)
	 */
	public boolean containsRow(PeakListRow row) {
		return rowsGroup.contains(row);
	}

	/**
	 * @see net.sf.mzmine.data.GroupRelatedPeaks#getBiggestPeakListRow()
	 */
	public PeakListRow getBiggestPeakListRow() {
		
		PeakListRow row, biggestRow = null;
		double maxIntensity = 0;

		Iterator<PeakListRow> itr = rowsGroup.iterator();
		while (itr.hasNext()){
			row = itr.next();
			if (row.getDataPointMaxIntensity() > maxIntensity){
				maxIntensity = row.getDataPointMaxIntensity();
				biggestRow = row;
			}
		}
		
		return biggestRow;
	}
	


}
