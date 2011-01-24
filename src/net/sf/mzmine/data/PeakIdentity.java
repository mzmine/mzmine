/*
 * Copyright 2006-2011 The MZmine 2 Development Team
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

import java.util.Map;

/**
 * This interface represents an identification result.
 */
public interface PeakIdentity {

	/**
	 * These variables define standard properties. The PROPERTY_NAME must be
	 * present in all instances of PeakIdentity. It defines the value which is
	 * returned by the toString() method.
	 */
	public static final String PROPERTY_NAME = "Name";
	public static final String PROPERTY_FORMULA = "Molecular formula";
	public static final String PROPERTY_METHOD = "Identification method";
	public static final String PROPERTY_ID = "ID";
	public static final String PROPERTY_URL = "URL";

	/**
	 * Returns the value of the PROPERTY_NAME property. This value must always
	 * be set. Same value is returned by the toString() method.
	 * 
	 * @return Name
	 */
	public String getName();

	/**
	 * Returns full, multi-line description of this identity, one property per
	 * line (key: value)
	 * 
	 * @return Description
	 */
	public String getDescription();

	/**
	 * Returns all the properties in the form of a map key --> value
	 * 
	 * @return Description
	 */
	public String getPropertyValue(String property);

	/**
	 * Returns all the properties in the form of a map key --> value
	 * 
	 * @return Description
	 */
	public Map<String, String> getAllProperties();

}
