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

package net.sf.mzmine.modules.identification.adductsearch;

import net.sf.mzmine.main.mzmineclient.MZmineCore;

public enum AdductType {

	ALLRELATED("All related peaks", 0.0),
	Na("[M+Na-H]", 21.9825),
	K("[M+K-H]", 37.9559),
	Mg("[M+Mg-2H]", 21.9694),
	NH3("[M+NH3]", 17.0265),
	Phosphate("[M+H3PO4]", 97.9769),
	Sulfate("[M+H2SO4]", 97.9674),
	Carbonate("[M+CH2O3]", 62.0004),
	Glycerol("[(Deuterium)]glycerol", 5.0),
	CUSTOM("Custom", 0.0);

	private final String name;
	private final double massDifference;

	AdductType(String name, double massDifference) {
		this.name = name;
		this.massDifference = massDifference;
	}

	public String getName() {
		return this.name;
	}

	public double getMassDifference() {
		return this.massDifference;
	}

	public String toString() {
		return this.name + ", "
				+ MZmineCore.getMZFormat().format(this.massDifference) + " m/z";
	}
}
