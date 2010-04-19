/*
 * Copyright 2006-2010 The MZmine 2 Development Team
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

public enum IonizationType {
	
	NO_IONIZATION("No ionization", "", 0),
	POSITIVE_HYDROGEN("+H", "H", 1.00794),
	NEGATIVE_HYDROGEN("-H", "H", -1.00794),
	POSITIVE_POTASSIUM("+K", "K", 39.0983),
	POSITIVE_SODIUM("+Na", "Na", 22.98976928);

	private final String name, element;
	private double addedMass, mass;

	IonizationType(String name, String element, double addedMass) {
		this.name = name;
		this.element = element;
		this.addedMass = addedMass;
		this.mass = Math.abs(mass);
	}

	public String typename() {
		return name;
	}

	public String getElement() {
		return element;
	}

	public double getAddedMass() {
		return addedMass;
	}

	public Polarity getPolarity() {
		return addedMass > 0 ? Polarity.Positive : Polarity.Negative;
	}

	public double getMass() {
		return mass;
	}

	public String toString() {
		return name;
	}

}
