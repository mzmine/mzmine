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

package io.github.mzmine.modules.dataprocessing.featdet_ionmobilitytracebuilder;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.MassListParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelectionParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;

public class IonMobilityTraceBuilderParameters extends SimpleParameterSet {

  public static final RawDataFilesParameter rawDataFiles = new RawDataFilesParameter();

  public static final MassListParameter massList = new MassListParameter("Mass list", "", false);

  public static final ScanSelectionParameter scanSelection =
      new ScanSelectionParameter("Scan " + "selection",
          "Filter scans based on their properties. Different noise levels ( -> mass "
              + "lists) are recommended for MS1 and MS/MS scans",
          new ScanSelection());

  public static final MZToleranceParameter mzTolerance = new MZToleranceParameter("m/z tolerance",
      "m/z tolerance between mobility scans to be assigned to the same mobilogram", 0.001, 5,
      false);

  public static final IntegerParameter minDataPointsRt = new IntegerParameter(
      "Minimum Retention Time Data Points",
      "Minimum " + "signals in a ion mobility ion trace (above previously set noise levels)", 7);

  public static final IntegerParameter minTotalSignals = new IntegerParameter(
      "Minimum total Signals",
      "Minimum " + "signals in a ion mobility ion trace (above previously set noise levels)", 50);

  public static final StringParameter suffix =
      new StringParameter("Suffix", "This string is added to filename as suffix", "iontraces");

  public IonMobilityTraceBuilderParameters() {
    super(new Parameter[] {rawDataFiles, massList, scanSelection, mzTolerance, minDataPointsRt,
        minTotalSignals, suffix});
  }
}
