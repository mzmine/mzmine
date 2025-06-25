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

  public static final OptionalModuleParameter<FormulaSortParameters> sorting = new OptionalModuleParameter<>(
      "Sorting", "Apply sorting to all resulting lists", new FormulaSortParameters(true), true);

  public static final ComboParameter<IonizationType> ionization = new ComboParameter<>(
      "Ionization type", "Ionization type", IonizationType.values(),
      IonizationType.POSITIVE_HYDROGEN);

  public static final MZToleranceParameter mzTolerance = new MZToleranceParameter(0.002, 5);

  public static final IntegerParameter maxBestFormulasPerFeature = new IntegerParameter(
      "Max best formulas per feature",
      "Enter the number of the maximum number of added formulas per feature", 5);

  public static final ElementsCompositionRangeParameter elements = new ElementsCompositionRangeParameter(
      "Elements", "Elements and ranges");

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

  public FormulaPredictionFeatureListParameters() {
    super(
        new Parameter[]{FEATURE_LISTS, ionization, sorting, mzTolerance, maxBestFormulasPerFeature,
            elements, elementalRatios, rdbeRestrictions, isotopeFilter, msmsFilter},
        "https://mzmine.github.io/mzmine_documentation/module_docs/id_spectra_chem_formula/chem-formula-pred.html");
  }

  @Override
  public @NotNull IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.SUPPORTED;
  }
}
