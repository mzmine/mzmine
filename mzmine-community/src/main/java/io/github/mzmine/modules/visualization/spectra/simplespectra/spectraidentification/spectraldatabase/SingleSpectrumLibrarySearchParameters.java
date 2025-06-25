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

package io.github.mzmine.modules.visualization.spectra.simplespectra.spectraidentification.spectraldatabase;

import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.msms.DDAMsMsInfo;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.filter_scan_merge_select.options.SpectraMergeSelectModuleOptions;
import io.github.mzmine.modules.dataprocessing.id_spectral_library_match.AdvancedSpectralLibrarySearchParameters;
import io.github.mzmine.modules.dataprocessing.id_spectral_library_match.SelectedRowsSpectralLibrarySearchParameters;
import io.github.mzmine.modules.dataprocessing.id_spectral_library_match.SpectralLibrarySearchParameters;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialog;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.AdvancedParametersParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.combowithinput.MsLevelFilter;
import io.github.mzmine.parameters.parametertypes.selectors.SpectralLibrarySelection;
import io.github.mzmine.parameters.parametertypes.selectors.SpectralLibrarySelectionType;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceComponent;
import io.github.mzmine.util.ExitCode;
import java.awt.Window;
import java.util.List;
import javafx.scene.control.CheckBox;

/**
 * Module to compare single spectra with spectral libraries
 *
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public class SingleSpectrumLibrarySearchParameters extends SimpleParameterSet {

  public static final OptionalParameter<DoubleParameter> usePrecursorMZ = new OptionalParameter<>(
      new DoubleParameter("Use precursor m/z",
          "Use precursor m/z as a filter. Precursor m/z of library entry and this scan need to be within m/z tolerance. Entries without precursor m/z are skipped.",
          MZmineCore.getConfiguration().getMZFormat(), 0d));

  public SingleSpectrumLibrarySearchParameters() {
    super(new Parameter[]{SpectralLibrarySearchParameters.libraries, usePrecursorMZ,
            SpectralLibrarySearchParameters.mzTolerancePrecursor,
            SpectralLibrarySearchParameters.mzTolerance,
            SpectralLibrarySearchParameters.removePrecursor, SpectralLibrarySearchParameters.minMatch,
            SpectralLibrarySearchParameters.similarityFunction,
            SpectralLibrarySearchParameters.advanced},
        "https://mzmine.github.io/mzmine_documentation/module_docs/id_spectral_library_search/spectral_library_search.html");
  }

  public SelectedRowsSpectralLibrarySearchParameters toSearchParameters(Scan scan) {
    final ParameterSet param = new SelectedRowsSpectralLibrarySearchParameters().cloneParameterSet();

    param.setParameter(SpectralLibrarySearchParameters.libraries,
        getValue(SpectralLibrarySearchParameters.libraries));

    param.setParameter(SpectralLibrarySearchParameters.msLevelFilter,
        MsLevelFilter.of(scan.getMSLevel()));
    param.setParameter(SpectralLibrarySearchParameters.spectraMergeSelect,
        SpectraMergeSelectModuleOptions.INPUT_SCANS);
    param.setParameter(SpectralLibrarySearchParameters.mzTolerancePrecursor,
        getValue(SpectralLibrarySearchParameters.mzTolerancePrecursor));
    param.setParameter(SpectralLibrarySearchParameters.mzTolerance,
        getValue(SpectralLibrarySearchParameters.mzTolerance));
    param.setParameter(SpectralLibrarySearchParameters.removePrecursor,
        getValue(SpectralLibrarySearchParameters.removePrecursor));
    param.setParameter(SpectralLibrarySearchParameters.minMatch,
        getValue(SpectralLibrarySearchParameters.minMatch));
    param.setParameter(SpectralLibrarySearchParameters.similarityFunction,
        getValue(SpectralLibrarySearchParameters.similarityFunction));
    param.setParameter(SpectralLibrarySearchParameters.advanced,
        getValue(SpectralLibrarySearchParameters.advanced));

    final AdvancedParametersParameter<AdvancedSpectralLibrarySearchParameters> advancedParam = param.getParameter(
        SpectralLibrarySearchParameters.advanced);
    advancedParam.setEmbeddedParameters(
        (AdvancedSpectralLibrarySearchParameters) getEmbeddedParameterValue(SpectralLibrarySearchParameters.advanced));

    return (SelectedRowsSpectralLibrarySearchParameters) param;
  }

  @Override
  public ExitCode showSetupDialog(boolean valueCheckRequired) {
    if ((getParameters() == null) || (getParameters().length == 0)) {
      return ExitCode.OK;
    }
    ParameterSetupDialog dialog = new ParameterSetupDialog(valueCheckRequired, this);

    CheckBox usePreComp = dialog.getComponentForParameter(usePrecursorMZ).getCheckBox();
    CheckBox cRemovePrec = dialog.getComponentForParameter(
        SpectralLibrarySearchParameters.removePrecursor);
    MZToleranceComponent mzTolPrecursor = dialog.getComponentForParameter(
        SpectralLibrarySearchParameters.mzTolerancePrecursor);

    // set initial
//    mzTolPrecursor.setDisable(!usePreComp.isSelected());
//    cRemovePrec.setDisable(!usePreComp.isSelected());

    // bind
    mzTolPrecursor.disableProperty().bind(usePreComp.selectedProperty().not());
    cRemovePrec.disableProperty().bind(usePreComp.selectedProperty().not());

    dialog.showAndWait();
    return dialog.getExitCode();
  }

  public ExitCode showSetupDialog(Scan scan, Window parent, boolean valueCheckRequired) {
    // set precursor mz to parameter if MS2 scan
    // otherwise leave the value to the one specified before
    double precursorMz =
        scan.getMsMsInfo() instanceof DDAMsMsInfo info ? info.getIsolationMz() : 0d;
    if (precursorMz != 0) {
      this.getParameter(usePrecursorMZ).getEmbeddedParameter().setValue(precursorMz);
    } else {
      this.getParameter(usePrecursorMZ).setValue(false);
    }

    return this.showSetupDialog(valueCheckRequired);
  }

  @Override
  public int getVersion() {
    return 2;
  }
}
