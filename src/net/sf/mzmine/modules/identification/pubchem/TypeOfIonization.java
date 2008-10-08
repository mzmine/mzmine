/*
 * Copyright 2006-2008 The MZmine Development Team
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

package net.sf.mzmine.modules.identification.pubchem;

public enum TypeOfIonization {
    NO_IONIZATION("No ionization", "", 0, 0), 
    POSITIVE_HYDROGEN("+H", "H", -1, 1.00794f), 
    NEGATIVE_HYDROGEN("-H", "H", 1, 1.00794f), 
    POSITIVE_POTASIO("+K", "K", -1, 39.0983f), 
    POSITIVE_SODIUM("+Na", "Na", -1, 22.98976928f);

    private final String name, element;
    private final int sign;
    private double mass;

    TypeOfIonization(String name, String element, int sign, double mass) {
        this.name = name;
        this.element = element;
        this.sign = sign;
        this.mass = mass;
    }

    public String typename() {
        return name;
    }
    
    public String getElement() {
        return element;
    }

    public int getSign() {
        return sign;
    }

    public boolean isPositiveCharge() {
    	if (sign < 0)
    		return true;
    	else
    		return false;
    }

    public double getMass() {
        return mass;
    }

    public String toString() {
        return name;
    }

}
