/*
 * Copyright 2006-2020 The MZmine Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.io.sqlexport;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.selectors.PeakListsParameter;

public class SQLExportParameters extends SimpleParameterSet {

    public static final PeakListsParameter peakList = new PeakListsParameter(1,
            1);

    public static final StringParameter connectionString = new StringParameter(
            "JDBC connection string",
            "JDBC connection string to identify the database, e.g.:\njdbc:mysql://localhost/dbname?user=sqluser&password=sqluserpw",
            50);

    public static final StringParameter tableName = new StringParameter(
            "Database table",
            "Name of the table into which the feature list is going to be exported",
            50);

    public static final SQLColumnSettingsParameter exportColumns = new SQLColumnSettingsParameter();

    public static final BooleanParameter emptyExport = new BooleanParameter(
            "Export empty feature list",
            "If selected, an empty feature list will be exported with null values for all column other than the rawdatafile and any constant values.");

    public SQLExportParameters() {
        super(new Parameter[] { peakList, connectionString, tableName,
                exportColumns, emptyExport });
    }

}
