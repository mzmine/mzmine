/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.modules.dataprocessing.id_postcolumnreaction;

import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.modules.dataprocessing.id_formulaprediction.restrictions.elements.ElementalHeuristicParameters;
import io.github.mzmine.modules.dataprocessing.id_formulaprediction.restrictions.rdbe.RDBERestrictionParameters;
import io.github.mzmine.modules.tools.isotopepatternscore.IsotopePatternScoreParameters;
import io.github.mzmine.modules.tools.msmsscore.MSMSScoreParameters;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.submodules.OptionalModuleParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;

public class PostColumnReactionFormulaPredictionParameters extends SimpleParameterSet {

  public static final ComboParameter<IonizationType> predIonization = new ComboParameter<>(
      "Ionization type", "Ionization type", IonizationType.values(),
      IonizationType.POSITIVE_HYDROGEN);

  public static final MZToleranceParameter predMZTolerance = new MZToleranceParameter(0.002, 5);

  public static final OptionalModuleParameter<ElementalHeuristicParameters> predElementalRatios = new OptionalModuleParameter<>(
      "Element count heuristics",
      "Restrict formulas by heuristic restrictions of elemental counts and ratios",
      new ElementalHeuristicParameters(), false);

  public static final OptionalModuleParameter<RDBERestrictionParameters> predRdbeRestrictions = new OptionalModuleParameter<>(
      "RDBE restrictions",
      "Search only for formulas which correspond to the given RDBE restrictions",
      new RDBERestrictionParameters(), false);

  public static final OptionalModuleParameter<IsotopePatternScoreParameters> predIsotopeFilter = new OptionalModuleParameter<>(
      "Isotope pattern filter", "Search only for formulas with a isotope pattern similar",
      new IsotopePatternScoreParameters(), false);

  public static final OptionalModuleParameter<MSMSScoreParameters> predMsmsFilter = new OptionalModuleParameter<>(
      "MS/MS filter", "Check MS/MS data", new MSMSScoreParameters(), false);

  public PostColumnReactionFormulaPredictionParameters() {
    super(predIonization, predMZTolerance, predElementalRatios, predRdbeRestrictions,
        predIsotopeFilter, predMsmsFilter);
  }
}
