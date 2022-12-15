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
import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.isotopes.MassListDeisotoperParameters;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialog;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.ModuleComboParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.PercentParameter;
import io.github.mzmine.parameters.parametertypes.selectors.SpectralLibrarySelectionParameter;
import io.github.mzmine.parameters.parametertypes.submodules.OptionalModuleParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceComponent;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.scans.similarity.SpectralSimilarityFunction;
import java.awt.Window;
import java.util.Collection;
import javafx.scene.control.CheckBox;

/**
 * Module to compare single spectra with spectral libraries
 *
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public class SingleSpectrumLibrarySearchParameters extends SimpleParameterSet {

  public static final SpectralLibrarySelectionParameter libraries = new SpectralLibrarySelectionParameter();

  public static final MZToleranceParameter mzTolerance = new MZToleranceParameter(
      "Spectral m/z tolerance",
      "Tolerance to match spectral signals in the query and library spectra (usually higher than precursor m/z tolerance (if used))",
      0.0015, 10);

  public static final OptionalParameter<DoubleParameter> usePrecursorMZ = new OptionalParameter<>(
      new DoubleParameter("Use precursor m/z",
          "Use precursor m/z as a filter. Precursor m/z of library entry and this scan need to be within m/z tolerance. Entries without precursor m/z are skipped.",
          MZmineCore.getConfiguration().getMZFormat(), 0d));
  public static final MZToleranceParameter mzTolerancePrecursor = new MZToleranceParameter(
      "Precursor m/z tolerance", "Precursor m/z tolerance is used to filter library entries", 0.001,
      5);

  public static final OptionalParameter<PercentParameter> ccsTolerance = new OptionalParameter<>(
      new PercentParameter("CCS tolerance [%]",
          "CCS tolerance for spectral library entries to be matched against a feature.\n"
              + "If the row or the library entry does not have a CCS value, no spectrum will be matched.",
          0.05), true);

  public static final BooleanParameter removePrecursor = new BooleanParameter("Remove precursor",
      "For MS2 scans, remove precursor signal prior to matching (+- precursor m/z tolerance)",
      false);

  public static final DoubleParameter noiseLevel = new DoubleParameter("Minimum ion intensity",
      "Signals below this level will be filtered away from mass lists",
      MZmineCore.getConfiguration().getIntensityFormat(), 0d);

  public static final IntegerParameter minMatch = new IntegerParameter("Minimum  matched signals",
      "Minimum number of matched signals in spectra and spectral library entry (within mz tolerance)",
      20);

  public static final OptionalParameter<IntegerParameter> needsIsotopePattern = new OptionalParameter<>(
      new IntegerParameter("Min matched isotope signals",
          "Useful for scans and libraries with isotope pattern. Minimum matched signals of 13C isotopes, distance of H and 2H or Cl isotopes. Can not be applied with deisotoping",
          3, 0, 1000), false);

  public static final OptionalModuleParameter<MassListDeisotoperParameters> deisotoping = new OptionalModuleParameter<>(
      "13C deisotoping", "Removes 13C isotope signals from mass lists",
      new MassListDeisotoperParameters(), true);

  public static final BooleanParameter cropSpectraToOverlap = new BooleanParameter(
      "Crop spectra to m/z overlap",
      "Crop query and library spectra to overlapping m/z range (+- spectra m/z tolerance). This is helptful if spectra were acquired with different fragmentation energies / methods.",
      true);

  public static final ModuleComboParameter<SpectralSimilarityFunction> similarityFunction = new ModuleComboParameter<>(
      "Similarity", "Algorithm to calculate similarity and filter matches",
      SpectralSimilarityFunction.FUNCTIONS, SpectralSimilarityFunction.weightedCosine);

  public SingleSpectrumLibrarySearchParameters() {
    super(new Parameter[]{libraries, usePrecursorMZ, mzTolerancePrecursor, removePrecursor,
        ccsTolerance, noiseLevel, deisotoping, needsIsotopePattern, cropSpectraToOverlap,
        mzTolerance, minMatch, similarityFunction});
  }

  @Override
  public boolean checkParameterValues(Collection<String> errorMessages) {
    boolean check = super.checkParameterValues(errorMessages);

    // not both isotope and deisotope
    boolean isotope =
        !getParameter(deisotoping).getValue() || !getParameter(needsIsotopePattern).getValue();
    if (!isotope) {
      errorMessages.add(
          "Choose only one of \"deisotoping\" and \"need isotope pattern\" at the same time");
      return false;
    }
    return check;
  }

  @Override
  public ExitCode showSetupDialog(boolean valueCheckRequired) {
    if ((getParameters() == null) || (getParameters().length == 0)) {
      return ExitCode.OK;
    }
    ParameterSetupDialog dialog = new ParameterSetupDialog(valueCheckRequired, this);

    CheckBox usePreComp = dialog.getComponentForParameter(usePrecursorMZ).getCheckBox();
    CheckBox cRemovePrec = dialog.getComponentForParameter(removePrecursor);
    MZToleranceComponent mzTolPrecursor = dialog.getComponentForParameter(mzTolerancePrecursor);

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
}
