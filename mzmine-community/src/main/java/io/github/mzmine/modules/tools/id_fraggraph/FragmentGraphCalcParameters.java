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

package io.github.mzmine.modules.tools.id_fraggraph;

import io.github.mzmine.modules.dataprocessing.group_spectral_networking.SignalFiltersParameters;
import io.github.mzmine.modules.dataprocessing.group_spectral_networking.SpectralSignalFilter;
import io.github.mzmine.modules.dataprocessing.id_formulaprediction.restrictions.elements.ElementalHeuristicParameters;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.elements.ElementsCompositionRangeParameter;
import io.github.mzmine.parameters.parametertypes.elements.ElementsParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.submodules.ParameterSetParameter;
import io.github.mzmine.parameters.parametertypes.submodules.SubModuleParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;

public class FragmentGraphCalcParameters extends SimpleParameterSet {

  public static final MZToleranceParameter ms1Tolerance = new MZToleranceParameter(
      "Precursor formula tolerance",
      "Maximum allowed tolerance for the prediction of precursor ion formulae.", 0.005, 5);

  public static final MZToleranceParameter ms2Tolerance = new MZToleranceParameter(
      "Fragment ion tolerance", "Maximum allowed tolerance for fragment ions.", 0.005, 10);

  public static final ParameterSetParameter<SignalFiltersParameters> ms2SignalFilter = new ParameterSetParameter<>(
      "Fragment spectrum signal filters",
      "Refine the MS2 spectrum by narrowing down the fragment ion search in spectra with a lot of signals.",
      (SignalFiltersParameters) (new SignalFiltersParameters().setValue(
          new SpectralSignalFilter(false, 10, 20, 15, 0.95))).cloneParameterSet());

  public static final ElementsCompositionRangeParameter elements = new ElementsCompositionRangeParameter(
      "Elements", "Define elements for sum formula prediction");
  public static final IntegerParameter maximumFormulae = new IntegerParameter(
      "Maximum number of formulae",
      "Restrict the precursor formula calculation to a certain number.", 50);

  public static final ParameterSetParameter<ElementalHeuristicParameters> heuristicParams = new ParameterSetParameter<>(
      "Heuristics parameters", "Refine calculated precursor and fragment formulae.",
      new ElementalHeuristicParameters());

  public FragmentGraphCalcParameters() {
    super(ms1Tolerance, ms2Tolerance, ms2SignalFilter, elements, maximumFormulae, heuristicParams);
  }
}
