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

package io.github.mzmine.modules.dataprocessing.id_lipidid.annotation_modules;

import io.github.mzmine.modules.dataprocessing.id_lipidid.scoring.LipidQcScoringUtils;
import io.github.mzmine.modules.dataprocessing.id_lipidid.scoring.LipidQcScoringUtils.ComponentWeights;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.WeightSliderParameter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Optional override weights for the lipid overall quality scoring model.
 * <p>
 * All weights are constrained to {@code [0, 100]}.
 */
public class LipidQcWeightParameters extends SimpleParameterSet {

  private static final @NotNull ComponentWeights DEFAULT_WEIGHTS = LipidQcScoringUtils.defaultComponentWeights(
      LipidAnalysisType.LC_REVERSED_PHASE);

  public static final WeightSliderParameter ms1Weight = new WeightSliderParameter(
      "MS1 accuracy weight", "Weight for the MS1 mass-accuracy score.",
      toIntWeight(DEFAULT_WEIGHTS.ms1Weight()), LipidQcScoringUtils.MIN_SUPPORTED_COMPONENT_WEIGHT,
      LipidQcScoringUtils.MAX_SUPPORTED_COMPONENT_WEIGHT);

  public static final WeightSliderParameter ms2Weight = new WeightSliderParameter(
      "MS2 evidence weight", "Weight for the MS2 explained-intensity score.",
      toIntWeight(DEFAULT_WEIGHTS.ms2Weight()), LipidQcScoringUtils.MIN_SUPPORTED_COMPONENT_WEIGHT,
      LipidQcScoringUtils.MAX_SUPPORTED_COMPONENT_WEIGHT);

  public static final WeightSliderParameter adductMatchWeight = new WeightSliderParameter(
      "Adduct weight (match)", "Weight when feature ion identity adduct and lipid adduct agree.",
      toIntWeight(DEFAULT_WEIGHTS.adductMatchWeight()),
      LipidQcScoringUtils.MIN_SUPPORTED_COMPONENT_WEIGHT,
      LipidQcScoringUtils.MAX_SUPPORTED_COMPONENT_WEIGHT);

  public static final WeightSliderParameter adductNoIonIdentityWeight = new WeightSliderParameter(
      "Adduct weight (missing ion identity)",
      "Weight when feature ion identity or lipid adduct is missing.",
      toIntWeight(DEFAULT_WEIGHTS.adductNoIonIdentityWeight()),
      LipidQcScoringUtils.MIN_SUPPORTED_COMPONENT_WEIGHT,
      LipidQcScoringUtils.MAX_SUPPORTED_COMPONENT_WEIGHT);

  public static final WeightSliderParameter adductMismatchWeight = new WeightSliderParameter(
      "Adduct weight (mismatch)",
      "Weight when feature ion identity adduct and lipid adduct do not match.",
      toIntWeight(DEFAULT_WEIGHTS.adductMismatchWeight()),
      LipidQcScoringUtils.MIN_SUPPORTED_COMPONENT_WEIGHT,
      LipidQcScoringUtils.MAX_SUPPORTED_COMPONENT_WEIGHT);

  public static final WeightSliderParameter isotopeWeight = new WeightSliderParameter(
      "Isotope similarity weight", "Weight for isotope pattern similarity score.",
      toIntWeight(DEFAULT_WEIGHTS.isotopeWeight()),
      LipidQcScoringUtils.MIN_SUPPORTED_COMPONENT_WEIGHT,
      LipidQcScoringUtils.MAX_SUPPORTED_COMPONENT_WEIGHT);

  public static final WeightSliderParameter interferenceWeight = new WeightSliderParameter(
      "Interference weight", "Weight for class/adduct interference score.",
      toIntWeight(DEFAULT_WEIGHTS.interferenceWeight()),
      LipidQcScoringUtils.MIN_SUPPORTED_COMPONENT_WEIGHT,
      LipidQcScoringUtils.MAX_SUPPORTED_COMPONENT_WEIGHT);

  public static final WeightSliderParameter elutionOrderNoTrendWeight = new WeightSliderParameter(
      "Elution-order weight (no trend support)",
      "Weight for elution-order score when no trend model/window support is available.",
      toIntWeight(DEFAULT_WEIGHTS.elutionOrderNoTrendWeight()),
      LipidQcScoringUtils.MIN_SUPPORTED_COMPONENT_WEIGHT,
      LipidQcScoringUtils.MAX_SUPPORTED_COMPONENT_WEIGHT);

  public static final WeightSliderParameter elutionOrderWithTrendWeight = new WeightSliderParameter(
      "Elution-order weight (trend support)",
      "Weight for elution-order score when trend model/window support is available.",
      toIntWeight(DEFAULT_WEIGHTS.elutionOrderWithTrendWeight()),
      LipidQcScoringUtils.MIN_SUPPORTED_COMPONENT_WEIGHT,
      LipidQcScoringUtils.MAX_SUPPORTED_COMPONENT_WEIGHT);

  public LipidQcWeightParameters() {
    super(ms1Weight, ms2Weight, adductMatchWeight, adductNoIonIdentityWeight, adductMismatchWeight,
        isotopeWeight, interferenceWeight, elutionOrderNoTrendWeight, elutionOrderWithTrendWeight);
  }

  public @NotNull ComponentWeights toComponentWeights(
      final @Nullable LipidAnalysisType analysisType) {
    return toComponentWeights(this, analysisType);
  }

  public static @NotNull ComponentWeights toComponentWeights(
      final @NotNull LipidQcWeightParameters parameters,
      final @Nullable LipidAnalysisType analysisType) {
    final @NotNull ComponentWeights defaults = LipidQcScoringUtils.defaultComponentWeights(
        analysisType);
    return new ComponentWeights(resolveWeight(parameters, ms1Weight, defaults.ms1Weight()),
        resolveWeight(parameters, adductMatchWeight, defaults.adductMatchWeight()),
        resolveWeight(parameters, adductNoIonIdentityWeight, defaults.adductNoIonIdentityWeight()),
        resolveWeight(parameters, adductMismatchWeight, defaults.adductMismatchWeight()),
        resolveWeight(parameters, isotopeWeight, defaults.isotopeWeight()),
        resolveWeight(parameters, interferenceWeight, defaults.interferenceWeight()),
        resolveWeight(parameters, ms2Weight, defaults.ms2Weight()),
        resolveWeight(parameters, elutionOrderNoTrendWeight, defaults.elutionOrderNoTrendWeight()),
        resolveWeight(parameters, elutionOrderWithTrendWeight,
            defaults.elutionOrderWithTrendWeight()));
  }

  public static void applyDefaultsForAnalysisType(final @NotNull LipidQcWeightParameters parameters,
      final @Nullable LipidAnalysisType analysisType) {
    final @NotNull ComponentWeights defaults = LipidQcScoringUtils.defaultComponentWeights(
        analysisType);
    parameters.setParameter(ms1Weight, toIntWeight(defaults.ms1Weight()));
    parameters.setParameter(ms2Weight, toIntWeight(defaults.ms2Weight()));
    parameters.setParameter(adductMatchWeight, toIntWeight(defaults.adductMatchWeight()));
    parameters.setParameter(adductNoIonIdentityWeight,
        toIntWeight(defaults.adductNoIonIdentityWeight()));
    parameters.setParameter(adductMismatchWeight, toIntWeight(defaults.adductMismatchWeight()));
    parameters.setParameter(isotopeWeight, toIntWeight(defaults.isotopeWeight()));
    parameters.setParameter(interferenceWeight, toIntWeight(defaults.interferenceWeight()));
    parameters.setParameter(elutionOrderNoTrendWeight,
        toIntWeight(defaults.elutionOrderNoTrendWeight()));
    parameters.setParameter(elutionOrderWithTrendWeight,
        toIntWeight(defaults.elutionOrderWithTrendWeight()));
  }

  private static double resolveWeight(final @NotNull LipidQcWeightParameters parameters,
      final @NotNull WeightSliderParameter parameter, final double fallback) {
    final @Nullable Integer value = parameters.getParameter(parameter).getValue();
    return value == null ? fallback : value.doubleValue();
  }

  private static int toIntWeight(final double weight) {
    return (int) Math.round(weight);
  }
}
