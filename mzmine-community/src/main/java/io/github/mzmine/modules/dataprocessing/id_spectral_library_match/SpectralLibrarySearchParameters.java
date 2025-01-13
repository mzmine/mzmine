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

package io.github.mzmine.modules.dataprocessing.id_spectral_library_match;

import io.github.mzmine.javafx.components.factories.FxTextFlows;
import io.github.mzmine.javafx.components.factories.FxTexts;
import io.github.mzmine.modules.dataprocessing.filter_scan_merge_select.SpectraMergeSelectParameter;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialog;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.AdvancedParametersParameter;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.combowithinput.ComboWithInputComponent;
import io.github.mzmine.parameters.parametertypes.combowithinput.MsLevelFilter;
import io.github.mzmine.parameters.parametertypes.combowithinput.MsLevelFilter.Options;
import io.github.mzmine.parameters.parametertypes.combowithinput.MsLevelFilterParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.selectors.SpectralLibrarySelectionParameter;
import io.github.mzmine.parameters.parametertypes.submodules.ModuleOptionsEnumComboParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.scans.similarity.SpectralSimilarityFunctions;
import java.util.Map;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.Region;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("deprecation")
public class SpectralLibrarySearchParameters extends SimpleParameterSet {

  public static final FeatureListsParameter peakLists = new FeatureListsParameter();


  public static final AdvancedParametersParameter<AdvancedSpectralLibrarySearchParameters> advanced = new AdvancedParametersParameter<>(
      new AdvancedSpectralLibrarySearchParameters());

  public static final SpectralLibrarySelectionParameter libraries = new SpectralLibrarySelectionParameter();

  public static final MsLevelFilterParameter msLevelFilter = new MsLevelFilterParameter(
      new Options[]{Options.MS1, Options.MS2}, new MsLevelFilter(Options.MS2));

  public static final SpectraMergeSelectParameter spectraMergeSelect = SpectraMergeSelectParameter.createSpectraLibrarySearchDefaultNoMSn();

  public static final MZToleranceParameter mzTolerancePrecursor = new MZToleranceParameter(
      "Precursor m/z tolerance", "Precursor m/z tolerance is used to filter library entries", 0.001,
      5);

  public static final MZToleranceParameter mzTolerance = new MZToleranceParameter(
      "Spectral m/z tolerance",
      "Spectral m/z tolerance is used to match all signals in the query and library spectra (usually higher than precursor m/z tolerance)",
      0.0015, 10);

  public static final BooleanParameter removePrecursor = new BooleanParameter("Remove precursor",
      "For MS2 scans, remove precursor signal prior to matching (+- 4 Da)", true);


  public static final IntegerParameter minMatch = new IntegerParameter("Minimum  matched signals",
      "Minimum number of matched signals in masslist and spectral library entry (within mz tolerance)",
      4);

  public static final ModuleOptionsEnumComboParameter<SpectralSimilarityFunctions> similarityFunction = new ModuleOptionsEnumComboParameter<>(
      "Similarity", "Algorithm to calculate similarity and filter matches",
      SpectralSimilarityFunctions.WEIGHTED_COSINE);

  // outdated ex parameters that were replaced but still need to be loaded from old batch files
  // better not static so that they are not shown for this class
  // private final so that they are used internally, only during load
  private final ComboParameter<LegacyScanMatchingSelection> scanMatchingSelection = new ComboParameter<>(
      "Scans for matching", """
      Choose the MS level and experimental scans to match against the library. MS1 for GC-EI-MS data,
      MERGED: will merge all fragment scans, creating one merged spectrum for each fragmentation energy,
              and one consensus spectrum merged from those different energies.
      ALL: will use all available raw fragment scans + the ones from merging.
      MS2: limits the final list to MS2 scans
      MS2 (merged): and a scan were all MSn scans are merged into one 'pseudo' MS2 scan
      MSn: defines all fragment scans of MS level 2 and higher
          """, LegacyScanMatchingSelection.values(), LegacyScanMatchingSelection.MERGED_MSN);

  public SpectralLibrarySearchParameters() {
    super(new Parameter[]{libraries, peakLists, spectraMergeSelect, msLevelFilter,
            mzTolerancePrecursor, mzTolerance, removePrecursor, minMatch, similarityFunction, advanced},
        "https://mzmine.github.io/mzmine_documentation/module_docs/id_spectral_library_search/spectral_library_search.html");
  }

  /**
   * for SelectedRowsParameters
   */
  protected SpectralLibrarySearchParameters(Parameter<?>... parameters) {
    super(parameters);
  }

  @Override
  public ExitCode showSetupDialog(boolean valueCheckRequired) {
    if ((getParameters() == null) || (getParameters().length == 0)) {
      return ExitCode.OK;
    }

    final Region message = FxTextFlows.newTextFlowInAccordion("Spectral libraries", true,
        FxTexts.text("You can find compatible and freely available spectral libraries "),
        FxTexts.hyperlinkText("here.",
            "https://mzmine.github.io/mzmine_documentation/module_docs/id_spectral_library_search/spectral_library_search.html#downloads-for-open-spectral-libraries"));

    ParameterSetupDialog dialog = new ParameterSetupDialog(valueCheckRequired, this, message);

    var msLevelParam = getParameter(SpectralLibrarySearchParameters.msLevelFilter);
    var msLevelFilter = msLevelParam.getValue();

    ComboWithInputComponent<Options> msFilterComp = dialog.getComponentForParameter(
        SpectralLibrarySearchParameters.msLevelFilter);
    CheckBox cRemovePrec = dialog.getComponentForParameter(removePrecursor);
    Node mzTolPrecursor = dialog.getComponentForParameter(mzTolerancePrecursor);

    mzTolPrecursor.setDisable(msLevelFilter.isMs1Only());
    cRemovePrec.setDisable(msLevelFilter.isMs1Only());
    msFilterComp.addValueChangedListener(() -> {
      msLevelParam.setValueFromComponent(msFilterComp);

      var msLevel = msLevelParam.getValue();
      mzTolPrecursor.setDisable(msLevel.isMs1Only());
      cRemovePrec.setDisable(msLevel.isMs1Only());
    });

    dialog.showAndWait();
    return dialog.getExitCode();
  }

  @Override
  public @NotNull IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.SUPPORTED;
  }

  @Override
  public void handleLoadedParameters(final Map<String, Parameter<?>> loadedParams) {
    if (loadedParams.containsKey(scanMatchingSelection.getName())) {
      // old batch with scan selection loaded - apply parameters to new parameters that replaced it
      // legacy parameter need to retrieve value directly because getValue would throw exception
      final LegacyScanMatchingSelection selection = scanMatchingSelection.getValue();
      var msLevel = selection == LegacyScanMatchingSelection.MS1 ? Options.MS1 : Options.MS2;
      setParameter(msLevelFilter, new MsLevelFilter(msLevel));
    }
    if (loadedParams.containsKey(mzTolerance.getName())) {
      // use spectral mz tolerance as merging tolerance
      getParameter(spectraMergeSelect).setMzTolerance(getValue(mzTolerance));
    }
  }

  @Override
  public Map<String, Parameter<?>> getNameParameterMap() {
    var nameParameterMap = super.getNameParameterMap();
    // add removed legacy parameters here so that they are loaded from xml
    // those parameters need to be handled in the {@link #loadValuesFromXML} method
    nameParameterMap.put(scanMatchingSelection.getName(), scanMatchingSelection);
    return nameParameterMap;
  }

  @Override
  public @Nullable String getVersionMessage(final int version) {
    return switch (version) {
      case 3 -> """
          mzmine version > 4.4.3 harmonized the fragment scan selection and merging throughout various modules.
          Ensure to configure the %s parameter to control which scans are matched.""".formatted(
          spectraMergeSelect.getName());
      default -> null;
    };
  }

  @Override
  public int getVersion() {
    return 3;
  }
}
