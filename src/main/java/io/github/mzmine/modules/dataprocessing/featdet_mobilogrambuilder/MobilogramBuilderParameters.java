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

package io.github.mzmine.modules.dataprocessing.featdet_mobilogrambuilder;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelectionParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;

public class MobilogramBuilderParameters extends SimpleParameterSet {

  public static final RawDataFilesParameter rawDataFiles = new RawDataFilesParameter();

  public static final ScanSelectionParameter scanSelection = new ScanSelectionParameter("Scan "
      + "selection", "Filter scans based on their properties. Different noise levels ( -> mass "
      + "lists) are recommended for MS1 and MS/MS scans", new ScanSelection());

  public static final MZToleranceParameter mzTolerance = new MZToleranceParameter("m/z width",
      "m/z width between mobility scans to be assigned to the same mobilogram. Correlates with MS"
          + " mass accuracy and resolution.", 0.002, 10, false);

  public static final IntegerParameter minPeaks = new IntegerParameter("Minimum peaks", "Minimum "
      + "peaks in a mobilogram (above previously set noise levels)", 7);

  public static final BooleanParameter addRawDp = new BooleanParameter("Add peaks from raw data",
      "If true: When a mobilogram has been detected with the previous pararameters, the raw data "
          + "will be scanned again for that m/z within the given tolerance. Data points that were"
          + " previously filtered by e.g. mass detection will be added to the mobilogram.", true);

  public MobilogramBuilderParameters() {
    super(new Parameter[]{rawDataFiles, scanSelection, mzTolerance, minPeaks, addRawDp});
  }
}
