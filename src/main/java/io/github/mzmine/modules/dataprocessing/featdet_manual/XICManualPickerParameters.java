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

  public static final HiddenParameter<RawDataFilesSelection> rawDataFiles = new HiddenParameter<>(
      new RawDataFilesParameter("Raw data file", 1, 100));

  public static final HiddenParameter<FeatureListsSelection> flists = new HiddenParameter<>(
      new FeatureListsParameter(1, 1));

  public static final HiddenParameter<Range<Double>> rtRange = new HiddenParameter<>(
      new DoubleRangeParameter("Retention time", "Retention time range",
          MZmineCore.getConfiguration().getRTFormat()));

  public static final HiddenParameter<Range<Double>> mzRange = new HiddenParameter<>(
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
