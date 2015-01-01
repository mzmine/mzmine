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

package net.sf.mzmine.modules.projectmethods.projectload.version_2_0;

public enum PeakListElementName_2_0 {

    PEAKLIST("peaklist"), PEAKLIST_DATE("created"), QUANTITY("quantity"), RAWFILE(
	    "raw_file"), PEAKLIST_NAME("pl_name"), ID("id"), RT("rt"), MZ("mz"), HEIGHT(
	    "height"), RTRANGE("rt_range"), MZRANGE("mz_range"), AREA("area"), STATUS(
	    "status"), COLUMN("column_id"), SCAN_ID("scan_id"), ROW("row"), PEAK_IDENTITY(
	    "identity"), PREFERRED("preferred"), IDPROPERTY("identity_property"), NAME(
	    "name"), COMMENT("comment"), PEAK("peak"), ISOTOPE_PATTERN(
	    "isotope_pattern"), DESCRIPTION("description"), CHARGE("charge"), ISOTOPE(
	    "isotope"), MZPEAKS("mzpeaks"), METHOD("applied_method"), METHOD_NAME(
	    "method_name"), METHOD_PARAMETERS("method_parameters"), REPRESENTATIVE_SCAN(
	    "best_scan"), FRAGMENT_SCAN("fragment_scan");

    private String elementName;

    private PeakListElementName_2_0(String itemName) {
	this.elementName = itemName;
    }

    public String getElementName() {
	return elementName;
    }

}
