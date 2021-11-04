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

package io.github.mzmine.modules.dataprocessing.filter_tracereducer;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialog;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.util.ExitCode;
import javafx.application.Platform;
import org.jetbrains.annotations.NotNull;

public class IonMobilityTraceReducerParameters extends SimpleParameterSet {

  public static final FeatureListsParameter flists = new FeatureListsParameter();

  public static final BooleanParameter removeOriginal = new BooleanParameter("Remove original",
      "If checked, the original feature list is removed after this module finishes.", false);

  public static final StringParameter suffix = new StringParameter("Suffix",
      "The suffix to give to the new feature list.", "reduced", true);

  public IonMobilityTraceReducerParameters() {
    super(new Parameter[]{flists, removeOriginal, suffix});
  }

  @Override
  public @NotNull IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.ONLY;
  }

  @Override
  public ExitCode showSetupDialog(boolean valueCheckRequired) {
    assert Platform.isFxApplicationThread();

    if ((parameters == null) || (parameters.length == 0)) {
      return ExitCode.OK;
    }

    String message = "<h2>WARNING</h2>Application of this module will prohibit further resolving or other "
        + "processing steps of ion mobility data. Therefore, this module should be applied after resolving.";

    if (MZmineCore.getConfiguration().isDarkMode()) {
      message =
          "<html><body style=\"background-color:#1d1d1d;color:white\">"
              + message + "</body></html>";
    } else {
      message =
          "<html><body style=\"background-color:white;color:black\">"
              + message + "</body></html>";
    }

    ParameterSetupDialog dialog = new ParameterSetupDialog(valueCheckRequired, this, message);
    dialog.showAndWait();
    return dialog.getExitCode();
  }
}
