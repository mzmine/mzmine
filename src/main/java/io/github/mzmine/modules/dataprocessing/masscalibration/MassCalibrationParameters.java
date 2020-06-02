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

package io.github.mzmine.modules.dataprocessing.masscalibration;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.MassListParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileSelectionType;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;

import java.text.NumberFormat;

public class MassCalibrationParameters extends SimpleParameterSet {

  public static final RawDataFilesParameter dataFiles = new RawDataFilesParameter();

  public static final MassListParameter massList = new MassListParameter();

  public static final FileNameParameter standardsList = new FileNameParameter("Standards list",
          "File with a list of standard molecules expected to appear in the dataset",
          FileSelectionType.OPEN);

  public static final DoubleParameter tolerance = new DoubleParameter("Range tolerance",
          "Range tolerance is the max distance allowed between errors to be included in the range.",
          NumberFormat.getNumberInstance(), 0.4, 0.0, Double.POSITIVE_INFINITY);

  public static final StringParameter suffix = new StringParameter("Suffix",
          "This string is added to mass list name as a suffix", "calibrated");

  public static final BooleanParameter autoRemove =
          new BooleanParameter("Remove original mass list",
                  "If checked, original mass list will be removed and only filtered version remains");

  public MassCalibrationParameters() {
    super(new Parameter[]{dataFiles, massList, standardsList, tolerance, suffix, autoRemove});
  }

  /*@Override
  public ExitCode showSetupDialog(boolean valueCheckRequired) {
    ShoulderPeaksFilterSetupDialog dialog =
        new ShoulderPeaksFilterSetupDialog(valueCheckRequired, this);
    dialog.showAndWait();
    return dialog.getExitCode();
  }*/

}
