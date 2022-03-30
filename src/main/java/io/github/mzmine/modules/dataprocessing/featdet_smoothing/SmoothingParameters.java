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

package io.github.mzmine.modules.dataprocessing.featdet_smoothing;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.featdet_smoothing.loess.LoessSmoothing;
import io.github.mzmine.modules.dataprocessing.featdet_smoothing.savitzkygolay.SavitzkyGolaySmoothing;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialog;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ModuleComboParameter;
import io.github.mzmine.parameters.parametertypes.OriginalFeatureListHandlingParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.util.ExitCode;
import org.jetbrains.annotations.NotNull;

public class SmoothingParameters extends SimpleParameterSet {

  public static final SmoothingAlgorithm sgSmoothing = MZmineCore.getModuleInstance(
      SavitzkyGolaySmoothing.class);

  public static final SmoothingAlgorithm loessSmoothing = MZmineCore.getModuleInstance(
      LoessSmoothing.class);

  public static final SmoothingAlgorithm[] smoothingAlgorithms = new SmoothingAlgorithm[]{
      sgSmoothing, loessSmoothing};
  public static final ModuleComboParameter<SmoothingAlgorithm> smoothingAlgorithm = new ModuleComboParameter<SmoothingAlgorithm>(
      "Smoothing algorithm", "Please select a smoothing algorithm.", smoothingAlgorithms,
      sgSmoothing);
  public static final FeatureListsParameter featureLists = new FeatureListsParameter();
  public static final OriginalFeatureListHandlingParameter handleOriginal = //
      new OriginalFeatureListHandlingParameter(false);

  public static final StringParameter suffix = new StringParameter("Suffix",
      "The suffix to be added to processed feature lists.", "sm");

  public SmoothingParameters() {
    super(new Parameter[]{featureLists, smoothingAlgorithm, handleOriginal, suffix},
        "https://mzmine.github.io/mzmine_documentation/module_docs/featdet_smoothing/smoothing.html");
  }

  @Override
  public ExitCode showSetupDialog(boolean valueCheckRequired) {
    ParameterSetupDialog dialog = new SmoothingSetupDialog(valueCheckRequired, this);
    dialog.showAndWait();
    return dialog.getExitCode();
  }

  @NotNull
  @Override
  public IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.SUPPORTED;
  }
}
