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
