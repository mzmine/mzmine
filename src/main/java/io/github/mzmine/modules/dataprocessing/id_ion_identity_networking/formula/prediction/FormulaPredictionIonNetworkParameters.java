/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */
package io.github.mzmine.modules.dataprocessing.id_ion_identity_networking.formula.prediction;


import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.id_formula_sort.FormulaSortParameters;
import io.github.mzmine.modules.dataprocessing.id_formulaprediction.restrictions.elements.ElementalHeuristicParameters;
import io.github.mzmine.modules.dataprocessing.id_formulaprediction.restrictions.rdbe.RDBERestrictionParameters;
import io.github.mzmine.modules.tools.isotopepatternscore.IsotopePatternScoreParameters;
import io.github.mzmine.modules.tools.msmsscore.MSMSScoreParameters;
import io.github.mzmine.parameters.Parameter;
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

public class FormulaPredictionIonNetworkParameters extends SimpleParameterSet {

  public static final FeatureListsParameter PEAK_LISTS = new FeatureListsParameter();

  public static final MZToleranceParameter mzTolerance = new MZToleranceParameter();

  public static final DoubleParameter ppmOffset = new DoubleParameter("Center by ppm offset",
      "Linear correction to mass difference offset. If all correct results are shifted by +4 ppm use -4 ppm to shift these molecular formulae to the center");

  public static final OptionalModuleParameter<FormulaSortParameters> sorting = new OptionalModuleParameter<FormulaSortParameters>(
      "Sorting", "Apply sorting to all resulting lists", new FormulaSortParameters(true));

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

  public static final OptionalModuleParameter elementalRatios = new OptionalModuleParameter(
      "Element count heuristics",
      "Restrict formulas by heuristic restrictions of elemental counts and ratios",
      new ElementalHeuristicParameters());

  public static final OptionalModuleParameter rdbeRestrictions = new OptionalModuleParameter(
      "RDBE restrictions",
      "Search only for formulas which correspond to the given RDBE restrictions",
      new RDBERestrictionParameters());

  public static final OptionalModuleParameter isotopeFilter = new OptionalModuleParameter(
      "Isotope pattern filter", "Search only for formulas with a isotope pattern similar",
      new IsotopePatternScoreParameters());

  public static final OptionalModuleParameter msmsFilter = new OptionalModuleParameter(
      "MS/MS filter", "Check MS/MS data", new MSMSScoreParameters());

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
}
