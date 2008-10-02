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
    NO_IONIZATION("No ionization", 0, 0), POSITIVE_HYDROGEN("+H", -1, 1.00794f), NEGATIVE_HYDROGEN(
            "-H", 1, 1.00794f), POSITIVE_POTASIO("+K", -1, 39.0983f), POSITIVE_SODIUM(
            "+Na", -1, 22.98976928f);

    private final String name;
    private final int sign;
    private double mass;

    TypeOfIonization(String name, int sign, double mass) {
        this.name = name;
        this.sign = sign;
        this.mass = mass;
    }

    public String typename() {
        return name;
    }

    public int getSign() {
        return sign;
    }

    public double getMass() {
        return mass;
    }

    public String toString() {
        return name;
    }

}
