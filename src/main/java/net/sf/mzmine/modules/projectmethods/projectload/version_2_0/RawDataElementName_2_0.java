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

public enum RawDataElementName_2_0 {

    RAWDATA("rawdata"), NAME("name"), QUANTITY_SCAN("num_scans"), ID("id"), SCAN(
	    "scan"), SCAN_ID("id"), MS_LEVEL("mslevel"), QUANTITY_FRAGMENT_SCAN(
	    "fragmentscans"), FRAGMENT_SCAN("fragmentscan"), QUANTITY(
	    "quantity"), PARENT_SCAN("parent"), PRECURSOR_MZ("precursor_mz"), PRECURSOR_CHARGE(
	    "precursor_charge"), RETENTION_TIME("rt"), CENTROIDED("centroid"), QUANTITY_DATAPOINTS(
	    "num_dp");

    private String elementName;

    private RawDataElementName_2_0(String itemName) {
	this.elementName = itemName;
    }

    public String getElementName() {
	return elementName;
    }

}
