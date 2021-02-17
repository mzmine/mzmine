/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.dataprocessing.filter_extractscans;

import java.text.DecimalFormat;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.filenames.DirectoryParameter;
import io.github.mzmine.parameters.parametertypes.ranges.RTRangeParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;

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

  public static final BooleanParameter useMassList =
      new BooleanParameter("Use centroid mass list", "Use centroid masses instead of raw data",
          true);

  public ExtractScansParameters() {
    super(new Parameter[]{useMassList, dataFiles, file, useCenterTime, scans, centerTime,
        rangeTime, autoMax, exportHeader});
  }

}
