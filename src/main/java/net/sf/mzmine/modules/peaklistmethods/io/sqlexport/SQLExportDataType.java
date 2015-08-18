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

package net.sf.mzmine.modules.peaklistmethods.io.sqlexport;

public enum SQLExportDataType {

    // Common row elements
    TITLE1("Common row elements", false, false, ""), //
    ID("      ID", false, true, "INT"), //
    MZ("      Average m/z", false, true, "DOUBLE"), //
    RT("      Average retention time", false, true, "DOUBLE"), //
    HEIGHT("      Average peak height", false, true, "DOUBLE"), //
    AREA("      Average peak area", false, true, "DOUBLE"), //
    COMMENT("      Comment", false, true, "STRING"), //

    // Identity elements
    TITLE2("Identity elements", false, false, ""), //
    IDENTITY("      Identity name", false, true, "STRING"), //
    ISOTOPEPATTERN("      Isotope pattern", false, true, "BLOB"), //
    MSMS("      MS/MS pattern", false, true, "BLOB"), //

    // Data file elements
    TITLE3("Data file elements", false, false, ""), //
    PEAKSTATUS("      Status", false, true, "STRING"), //
    PEAKMZ("      m/z", false, true, "DOUBLE"), //
    PEAKRT("      RT", false, true, "DOUBLE"), //
    PEAKRT_START("      RT start", false, true, "DOUBLE"), //
    PEAKRT_END("      RT end", false, true, "DOUBLE"), //
    PEAKDURATION("      Duration", false, true, "DOUBLE"), //
    PEAKHEIGHT("      Height", false, true, "DOUBLE"), //
    PEAKAREA("      Area", false, true, "DOUBLE"), //
    PEAKCHARGE("      Charge", false, true, "INT"), //
    DATAPOINTS("      # Data points", false, true, "INT"), //
    FWHM("      FWHM", false, true, "DOUBLE"), //
    TAILINGFACTOR("      Tailing factor", false, true, "DOUBLE"), //
    ASYMMETRYFACTOR("      Asymmetry factor", false, true, "DOUBLE"), //
    RAWFILE("      Raw data file name", false, true, "STRING"), //

    TITLE4("Other", false, false, ""), //
    CONSTANT("      Constant value", true, true, "");

    private final String name;
    private final boolean hasAdditionalValue;
    private final boolean isSelectableValue;
    private final String valueType;

    SQLExportDataType(String name, boolean hasAdditionalValue,
	    boolean isSelectableValue, String valueType) {
	this.name = name;
	this.hasAdditionalValue = hasAdditionalValue;
	this.isSelectableValue = isSelectableValue;
	this.valueType = valueType;
    }

    public String toString() {
	return this.name;
    }

    public boolean hasAdditionalValue() {
	return hasAdditionalValue;
    }

    public boolean isSelectableValue() {
	return isSelectableValue;
    }

    public String valueType() {
	return this.valueType;
    }
}
