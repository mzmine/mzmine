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
package io.github.mzmine.modules.dataprocessing.id_formulapredictionfeaturelist;

import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.modules.dataprocessing.id_formula_sort.FormulaSortParameters;
import io.github.mzmine.modules.dataprocessing.id_formulaprediction.restrictions.elements.ElementalHeuristicParameters;
import io.github.mzmine.modules.dataprocessing.id_formulaprediction.restrictions.rdbe.RDBERestrictionParameters;
import io.github.mzmine.modules.tools.isotopepatternscore.IsotopePatternScoreParameters;
import io.github.mzmine.modules.tools.msmsscore.MSMSScoreParameters;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.elements.ElementsCompositionRangeParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.submodules.OptionalModuleParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import org.jetbrains.annotations.NotNull;

public class FormulaPredictionFeatureListParameters extends SimpleParameterSet {

  public static final FeatureListsParameter FEATURE_LISTS = new FeatureListsParameter();

  public static final OptionalModuleParameter<FormulaSortParameters> sorting =
      new OptionalModuleParameter<>("Sorting", "Apply sorting to all resulting lists",
          new FormulaSortParameters(true));

  public static final IntegerParameter charge = new IntegerParameter("Charge", "Charge");

  public static final ComboParameter<IonizationType> ionization =
      new ComboParameter<IonizationType>("Ionization type", "Ionization type",
          IonizationType.values());

  public static final MZToleranceParameter mzTolerance = new MZToleranceParameter();

  public static final IntegerParameter maxBestFormulasPerFeature =
      new IntegerParameter("Max best formulas per feature",
          "Enter the number of the maximum number of added formulas per feature");

  public static final ElementsCompositionRangeParameter elements =
      new ElementsCompositionRangeParameter("Elements", "Elements and ranges");

  public static final OptionalModuleParameter elementalRatios =
      new OptionalModuleParameter("Element count heuristics",
          "Restrict formulas by heuristic restrictions of elemental counts and ratios",
          new ElementalHeuristicParameters());

  public static final OptionalModuleParameter rdbeRestrictions =
      new OptionalModuleParameter("RDBE restrictions",
          "Search only for formulas which correspond to the given RDBE restrictions",
          new RDBERestrictionParameters());

  public static final OptionalModuleParameter isotopeFilter = new OptionalModuleParameter(
      "Isotope pattern filter", "Search only for formulas with a isotope pattern similar",
      new IsotopePatternScoreParameters());

  public static final OptionalModuleParameter msmsFilter =
      new OptionalModuleParameter("MS/MS filter", "Check MS/MS data", new MSMSScoreParameters());

  public FormulaPredictionFeatureListParameters() {
    super(new Parameter[] {charge, ionization, FEATURE_LISTS, sorting, mzTolerance,
        maxBestFormulasPerFeature, elements, elementalRatios, rdbeRestrictions, isotopeFilter,
        msmsFilter});
  }

  @Override
  public @NotNull IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.SUPPORTED;
  }
}
