/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.modules.dataprocessing.filter_scansmoothing;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;

public class ScanSmoothingParameters extends SimpleParameterSet {

  public static final RawDataFilesParameter dataFiles = new RawDataFilesParameter();

  public static final DoubleParameter timeSpan = new DoubleParameter("Time (min)",
      "Time span over which intensities will be averaged in the same m/z over scans.\nThe max between this and Scan Span will be used.",
      MZmineCore.getConfiguration().getRTFormat(), 0.05);

  public static final IntegerParameter scanSpan = new IntegerParameter("Scan span",
      "Number of scan in which intensities will be averaged in the same m/z.\nThe max between this and Time Span will be used.",
      5);

  public static final DoubleParameter mzTolerance = new DoubleParameter("m/z tolerance",
      "m/z range in which intensities will be averaged. The max between this\nand m/z points will be used. If both 0 no mz smoothing will be performed.",
      MZmineCore.getConfiguration().getRTFormat(), 0.05);

  public static final IntegerParameter mzPoints = new IntegerParameter("m/z min points",
      "Number of m/z points used to smooth. The max between this and m/z tol will be used.\nIf both 0 no m/z smoothing will be performed.",
      3);

  public static final DoubleParameter minimumHeight = new DoubleParameter("Min height",
      "Minimum intensity of the highest data point in the chromatogram.\nIf chromatogram height is below this level, it is not used in the average calculation.",
      MZmineCore.getConfiguration().getIntensityFormat(), 0.0);

  public static final BooleanParameter removeOld =
      new BooleanParameter("Remove previous files", "Remove processed files to save memory.", false);

  public static final StringParameter suffix =
      new StringParameter("Suffix", "This string is added to filename as suffix", "smooth");

  public ScanSmoothingParameters() {
    super(new Parameter[] {dataFiles, suffix, timeSpan, scanSpan, mzTolerance, mzPoints,
        minimumHeight, removeOld},
        "https://mzmine.github.io/mzmine_documentation/module_docs/filter_raw_data/scan_smoothing.html");
  }

}
