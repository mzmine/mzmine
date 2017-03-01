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

package net.sf.mzmine.modules.peaklistmethods.io.xmlexport;

public enum PeakListElementName {

    PEAKLIST("peaklist"), PEAKLIST_DATE("created"), QUANTITY("quantity"), RAWFILE(
	    "raw_file"), NAME("name"), ID("id"), IDPROPERTY("identity_property"), RT(
	    "rt"), MASS("mz"), HEIGHT("height"), RTRANGE("rt_range"), MZRANGE(
	    "mz_range"), AREA("area"), STATUS("status"), COLUMN("column_id"), SCAN(
	    "scan"), SCAN_ID("scan_id"), DETECTION("detection_method"), MASS_DETECTOR(
	    "mass_detector"), CHROMATO_CONSTRUCTOR("chromato_builder"), ROW(
	    "row"), PEAK_IDENTITY("identity"), PREFERRED("preferred"), PEAK(
	    "peak"), MZPEAK("mzpeak"), SEPARATOR(";"), PROCESS("applied_method"), ITEM(
	    "item");

    private String elementName;

    private PeakListElementName(String itemName) {
	this.elementName = itemName;
    }

    public String getElementName() {
	return elementName;
    }

}
