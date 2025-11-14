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
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.modules.dataprocessing.id_formula_sort.FormulaSortParameters;
import io.github.mzmine.modules.dataprocessing.id_formulaprediction.restrictions.elements.ElementalHeuristicParameters;
import io.github.mzmine.modules.dataprocessing.id_formulaprediction.restrictions.rdbe.RDBERestrictionParameters;
import io.github.mzmine.modules.tools.isotopepatternscore.IsotopePatternScoreParameters;
import io.github.mzmine.modules.tools.msmsscore.MSMSScoreParameters;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.elements.ElementsCompositionRangeParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelection;
import io.github.mzmine.parameters.parametertypes.submodules.OptionalModuleParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.formula.MolecularFormulaRange;

public class FormulaPredictionFeatureListParameters extends SimpleParameterSet {

  public static final FeatureListsParameter FEATURE_LISTS = new FeatureListsParameter();

  public static final OptionalModuleParameter<FormulaSortParameters> sorting = new OptionalModuleParameter<>(
      "Sorting", "Apply sorting to all resulting lists", new FormulaSortParameters(true), true);

  public static final ComboParameter<IonizationType> ionization = new ComboParameter<>(
      "Fallback adduct",
      "Ionization type of features that are not annotated by IIN or other annotations.",
      IonizationType.values(), IonizationType.POSITIVE_HYDROGEN);

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

  public static final DoubleParameter highMassLimit = new DoubleParameter(
      "Exclude features above m/z",
      "Exclude masses above the given m/z ratio as computation for high m/zs will take more time.",
      ConfigService.getGuiFormats().mzFormat(), 800d, 0d, Double.MAX_VALUE);

  public FormulaPredictionFeatureListParameters() {
    super(
        new Parameter[]{FEATURE_LISTS, ionization, sorting, mzTolerance, maxBestFormulasPerFeature,
            elements, elementalRatios, rdbeRestrictions, isotopeFilter, msmsFilter, highMassLimit},
        "https://mzmine.github.io/mzmine_documentation/module_docs/id_spectra_chem_formula/chem-formula-pred.html");
  }

  @Override
  public @NotNull IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.SUPPORTED;
  }

  @Override
  public int getVersion() {
    return 2;
  }

  @Override
  public @Nullable String getVersionMessage(int version) {
    return switch (version) {
      case 2 ->
          "Formula prediction for feature lists was updated and parallelized. A filter to exclude high m/z ratios was added for performance tuning. Adduct annotations by IIN now override the 'Ionization type'. Parameter name was changed to 'Fallback adduct'";
      default -> null;
    };
  }

  @Override
  public Map<String, Parameter<?>> getNameParameterMap() {
    var map = super.getNameParameterMap();
    map.put("Ionization type", getParameter(ionization));
    return map;
  }

  ;

  public static FormulaPredictionFeatureListParameters create(@NotNull FeatureListsSelection flists,
      boolean sortingEnabled, @NotNull FormulaSortParameters sortingParameters,
      @NotNull IonizationType fallbackIon, @NotNull MZTolerance mzTol, Integer maxFormuals,
      @NotNull MolecularFormulaRange allowedElements, boolean ratioCheck,
      @NotNull ElementalHeuristicParameters ratioCheckParam, boolean rdbeCheck,
      @NotNull RDBERestrictionParameters rdbeParam, boolean isotopeCheck,
      @NotNull IsotopePatternScoreParameters isotopeParam, boolean msmsCheck,
      @NotNull MSMSScoreParameters msmsParam) {
    final ParameterSet param = new FormulaPredictionFeatureListParameters().cloneParameterSet();

    param.setParameter(FEATURE_LISTS, flists);
    param.setParameter(sorting, sortingEnabled);
    param.getParameter(sorting).setEmbeddedParameters(sortingParameters);
    param.setParameter(ionization, fallbackIon);
    param.setParameter(mzTolerance, mzTol);
    param.setParameter(maxBestFormulasPerFeature, maxFormuals);
    param.setParameter(elements, allowedElements);
    param.setParameter(elementalRatios, ratioCheck);
    param.getParameter(elementalRatios).setEmbeddedParameters(ratioCheckParam);
    param.setParameter(rdbeRestrictions, rdbeCheck);
    param.getParameter(rdbeRestrictions).setEmbeddedParameters(rdbeParam);
    param.setParameter(isotopeFilter, isotopeCheck);
    param.getParameter(isotopeFilter).setEmbeddedParameters(isotopeParam);
    param.setParameter(msmsFilter, msmsCheck);
    param.getParameter(msmsFilter).setEmbeddedParameters(msmsParam);

    return (FormulaPredictionFeatureListParameters) param;
  }
}
