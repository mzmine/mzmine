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

package io.github.mzmine.modules.visualization.spectra.simplespectra.spectraidentification.sumformula;

import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.id_formulaprediction.restrictions.elements.ElementalHeuristicParameters;
import io.github.mzmine.modules.dataprocessing.id_formulaprediction.restrictions.rdbe.RDBERestrictionParameters;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.elements.ElementsCompositionRangeParameter;
import io.github.mzmine.parameters.parametertypes.submodules.OptionalModuleParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;

/**
 * Parameters for sum formula prediction in spectra.
 * 
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public class SpectraIdentificationSumFormulaParameters extends SimpleParameterSet {

  public static final IntegerParameter charge = new IntegerParameter("Charge", "Charge");

  public static final ComboParameter<IonizationType> ionization =
      new ComboParameter<IonizationType>("Ionization type", "Ionization type",
          IonizationType.values());

  public static final MZToleranceParameter mzTolerance = new MZToleranceParameter();

  public static final DoubleParameter noiseLevel = new DoubleParameter("Noise level",
      "Intensities less than this value are interpreted as noise",
      MZmineCore.getConfiguration().getIntensityFormat(), 0.0);

  public static final ElementsCompositionRangeParameter elements =
      new ElementsCompositionRangeParameter("Elements", "Elements and ranges");

  public static final OptionalModuleParameter<ElementalHeuristicParameters> elementalRatios =
      new OptionalModuleParameter<ElementalHeuristicParameters>("Element count heuristics",
          "Restrict formulas by heuristic restrictions of elemental counts and ratios",
          new ElementalHeuristicParameters());

  public static final OptionalModuleParameter<RDBERestrictionParameters> rdbeRestrictions =
      new OptionalModuleParameter<RDBERestrictionParameters>("RDBE restrictions",
          "Search only for formulas which correspond to the given RDBE restrictions",
          new RDBERestrictionParameters());

  public SpectraIdentificationSumFormulaParameters() {
    super(new Parameter[] {charge, ionization, mzTolerance, noiseLevel, elements, elementalRatios,
        rdbeRestrictions});
  }

}
