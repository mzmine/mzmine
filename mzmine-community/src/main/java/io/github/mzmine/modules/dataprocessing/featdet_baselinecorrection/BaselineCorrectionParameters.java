/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.modules.dataprocessing.featdet_baselinecorrection;

import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.parameters.dialogs.ParameterDialogWithPreviewPanes;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialog;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.OriginalFeatureListHandlingParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.submodules.ModuleOptionsEnumComboParameter;
import io.github.mzmine.util.ExitCode;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;
import javafx.application.Platform;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BaselineCorrectionParameters extends SimpleParameterSet {

  public static final FeatureListsParameter flists = new FeatureListsParameter();

  public static final StringParameter suffix = new StringParameter("Suffix",
      "Suffix for the new feature list.", "bl");

  public static final ModuleOptionsEnumComboParameter<BaselineCorrectors> correctionAlgorithm = new ModuleOptionsEnumComboParameter<>(
      "Baseline corrector", "Select the baseline correction algorithm.",
      new BaselineCorrectors[]{BaselineCorrectors.LOESS, BaselineCorrectors.POLYNOMIAL,
          BaselineCorrectors.SPLINE}, BaselineCorrectors.LOESS);

  public static final OriginalFeatureListHandlingParameter handleOriginal = new OriginalFeatureListHandlingParameter(
      false);

  public BaselineCorrectionParameters() {
    super(flists, suffix, correctionAlgorithm, handleOriginal);
  }

  @Override
  public boolean checkParameterValues(Collection<String> errorMessages,
      boolean skipRawDataAndFeatureListParameters) {
    boolean value = super.checkParameterValues(errorMessages, skipRawDataAndFeatureListParameters);

    final @NotNull ModularFeatureList[] flists = getValue(
        BaselineCorrectionParameters.flists).getMatchingFeatureLists();
    final String error = Arrays.stream(flists).filter(flist -> flist.getNumberOfRawDataFiles() > 1)
        .map(ModularFeatureList::getName).collect(Collectors.joining(", "));

    if (error != null && !error.isBlank()) {
      errorMessages.add("Feature lists " + error
          + " contain more than one raw file. This module is intended to be used directly after chromatogram detection, not after alignment.");
    }

    if (!value || !errorMessages.isEmpty()) {
      return false;
    }
    return true;
  }

  @Override
  public ExitCode showSetupDialog(boolean valueCheckRequired) {
    assert Platform.isFxApplicationThread();

    if ((parameters == null) || (parameters.length == 0)) {
      return ExitCode.OK;
    }
    ParameterSetupDialog dialog = new ParameterDialogWithPreviewPanes(valueCheckRequired, this,
        BaselineCorrectionPreview::new);
    dialog.showAndWait();
    return dialog.getExitCode();
  }

  @Override
  public int getVersion() {
    return 2;
  }

  @Override
  public @Nullable String getVersionMessage(int version) {
    return switch (version) {
      case 2 ->
          "Baseline correction parameters were updated to use a relative percentage of sampled data points instead of an absolute number.";
      default -> null;
    };
  }
}
