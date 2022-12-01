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
