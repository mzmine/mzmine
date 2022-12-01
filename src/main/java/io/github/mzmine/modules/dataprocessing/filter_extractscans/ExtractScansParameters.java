/*
 * Copyright (c) 2004-2022 The MZmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
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
