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

package net.sf.mzmine.data;

/**
 *
 */
public interface GroupRelatedPeaks {

	/**
	 * This method verifies if this identity (group) contains the peak list row as a member.
	 * Return a boolean value.
	 * 
	 * @param PeakListRow
	 * @return boolean
	 */
	public boolean containsRow(PeakListRow row);

	/**
	 * This method add the peak list row as member of this identity (group).
	 * 
	 * @param row
	 */
	public void addRow(PeakListRow row);

	/**
	 * Returns all members of this identity (group) as an array of PeakListRow.
	 * 
	 * @return PeakListRow[]
	 */
	public PeakListRow[] getRows();

	/**
	 * Returns the name of this identity (group).
	 * 
	 * @return String
	 */
	public String getGroupName();

	/**
	 * Set the name of this identity (group).
	 * 
	 * @param name
	 */
	public void setGroupName(String name);
	
	
	/**
	 * Returns peak list row with the biggest peak (intensity) of this group.
	 * 
	 * @return PeakListRow
	 */
	public PeakListRow getBiggestPeakListRow();

}
