/*
 * Copyright 2006 The MZmine Development Team
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

package net.sf.mzmine.data;

/**
 * This interface defines methods adding/fetching DataUnits to/from another DataUnit.
 */
public interface DataUnit {

	/**
	 * Adds a new data unit
	 */
	public void addData(Class dataType, DataUnit data);

	/**
	 * Returns all added data units of given type.
	 * Data units are stored in the array in the same order as they were added
	 */
	public DataUnit[] getData(Class dataType);

	/**
	 * Returns the latest added data unit of given type
	 */
	public DataUnit getLastData(Class dataType);

	/**
	 * Checks if there are any data units of given type
	 */
	public boolean hasData(Class dataType);

	/**
	 * Removes all data units of given type
	 * @return	true if something was removed
	 */
	public boolean removeAllData(Class dataType);

	/**
	 * Removes a single data unit
	 * @return	true if a data unit was removed
	 */
	public boolean removeData(Class dataType, DataUnit data);

}