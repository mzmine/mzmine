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

package net.sf.mzmine.modules.peaklistmethods.identification.mascot.data;

public enum SerieIonType {

    A_SERIES("a", new FragmentIonType[] { FragmentIonType.A_ION,
	    FragmentIonType.A_H2O_ION, FragmentIonType.A_NH3_ION }), A_DOUBLE_SERIES(
	    "a++", new FragmentIonType[] { FragmentIonType.A_DOUBLE_ION,
		    FragmentIonType.A_H2O_DOUBLE_ION,
		    FragmentIonType.A_NH3_DOUBLE_ION }), B_SERIES("b",
	    new FragmentIonType[] { FragmentIonType.B_ION,
		    FragmentIonType.B_H2O_ION, FragmentIonType.B_NH3_ION }), B_DOUBLE_SERIES(
	    "b++", new FragmentIonType[] { FragmentIonType.B_DOUBLE_ION,
		    FragmentIonType.B_H2O_DOUBLE_ION,
		    FragmentIonType.B_NH3_DOUBLE_ION }), Y_SERIES("y",
	    new FragmentIonType[] { FragmentIonType.Y_ION,
		    FragmentIonType.Y_H2O_ION, FragmentIonType.Y_NH3_ION }), Y_DOUBLE_SERIES(
	    "y++", new FragmentIonType[] { FragmentIonType.Y_DOUBLE_ION,
		    FragmentIonType.Y_H2O_DOUBLE_ION,
		    FragmentIonType.Y_NH3_DOUBLE_ION }), C_SERIES("c",
	    new FragmentIonType[] { FragmentIonType.C_ION }), C_DOUBLE_SERIES(
	    "c++", new FragmentIonType[] { FragmentIonType.C_DOUBLE_ION }), X_SERIES(
	    "x", new FragmentIonType[] { FragmentIonType.X_ION }), X_DOUBLE_SERIES(
	    "x++", new FragmentIonType[] { FragmentIonType.X_DOUBLE_ION }), Z_SERIES(
	    "z", new FragmentIonType[] { FragmentIonType.Z_ION }), Z_DOUBLE_SERIES(
	    "z++", new FragmentIonType[] { FragmentIonType.Z_DOUBLE_ION }), ZH_SERIES(
	    "zH", new FragmentIonType[] { FragmentIonType.ZH_ION }), ZH_DOUBLE_SERIES(
	    "zH++", new FragmentIonType[] { FragmentIonType.ZH_DOUBLE_ION }), ZHH_SERIES(
	    "zHH", new FragmentIonType[] { FragmentIonType.ZHH_ION }), ZHH_DOUBLE_SERIES(
	    "zHH++", new FragmentIonType[] { FragmentIonType.ZHH_DOUBLE_ION });

    private final String name;
    private FragmentIonType[] ionTypes;

    SerieIonType(String name, FragmentIonType[] ionTypes) {
	this.name = name;
	this.ionTypes = ionTypes;
    }

    public String typename() {
	return name;
    }

    public FragmentIonType[] getIonTypes() {
	return ionTypes;
    }

    public String getName() {
	return name;
    }

}
