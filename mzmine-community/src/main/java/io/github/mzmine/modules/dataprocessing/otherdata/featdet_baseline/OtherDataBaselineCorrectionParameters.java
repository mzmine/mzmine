/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.modules.dataprocessing.otherdata.featdet_baseline;

import io.github.mzmine.modules.dataprocessing.featdet_baselinecorrection.BaselineCorrectors;
import io.github.mzmine.parameters.dialogs.ParameterDialogWithPreviewPanes;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialog;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.other_detectors.OtherRawOrProcessed;
import io.github.mzmine.parameters.parametertypes.other_detectors.OtherTraceSelection;
import io.github.mzmine.parameters.parametertypes.other_detectors.OtherTraceSelectionParameter;
import io.github.mzmine.parameters.parametertypes.submodules.ModuleOptionsEnumComboParameter;
import io.github.mzmine.util.ExitCode;
import java.util.List;
import javafx.application.Platform;

public class OtherDataBaselineCorrectionParameters extends SimpleParameterSet {

  public static final OtherTraceSelectionParameter traces = new OtherTraceSelectionParameter(
      "Select traces", "Select the traces you want to process.", OtherTraceSelection.rawUv(),
      List.of(OtherRawOrProcessed.RAW, OtherRawOrProcessed.PREPROCESSED));

  public static final StringParameter suffix = new StringParameter("Suffix",
      "Suffix for the baseline corrected traces.", "bl");

  public static final ModuleOptionsEnumComboParameter<BaselineCorrectors> correctionAlgorithm = new ModuleOptionsEnumComboParameter<>(
      "Baseline corrector", "Select the baseline correction algorithm.",
      new BaselineCorrectors[]{BaselineCorrectors.LOESS, BaselineCorrectors.POLYNOMIAL,
          BaselineCorrectors.SPLINE}, BaselineCorrectors.LOESS);

  public OtherDataBaselineCorrectionParameters() {
    super(traces, suffix, correctionAlgorithm);
  }

  @Override
  public ExitCode showSetupDialog(boolean valueCheckRequired) {
    assert Platform.isFxApplicationThread();

    if ((parameters == null) || (parameters.length == 0)) {
      return ExitCode.OK;
    }
    ParameterSetupDialog dialog = new ParameterDialogWithPreviewPanes(valueCheckRequired, this,
        OtherDataBaselineCorrectionPreview::new);
    dialog.showAndWait();
    return dialog.getExitCode();
  }
}
