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

package io.github.mzmine.modules.dataprocessing.featdet_recursiveimsbuilder;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.ParameterSetParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelectionParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import org.jetbrains.annotations.NotNull;

public class RecursiveIMSBuilderParameters extends SimpleParameterSet {

  public static final RawDataFilesParameter rawDataFiles = new RawDataFilesParameter();

  public static final ScanSelectionParameter scanSelection = new ScanSelectionParameter(
      new ScanSelection(1));

  public static final MZToleranceParameter mzTolerance = new MZToleranceParameter("m/z tolerance",
      "The m/z tolerance to build ion traces. The tolerance is specified as a +- tolerance. "
          + "m/z 500.000 with a tolerance of 0.01 will allow m/z 499.99 to 501.01.",0.005, 15);

  public static final IntegerParameter minNumConsecutive = new IntegerParameter(
      "Minimum consecutive retention time data points",
      "The minimum number of consecutive detections in frames (retention time dimension).", 5);

  public static final IntegerParameter minNumDatapoints = new IntegerParameter(
      "Minimum number of data points",
      "The minimum number of consecutive detections in frames (retention time dimension).", 100);

  public static final ParameterSetParameter advancedParameters =
      new ParameterSetParameter("Advanced parameters",
          "Allows adjustment of internal binning parameters for mobilograms",
          new RecursiveIMSBuilderAdvancedParameters());

  public RecursiveIMSBuilderParameters() {
    super(new Parameter[]{rawDataFiles, scanSelection, mzTolerance, minNumConsecutive,
        minNumDatapoints, advancedParameters},
        "https://mzmine.github.io/mzmine_documentation/module_docs/lc-ims-ms_featdet/recursive_ims_builder/recursive-ims-builder.html");
  }

  @NotNull
  @Override
  public IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.ONLY;
  }
}
