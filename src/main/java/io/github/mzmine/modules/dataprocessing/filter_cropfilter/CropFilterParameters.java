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

package io.github.mzmine.modules.dataprocessing.filter_cropfilter;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.ranges.MZRangeParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelectionParameter;

public class CropFilterParameters extends SimpleParameterSet {

  public static final RawDataFilesParameter dataFiles = new RawDataFilesParameter();

  public static final ScanSelectionParameter scanSelection = new ScanSelectionParameter();

  public static final MZRangeParameter mzRange =
      new MZRangeParameter("m/z", "m/z boundary of the cropped region");

  public static final StringParameter suffix =
      new StringParameter("Suffix", "This string is added to filename as suffix", "filtered");

  public static final BooleanParameter autoRemove =
      new BooleanParameter("Remove source file after filtering",
          "If checked, original file will be removed and only filtered version remains");

  public CropFilterParameters() {
    super(new Parameter[] {dataFiles, scanSelection, mzRange, suffix, autoRemove},
        "https://mzmine.github.io/mzmine_documentation/module_docs/filter_raw_data/crop-filter.html");
  }

}
