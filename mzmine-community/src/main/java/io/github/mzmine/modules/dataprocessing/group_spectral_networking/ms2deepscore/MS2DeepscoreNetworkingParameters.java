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

package io.github.mzmine.modules.dataprocessing.group_spectral_networking.ms2deepscore;

import static io.github.mzmine.javafx.components.factories.FxTexts.linebreak;
import static io.github.mzmine.javafx.components.factories.FxTexts.text;

import io.github.mzmine.javafx.components.factories.ArticleReferences;
import io.github.mzmine.javafx.components.factories.FxTextFlows;
import io.github.mzmine.modules.dataprocessing.filter_scan_merge_select.SpectraMergeSelectParameter;
import io.github.mzmine.modules.io.download.AssetGroup;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialog;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.PercentParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameWithDownloadParameter;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.files.ExtensionFilters;
import io.github.mzmine.util.files.FileAndPathUtil;
import java.io.File;
import java.util.Collection;
import java.util.List;
import javafx.scene.layout.Region;
import org.jetbrains.annotations.NotNull;

/**
 * Define any parameters here (see io.github.mzmine.parameters for parameter types) static is needed
 * here to use this parameter as a key to lookup values
 * <p>
 * var flists = parameters.getValue(EmptyFeatureListParameters.featureLists);
 */
public class MS2DeepscoreNetworkingParameters extends SimpleParameterSet {

  public static final SpectraMergeSelectParameter spectraMergeSelect = SpectraMergeSelectParameter.createGnpsSingleScanDefault();

  public static final IntegerParameter minSignals = new IntegerParameter("Minimum signals",
      "The minimum number of fragments for using a spectrum (minimum = 3, default = 4)", 4, 3,
      null);

  public static final PercentParameter minScore = new PercentParameter("Min similarity",
      "The minimum similarity score to store the MS2Deepscore prediction", 0.9, 0.0, 1.0);

  public static final FileNameWithDownloadParameter ms2deepscoreModelFile = new FileNameWithDownloadParameter(
      "MS2Deepscore model",
      "The file location of the MS2Deepscore model, click download to download the model.",
      List.of(ExtensionFilters.PT), AssetGroup.MS2DEEPSCORE);


  public MS2DeepscoreNetworkingParameters() {
    /*
     * The order of the parameters is used to construct the parameter dialog automatically
     */
    super(
        "https://mzmine.github.io/mzmine_documentation/module_docs/group_spectral_net/molecular_networking.html",
        ms2deepscoreModelFile, spectraMergeSelect, minSignals, minScore);
  }

  /**
   * Settings file is always in the same folder and has a suffix of _settings.json instead of .pt
   * format.
   *
   * @param modelFile the model.pt file
   * @return the derived settings file
   */
  @NotNull
  public static File findModelSettingsFile(final File modelFile) {
    File settingsFile = FileAndPathUtil.getRealFilePathWithSuffix(modelFile, "_settings", "json");
    if (!settingsFile.exists()) {
      var otherFile = new File(modelFile.getParentFile(), "settings.json");
      if (otherFile.exists()) {
        return otherFile;
      }
    }
    // return settings file even if it may not exist
    return settingsFile;
  }

  @Override
  public boolean checkParameterValues(final Collection<String> errorMessages,
      final boolean skipRawDataAndFeatureListParameters) {
    boolean result = super.checkParameterValues(errorMessages, skipRawDataAndFeatureListParameters);

    var modelFile = getValue(ms2deepscoreModelFile);
    if (modelFile == null || !modelFile.exists()) {
      errorMessages.add("Cannot find model file please download the MS2Deepscore model.");
      return false;
    }

    File settingsFile = findModelSettingsFile(modelFile);
    if (!settingsFile.exists()) {
      errorMessages.add("""
          Cannot find model settings file. It should be located in the folder together with the model file and follow this naming pattern:
          Model: ms2deepscore_mode.pt; Settings: ms2deepscore_mode_settings.json.""");
      return false;
    }

    return result;
  }

  @Override
  public ExitCode showSetupDialog(boolean valueCheckRequired) {
    final Region message = FxTextFlows.newTextFlowInAccordion("How to cite",
        text("When using MS2Deepscore please cite:"), linebreak(),
        ArticleReferences.MS2DEEPSCORE.hyperlinkText());

    ParameterSetupDialog dialog = new ParameterSetupDialog(valueCheckRequired, this, message);
    dialog.showAndWait();
    return dialog.getExitCode();
  }
}
