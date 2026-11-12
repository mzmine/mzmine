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

package io.github.mzmine.modules.dataprocessing.filter_duplicatefilter_gc_ei;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.OriginalFeatureListHandlingParameter;
import io.github.mzmine.parameters.parametertypes.OriginalFeatureListHandlingParameter.OriginalFeatureListOption;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelection;
import io.github.mzmine.parameters.parametertypes.submodules.ModuleOptionsEnumComboParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance.Unit;
import io.github.mzmine.parameters.parametertypes.tolerances.RTToleranceParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.ToleranceType;
import io.github.mzmine.util.scans.similarity.HandleUnmatchedSignalOptions;
import io.github.mzmine.util.scans.similarity.SpectralSimilarityFunctions;
import io.github.mzmine.util.scans.similarity.Weights;
import io.github.mzmine.util.scans.similarity.impl.cosine.WeightedCosineSpectralSimilarityParameters;
import org.jetbrains.annotations.NotNull;

public class GcEiDuplicateFilterParameters extends SimpleParameterSet {

  public static final FeatureListsParameter FEATURE_LISTS = new FeatureListsParameter();

  public static final RTToleranceParameter RT_TOLERANCE = new RTToleranceParameter(
      "Retention time tolerance", """
      Maximum retention time difference between duplicate rows.
      After gap filling, duplicates of the same compound have almost the same retention time. \
      Usually the same tolerance as used for the GC-EI spectral deconvolution.""",
      new RTTolerance(0.04f, Unit.MINUTES));

  public static final MZToleranceParameter MZ_TOLERANCE = new MZToleranceParameter(
      ToleranceType.SCAN_TO_SCAN, """
      Used to merge and compare the pseudo spectra, to check the quantifier m/z values, and to \
      re-extract merged features.""", 0.002, 10);

  public static final ComboParameter<GcEiDuplicateMzCheck> MZ_CHECK = new ComboParameter<>(
      "Quantifier m/z check", """
      Defines how the quantifier m/z values (the m/z of the rows) are compared:
      Quantifier in both spectra: the quantifier m/z of each row is part of the pseudo spectrum of the other row.
      Same quantifier m/z: both rows have the same quantifier m/z.
      No m/z check: only retention time and spectral similarity are compared.""",
      GcEiDuplicateMzCheck.values(), GcEiDuplicateMzCheck.QUANTIFIER_IN_BOTH_SPECTRA);

  public static final double DEFAULT_MIN_COSINE = 0.7;

  public static final ModuleOptionsEnumComboParameter<SpectralSimilarityFunctions> SIMILARITY_FUNCTION = new ModuleOptionsEnumComboParameter<>(
      "Spectral similarity",
      "Algorithm and minimum score to compare the merged pseudo spectra of two rows.",
      SpectralSimilarityFunctions.WEIGHTED_COSINE);

  static {
    // GC-EI defaults like in the processing wizard
    setGcEiSimilarityDefaults(
        SIMILARITY_FUNCTION.getEmbeddedParameters(SpectralSimilarityFunctions.WEIGHTED_COSINE),
        DEFAULT_MIN_COSINE);
  }

  public static final ComboParameter<GcEiDuplicateHandling> HANDLING = new ComboParameter<>(
      "Duplicate handling", """
      The best row of a group of duplicates is kept (most detected features). Its duplicates are removed.
      Merge into best row: features of the duplicates replace missing or estimated (gap-filled) features of the best row. \
      They are re-extracted at the quantifier m/z of the best row.
      Remove duplicates: no features are transferred.""", GcEiDuplicateHandling.values(),
      GcEiDuplicateHandling.MERGE);

  public static final StringParameter SUFFIX = new StringParameter("Name suffix",
      "Suffix to be added to feature list name", "dup");

  public static final OriginalFeatureListHandlingParameter HANDLE_ORIGINAL = new OriginalFeatureListHandlingParameter(
      true);

  public GcEiDuplicateFilterParameters() {
    super(new Parameter[]{FEATURE_LISTS, RT_TOLERANCE, MZ_TOLERANCE, MZ_CHECK, SIMILARITY_FUNCTION,
            HANDLING, SUFFIX, HANDLE_ORIGINAL},
        "https://mzmine.github.io/mzmine_documentation/module_docs/filter_duplicate_features_gc_ei/gc-ei-duplicate-filter.html");
  }

  /**
   * Creates a parameter set for programmatic use, e.g., in the processing wizard. Uses the weighted
   * cosine similarity with NIST (GC) weights.
   */
  public static @NotNull GcEiDuplicateFilterParameters create(
      @NotNull final FeatureListsSelection flists, @NotNull final RTTolerance rtTolerance,
      @NotNull final MZTolerance mzTolerance, @NotNull final GcEiDuplicateMzCheck mzCheck,
      final double minCosine, @NotNull final GcEiDuplicateHandling handling,
      @NotNull final String suffix, @NotNull final OriginalFeatureListOption handleOriginal) {
    final GcEiDuplicateFilterParameters params = (GcEiDuplicateFilterParameters) new GcEiDuplicateFilterParameters().cloneParameterSet();
    params.setParameter(FEATURE_LISTS, flists);
    params.setParameter(RT_TOLERANCE, rtTolerance);
    params.setParameter(MZ_TOLERANCE, mzTolerance);
    params.setParameter(MZ_CHECK, mzCheck);
    params.setParameter(HANDLING, handling);
    params.setParameter(SUFFIX, suffix);
    params.setParameter(HANDLE_ORIGINAL, handleOriginal);

    setGcEiSimilarityDefaults(params.getParameter(SIMILARITY_FUNCTION)
        .setOptionGetParameters(SpectralSimilarityFunctions.WEIGHTED_COSINE), minCosine);
    return params;
  }

  /**
   * decision: weighted cosine with NIST (GC) weights gave better duplicate detection than the
   * composite cosine
   */
  private static void setGcEiSimilarityDefaults(@NotNull final ParameterSet weightedCosineParams,
      final double minCosine) {
    weightedCosineParams.setParameter(WeightedCosineSpectralSimilarityParameters.weight,
        Weights.NIST_GC);
    weightedCosineParams.setParameter(WeightedCosineSpectralSimilarityParameters.minCosine,
        minCosine);
    weightedCosineParams.setParameter(WeightedCosineSpectralSimilarityParameters.handleUnmatched,
        HandleUnmatchedSignalOptions.KEEP_ALL_AND_MATCH_TO_ZERO);
  }
}
