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

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialog;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.RemoveOriginalSourcesParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.util.ExitCode;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;

public class SmoothingParameters extends SimpleParameterSet {

  public static final FeatureListsParameter featureLists = new FeatureListsParameter();

  public static final OptionalParameter<ComboParameter<Integer>> rtSmoothing = new OptionalParameter<>(
      new ComboParameter<Integer>("Retention time smoothing",
          "Enables intensity smoothing along the rt axis.",
          new Integer[]{0, 5, 7, 9, 11, 13, 15, 17, 19, 21, 23, 25}, 5));

  public static final OptionalParameter<ComboParameter<Integer>> mobilitySmoothing = new OptionalParameter<>(
      new ComboParameter<Integer>("Mobility smoothing",
          "Enables intensity smoothing of the summed mobilogram.",
          new Integer[]{0, 5, 7, 9, 11, 13, 15, 17, 19, 21, 23, 25}, 5));

  public static final BooleanParameter removeOriginal = new RemoveOriginalSourcesParameter();

  public static final StringParameter suffix = new StringParameter("Suffix",
      "The suffix to be added to processed feature lists.", " sm");

  public SmoothingParameters() {
    super(new Parameter[]{featureLists, rtSmoothing, mobilitySmoothing, removeOriginal, suffix});
  }

  @Override
  public boolean checkParameterValues(Collection<String> errorMessages) {
    boolean superCheck = super.checkParameterValues(errorMessages);
    if (!superCheck) {
      return false;
    }

    if (!this.getParameter(mobilitySmoothing).getValue()
        && !this.getParameter(rtSmoothing).getValue()) {
      errorMessages.add("At least one smoothing type must be selected");
      return false;
    }
    return true;
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
