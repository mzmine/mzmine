/*
 * Copyright 2006-2015 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.rawdatamethods.exportscans;

import java.text.DecimalFormat;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.BooleanParameter;
import net.sf.mzmine.parameters.parametertypes.DoubleParameter;
import net.sf.mzmine.parameters.parametertypes.IntegerParameter;
import net.sf.mzmine.parameters.parametertypes.MassListParameter;
import net.sf.mzmine.parameters.parametertypes.OptionalParameter;
import net.sf.mzmine.parameters.parametertypes.filenames.DirectoryParameter;
import net.sf.mzmine.parameters.parametertypes.ranges.RTRangeParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;

public class ExtractScansParameters extends SimpleParameterSet {

  public static final RawDataFilesParameter dataFiles = new RawDataFilesParameter();
  public static final IntegerParameter scans = new IntegerParameter("Scan count to be exported",
      "Total scan count to be exported centered around the center time", 30, true);
  public static final BooleanParameter useCenterTime = new BooleanParameter("Use center time",
      "If checked the center time and scan count are used for export. Otherwise all scans in the given time range are exported.",
      true);
  public static final DoubleParameter centerTime =
      new DoubleParameter("Center time", "Center time", new DecimalFormat("#.##"), 0.0);

  public static final RTRangeParameter rangeTime = new RTRangeParameter("Time range",
      "If \"use center time\" is unchecked all scans between the minimum and maximum time are exported (inclusive).",
      false, null);

  public static final DirectoryParameter file =
      new DirectoryParameter("Output directory", "Directory to write scans to");
  public static final BooleanParameter autoMax = new BooleanParameter("Auto search max",
      "Automatically search for maximum TIC intensity as center scan", true);
  public static final BooleanParameter exportHeader =
      new BooleanParameter("Export header", "Exports a header for each scan file", true);
  public static final BooleanParameter exportSummary =
      new BooleanParameter("Export summary", "Exports a summary Microsoft Excel file", true);

  public static final OptionalParameter<MassListParameter> useMassList =
      new OptionalParameter<>(new MassListParameter());

  public ExtractScansParameters() {
    super(new Parameter[] {useMassList, dataFiles, file, useCenterTime, scans, centerTime,
        rangeTime, autoMax, exportHeader});
  }

}
