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

package io.github.mzmine.modules.dataprocessing.id_lipidid.scoring;

import io.github.mzmine.datamodel.features.FeatureList.FeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.modules.dataprocessing.id_lipidid.annotation_modules.LipidAnalysisType;
import io.github.mzmine.modules.dataprocessing.id_lipidid.annotation_modules.LipidAnnotationModule;
import io.github.mzmine.modules.dataprocessing.id_lipidid.annotation_modules.LipidAnnotationParameters;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.LipidFragmentationRuleRating;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.matched_levels.MatchedLipid;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.ILipidClass;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidFragment;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.ParameterUtils;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.Comparators;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Central scoring model used by lipid QC and lipid-annotation ranking.
 * <p>
 * The final annotation score is a weighted mean of all enabled component scores. Components are
 * normalized to {@code [0, 1]} before weighting. Default component weights:
 * <ul>
 *   <li>MS1 mass accuracy: {@code 5}</li>
 *   <li>Lipid ion vs ion identity: dynamic,
 *   {@code 50} if adducts mismatch, otherwise {@code 10}</li>
 *   <li>Isotope pattern: {@code 2}</li>
 *   <li>Interference: {@code 3}</li>
 *   <li>MS2 explained intensity: {@code 10}</li>
 *   <li>Elution order: dynamic, {@code 30} if a trend model is available, otherwise {@code 10}</li>
 * </ul>
 * <p>
 * Always included:
 * <ul>
 *   <li>MS1 mass accuracy: linear scaling, {@code 0 ppm -> 1.0},
 *   {@code >= configured MS1 tolerance -> 0.0}</li>
 *   <li>Lipid ion vs ion identity adduct agreement: binary ({@code 1.0} if equal, else
 *   {@code 0.0})</li>
 *   <li>Isotope pattern similarity (missing measured or theoretical pattern -> {@code 0.0})</li>
 *   <li>Interference score derived from competing annotations in the same row</li>
 * </ul>
 * Optional:
 * <ul>
 *   <li>MS2 explained intensity score ({@code [0, 1]})</li>
 *   <li>Elution-order score (mode-dependent; disabled for direct infusion/imaging)</li>
 * </ul>
 * Elution-order behavior:
 * <ul>
 *   <li>Reversed-phase LC: combines carbon-trend and DBE-trend linear models</li>
 *   <li>HILIC: class-window model (inside dominant RT window: {@code 1.0}, outside:
 *   {@code 0.0})</li>
 *   <li>Direct infusion / imaging: unavailable ({@code 0.0})</li>
 * </ul>
 */

/**
 * Utility class providing scoring functions for lipid annotation quality control. Computes
 * individual quality metrics (MS1 mass accuracy, MS2 fragmentation, adduct probability, isotope
 * pattern, elution order, isobaric interference) and combines them into a single weighted overall
 * annotation score.
 */
public final class LipidQcScoringUtils {

  public static final int MIN_SUPPORTED_COMPONENT_WEIGHT = 0;
  public static final int MAX_SUPPORTED_COMPONENT_WEIGHT = 100;

  private static final double RT_DELTA_NO_PENALTY_NORMALIZED = 0.006d;
  private static final double RT_DELTA_FULL_PENALTY_NORMALIZED = 0.07d;
  private static final double RT_DELTA_PENALTY_EXPONENT = 1.35d;
  private static final double DEFAULT_MS1_SCORING_PPM_TOLERANCE = 5d;
  private static final double WEIGHT_MS1 = 40d;
  private static final double WEIGHT_ADDUCT_MATCH = 10d;
  private static final double WEIGHT_ADDUCT_NO_ION_IDENTITY = 10d;
  private static final double WEIGHT_ADDUCT_MISMATCH = 50d;
  private static final double WEIGHT_ISOTOPE = 20d;
  private static final double WEIGHT_INTERFERENCE = 5d;
  private static final double WEIGHT_MS2 = 100d;
  private static final double WEIGHT_ELUTION_ORDER_NO_TREND = 30d;
  private static final double WEIGHT_ELUTION_ORDER_WITH_TREND = 10d;
  private static final int HILIC_MIN_CLASS_ROWS = 4;
  private static final double HILIC_WINDOW_SUPPORT_FRACTION = 0.6d;
  private static final double HILIC_WINDOW_EDGE_TOLERANCE_MINUTES = 0.02d;
  private static final double HILIC_WINDOW_EDGE_RELATIVE_TOLERANCE = 0.02d;

  private LipidQcScoringUtils() {
  }

  public static boolean hasSufficientEvidence(final @NotNull Collection<LipidFragment> fragments) {
    final long majorCount = fragments.stream().filter(
        fragment -> fragment.getLipidFragmentationRuleRating()
            == LipidFragmentationRuleRating.MAJOR).count();
    if (majorCount > 0) {
      return true;
    }

    final long minorCount = fragments.stream().filter(
        fragment -> fragment.getLipidFragmentationRuleRating()
            == LipidFragmentationRuleRating.MINOR).count();
    return minorCount >= 2;
  }

  /**
   * RT-trend score payload for one trend model.
   *
   * @param score           normalized trend score in {@code [0, 1]}
   * @param residualRt      absolute RT residual in minutes
   * @param normalizedDelta residual normalized by method length
   * @param available       true if sufficient data was available to fit the model
   */
  public record TrendScore(double score, double residualRt, double normalizedDelta,
                           boolean available) {

  }

  /**
   * Interference diagnostics for one row.
   *
   * @param classPenaltyCount           penalties from competing lipid classes in the same row
   * @param sameClassAdductPenaltyCount penalties from same-class, multi-adduct ambiguity
   */
  public record InterferenceMetrics(int classPenaltyCount, int sameClassAdductPenaltyCount) {

    public int totalPenaltyCount() {
      return Math.max(0, classPenaltyCount) + Math.max(0, sameClassAdductPenaltyCount);
    }
  }

  public record ElutionOrderMetrics(double combinedScore, @NotNull TrendScore carbonsTrend,
                                    @NotNull TrendScore dbeTrend, @Nullable String detailOverride) {

  }

  /**
   * Component weights used by overall lipid annotation quality scoring.
   * <p>
   * Values are clamped to {@code [0, 100]}.
   */
  public record ComponentWeights(double ms1Weight, double adductMatchWeight,
                                 double adductNoIonIdentityWeight, double adductMismatchWeight,
                                 double isotopeWeight, double interferenceWeight, double ms2Weight,
                                 double elutionOrderNoTrendWeight,
                                 double elutionOrderWithTrendWeight) {

    public ComponentWeights {
      ms1Weight = clampWeightToSupportedRange(ms1Weight);
      adductMatchWeight = clampWeightToSupportedRange(adductMatchWeight);
      adductNoIonIdentityWeight = clampWeightToSupportedRange(adductNoIonIdentityWeight);
      adductMismatchWeight = clampWeightToSupportedRange(adductMismatchWeight);
      isotopeWeight = clampWeightToSupportedRange(isotopeWeight);
      interferenceWeight = clampWeightToSupportedRange(interferenceWeight);
      ms2Weight = clampWeightToSupportedRange(ms2Weight);
      elutionOrderNoTrendWeight = clampWeightToSupportedRange(elutionOrderNoTrendWeight);
      elutionOrderWithTrendWeight = clampWeightToSupportedRange(elutionOrderWithTrendWeight);
    }
  }

  public static @NotNull ComponentWeights defaultComponentWeights(
      final @Nullable LipidAnalysisType analysisType) {
    final LipidAnalysisType resolvedType =
        analysisType == null ? LipidAnalysisType.LC_REVERSED_PHASE : analysisType;
    return switch (resolvedType) {
      case LC_REVERSED_PHASE, LC_HILIC, DIRECT_INFUSION, IMAGING ->
          new ComponentWeights(WEIGHT_MS1, WEIGHT_ADDUCT_MATCH, WEIGHT_ADDUCT_NO_ION_IDENTITY,
              WEIGHT_ADDUCT_MISMATCH, WEIGHT_ISOTOPE, WEIGHT_INTERFERENCE, WEIGHT_MS2,
              WEIGHT_ELUTION_ORDER_NO_TREND, WEIGHT_ELUTION_ORDER_WITH_TREND);
    };
  }

  /**
   * Computes elution-order metrics for the feature list's detected lipid-analysis mode.
   *
   * @param featureList feature list that provides RT context and mode provenance
   * @param row         selected row
   * @param match       lipid annotation candidate for the row
   * @return mode-specific elution metrics and combined score
   */
  public static @NotNull ElutionOrderMetrics computeElutionOrderMetrics(
      final @NotNull ModularFeatureList featureList, final @NotNull FeatureListRow row,
      final @NotNull MatchedLipid match) {
    return computeElutionOrderMetrics(featureList, row, match,
        detectLipidAnalysisType(featureList));
  }

  /**
   * Computes elution-order metrics for an explicit lipid-analysis mode.
   *
   * @param featureList  feature list that provides RT context
   * @param row          selected row
   * @param match        lipid annotation candidate for the row
   * @param analysisType lipid-analysis mode; {@code null} falls back to reversed-phase LC
   * @return mode-specific elution metrics and combined score
   */
  public static @NotNull ElutionOrderMetrics computeElutionOrderMetrics(
      final @NotNull ModularFeatureList featureList, final @NotNull FeatureListRow row,
      final @NotNull MatchedLipid match, final @Nullable LipidAnalysisType analysisType) {
    final LipidAnalysisType resolvedAnalysisType = Objects.requireNonNullElse(analysisType,
        LipidAnalysisType.LC_REVERSED_PHASE);
    return switch (resolvedAnalysisType) {
      case LC_HILIC -> computeHilicElutionOrderMetrics(featureList, row, match);
      case LC_REVERSED_PHASE -> computeRpElutionOrderMetrics(featureList, row, match);
      case DIRECT_INFUSION, IMAGING ->
          createUnavailableElutionMetrics("Disabled for current lipid analysis mode");
    };
  }

  /**
   * Detects lipid-analysis mode from the latest applied {@link LipidAnnotationModule} step.
   *
   * @param featureList feature list that contains applied methods
   * @return detected analysis mode or {@code null} if not available
   */
  public static @Nullable LipidAnalysisType detectLipidAnalysisType(
      final @NotNull ModularFeatureList featureList) {
    final List<FeatureListAppliedMethod> appliedMethods = featureList.getAppliedMethods();
    for (int i = appliedMethods.size() - 1; i >= 0; i--) {
      final FeatureListAppliedMethod appliedMethod = appliedMethods.get(i);
      if (!(appliedMethod.getModule() instanceof LipidAnnotationModule)) {
        continue;
      }
      try {
        return appliedMethod.getParameters()
            .getParameter(LipidAnnotationParameters.lipidAnalysisType).getValue();
      } catch (RuntimeException ignored) {
        return null;
      }
    }
    return null;
  }

  /**
   * Detects MS1 matching tolerance from the latest applied {@link LipidAnnotationModule} step.
   *
   * @param featureList feature list that contains applied methods
   * @return detected tolerance or {@code null} if unavailable
   */
  public static @Nullable MZTolerance detectMs1Tolerance(
      final @NotNull ModularFeatureList featureList) {
    final List<FeatureListAppliedMethod> appliedMethods = featureList.getAppliedMethods();
    for (int i = appliedMethods.size() - 1; i >= 0; i--) {
      final FeatureListAppliedMethod appliedMethod = appliedMethods.get(i);
      if (!(appliedMethod.getModule() instanceof LipidAnnotationModule)) {
        continue;
      }
      try {
        return appliedMethod.getParameters().getParameter(LipidAnnotationParameters.mzTolerance)
            .getValue();
      } catch (RuntimeException ignored) {
        return null;
      }
    }
    return null;
  }

  private static @NotNull ElutionOrderMetrics computeRpElutionOrderMetrics(
      final @NotNull ModularFeatureList featureList, final @NotNull FeatureListRow row,
      final @NotNull MatchedLipid match) {
    if (row.getAverageRT() == null) {
      return createUnavailableElutionMetrics("Missing RT context");
    }
    final int selectedCarbons = match.getLipidAnnotation().getChainsCarbonCount();
    final int selectedDbe = match.getLipidAnnotation().getChainsDoubleBondCount();
    if (selectedCarbons < 0 || selectedDbe < 0) {
      return createUnavailableElutionMetrics("Missing lipid chain information");
    }

    final ILipidClass selectedClass = match.getLipidAnnotation().getLipidClass();
    final double observedRt = row.getAverageRT();
    final double methodLength = computeRtMethodLength(featureList);

    final List<double[]> carbonTrendPoints = new ArrayList<>();
    final List<double[]> dbeTrendPoints = new ArrayList<>();
    for (final FeatureListRow other : featureList.getRows()) {
      collectTrendPointsFromRow(other, selectedClass, selectedCarbons, selectedDbe,
          carbonTrendPoints, dbeTrendPoints);
    }

    final TrendScore carbonsTrend = computeTrendScore(carbonTrendPoints, selectedCarbons,
        observedRt, methodLength);
    final TrendScore dbeTrend = computeTrendScore(dbeTrendPoints, selectedDbe, observedRt,
        methodLength);
    final double combined = combineTrendScores(carbonsTrend, dbeTrend);
    return new ElutionOrderMetrics(combined, carbonsTrend, dbeTrend, null);
  }

  private static void collectTrendPointsFromRow(final @NotNull FeatureListRow row,
      final @NotNull ILipidClass selectedClass, final int selectedCarbons, final int selectedDbe,
      final @NotNull List<double[]> carbonTrendPoints,
      final @NotNull List<double[]> dbeTrendPoints) {
    final Float rowRt = row.getAverageRT();
    if (rowRt == null || !Float.isFinite(rowRt)) {
      return;
    }

    final List<MatchedLipid> rowMatches = row.getLipidMatches();
    if (rowMatches.isEmpty()) {
      return;
    }

    final Set<Integer> addedCarbons = new HashSet<>();
    final Set<Integer> addedDbes = new HashSet<>();
    final double rtValue = rowRt.doubleValue();
    for (final MatchedLipid rowMatch : rowMatches) {
      if (!rowMatch.getLipidAnnotation().getLipidClass().equals(selectedClass)) {
        continue;
      }
      final int rowCarbons = rowMatch.getLipidAnnotation().getChainsCarbonCount();
      final int rowDbe = rowMatch.getLipidAnnotation().getChainsDoubleBondCount();
      if (rowCarbons < 0 || rowDbe < 0) {
        continue;
      }

      if (rowDbe == selectedDbe && addedCarbons.add(rowCarbons)) {
        carbonTrendPoints.add(new double[]{rowCarbons, rtValue});
      }
      if (rowCarbons == selectedCarbons && addedDbes.add(rowDbe)) {
        dbeTrendPoints.add(new double[]{rowDbe, rtValue});
      }
    }
  }

  private static @NotNull ElutionOrderMetrics computeHilicElutionOrderMetrics(
      final @NotNull ModularFeatureList featureList, final @NotNull FeatureListRow row,
      final @NotNull MatchedLipid match) {
    if (row.getAverageRT() == null) {
      return createUnavailableElutionMetrics("Missing RT context");
    }
    final ILipidClass selectedClass = match.getLipidAnnotation().getLipidClass();
    final double[] classRtValues = collectClassRetentionTimes(featureList, selectedClass);
    if (classRtValues.length < HILIC_MIN_CLASS_ROWS) {
      return createUnavailableElutionMetrics(
          "HILIC class-window model needs at least " + HILIC_MIN_CLASS_ROWS + " class rows");
    }

    final int supportCount = Math.max(3,
        (int) Math.ceil(classRtValues.length * HILIC_WINDOW_SUPPORT_FRACTION));
    final RtWindow dominantWindow = computeNarrowestWindow(classRtValues, supportCount);
    final double observedRt = row.getAverageRT();
    final double windowWidth = dominantWindow.upperRt() - dominantWindow.lowerRt();
    final double edgeTolerance = Math.max(HILIC_WINDOW_EDGE_TOLERANCE_MINUTES,
        windowWidth * HILIC_WINDOW_EDGE_RELATIVE_TOLERANCE);

    final boolean inWindow = observedRt >= dominantWindow.lowerRt() - edgeTolerance
        && observedRt <= dominantWindow.upperRt() + edgeTolerance;

    final double distanceToWindow = inWindow ? 0d
        : Math.min(Math.abs(observedRt - dominantWindow.lowerRt()),
            Math.abs(observedRt - dominantWindow.upperRt()));

    final double normalizedDistance = normalizeRtDeltaByMethodLength(distanceToWindow,
        computeRtMethodLength(featureList));

    final double score = inWindow ? 1d : 0d;
    final TrendScore windowScore = new TrendScore(score, distanceToWindow, normalizedDistance,
        true);

    final String detail = String.format(
        "HILIC class window %.2f-%.2f min (%d/%d rows); selected RT %.2f is %s the window",
        dominantWindow.lowerRt(), dominantWindow.upperRt(), dominantWindow.supportCount(),
        dominantWindow.totalCount(), observedRt, inWindow ? "inside" : "outside");

    return new ElutionOrderMetrics(score, windowScore, windowScore, detail);
  }

  private static @NotNull ElutionOrderMetrics createUnavailableElutionMetrics(
      final @NotNull String reason) {
    final TrendScore missing = new TrendScore(0d, Double.NaN, Double.NaN, false);
    return new ElutionOrderMetrics(0d, missing, missing, reason);
  }

  private static @NotNull double[] collectClassRetentionTimes(
      final @NotNull ModularFeatureList featureList, final @NotNull ILipidClass selectedClass) {
    final List<Double> classRts = new ArrayList<>();
    for (final FeatureListRow candidateRow : featureList.getRows()) {
      final Float candidateRt = candidateRow.getAverageRT();
      if (candidateRt == null || !Float.isFinite(candidateRt)) {
        continue;
      }
      final List<MatchedLipid> rowMatches = candidateRow.getLipidMatches();
      if (rowMatches.isEmpty()) {
        continue;
      }
      final boolean hasMatchingClass = rowMatches.stream().anyMatch(
          candidate -> candidate.getLipidAnnotation().getLipidClass().equals(selectedClass));
      if (hasMatchingClass) {
        classRts.add((double) candidateRt);
      }
    }
    return classRts.stream().mapToDouble(Double::doubleValue).toArray();
  }

  private static @NotNull RtWindow computeNarrowestWindow(final @NotNull double[] rtValues,
      final int requestedSupportCount) {
    final double[] sorted = Arrays.copyOf(rtValues, rtValues.length);
    Arrays.sort(sorted);
    final int supportCount = Math.max(2, Math.min(sorted.length, requestedSupportCount));
    int bestStart = 0;
    double bestWidth = Double.POSITIVE_INFINITY;
    for (int start = 0; start <= sorted.length - supportCount; start++) {
      final int end = start + supportCount - 1;
      final double width = sorted[end] - sorted[start];
      if (width < bestWidth) {
        bestWidth = width;
        bestStart = start;
      }
    }
    final int bestEnd = bestStart + supportCount - 1;
    return new RtWindow(sorted[bestStart], sorted[bestEnd], supportCount, sorted.length);
  }

  private static @NotNull TrendScore computeTrendScore(final @NotNull List<double[]> points,
      final double selectedPredictor, final double observedRt, final double methodLength) {
    if (points.size() < 3) {
      return new TrendScore(0d, Double.NaN, Double.NaN, false);
    }
    final double expectedRt = predictRtByLinearFitGlobal(points, selectedPredictor);
    final double residual = Math.abs(observedRt - expectedRt);
    final double normalizedDelta = normalizeRtDeltaByMethodLength(residual, methodLength);
    final double score = computeEcnRtScoreFromDelta(residual, methodLength);
    return new TrendScore(score, residual, normalizedDelta, true);
  }

  private static double combineTrendScores(final @NotNull TrendScore carbonsTrend,
      final @NotNull TrendScore dbeTrend) {
    if (carbonsTrend.available() && dbeTrend.available()) {
      return clampToUnit((carbonsTrend.score() + dbeTrend.score()) / 2d);
    }
    if (carbonsTrend.available()) {
      return Math.max(0.05d, clampToUnit(carbonsTrend.score() * 0.8d));
    }
    if (dbeTrend.available()) {
      return Math.max(0.05d, clampToUnit(dbeTrend.score() * 0.8d));
    }
    return 0d;
  }

  public static @NotNull String formatElutionOrderDetail(
      final @NotNull ElutionOrderMetrics metrics) {
    if (metrics.detailOverride() != null && !metrics.detailOverride().isBlank()) {
      return metrics.detailOverride();
    }
    return "C trend: " + formatTrendDetail(metrics.carbonsTrend()) + " | DBE trend: "
        + formatTrendDetail(metrics.dbeTrend());
  }

  private static @NotNull String formatTrendDetail(final @NotNull TrendScore trendScore) {
    if (!trendScore.available()) {
      return "n/a";
    }
    if (Double.isFinite(trendScore.normalizedDelta())) {
      return String.format("%.0f%% (Δ=%.2f, %.1f%% method)", trendScore.score() * 100d,
          trendScore.residualRt(), trendScore.normalizedDelta() * 100d);
    }
    return String.format("%.0f%% (Δ=%.2f)", trendScore.score() * 100d, trendScore.residualRt());
  }

  private static double predictRtByLinearFitGlobal(final @NotNull List<double[]> points,
      final double predictorValue) {
    double sumX = 0d;
    double sumY = 0d;
    double sumXY = 0d;
    double sumXX = 0d;
    for (final double[] p : points) {
      sumX += p[0];
      sumY += p[1];
      sumXY += p[0] * p[1];
      sumXX += p[0] * p[0];
    }
    final int n = points.size();
    final double denom = n * sumXX - sumX * sumX;
    if (Math.abs(denom) < 1e-8d) {
      return sumY / n;
    }
    final double slope = (n * sumXY - sumX * sumY) / denom;
    final double intercept = (sumY - slope * sumX) / n;
    return intercept + slope * predictorValue;
  }

  /**
   * Clamps a value to {@code [0, 1]}. Non-finite input is mapped to {@code 0}.
   */
  public static double clampToUnit(final double value) {
    if (!Double.isFinite(value)) {
      return 0d;
    }
    return Math.max(0d, Math.min(1d, value));
  }

  private static double clampWeightToSupportedRange(final double value) {
    if (!Double.isFinite(value)) {
      return 0d;
    }
    return Math.max(MIN_SUPPORTED_COMPONENT_WEIGHT,
        Math.min(MAX_SUPPORTED_COMPONENT_WEIGHT, value));
  }

  private static double normalizeRtDeltaByMethodLength(final double deltaRt,
      final double methodLength) {
    if (!Double.isFinite(deltaRt) || !Double.isFinite(methodLength) || methodLength <= 0d) {
      return Double.NaN;
    }
    return Math.abs(deltaRt) / methodLength;
  }

  private static double computeEcnRtScoreFromDelta(final double deltaRt,
      final double methodLength) {
    final double normalizedDelta = normalizeRtDeltaByMethodLength(deltaRt, methodLength);
    if (!Double.isFinite(normalizedDelta)) {
      return 0.4d;
    }
    if (normalizedDelta <= RT_DELTA_NO_PENALTY_NORMALIZED) {
      return 1d;
    }
    final double penaltyRange = RT_DELTA_FULL_PENALTY_NORMALIZED - RT_DELTA_NO_PENALTY_NORMALIZED;
    if (penaltyRange <= 0d) {
      return clampToUnit(1d - normalizedDelta / RT_DELTA_FULL_PENALTY_NORMALIZED);
    }
    final double scaledPenalty = (normalizedDelta - RT_DELTA_NO_PENALTY_NORMALIZED) / penaltyRange;
    final double nonLinearPenalty = Math.pow(clampToUnit(scaledPenalty), RT_DELTA_PENALTY_EXPONENT);
    return clampToUnit(1d - nonLinearPenalty);
  }

  /**
   * Converts the number of interference penalties into a score.
   * <ul>
   *   <li>{@code 0 penalties -> 1.0}</li>
   *   <li>{@code 1 penalty -> 0.5}</li>
   *   <li>{@code >= 2 penalties -> 0.0}</li>
   * </ul>
   */
  public static double computeInterferenceScore(final int interferenceCount) {
    return switch (Math.max(0, interferenceCount)) {
      case 0 -> 1d;
      case 1 -> 0.5d;
      default -> 0d;
    };
  }

  /**
   * Computes the overall annotation score with default settings: includes MS2 and elution-order
   * contributions, using auto-detected analysis mode.
   */
  public static double computeCombinedAnnotationScore(final @NotNull ModularFeatureList featureList,
      final @NotNull FeatureListRow row, final @NotNull MatchedLipid match) {
    return computeCombinedAnnotationScore(featureList, row, match, true, true,
        detectLipidAnalysisType(featureList));
  }

  /**
   * Computes the overall annotation score with optional MS2 and elution-order components, using
   * auto-detected analysis mode.
   *
   * @param includeMs2Score          true to include MS2 explained-intensity score
   * @param includeElutionOrderScore true to include RT/elution-order score
   */
  public static double computeCombinedAnnotationScore(final @NotNull ModularFeatureList featureList,
      final @NotNull FeatureListRow row, final @NotNull MatchedLipid match,
      final boolean includeMs2Score, final boolean includeElutionOrderScore) {
    return computeCombinedAnnotationScore(featureList, row, match, includeMs2Score,
        includeElutionOrderScore, detectLipidAnalysisType(featureList));
  }

  /**
   * Computes the overall annotation score for a specific analysis mode.
   * <p>
   * Final score = arithmetic mean of all enabled component scores.
   *
   * @param featureList              feature list used for context-dependent scores
   * @param row                      selected row
   * @param match                    annotation candidate
   * @param includeMs2Score          true to include MS2 explained-intensity score
   * @param includeElutionOrderScore true to include elution-order score
   * @param analysisType             explicit analysis mode; {@code null} falls back to
   *                                 auto-detection
   * @return normalized combined score in {@code [0, 1]}
   */
  public static double computeCombinedAnnotationScore(final @NotNull ModularFeatureList featureList,
      final @NotNull FeatureListRow row, final @NotNull MatchedLipid match,
      final boolean includeMs2Score, final boolean includeElutionOrderScore,
      final @Nullable LipidAnalysisType analysisType) {
    return computeCombinedAnnotationScore(featureList, row, match, includeMs2Score,
        includeElutionOrderScore, analysisType, null);
  }

  /**
   * Computes the overall annotation score for a specific analysis mode and optional custom
   * component weights.
   */
  public static double computeCombinedAnnotationScore(final @NotNull ModularFeatureList featureList,
      final @NotNull FeatureListRow row, final @NotNull MatchedLipid match,
      final boolean includeMs2Score, final boolean includeElutionOrderScore,
      final @Nullable LipidAnalysisType analysisType,
      final @Nullable ComponentWeights customComponentWeights) {
    return computeCombinedAnnotationScore(featureList, row, match, includeMs2Score,
        includeElutionOrderScore, analysisType, customComponentWeights, null);
  }

  /**
   * Computes the overall annotation score for a specific analysis mode and optional custom
   * component weights.
   */
  public static double computeCombinedAnnotationScore(final @NotNull ModularFeatureList featureList,
      final @NotNull FeatureListRow row, final @NotNull MatchedLipid match,
      final boolean includeMs2Score, final boolean includeElutionOrderScore,
      final @Nullable LipidAnalysisType analysisType,
      final @Nullable ComponentWeights customComponentWeights,
      final @Nullable MZTolerance ms1Tolerance) {
    final LipidAnalysisType resolvedAnalysisType =
        analysisType != null ? analysisType : detectLipidAnalysisType(featureList);
    final ComponentWeights componentWeights = Objects.requireNonNullElse(customComponentWeights,
        defaultComponentWeights(resolvedAnalysisType));

    final @Nullable MZTolerance resolvedMs1Tolerance =
        ms1Tolerance != null ? ms1Tolerance : detectMs1Tolerance(featureList);
    final double elutionOrderWeight;
    final double elutionOrderScore;
    if (includeElutionOrderScore) {
      final ElutionOrderMetrics elutionMetrics = computeElutionOrderMetrics(featureList, row, match,
          resolvedAnalysisType);
      elutionOrderScore = elutionMetrics.combinedScore();
      elutionOrderWeight = computeElutionOrderWeight(elutionMetrics, componentWeights);
    } else {
      elutionOrderScore = 0d;
      elutionOrderWeight = componentWeights.elutionOrderWithTrendWeight();
    }
    return computeOverallQualityScore(row, match, elutionOrderScore, elutionOrderWeight,
        includeMs2Score, includeElutionOrderScore, componentWeights, resolvedMs1Tolerance);
  }

  /**
   * Computes the weighted overall quality score from normalized component scores.
   * <p>
   * All provided component scores are clamped to {@code [0, 1]} before weighting.
   *
   * @param ms1Score                 MS1 accuracy score
   * @param ms2Score                 MS2 explained-intensity score
   * @param adductScore              adduct agreement score
   * @param isotopeScore             isotope similarity score
   * @param interferenceScore        interference score
   * @param elutionOrderScore        elution-order score
   * @param includeMs2Score          true to include MS2 in the weighted mean
   * @param includeElutionOrderScore true to include elution order in the weighted mean
   * @return weighted mean score in {@code [0, 1]}
   */
  public static double computeWeightedQualityScore(final double ms1Score, final double ms2Score,
      final double adductScore, final double isotopeScore, final double interferenceScore,
      final double elutionOrderScore, final boolean includeMs2Score,
      final boolean includeElutionOrderScore) {
    return computeWeightedQualityScore(ms1Score, ms2Score, adductScore, isotopeScore,
        interferenceScore, elutionOrderScore, includeMs2Score, includeElutionOrderScore,
        WEIGHT_ADDUCT_MATCH, WEIGHT_ELUTION_ORDER_WITH_TREND);
  }

  /**
   * Computes the weighted overall quality score from normalized component scores.
   * <p>
   * All provided component scores are clamped to {@code [0, 1]} before weighting.
   *
   * @param ms1Score                 MS1 accuracy score
   * @param ms2Score                 MS2 explained-intensity score
   * @param adductScore              adduct agreement score
   * @param isotopeScore             isotope similarity score
   * @param interferenceScore        interference score
   * @param elutionOrderScore        elution-order score
   * @param includeMs2Score          true to include MS2 in the weighted mean
   * @param includeElutionOrderScore true to include elution order in the weighted mean
   * @param adductWeight             adduct component weight applied for this evaluation
   * @return weighted mean score in {@code [0, 1]}
   */
  public static double computeWeightedQualityScore(final double ms1Score, final double ms2Score,
      final double adductScore, final double isotopeScore, final double interferenceScore,
      final double elutionOrderScore, final boolean includeMs2Score,
      final boolean includeElutionOrderScore, final double adductWeight) {
    return computeWeightedQualityScore(ms1Score, ms2Score, adductScore, isotopeScore,
        interferenceScore, elutionOrderScore, includeMs2Score, includeElutionOrderScore,
        adductWeight, WEIGHT_ELUTION_ORDER_WITH_TREND);
  }

  /**
   * Computes the weighted overall quality score from normalized component scores.
   * <p>
   * All provided component scores are clamped to {@code [0, 1]} before weighting.
   *
   * @param ms1Score                 MS1 accuracy score
   * @param ms2Score                 MS2 explained-intensity score
   * @param adductScore              adduct agreement score
   * @param isotopeScore             isotope similarity score
   * @param interferenceScore        interference score
   * @param elutionOrderScore        elution-order score
   * @param includeMs2Score          true to include MS2 in the weighted mean
   * @param includeElutionOrderScore true to include elution order in the weighted mean
   * @param adductWeight             adduct component weight applied for this evaluation
   * @param elutionOrderWeight       elution-order component weight applied for this evaluation
   * @return weighted mean score in {@code [0, 1]}
   */
  public static double computeWeightedQualityScore(final double ms1Score, final double ms2Score,
      final double adductScore, final double isotopeScore, final double interferenceScore,
      final double elutionOrderScore, final boolean includeMs2Score,
      final boolean includeElutionOrderScore, final double adductWeight,
      final double elutionOrderWeight) {
    return computeWeightedQualityScore(ms1Score, ms2Score, adductScore, isotopeScore,
        interferenceScore, elutionOrderScore, includeMs2Score, includeElutionOrderScore,
        defaultComponentWeights(LipidAnalysisType.LC_REVERSED_PHASE), adductWeight,
        elutionOrderWeight);
  }

  /**
   * Computes the weighted overall quality score from normalized component scores and explicit
   * component weights.
   */
  public static double computeWeightedQualityScore(final double ms1Score, final double ms2Score,
      final double adductScore, final double isotopeScore, final double interferenceScore,
      final double elutionOrderScore, final boolean includeMs2Score,
      final boolean includeElutionOrderScore, final @NotNull ComponentWeights componentWeights,
      final double adductWeight, final double elutionOrderWeight) {
    final double resolvedMs1Weight = clampWeightToSupportedRange(componentWeights.ms1Weight());
    final double resolvedIsotopeWeight = clampWeightToSupportedRange(
        componentWeights.isotopeWeight());
    final double resolvedInterferenceWeight = clampWeightToSupportedRange(
        componentWeights.interferenceWeight());
    final double resolvedMs2Weight = clampWeightToSupportedRange(componentWeights.ms2Weight());
    final double resolvedAdductWeight = Math.max(0d, adductWeight);
    final double resolvedElutionWeight = Math.max(0d, elutionOrderWeight);

    double weightedSum =
        resolvedMs1Weight * clampToUnit(ms1Score) + resolvedAdductWeight * clampToUnit(adductScore)
            + resolvedIsotopeWeight * clampToUnit(isotopeScore)
            + resolvedInterferenceWeight * clampToUnit(interferenceScore);
    double weightSum = resolvedMs1Weight + resolvedAdductWeight + resolvedIsotopeWeight
        + resolvedInterferenceWeight;

    if (includeMs2Score) {
      weightedSum += resolvedMs2Weight * clampToUnit(ms2Score);
      weightSum += resolvedMs2Weight;
    }
    if (includeElutionOrderScore) {
      weightedSum += resolvedElutionWeight * clampToUnit(elutionOrderScore);
      weightSum += resolvedElutionWeight;
    }
    return weightSum <= 0d ? 0d : clampToUnit(weightedSum / weightSum);
  }

  private static double computeOverallQualityScore(final @NotNull FeatureListRow row,
      final @NotNull MatchedLipid match, final double elutionOrderScore,
      final double elutionOrderWeight, final boolean includeMs2Score,
      final boolean includeElutionOrderScore, final @NotNull ComponentWeights componentWeights,
      final @Nullable MZTolerance ms1Tolerance) {
    final double ms1Score = computeMs1Score(row, match, ms1Tolerance);
    final double ms2Score = computeMs2Score(match);
    final double adductScore = computeAdductScore(row, match);
    final double adductWeight = computeAdductWeight(row, match, componentWeights);
    final double isotopeScore = computeIsotopeScore(row, match);
    final InterferenceMetrics interferenceMetrics = computeInterferenceMetrics(row);
    final double interferenceScore = computeInterferenceScore(
        interferenceMetrics.totalPenaltyCount());
    return computeWeightedQualityScore(ms1Score, ms2Score, adductScore, isotopeScore,
        interferenceScore, elutionOrderScore, includeMs2Score, includeElutionOrderScore,
        componentWeights, adductWeight, elutionOrderWeight);
  }

  public static double computeMs1Score(final @NotNull FeatureListRow row,
      final @NotNull MatchedLipid match) {
    return computeMs1Score(row, match, null);
  }

  public static double computeMs1Score(final @NotNull FeatureListRow row,
      final @NotNull MatchedLipid match, final @Nullable MZTolerance ms1Tolerance) {
    final double exactMz = MatchedLipid.getExactMass(match);
    final double observedMz =
        match.getAccurateMz() != null ? match.getAccurateMz() : row.getAverageMZ();
    final double ppm = (observedMz - exactMz) / exactMz * 1e6;
    final double absPpm = Math.abs(ppm);
    return computeMs1ScoreFromPpm(absPpm, exactMz, ms1Tolerance);
  }

  public static double computeMs1ScoreFromPpm(final double absPpm, final double mzValue,
      final @Nullable MZTolerance ms1Tolerance) {
    if (!Double.isFinite(absPpm)) {
      return 0d;
    }
    final double ppmTolerance = resolveMs1ScoringPpmTolerance(mzValue, ms1Tolerance);
    return clampToUnit(1d - Math.min(absPpm, ppmTolerance) / ppmTolerance);
  }

  public static double resolveMs1ScoringPpmTolerance(final double mzValue,
      final @Nullable MZTolerance ms1Tolerance) {
    if (!Double.isFinite(mzValue) || mzValue <= 0d || ms1Tolerance == null) {
      return DEFAULT_MS1_SCORING_PPM_TOLERANCE;
    }
    final double ppmTolerance = ms1Tolerance.getPpmToleranceForMass(mzValue);
    if (!Double.isFinite(ppmTolerance) || ppmTolerance <= 0d) {
      return DEFAULT_MS1_SCORING_PPM_TOLERANCE;
    }
    return ppmTolerance;
  }

  private static double computeMs2Score(final @NotNull MatchedLipid match) {
    final double explainedIntensity = match.getMsMsScore() == null ? 0d : match.getMsMsScore();
    return clampToUnit(explainedIntensity);
  }

  private static double computeAdductScore(final @NotNull FeatureListRow row,
      final @NotNull MatchedLipid match) {
    if (row.getBestIonIdentity() == null || match.getIonizationType() == null) {
      return 0d;
    }
    final String featureAdduct = normalizeAdduct(row.getBestIonIdentity().toString());
    final String lipidAdduct = normalizeAdduct(match.getIonizationType().getAdductName());
    return featureAdduct.equals(lipidAdduct) ? 1d : 0d;
  }

  /**
   * Dynamic adduct weight used by the overall quality score.
   * <ul>
   *   <li>If ion identity is missing: {@code 10}</li>
   *   <li>If adducts mismatch: {@code 50}</li>
   *   <li>If adducts match: {@code 10}</li>
   * </ul>
   */
  public static double computeAdductWeight(final @NotNull FeatureListRow row,
      final @NotNull MatchedLipid match) {
    return computeAdductWeight(row, match, defaultComponentWeights(null));
  }

  public static double computeAdductWeight(final @NotNull FeatureListRow row,
      final @NotNull MatchedLipid match, final @NotNull ComponentWeights componentWeights) {
    if (row.getBestIonIdentity() == null || match.getIonizationType() == null) {
      return componentWeights.adductNoIonIdentityWeight();
    }
    final String featureAdduct = normalizeAdduct(row.getBestIonIdentity().toString());
    final String lipidAdduct = normalizeAdduct(match.getIonizationType().getAdductName());
    return featureAdduct.equals(lipidAdduct) ? componentWeights.adductMatchWeight()
        : componentWeights.adductMismatchWeight();
  }

  /**
   * Dynamic elution-order weight used by the overall quality score.
   * <ul>
   *   <li>If metrics are missing or no trend model is available: {@code 10}</li>
   *   <li>If at least one trend model is available: {@code 30}</li>
   * </ul>
   */
  public static double computeElutionOrderWeight(final @Nullable ElutionOrderMetrics metrics) {
    return computeElutionOrderWeight(metrics, defaultComponentWeights(null));
  }

  public static double computeElutionOrderWeight(final @Nullable ElutionOrderMetrics metrics,
      final @NotNull ComponentWeights componentWeights) {
    if (metrics == null) {
      return componentWeights.elutionOrderNoTrendWeight();
    }
    final boolean trendAvailable =
        metrics.carbonsTrend().available() || metrics.dbeTrend().available();
    return trendAvailable ? componentWeights.elutionOrderWithTrendWeight()
        : componentWeights.elutionOrderNoTrendWeight();
  }

  private static double computeIsotopeScore(final @NotNull FeatureListRow row,
      final @NotNull MatchedLipid match) {
    if (row.getBestIsotopePattern() == null || match.getIsotopePattern() == null) {
      return 0d;
    }
    return io.github.mzmine.modules.tools.isotopepatternscore.IsotopePatternScoreCalculator.getSimilarityScore(
        row.getBestIsotopePattern(), match.getIsotopePattern(),
        new io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance(0.003, 10d), 0d);
  }

  private static @NotNull String normalizeAdduct(final @Nullable String adduct) {
    return adduct == null ? "" : adduct.replaceAll("\\s+", "").toLowerCase();
  }

  /**
   * Computes interference metrics from all lipid matches in one row.
   * <p>
   * Two independent penalty channels are tracked and added:
   * <ul>
   *   <li>Competing lipid classes in the row:
   *   {@code classPenaltyCount = max(0, uniqueClasses - 1)}</li>
   *   <li>Same-class adduct ambiguity:
   *   for each lipid class in the row, add one penalty if there is more than one unique
   *   annotation and more than one unique non-empty adduct</li>
   * </ul>
   * The resulting total penalty count is mapped to the interference score by
   * {@link #computeInterferenceScore(int)} ({@code 0 -> 1.0}, {@code 1 -> 0.5}, {@code >=2 -> 0.0}).
   */
  public static @NotNull InterferenceMetrics computeInterferenceMetrics(
      final @NotNull FeatureListRow row) {
    final List<MatchedLipid> matches = row.getLipidMatches();
    if (matches.isEmpty()) {
      return new InterferenceMetrics(0, 0);
    }

    final long uniqueClasses = matches.stream()
        .map(m -> m.getLipidAnnotation().getLipidClass().getName()).distinct().count();
    final int classPenaltyCount = (int) Math.max(0L, uniqueClasses - 1L);

    final Map<String, List<MatchedLipid>> byClass = new TreeMap<>();
    for (final MatchedLipid match : matches) {
      final String className = match.getLipidAnnotation().getLipidClass().getName();
      byClass.computeIfAbsent(className, _ -> new ArrayList<>()).add(match);
    }
    int sameClassAdductPenaltyCount = 0;
    for (final List<MatchedLipid> sameClassMatches : byClass.values()) {
      final long uniqueAnnotations = sameClassMatches.stream()
          .map(m -> m.getLipidAnnotation().getAnnotation()).filter(Objects::nonNull).distinct()
          .count();
      final long uniqueAdducts = sameClassMatches.stream().map(m -> normalizeAdductForInterference(
              m.getIonizationType() != null ? m.getIonizationType().getAdductName() : null))
          .filter(adduct -> !adduct.isBlank()).distinct().count();
      if (uniqueAnnotations > 1 && uniqueAdducts > 1) {
        sameClassAdductPenaltyCount++;
      }
    }
    return new InterferenceMetrics(classPenaltyCount, sameClassAdductPenaltyCount);
  }

  public static @NotNull String interferenceDetail(final @NotNull InterferenceMetrics metrics) {
    if (metrics.totalPenaltyCount() == 0) {
      return "No competing lipid classes or adduct-conflicting annotations in selected row.";
    }
    if (metrics.classPenaltyCount() > 0 && metrics.sameClassAdductPenaltyCount() > 0) {
      return "Multiple lipid classes and same-class annotations with different adducts are present.";
    }
    if (metrics.classPenaltyCount() > 0) {
      return metrics.classPenaltyCount() == 1 ? "One competing lipid class present in selected row."
          : "Multiple competing lipid classes present in selected row.";
    }
    return metrics.sameClassAdductPenaltyCount() == 1
        ? "Same lipid class has multiple annotations supported by different adducts."
        : "Multiple lipid classes show annotation ambiguity across different adducts.";
  }

  private static @NotNull String normalizeAdductForInterference(final @Nullable String adduct) {
    return adduct == null ? "" : adduct.replaceAll("\\s+", "").toLowerCase();
  }

  private static double computeRtMethodLength(final @NotNull ModularFeatureList featureList) {
    double minRt = Double.POSITIVE_INFINITY;
    double maxRt = Double.NEGATIVE_INFINITY;
    for (final FeatureListRow row : featureList.getRows()) {
      final Float rt = row.getAverageRT();
      if (rt == null || !Float.isFinite(rt)) {
        continue;
      }
      minRt = Math.min(minRt, rt);
      maxRt = Math.max(maxRt, rt);
    }
    if (!Double.isFinite(minRt) || !Double.isFinite(maxRt) || maxRt <= minRt) {
      return 0d;
    }
    return maxRt - minRt;
  }

  private record RtWindow(double lowerRt, double upperRt, int supportCount, int totalCount) {

  }

  /**
   * Re-scores all lipid annotations in the feature list using the parameters from the last applied
   * {@link LipidAnnotationModule} method. Intended for use after the user manually modifies
   * annotations (e.g. removes entries in the lipid QC dashboard) so that stored scores remain
   * consistent without requiring a full re-annotation run.
   * <p>
   * If no {@link LipidAnnotationModule} entry is found in the applied methods the call is a no-op.
   *
   * @param featureList feature list whose annotations should be re-scored
   */
  public static void rescoreOverallQualityScores(final @NotNull ModularFeatureList featureList) {
    final List<FeatureListAppliedMethod> methods = featureList.getAppliedMethods();
    FeatureListAppliedMethod method = ParameterUtils.getLatestModuleCall(
        featureList.getAppliedMethods(), LipidAnnotationModule.class);
    if (method == null) {
      return;
    }

    try {

      final ParameterSet param = method.getParameters();
      final boolean includeMs2Score = Boolean.TRUE.equals(
          param.getParameter(LipidAnnotationParameters.searchForMSMSFragments).getValue());
      final LipidAnalysisType analysisType = param.getParameter(
          LipidAnnotationParameters.lipidAnalysisType).getValue();
      final boolean includeElutionOrderScore =
          analysisType == null || analysisType.hasRetentionTimePattern();

      final ComponentWeights customQcWeights =
          param.getValue(LipidAnnotationParameters.customQcWeights) ? param.getParameter(
                  LipidAnnotationParameters.customQcWeights).getEmbeddedParameters()
              .toComponentWeights(analysisType) : null;

      final MZTolerance mzTolerance = param.getParameter(LipidAnnotationParameters.mzTolerance)
          .getValue();
      computeAndStoreOverallQualityScores(featureList, includeMs2Score, includeElutionOrderScore,
          analysisType, customQcWeights, mzTolerance);
    } catch (Exception e) {
      // silent, old parameter set version
    }
  }

  /**
   * Computes and stores overall quality scores for all lipid annotations in the feature list.
   * <p>
   * This method should be called after all lipid annotations have been added to the feature list,
   * as it requires the complete context (all rows with annotations) to compute context-dependent
   * scores like elution order trends and interference metrics.
   *
   * @param featureList              feature list containing lipid annotations
   * @param includeMs2Score          true to include MS2 explained-intensity score
   * @param includeElutionOrderScore true to include elution-order score
   * @param analysisType             lipid analysis mode or null for auto-detection
   * @param customQcWeights          custom component weights or null for defaults
   * @param mzTolerance              MS1 tolerance used for scoring or null for default
   */
  public static void computeAndStoreOverallQualityScores(
      final @NotNull ModularFeatureList featureList, final boolean includeMs2Score,
      final boolean includeElutionOrderScore, final @Nullable LipidAnalysisType analysisType,
      final @Nullable ComponentWeights customQcWeights, final @Nullable MZTolerance mzTolerance) {
    for (final FeatureListRow row : featureList.getRows()) {
      final List<MatchedLipid> lipidMatches = row.getLipidMatches();
      if (lipidMatches.isEmpty()) {
        continue;
      }
      for (final MatchedLipid match : lipidMatches) {
        final double score = computeCombinedAnnotationScore(featureList, row, match,
            includeMs2Score, includeElutionOrderScore, analysisType, customQcWeights, mzTolerance);
        match.setOverallQualityScore((float) clampToUnit(score));
      }
    }
  }

  public static void sortLipidAnnotationsByOverallScore(@NotNull final FeatureListRow row) {
    row.setLipidAnnotations(row.getLipidMatches().stream().sorted(
            Comparator.comparing(MatchedLipid::getOverallQualityScore, Comparators.scoreDescending()))
        .toList());
  }
}
