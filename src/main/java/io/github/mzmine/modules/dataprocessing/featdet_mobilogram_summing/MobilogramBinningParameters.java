/*
 * Copyright (c) 2004-2022 The MZmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.dataprocessing.featdet_mobilogram_summing;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.util.ExitCode;
import javafx.application.Platform;
import org.jetbrains.annotations.NotNull;

/**
 * @author Steffen https://github.com/SteffenHeu
 */
public class MobilogramBinningParameters extends SimpleParameterSet {

  public static final int DEFAULT_TIMS_BIN_WIDTH = 1;
  public static final int DEFAULT_DTIMS_BIN_WIDTH = 1;
  public static final int DEFAULT_TWIMS_BIN_WIDTH = 1;

  public static final FeatureListsParameter featureLists = new FeatureListsParameter();

  public static final IntegerParameter timsBinningWidth = new IntegerParameter(
      "Override default TIMS (Vs/cm²) binning width",
      "The binning width in scans of the selected raw data file.\n"
          + " The default binning width is " + DEFAULT_TIMS_BIN_WIDTH + ".",
      DEFAULT_TIMS_BIN_WIDTH, 1, 1000);

  public static final IntegerParameter twimsBinningWidth = new IntegerParameter(
      "Travelling wave (ms) binning width",
      "The binning width in scans of the selected raw data file."
          + "The default binning width is " + DEFAULT_TWIMS_BIN_WIDTH + ".",
      DEFAULT_TWIMS_BIN_WIDTH, 1, 1000);

  public static final IntegerParameter dtimsBinningWidth = new IntegerParameter(
      "Drift tube (ms) binning width",
      "The binning width in scans of the selected raw data file.\n"
          + "The default binning width is " + DEFAULT_TIMS_BIN_WIDTH + ".",
      DEFAULT_DTIMS_BIN_WIDTH, 1, 1000);

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

  @NotNull
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
