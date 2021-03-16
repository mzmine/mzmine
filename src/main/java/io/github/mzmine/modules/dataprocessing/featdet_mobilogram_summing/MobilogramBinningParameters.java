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

package io.github.mzmine.modules.dataprocessing.featdet_mobilogram_summing;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.util.ExitCode;
import java.text.DecimalFormat;
import javafx.application.Platform;

/**
 * @author Steffen https://github.com/SteffenHeu
 */
public class MobilogramBinningParameters extends SimpleParameterSet {

  public static final FeatureListsParameter featureLists = new FeatureListsParameter();

  public static final DoubleParameter timsBinningWidth = new DoubleParameter(
      "TIMS binning width (Vs/cm²)",
      "The binning width in mobility units of the selected raw data file.",
      new DecimalFormat("0.0000"), 0.005, 0.00001, 1E6);

  public static final DoubleParameter dtimsBinningWidth = new DoubleParameter(
      "Drift tube binning width (ms)",
      "The binning width in mobility units of the selected raw data file.",
      new DecimalFormat("0.00"), 0.5, 0.00001, 1E6);

  public static final DoubleParameter twimsBinningWidth = new DoubleParameter(
      "Travelling wave binning width (ms)",
      "The binning width in mobility units of the selected raw data file.",
      new DecimalFormat("0.00"), 0.5, 0.00001, 1E6);

  public static final ComboParameter<BinningSource> summingSource = new ComboParameter<BinningSource>(
      "Data source",
      "\"Preprocessed\" will use the already summed mobilogram. This is more performant "
          + "and applicable if the new binning width is bigger than the old binning width.\n If a "
          + "lower binning with than previously used is entered, the mobilogram will contain zero values.\n"
          + "\"Raw\" will use the data from the originally detected data points. This will take "
          + "longer, but allow re-binning with a lower binning width than used previously.",
      BinningSource.values(), BinningSource.PREPROCESSED);

  public static final BooleanParameter createNewFeatureList = new BooleanParameter(
      "Create new feature list", "Specifies if a new feature list shall be created.");

  public static final StringParameter suffix = new StringParameter("Suffix",
      "Suffix of the new feature list (in case a new feature list is created)", "summed");

  public MobilogramBinningParameters() {
    super(new Parameter[]{featureLists, timsBinningWidth, dtimsBinningWidth, twimsBinningWidth,
        summingSource, createNewFeatureList, suffix});
  }

  @Override
  public IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.ONLY;
  }

  @Override
  public ExitCode showSetupDialog(boolean valueCheckRequired) {
    assert Platform.isFxApplicationThread();

    if ((parameters == null) || (parameters.length == 0)) {
      return ExitCode.OK;
    }
    MobilogramBinningSetupDialog dialog = new MobilogramBinningSetupDialog(valueCheckRequired,
        this);
    dialog.showAndWait();
    return dialog.getExitCode();
  }
}
