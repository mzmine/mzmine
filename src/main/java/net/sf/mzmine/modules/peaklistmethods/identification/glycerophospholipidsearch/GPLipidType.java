/*
 * Copyright 2006-2015 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.peaklistmethods.identification.glycerophospholipidsearch;

public enum GPLipidType {

    PC("Phosphatidylcholine", "PC", "C8H18NO6P"), //
    PE("Phosphatidylethanolamine", "PE", "C5H12NO6P"), //
    PI("Phosphatidylinositol", "PI", "C9H17O11P"), //
    PS("Phosphatidylserine", "PS", "C6H12NO8P");

    private final String name, abbr, formula;

    GPLipidType(String name, String abbr, String formula) {
	this.name = name;
	this.abbr = abbr;
	this.formula = formula;
    }

    public String getAbbr() {
	return abbr;
    }

    public String getFormula() {
	return formula;
    }

    public String getName() {
	return this.name;
    }

    @Override
    public String toString() {
	return this.name;
    }

}
