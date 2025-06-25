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
package io.github.mzmine.modules.dataprocessing.id_ion_identity_networking.formula.prediction;


import io.github.mzmine.main.ConfigService;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.id_formula_sort.FormulaSortParameters;
import io.github.mzmine.modules.dataprocessing.id_formulaprediction.restrictions.elements.ElementalHeuristicParameters;
import io.github.mzmine.modules.dataprocessing.id_formulaprediction.restrictions.rdbe.RDBERestrictionParameters;
import io.github.mzmine.modules.tools.isotopepatternscore.IsotopePatternScoreParameters;
import io.github.mzmine.modules.tools.msmsscore.MSMSScoreParameters;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.OptionForValues;
import io.github.mzmine.parameters.parametertypes.OptionForValuesParameter;
import io.github.mzmine.parameters.parametertypes.ValueOption;
import io.github.mzmine.parameters.parametertypes.elements.ElementsCompositionRangeParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.submodules.OptionalModuleParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import io.github.mzmine.util.maths.MathOperator;
import org.jetbrains.annotations.NotNull;

public class FormulaPredictionIonNetworkParameters extends SimpleParameterSet {

  public static final FeatureListsParameter PEAK_LISTS = new FeatureListsParameter();

  public static final MZToleranceParameter mzTolerance = new MZToleranceParameter(0.002, 5);

  public static final DoubleParameter ppmOffset = new DoubleParameter("Center by ppm offset",
      "Linear correction to mass difference offset. If all correct results are shifted by +4 ppm use -4 ppm to shift these molecular formulae to the center",
      ConfigService.getGuiFormats().ppmFormat(), 0d);

  public static final OptionalModuleParameter<FormulaSortParameters> sorting = new OptionalModuleParameter<FormulaSortParameters>(
      "Sorting", "Apply sorting to all resulting lists", new FormulaSortParameters(true), true);

  public static final ElementsCompositionRangeParameter elements = new ElementsCompositionRangeParameter(
      "Elements", "Elements and ranges");

  public static final OptionForValuesParameter handleHigherMz = new OptionForValuesParameter(
      "Handle higher m/z",
      "Options to exclude or simplify formula prediction on higher m/z values, as they take long "
      + "to compute (high search space). Simplify means that formulas greater equal the value are only computed "
      + "for the average neutral mass - not for all ions individually.",
      MZmineCore.getConfiguration().getMZFormat(), true, ValueOption.values(),
      new MathOperator[]{MathOperator.GREATER_EQ},
      new OptionForValues(ValueOption.EXCLUDE, MathOperator.GREATER_EQ, 800));

  public static final OptionalModuleParameter<ElementalHeuristicParameters> elementalRatios = new OptionalModuleParameter<>(
      "Element count heuristics",
      "Restrict formulas by heuristic restrictions of elemental counts and ratios",
      new ElementalHeuristicParameters(), true);

  public static final OptionalModuleParameter<RDBERestrictionParameters> rdbeRestrictions = new OptionalModuleParameter<>(
      "RDBE restrictions",
      "Search only for formulas which correspond to the given RDBE restrictions",
      new RDBERestrictionParameters(), true);

  public static final OptionalModuleParameter<IsotopePatternScoreParameters> isotopeFilter = new OptionalModuleParameter<>(
      "Isotope pattern filter", "Search only for formulas with a isotope pattern similar",
      new IsotopePatternScoreParameters(), true);

  public static final OptionalModuleParameter<MSMSScoreParameters> msmsFilter = new OptionalModuleParameter<>(
      "MS/MS filter", "Check MS/MS data", new MSMSScoreParameters(), true);

  public FormulaPredictionIonNetworkParameters() {
    this(false);
  }

  public FormulaPredictionIonNetworkParameters(boolean isSub) {
    super(isSub ? //
        new Parameter[]{ppmOffset, sorting, elements, handleHigherMz, elementalRatios,
            rdbeRestrictions, isotopeFilter, msmsFilter}
        : new Parameter[]{PEAK_LISTS, mzTolerance, ppmOffset, sorting, elements, handleHigherMz,
            elementalRatios, rdbeRestrictions, isotopeFilter, msmsFilter});
  }

  @Override
  public @NotNull IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.SUPPORTED;
  }
}
