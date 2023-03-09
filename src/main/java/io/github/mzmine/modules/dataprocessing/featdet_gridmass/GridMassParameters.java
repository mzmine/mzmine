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

package io.github.mzmine.modules.dataprocessing.featdet_gridmass;

import com.google.common.collect.Range;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.ranges.DoubleRangeParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelectionParameter;
import java.util.Collection;

public class GridMassParameters extends SimpleParameterSet {

  public static final RawDataFilesParameter dataFiles = new RawDataFilesParameter();

  public static final ScanSelectionParameter scanSelection = new ScanSelectionParameter(
      new ScanSelection(1));

  public static final DoubleRangeParameter timeSpan = new DoubleRangeParameter(
      "Min-max width time (min)", "Time range for a peak to be recognized as a 'mass'.\n"
      + "The optimal value depends on the chromatography system setup.\nSee 2D raw data to determine typical time spans.",
      MZmineCore.getConfiguration().getRTFormat(), Range.closed(0.1, 3.0));

  public static final DoubleParameter minimumHeight = new DoubleParameter("Minimum height",
      "Minimum GLOBAL intensity of the highest data point in the mass. A value closer to 95% of the baseline-corrected distribution is recommended.",
      MZmineCore.getConfiguration().getMZFormat(), 20.0);

  public static final DoubleParameter mzTolerance = new DoubleParameter("M/Z Tolerance",
      "Maximum difference in m/z to recognize features/peaks as the same.",
      MZmineCore.getConfiguration().getMZFormat(), 0.10);

  public static final StringParameter suffix = new StringParameter("Suffix",
      "This string is added to filename as suffix", "chromatograms");

  public static final DoubleParameter smoothingTimeSpan = new DoubleParameter(
      "Smoothing time (min)",
      "Time to perform intensity smoothing in time space. \nA value close to minimum time span is recomended. \nSee 2D plot to use at least 3 scans.",
      MZmineCore.getConfiguration().getRTFormat(), 0.05);

  public static final DoubleParameter smoothingTimeMZ = new DoubleParameter("Smoothing m/z",
      "MZ tolerance to perform intensity smoothing in time space. \nA value smaller than m/z tolerance is recommended. \nSee 2D plot to observe closer values.",
      MZmineCore.getConfiguration().getMZFormat(), 0.05);

  public static final DoubleParameter intensitySimilarity = new DoubleParameter(
      "False+: Intensity similarity ratio",
      "To reduce false positives removing similarly joint masses crowed along time (solvents or artifacts) > max time span.",
      MZmineCore.getConfiguration().getMZFormat(), 0.50);

  public static final String[] debugLevels = new String[]{"No debug", "Basic information",
      "Final peak information", "All information"};

  public static final ComboParameter<String> showDebug = new ComboParameter<String>(
      "Debugging level", "Shows details of the process. Useful to optimize parameters.",
      debugLevels, debugLevels[0]);

  public static final StringParameter ignoreTimes = new StringParameter("False+: Ignore times",
      "To avoid estimation of features at specific times in minutes. Use 0-0 to ignore. Format: time1-time2, time3-time4, ... ",
      "0-0");

  public GridMassParameters() {
    super(new Parameter[]{dataFiles, scanSelection, suffix, minimumHeight, mzTolerance, timeSpan,
            smoothingTimeSpan, smoothingTimeMZ, intensitySimilarity, ignoreTimes, showDebug},
        "https://mzmine.github.io/mzmine_documentation/module_docs/lc-ms_featdet/featdet_gridmass/gridmass.html");
  }

  @Override
  public boolean checkParameterValues(Collection<String> errorMessages) {
    boolean allParameterOK = super.checkParameterValues(errorMessages);

    var selection = getValue(scanSelection);
    if (!selection.getMsLevelFilter().isSingleMsLevel(1)) {
      errorMessages.add("Grid Mass module is only suitable for MS level 1 data."
          + "\nPlease, choose the correct level in Scans.");
      allParameterOK = false;
    }

    return allParameterOK;
  }

}
