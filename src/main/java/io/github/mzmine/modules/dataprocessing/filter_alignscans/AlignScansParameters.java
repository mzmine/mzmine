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
