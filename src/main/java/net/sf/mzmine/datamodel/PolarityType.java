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

package net.sf.mzmine.datamodel;

/**
 * Represents the polarity of ionization.
 */
public enum PolarityType {

    POSITIVE(+1, "+"), //
    NEGATIVE(-1, "-"), //
    NEUTRAL(0, "n"), //
    UNKNOWN(0, "?");

    private final int sign;
    private final String charValue;

    PolarityType(int sign, String charValue) {
        this.sign = sign;
        this.charValue = charValue;
    }

    /**
     * @return +1 for positive polarity, -1 for negative polarity, and 0 for
     *         neutral or unknown polarity.
     */
    public int getSign() {
        return sign;
    }

    public String asSingleChar() {
        return charValue;
    }

    public static PolarityType fromSingleChar(String s) {
        for (PolarityType p : values()) {
            if (p.charValue.equals(s))
                return p;
        }
        return UNKNOWN;
    }

}
