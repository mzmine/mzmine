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

package io.github.mzmine.modules.dataprocessing.featdet_ionmobilitytracebuilder;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.ParameterSetParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelectionParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import org.jetbrains.annotations.NotNull;

public class IonMobilityTraceBuilderParameters extends SimpleParameterSet {

  public static final RawDataFilesParameter rawDataFiles = new RawDataFilesParameter();

  public static final ScanSelectionParameter scanSelection =
      new ScanSelectionParameter("Scan " + "selection",
          "Filter scans based on their properties. Different noise levels ( -> mass "
              + "lists) are recommended for MS1 and MS/MS scans",
          new ScanSelection());

  public static final MZToleranceParameter mzTolerance = new MZToleranceParameter("m/z tolerance",
      "m/z tolerance between mobility scans to be assigned to the same mobilogram", 0.005, 10,
      false);

  public static final IntegerParameter minDataPointsRt = new IntegerParameter(
      "Minimum consecutive retention time data points",
      "Minimum number of consecutive time resolved data points in an ion mobility trace."
          + " In other words, chromatographic peak width in number of data points",
      7);

  public static final IntegerParameter minTotalSignals =
      new IntegerParameter("Minimum total Signals",
          "Minimum number of signals (data points) in an ion mobility trace", 200);

  public static final StringParameter suffix = new StringParameter("Suffix",
      "This string is added to filename as suffix", "ionmobilitytrace");

  public static final ParameterSetParameter advancedParameters =
      new ParameterSetParameter("Advanced parameters",
          "Allows adjustment of internal binning parameters for mobilograms",
          new AdvancedImsTraceBuilderParameters());

  public IonMobilityTraceBuilderParameters() {
    super(new Parameter[]{rawDataFiles, scanSelection, mzTolerance, minDataPointsRt,
        minTotalSignals, suffix, advancedParameters}, "https://mzmine.github.io/mzmine_documentation/module_docs/featdet_ion_mobility_trace_builder/ion-mobility-trace-builder.html");
  }

  @NotNull
  @Override
  public IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.ONLY;
  }
}
