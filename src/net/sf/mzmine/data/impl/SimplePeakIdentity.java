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

package net.sf.mzmine.data.impl;

import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;

import net.sf.mzmine.data.PeakIdentity;

/**
 * Simple PeakIdentity implementation;
 */
public class SimplePeakIdentity implements PeakIdentity {

	private Hashtable<String, String> properties;

	/**
	 * This constructor is protected so only derived classes can use it. Other
	 * modules using this class should always set the name by default.
	 */
	protected SimplePeakIdentity() {
		this("Unknown name");
	}

	public SimplePeakIdentity(String name) {

		// Check name
		if (name == null) {
			throw new IllegalArgumentException(
					"Identity properties must contain name");
		}

		this.properties = new Hashtable<String, String>();

		properties.put(PROPERTY_NAME, name);

	}

	public SimplePeakIdentity(String name, String formula, String method,
			String id, String url) {

		// Check name
		if (name == null) {
			throw new IllegalArgumentException(
					"Identity properties must contain name");
		}

		this.properties = new Hashtable<String, String>();

		properties.put(PROPERTY_NAME, name);

		if (formula != null)
			properties.put(PROPERTY_FORMULA, formula);
		if (method != null)
			properties.put(PROPERTY_METHOD, method);
		if (id != null)
			properties.put(PROPERTY_ID, id);
		if (url != null)
			properties.put(PROPERTY_URL, url);

	}

	public SimplePeakIdentity(Hashtable<String, String> properties) {

		// Check for name
		if (properties.get(PROPERTY_NAME) == null) {
			throw new IllegalArgumentException(
					"Identity properties must contain name");
		}

		this.properties = properties;
	}

	public String getName() {
		return properties.get(PROPERTY_NAME);
	}

	public String toString() {
		return getName();
	}

	public String getDescription() {
		StringBuilder description = new StringBuilder();
		for (Entry<String, String> entry : properties.entrySet()) {
			if (description.length() > 0)
				description.append('\n');
			description.append(entry.getKey());
			description.append(": ");
			description.append(entry.getValue());

		}
		return description.toString();
	}

	public Map<String, String> getAllProperties() {
		Hashtable<String, String> propertiesCopy = new Hashtable<String, String>(
				properties);
		return propertiesCopy;
	}

	public String getPropertyValue(String property) {
		return properties.get(property);
	}

	public void setPropertyValue(String property, String value) {

		// Check name
		if ((property.equals(PROPERTY_NAME)) && (value == null)) {
			throw new IllegalArgumentException(
					"Identity properties must contain name");
		}

		properties.put(property, value);
	}

}
