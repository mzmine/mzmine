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
package io.github.mzmine.modules.dataprocessing.featdet_targeted;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.PercentParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileSelectionType;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.RTToleranceParameter;

public class TargetedPeakDetectionParameters extends SimpleParameterSet {

  public static final RawDataFilesParameter rawDataFile = new RawDataFilesParameter();
  public static final IntegerParameter msLevel =
      new IntegerParameter("MS level", "MS level", 1, true);
  public static final StringParameter suffix =
      new StringParameter("Name suffix", "Suffix to be added to feature list name", "detectedPeak");
  public static final FileNameParameter peakListFile = new FileNameParameter("Feature list file",
      "Name of the file that contains a list of peaks for targeted feature detection.",
      FileSelectionType.OPEN);
  public static final StringParameter fieldSeparator = new StringParameter("Field separator",
      "Character(s) used to separate fields in the database file", ",");
  public static final BooleanParameter ignoreFirstLine =
      new BooleanParameter("Ignore first line", "Ignore the first line of database file");
  public static final PercentParameter intTolerance = new PercentParameter("Intensity tolerance",
      "Maximum allowed deviation from expected /\\ shape of a peak in chromatographic direction");
  public static final DoubleParameter noiseLevel = new DoubleParameter("Noise level",
      "Intensities less than this value are interpreted as noise",
      MZmineCore.getConfiguration().getIntensityFormat());
  public static final MZToleranceParameter MZTolerance = new MZToleranceParameter();
  public static final RTToleranceParameter RTTolerance = new RTToleranceParameter();

  public TargetedPeakDetectionParameters() {
    super(new Parameter[] {rawDataFile, msLevel, suffix, peakListFile, fieldSeparator,
        ignoreFirstLine, intTolerance, noiseLevel, MZTolerance, RTTolerance},
        "https://mzmine.github.io/mzmine_documentation/module_docs/lc-ms_featdet/targeted_featdet/targeted-featdet.html");
  }
}
