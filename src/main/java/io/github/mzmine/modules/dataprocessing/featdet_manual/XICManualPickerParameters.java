/*
 *  Copyright 2006-2022 The MZmine Development Team
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

package io.github.mzmine.modules.dataprocessing.featdet_manual;

import com.google.common.collect.Range;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialog;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.HiddenParameter;
import io.github.mzmine.parameters.parametertypes.ranges.DoubleRangeParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelection;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelection;
import io.github.mzmine.util.ExitCode;
import org.jetbrains.annotations.NotNull;

public class XICManualPickerParameters extends SimpleParameterSet {

  public static final HiddenParameter<RawDataFilesParameter, RawDataFilesSelection> rawDataFiles = new HiddenParameter<>(
      new RawDataFilesParameter("Raw data file", 1, 100));

  public static final HiddenParameter<FeatureListsParameter, FeatureListsSelection> flists = new HiddenParameter<>(
      new FeatureListsParameter(1, 1));

  public static final HiddenParameter<DoubleRangeParameter, Range<Double>> rtRange = new HiddenParameter<>(
      new DoubleRangeParameter("Retention time", "Retention time range",
          MZmineCore.getConfiguration().getRTFormat()));

  public static final HiddenParameter<DoubleRangeParameter, Range<Double>> mzRange = new HiddenParameter<>(
      new DoubleRangeParameter("m/z range", "m/z range",
          MZmineCore.getConfiguration().getMZFormat()));

  public XICManualPickerParameters() {
    super(new Parameter[]{rawDataFiles, flists, rtRange, mzRange});
  }

  @Override
  public ExitCode showSetupDialog(boolean valueCheckRequired) {

    ParameterSetupDialog dialog = new XICManualPickerDialog(true, this);
    dialog.showAndWait();

    return dialog.getExitCode();
  }

  @Override
  public @NotNull IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.UNSUPPORTED;
  }
}
