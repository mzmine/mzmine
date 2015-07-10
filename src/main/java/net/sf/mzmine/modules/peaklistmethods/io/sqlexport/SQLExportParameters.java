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

import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.BooleanParameter;
import net.sf.mzmine.parameters.parametertypes.StringParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.PeakListsParameter;

public class SQLExportParameters extends SimpleParameterSet {

    public static final PeakListsParameter peakList = new PeakListsParameter(1,
            1);

    public static final StringParameter connectionString = new StringParameter(
            "JDBC connection string",
            "JDBC connection string to identify the database, e.g.:\njdbc:mysql://localhost/dbname?user=sqluser&password=sqluserpw",
            50);

    public static final StringParameter tableName = new StringParameter(
            "Database table",
            "Name of the table into which the peak list is going to be exported",
            50);

    public static final SQLColumnSettingsParameter exportColumns = new SQLColumnSettingsParameter();

    public static final BooleanParameter emptyExport = new BooleanParameter(
            "Export empty peak list",
            "If selected, an empty peak list will be exported with null values for all column other than the rawdatafile and any constant values.");

    public SQLExportParameters() {
        super(new Parameter[] { peakList, connectionString, tableName,
                exportColumns, emptyExport });
    }

}
