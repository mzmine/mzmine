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

package net.sf.mzmine.modules.peaklistmethods.identification.adductsearch;

import java.util.Arrays;

import net.sf.mzmine.main.MZmineCore;

public class AdductType {

    // Default adducts.
    private static final AdductType NA = new AdductType("[M+Na-H]", 21.9825);
    private static final AdductType K = new AdductType("[M+K-H]", 37.9559);
    private static final AdductType MG = new AdductType("[M+Mg-2H]", 21.9694);
    private static final AdductType NH3 = new AdductType("[M+NH3]", 17.0265);
    private static final AdductType PHOSPHATE = new AdductType("[M+H3PO4]",
	    97.9769);
    private static final AdductType SULFATE = new AdductType("[M+H2SO4]",
	    97.9674);
    private static final AdductType CARBONATE = new AdductType("[M+H2CO3]",
	    62.0004);
    private static final AdductType GLYCEROL = new AdductType(
	    "[(Deuterium)]glycerol", 5.0);
    private static final AdductType[] DEFAULT_VALUES = { NA, K, MG, NH3,
	    PHOSPHATE, SULFATE, CARBONATE, GLYCEROL };

    private final String name;
    private final double massDifference;

    public AdductType(final String aName, final double difference) {

	name = aName;
	massDifference = difference;
    }

    public String getName() {

	return name;
    }

    public double getMassDifference() {

	return massDifference;
    }

    /**
     * Get the default adducts.
     *
     * @return the list of default adducts.
     */
    public static AdductType[] getDefaultValues() {

	return Arrays.copyOf(DEFAULT_VALUES, DEFAULT_VALUES.length);
    }

    @Override
    public boolean equals(final Object obj) {

	final boolean eq;
	if (obj instanceof AdductType) {

	    final AdductType adduct = (AdductType) obj;

	    eq = adduct == this
		    || (name == null && adduct.name == null || name != null
			    && name.equals(adduct.name))
		    && massDifference == adduct.massDifference;
	} else {

	    eq = false;
	}

	return eq;
    }

    public String toString() {

	return name
		+ ' '
		+ MZmineCore.getConfiguration().getMZFormat()
			.format(massDifference) + " m/z";
    }
}
