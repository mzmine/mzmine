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

package io.github.mzmine.modules.dataprocessing.id_untargetedLabeling;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.metadata.MetadataGroupingParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.RTToleranceParameter;
import java.text.NumberFormat;

/**
 * Parameters for the untargeted isotope labeling analysis module.
 */
public class UntargetedLabelingParameters extends SimpleParameterSet {

  // ---- INPUT DATA PARAMETERS ----
  /**
   * Feature lists containing both labeled and unlabeled samples
   */
  public static final FeatureListsParameter featureLists = new FeatureListsParameter(
      "Feature lists", "Aligned feature lists containing both labeled and unlabeled samples", 1,
      Integer.MAX_VALUE, false);

  /**
   * Metadata column for sample grouping
   */
  public static final MetadataGroupingParameter metadataGrouping = new MetadataGroupingParameter(
      "Sample grouping",
      "Select metadata column to distinguish between labeled and unlabeled samples");

  /**
   * Value identifying unlabeled samples in metadata
   */
  public static final StringParameter unlabeledGroupValue = new StringParameter(
      "Unlabeled group value",
      "Value in the selected metadata column that identifies unlabeled samples", "unlabeled");

  /**
   * Value identifying labeled samples in metadata
   */
  public static final StringParameter labeledGroupValue = new StringParameter("Labeled group value",
      "Value in the selected metadata column that identifies labeled samples", "labeled");

  /**
   * Name for the result feature list
   */
  public static final StringParameter suffix = new StringParameter("Suffix",
      "Suffix to add to feature list name", "labeled");

  // ---- TRACER PARAMETERS ----
  /**
   * Isotope tracer identifier (e.g., "13C", "15N", "34S")
   */
  public static final StringParameter tracerType = new StringParameter("Tracer isotope",
      "Isotope used for labeling (e.g., \"13C\", \"15N\", \"34S\")", "13C");

  /**
   * Maximum number of isotopologues to search for
   */
  public static final IntegerParameter maximumIsotopologues = new IntegerParameter(
      "Maximum isotopologues", "Maximum number of isotopologues to search for", 10, 2, 100);

  /**
   * Minimum number of peaks required in an isotope pattern
   */
  public static final IntegerParameter minimumIsotopePatternSize = new IntegerParameter(
      "Minimum isotope pattern size", "Minimum number of peaks required in an isotope pattern", 2,
      2, 50);

  // ---- SEARCH TOLERANCE PARAMETERS ----
  /**
   * Retention time tolerance for matching isotopologues
   */
  public static final RTToleranceParameter rtTolerance = new RTToleranceParameter();

  /**
   * m/z tolerance for matching isotopologues
   */
  public static final MZToleranceParameter mzTolerance = new MZToleranceParameter();

  /**
   * Intensity threshold below which a peak is considered noise
   */
  public static final DoubleParameter noiseLevel = new DoubleParameter("Noise level",
      "Intensity threshold below which a peak is considered noise",
      NumberFormat.getNumberInstance());

  // ---- ANALYSIS PARAMETERS ----
  /**
   * Type of feature intensity to use
   */
  public static final ComboParameter<String> intensityMeasure = new ComboParameter<>(
      "Intensity measure", "Type of feature intensity to use",
      new String[]{"Height", "Area", "Area (including background)"}, "Area");

  /**
   * p-value threshold for calling significance
   */
  public static final DoubleParameter pValueCutoff = new DoubleParameter("p-value cutoff",
      "p-value threshold for calling significance of label enrichment",
      NumberFormat.getNumberInstance(), 0.05);

  /**
   * Whether single replicates exist for unlabeled and labeled samples
   */
  public static final BooleanParameter singleSample = new BooleanParameter("Single sample",
      "Set to true if only single replicates exist for unlabeled and labeled samples", false);

  // ---- PATTERN VALIDATION PARAMETERS ----
  /**
   * Tolerance parameter for enforcing monotonicity in unlabeled samples
   */
  public static final DoubleParameter monotonicityTolerance = new DoubleParameter(
      "Monotonicity tolerance",
      "Tolerance parameter for enforcing monotonic decrease from M0 to Mn in unlabeled samples (0=strict)",
      NumberFormat.getNumberInstance(), 0.1, 0.0, 1.0);

  /**
   * Tolerance parameter for enforcing enrichment in labeled samples
   */
  public static final DoubleParameter enrichmentTolerance = new DoubleParameter(
      "Enrichment tolerance",
      "Tolerance parameter for enforcing enrichment of higher isotopologues in labeled samples (0=strict)",
      NumberFormat.getNumberInstance(), 0.1, 0.0, 1.0);

  /**
   * Whether to allow incomplete isotope patterns (e.g., patterns without M+0)
   */
  public static final BooleanParameter allowIncompletePatterns = new BooleanParameter(
      "Allow incomplete patterns",
      "Allow detection of incomplete isotope patterns (e.g., M+6 without M+0 for glucose)", true);

  // ---- RESULT ANNOTATION TYPES ----
  /**
   * Data type for isotope cluster ID
   */
  public static final IsotopeClusterType isotopeClusterType = new IsotopeClusterType();

  /**
   * Data type for isotopologue rank
   */
  public static final IsotopologueRankType isotopologueRankType = new IsotopologueRankType();

  /**
   * Constructor for UntargetedLabelingParameters
   */
  public UntargetedLabelingParameters() {
    super(new Parameter[]{
        // Input data parameters
        featureLists, metadataGrouping, unlabeledGroupValue, labeledGroupValue, suffix,

        // Tracer parameters
        tracerType, maximumIsotopologues, minimumIsotopePatternSize,

        // Search tolerance parameters
        rtTolerance, mzTolerance, noiseLevel,

        // Analysis parameters
        intensityMeasure, pValueCutoff, singleSample,

        // Pattern validation parameters
        monotonicityTolerance, enrichmentTolerance, allowIncompletePatterns});
  }
}