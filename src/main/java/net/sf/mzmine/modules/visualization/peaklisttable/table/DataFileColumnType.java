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

package net.sf.mzmine.modules.visualization.peaklisttable.table;

import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.Feature.FeatureStatus;

public enum DataFileColumnType {

    STATUS("Status", FeatureStatus.class),
    PEAKSHAPE("Peak shape", Feature.class),
    MZ("m/z", Double.class),
    RT("RT", Double.class),
    RT_START("RT start", Double.class),
    RT_END("RT end", Double.class),
    DURATION("Duration", Double.class),
    HEIGHT("Height", Double.class),
    AREA("Area", Double.class),
    CHARGE("Charge", Integer.class),
    DATAPOINTS("# Data points", Integer.class),
    FWHM("FWHM", Double.class),
    TF("Tailing factor", Double.class),
    AF("Asymmetry factor", Double.class);

    private final String columnName;
    private final Class<?> columnClass;

    DataFileColumnType(String columnName, Class<?> columnClass) {
	this.columnName = columnName;
	this.columnClass = columnClass;
    }

    public String getColumnName() {
	return columnName;
    }

    public Class<?> getColumnClass() {
	return columnClass;
    }

    public String toString() {
	return columnName;
    }
}