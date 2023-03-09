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
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.combowithinput.MsLevelFilter;
import io.github.mzmine.parameters.parametertypes.combowithinput.MsLevelFilter.Options;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.selectors.SpectralLibrarySelectionParameter;
import io.github.mzmine.parameters.parametertypes.submodules.ModuleComboParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.scans.similarity.SpectralSimilarityFunction;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import org.jetbrains.annotations.NotNull;

public class SpectralLibrarySearchParameters extends SimpleParameterSet {

  public static final FeatureListsParameter peakLists = new FeatureListsParameter();


  public static final AdvancedParametersParameter<AdvancedSpectralLibrarySearchParameters> advanced = new AdvancedParametersParameter<>(
      new AdvancedSpectralLibrarySearchParameters());

  public static final SpectralLibrarySelectionParameter libraries = new SpectralLibrarySelectionParameter();

  public static final ComboParameter<ScanMatchingSelection> scanMatchingSelection = new ComboParameter<>(
      "Scans for matching", """
      Choose the MS level and experimental scans to match against the library. MS1 for GC-EI-MS data,
      MERGED: will merge all fragment scans, creating one merged spectrum for each fragmentation energy,
              and one consensus spectrum merged from those different energies.
      ALL: will use all available raw fragment scans + the ones from merging.
      MS2: limits the final list to MS2 scans
      MS2 (merged): and a scan were all MSn scans are merged into one 'pseudo' MS2 scan
      MSn: defines all fragment scans of MS level 2 and higher
          """, ScanMatchingSelection.values(), ScanMatchingSelection.MERGED_MSN);

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

  public SpectralLibrarySearchParameters() {
    super(new Parameter[]{peakLists, libraries, scanMatchingSelection, mzTolerancePrecursor,
            mzTolerance, removePrecursor, minMatch, similarityFunction, advanced},
        "https://mzmine.github.io/mzmine_documentation/module_docs/id_spectral_library_search/spectral_library_search.html");
  }

  /**
   * for SelectedRowsParameters
   */
  protected SpectralLibrarySearchParameters(Parameter[] parameters) {
    super(parameters);
  }

  @Override
  public ExitCode showSetupDialog(boolean valueCheckRequired) {
    if ((getParameters() == null) || (getParameters().length == 0)) {
      return ExitCode.OK;
    }
    ParameterSetupDialog dialog = new ParameterSetupDialog(valueCheckRequired, this);

    var selection = getValue(scanMatchingSelection);
    var msLevelFilter = selection.getMsLevelFilter();

    var msSelectionComp = dialog.getComponentForParameter(scanMatchingSelection);
    CheckBox cRemovePrec = dialog.getComponentForParameter(removePrecursor);
    Node mzTolPrecursor = dialog.getComponentForParameter(mzTolerancePrecursor);

    mzTolPrecursor.setDisable(msLevelFilter.isMs1Only());
    cRemovePrec.setDisable(msLevelFilter.isMs1Only());
    msSelectionComp.getSelectionModel().selectedItemProperty()
        .addListener((observable, oldValue, newValue) -> {
          try {
            var newLevelFilter = newValue.getMsLevelFilter();
            boolean isMS1 = newLevelFilter.isMs1Only();
            mzTolPrecursor.setDisable(isMS1);
            cRemovePrec.setDisable(isMS1);
          } catch (Exception ex) {
            // do nothing user might be still typing
            mzTolPrecursor.setDisable(true);
            cRemovePrec.setDisable(true);
          }
    });

    dialog.showAndWait();
    return dialog.getExitCode();
  }

  public enum ScanMatchingSelection {
    MS1, MERGED_MS2, MERGED_MSN, ALL_MS2, ALL_MSN;

    @Override
    public String toString() {
      return switch (this) {
        case MS1 -> "MS1";
        case MERGED_MS2 -> "MS2 (merged)";
        case MERGED_MSN -> "MS level ≥ 2 (merged)";
        case ALL_MS2 -> "MS2 (all scans)";
        case ALL_MSN -> "MS level ≥ 2 (all scans)";
      };
    }

    public MsLevelFilter getMsLevelFilter() {
      return switch (this) {
        case MS1 -> new MsLevelFilter(Options.MS1);
        case MERGED_MS2, ALL_MS2 -> new MsLevelFilter(Options.MS2);
        case MERGED_MSN, ALL_MSN -> new MsLevelFilter(Options.MSn);
      };
    }

    public boolean isAll() {
      return this == ALL_MS2 || this == ALL_MSN;
    }
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
