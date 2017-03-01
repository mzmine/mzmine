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

package net.sf.mzmine.modules.peaklistmethods.io.csvexport;

public enum ExportRowDataFileElement {

    PEAK_STATUS("Peak status", false),
    PEAK_MZ("Peak m/z", false),
    PEAK_RT("Peak RT", false),
    PEAK_RT_START("Peak RT start", false),
    PEAK_RT_END("Peak RT end", false),
    PEAK_DURATION("Peak duration time", false),
    PEAK_HEIGHT("Peak height", false),
    PEAK_AREA("Peak area", false),
    PEAK_CHARGE("Peak charge", false),
    PEAK_DATAPOINTS("Peak # data points", false),
    PEAK_FWHM("Peak FWHM", false),
    PEAK_TAILINGFACTOR("Peak tailing factor", false),
    PEAK_ASYMMETRYFACTOR("Peak asymmetry factor", false),
    PEAK_MZMIN("Peak m/z min", false),
    PEAK_MZMAX("Peak m/z max", false);

    private final String name;
    private final boolean common;

    ExportRowDataFileElement(String name, boolean common) {
	this.name = name;
	this.common = common;
    }

    public boolean isCommon() {
	return this.common;
    }

    public String toString() {
	return this.name;
    }

}
