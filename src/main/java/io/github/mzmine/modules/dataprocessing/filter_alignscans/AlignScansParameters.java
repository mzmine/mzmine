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

package io.github.mzmine.modules.dataprocessing.filter_alignscans;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;

public class AlignScansParameters extends SimpleParameterSet {

  public static final RawDataFilesParameter dataFiles = new RawDataFilesParameter();

  public static final IntegerParameter scanSpan = new IntegerParameter("Horizontal Scans",
      "Number of scans to be considered in the correlation (to the left and to the right of the scan being aligned).",
      5);

  public static final IntegerParameter mzSpan = new IntegerParameter("Max Vertical Alignment",
      "Maximum number of shifts to be compared. This depends on equipment, normally this should be 1.",
      1);

  public static final DoubleParameter minimumHeight = new DoubleParameter("Minimum height",
      "Minimum intensity to be considered for the align correlation.\nIf chromatogram height is below this level, it is not used in the correlation calculation.",
      MZmineCore.getConfiguration().getIntensityFormat(), 1000.0);

  public static final BooleanParameter logTransform = new BooleanParameter("Correlation in log",
      "Transform intensities to Log scale before comparing correlation.", false);

  public static final BooleanParameter removeOld =
      new BooleanParameter("Remove previous files", "Remove processed files to save memory.", false);

  public static final StringParameter suffix =
      new StringParameter("Suffix", "This string is added to filename as suffix", "align");

  public AlignScansParameters() {
    super(new Parameter[] {dataFiles, suffix, scanSpan, mzSpan, minimumHeight, logTransform,
        removeOld},
        "https://mzmine.github.io/mzmine_documentation/module_docs/filter_raw_data/align-scans.html");
  }

}
