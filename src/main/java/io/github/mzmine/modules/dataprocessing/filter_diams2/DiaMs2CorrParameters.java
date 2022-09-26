/*
 *  Copyright 2006-2020 The MZmine Development Team
 *
 *  This file is part of MZmine.
 *
 *  MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 *  General Public License as published by the Free Software Foundation; either version 2 of the
 *  License, or (at your option) any later version.
 *
 *  MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 *  the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 *  Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with MZmine; if not,
 *  write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 *  USA
 */

package io.github.mzmine.modules.dataprocessing.filter_diams2;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelectionParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import java.text.DecimalFormat;
import org.jetbrains.annotations.NotNull;

public class DiaMs2CorrParameters extends SimpleParameterSet {

  public static final FeatureListsParameter flists = new FeatureListsParameter();

  public static final ScanSelectionParameter ms2ScanSelection = new ScanSelectionParameter(
      "MS2 scan selection", "Select the ms2 scans you want included for DIA MS2 correlation.",
      new ScanSelection(2));

  public static final DoubleParameter minMs1Intensity = new DoubleParameter(
      "Minimum feature intensity",
      "The minimum MS1 intensity of a feature to be considered to build a pseudo MS2 spectrum.",
      MZmineCore.getConfiguration().getIntensityFormat(), 5E3);

  public static final DoubleParameter minMs2Intensity = new DoubleParameter(
      "Minimum fragment intensity", "Minimum intensity of fragment ions to be considered.",
      MZmineCore.getConfiguration().getIntensityFormat(), 1E3);

  public static final IntegerParameter numCorrPoints = new IntegerParameter(
      "Number of correlated points",
      "The number of points to be correlated in MS1 and MS2 features.\n"
          + "Depends on cycle time and chromatographic resolution.", 5);

  public static final DoubleParameter minPearson = new DoubleParameter(
      "Minimum pearson correlation",
      "The minimum pearson correlation (R\\u00B2) between the MS1 and MS2 ion shapes.",
      new DecimalFormat("0.00"), 0.80, 0d, 1d);

  public static final MZToleranceParameter ms2ScanToScanAccuracy = new MZToleranceParameter(
      "MS2 scan-to-scan accuracy",
      "m/z tolerance to build MS2 EICs. Described by the scan-to-scan mass accuracy of MS2 scans.",
      0.003, 15);

  public DiaMs2CorrParameters() {
    super(new Parameter[]{flists, ms2ScanSelection, minMs1Intensity, minMs2Intensity, numCorrPoints,
        minPearson, ms2ScanToScanAccuracy});
  }

  @Override
  public @NotNull IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.SUPPORTED;
  }
}
