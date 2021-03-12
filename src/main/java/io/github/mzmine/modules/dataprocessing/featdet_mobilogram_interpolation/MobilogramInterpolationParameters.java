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

package io.github.mzmine.modules.dataprocessing.featdet_mobilogram_interpolation;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.util.ExitCode;
import javafx.application.Platform;

public class MobilogramInterpolationParameters extends SimpleParameterSet {

  public static final FeatureListsParameter featureLists = new FeatureListsParameter();

  public static final IntegerParameter windowWidth = new IntegerParameter("Window width",
      "The width (in mobility scans) of the filter window.");

  public static final IntegerParameter numIntensities = new IntegerParameter(
      "Number of non-zero intensities",
      "The minimum number of intensities to be detected in the search window.");

  public static final IntegerParameter filterWidth = new IntegerParameter(
      "Interpolation filter width",
      "The number of mobility scans to be taken into account when interpolating between two non-zero points.");

  public static final BooleanParameter createNewFeatureList = new BooleanParameter(
      "Create new feature list", "If checked, a new feature list will be created.");

  public static final StringParameter suffix = new StringParameter("Suffix",
      "Suffix of the new feature list (if a new feature list is created).");


  public MobilogramInterpolationParameters() {
    super(new Parameter[]{featureLists, windowWidth, numIntensities, suffix, filterWidth,
        createNewFeatureList});
  }

  @Override
  public ExitCode showSetupDialog(boolean valueCheckRequired) {
    assert Platform.isFxApplicationThread();

    if ((parameters == null) || (parameters.length == 0)) {
      return ExitCode.OK;
    }

    MobilogramInterpolationSetupDialog dialog = new MobilogramInterpolationSetupDialog(
        valueCheckRequired,
        this);
    dialog.showAndWait();
    return dialog.getExitCode();
  }

  @Override
  public IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.ONLY;
  }
}
