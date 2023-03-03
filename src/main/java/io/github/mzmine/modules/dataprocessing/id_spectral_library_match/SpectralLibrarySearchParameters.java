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

package io.github.mzmine.modules.dataprocessing.id_spectral_library_match;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialog;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.AdvancedParametersParameter;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.combowithinput.MsLevelFilter;
import io.github.mzmine.parameters.parametertypes.combowithinput.MsLevelFilter.Options;
import io.github.mzmine.parameters.parametertypes.combowithinput.MsLevelFilterParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.selectors.SpectralLibrarySelectionParameter;
import io.github.mzmine.parameters.parametertypes.submodules.ModuleComboParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.scans.similarity.SpectralSimilarityFunction;
import java.util.Map;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import org.jetbrains.annotations.NotNull;

public class SpectralLibrarySearchParameters extends SimpleParameterSet {

  public static final FeatureListsParameter peakLists = new FeatureListsParameter();


  public static final AdvancedParametersParameter<AdvancedSpectralLibrarySearchParameters> advanced = new AdvancedParametersParameter<>(
      new AdvancedSpectralLibrarySearchParameters());

  public static final SpectralLibrarySelectionParameter libraries = new SpectralLibrarySelectionParameter();

  public static final MsLevelFilterParameter msLevel = new MsLevelFilterParameter(
      new Options[]{Options.MS1, Options.MS2, Options.MSn, Options.SPECIFIC_LEVEL},
      new MsLevelFilter(Options.MS2, 2));

  public static final BooleanParameter allMS2Spectra = new BooleanParameter(
      "Check all scan (fragmentation scans only)",
      "Check all (or only most intense) MS2 scan. This option does not apply to MS1 scans.", false);

  public static final MZToleranceParameter mzTolerancePrecursor = new MZToleranceParameter(
      "Precursor m/z tolerance", "Precursor m/z tolerance is used to filter library entries", 0.001,
      5);

  public static final MZToleranceParameter mzTolerance = new MZToleranceParameter(
      "Spectral m/z tolerance",
      "Spectral m/z tolerance is used to match all signals in the query and library spectra (usually higher than precursor m/z tolerance)",
      0.0015, 10);

  public static final BooleanParameter removePrecursor = new BooleanParameter("Remove precursor",
      "For MS2 scans, remove precursor signal prior to matching (+- precursor m/z tolerance)",
      true);


  public static final IntegerParameter minMatch = new IntegerParameter("Minimum  matched signals",
      "Minimum number of matched signals in masslist and spectral library entry (within mz tolerance)",
      4);

  public static final ModuleComboParameter<SpectralSimilarityFunction> similarityFunction = new ModuleComboParameter<>(
      "Similarity", "Algorithm to calculate similarity and filter matches",
      SpectralSimilarityFunction.FUNCTIONS, SpectralSimilarityFunction.weightedCosine);


  /**
   * for SelectedRowsParameters
   */
  protected SpectralLibrarySearchParameters(Parameter[] parameters) {
    super(parameters);
  }

  public SpectralLibrarySearchParameters() {
    super(new Parameter[]{peakLists, libraries, msLevel, allMS2Spectra, mzTolerancePrecursor,
            mzTolerance, removePrecursor, minMatch, similarityFunction, advanced},
        "https://mzmine.github.io/mzmine_documentation/module_docs/id_spectral_library_search/spectral_library_search.html");
  }


  @Override
  public Map<String, Parameter<?>> getNameParameterMap() {
    // parameters were renamed but stayed the same type
    var nameParameterMap = super.getNameParameterMap();
    // we use the same parameters here so no need to increment the version. Loading will work fine
    nameParameterMap.put("MS level", msLevel);
    nameParameterMap.put("Check all scans (only for MS2)", allMS2Spectra);
    return nameParameterMap;
  }

  @Override
  public ExitCode showSetupDialog(boolean valueCheckRequired) {
    if ((getParameters() == null) || (getParameters().length == 0)) {
      return ExitCode.OK;
    }
    ParameterSetupDialog dialog = new ParameterSetupDialog(valueCheckRequired, this);

    var msLevelFilter = getValue(msLevel);
    boolean isMs1 = msLevelFilter.isMs1Only();

    var msLevelComp = dialog.getComponentForParameter(msLevel);
    CheckBox cRemovePrec = dialog.getComponentForParameter(removePrecursor);
    CheckBox cAllMS2 = dialog.getComponentForParameter(allMS2Spectra);
    Node mzTolPrecursor = dialog.getComponentForParameter(mzTolerancePrecursor);

    mzTolPrecursor.setDisable(isMs1);
    cRemovePrec.setDisable(isMs1);
    cAllMS2.setDisable(isMs1);

    msLevelComp.addValueChangedListener(() -> {
      try {
        var tmpParam = getParameter(msLevel).cloneParameter();
        tmpParam.setValueFromComponent(msLevelComp);
        var msFilter = tmpParam.getValue();
        boolean isMS1 = msFilter.isMs1Only();
        mzTolPrecursor.setDisable(isMS1);
        cRemovePrec.setDisable(isMS1);
        cAllMS2.setDisable(isMS1);
      } catch (Exception ex) {
        // do nothing user might be still typing
        mzTolPrecursor.setDisable(true);
        cRemovePrec.setDisable(true);
        cAllMS2.setDisable(true);
      }
    });

    dialog.showAndWait();
    return dialog.getExitCode();
  }

  @Override
  public @NotNull IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.SUPPORTED;
  }

  @Override
  public int getVersion() {
    return 2;
  }
}
