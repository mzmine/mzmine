/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 */

package io.github.mzmine.modules.dataanalysis.featurecomparison;

import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.util.MathUtils;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Pure, descriptive feature-table comparison calculations.
 *
 * <p>The blank-subtraction task intentionally changes a feature list. This service keeps the
 * source table untouched and exposes unavailable evidence instead of turning it into a pass.</p>
 */
public final class FeatureComparison {

  private FeatureComparison() {
  }

  /** Summarizes observations without imputing unavailable values. */
  public static @NotNull GroupSummary summarize(@NotNull List<Observation> observations) {
    int detected = 0;
    int estimated = 0;
    int aggregated = 0;
    int missing = 0;
    final List<Double> quantified = new ArrayList<>();
    for (final Observation observation : observations) {
      final FeatureStatus status = observation.status();
      if (status == FeatureStatus.DETECTED || status == FeatureStatus.MANUAL) {
        detected++;
      } else if (status == FeatureStatus.ESTIMATED) {
        estimated++;
      } else if (status == FeatureStatus.COMPOUND_AGGREGATED) {
        aggregated++;
      }

      final Double abundance = observation.abundance();
      // decision: UNKNOWN status always means unavailable, even if stale numerical data remains.
      if (status == null || status == FeatureStatus.UNKNOWN || abundance == null
          || !Double.isFinite(abundance) || abundance < 0d) {
        missing++;
        continue;
      }
      quantified.add(abundance);
    }

    final double[] values = quantified.stream().mapToDouble(Double::doubleValue).toArray();
    final double calculatedMean = values.length == 0 ? Double.NaN : MathUtils.calcAvg(values);
    final Double mean = Double.isFinite(calculatedMean) ? calculatedMean : null;
    return new GroupSummary(observations.size(), detected, estimated, aggregated, missing,
        quantified.size(), mean, calculateCv(values, mean));
  }

  /** Compares one immutable row snapshot using explicitly selected sample indexes. */
  public static @NotNull FeatureComparisonResult compare(@NotNull RowInput row,
      @NotNull FeatureComparisonSpec spec) {
    validateIndexesInRange(row, spec);
    final GroupSummary target = summarize(select(row.samples(), spec.targetIndexes()));
    final GroupSummary reference = summarize(select(row.samples(), spec.referenceIndexes()));
    final GroupSummary blank = spec.blankIndexes().isEmpty() ? null : summarize(
        select(row.samples(), spec.blankIndexes()));
    final List<String> reasons = new ArrayList<>();

    final DifferenceEvaluation difference = evaluateDifference(target, reference, spec, reasons);
    final BlankEvaluation blankEvaluation = evaluateBlank(target, blank, spec, reasons);
    return new FeatureComparisonResult(row.rowId(), target, reference, blank,
        difference.foldChange(), blankEvaluation.blankRatio(), difference.status(),
        blankEvaluation.status(), difference.passes() && blankEvaluation.passes(), reasons);
  }

  /** Returns all results ranked over the complete input before a caller filters or applies paging. */
  public static @NotNull List<FeatureComparisonResult> compareAll(@NotNull List<RowInput> rows,
      @NotNull FeatureComparisonSpec spec) {
    return compareAll(rows, spec, () -> false);
  }

  /** Compares every row while allowing a caller-owned task to interrupt long result tables. */
  public static @NotNull List<FeatureComparisonResult> compareAll(@NotNull List<RowInput> rows,
      @NotNull FeatureComparisonSpec spec, @NotNull BooleanSupplier canceled) {
    return compareAll((Iterable<RowInput>) rows, spec, canceled);
  }

  /**
   * Compares lazily projected rows without requiring callers to retain an additional input graph.
   */
  public static @NotNull List<FeatureComparisonResult> compareAll(@NotNull Iterable<RowInput> rows,
      @NotNull FeatureComparisonSpec spec, @NotNull BooleanSupplier canceled) {
    final List<FeatureComparisonResult> results = new ArrayList<>();
    for (final RowInput row : rows) {
      checkCanceled(canceled);
      results.add(compare(row, spec));
    }
    checkCanceled(canceled);
    results.sort(resultComparator());
    checkCanceled(canceled);
    return List.copyOf(results);
  }

  private static @NotNull DifferenceEvaluation evaluateDifference(@NotNull GroupSummary target,
      @NotNull GroupSummary reference, @NotNull FeatureComparisonSpec spec,
      @NotNull List<String> reasons) {
    final double targetDetectionFraction = (double) target.detectedCount() / target.sampleCount();
    if (target.detectedCount() == 0) {
      reasons.add("not_detected_in_target");
      return new DifferenceEvaluation(null, "not_detected_in_target", false);
    }
    if (targetDetectionFraction < spec.minimumDetectionFraction()) {
      reasons.add("target_detection_below_minimum");
      return new DifferenceEvaluation(null, "target_detection_below_minimum", false);
    }
    // decision: a detected target with no quantified or detected reference is sample-only evidence.
    if (reference.quantifiedCount() == 0 && reference.detectedCount() == 0) {
      if (target.mean() == null || target.mean() <= 0d) {
        reasons.add("sample_only_target_signal_unavailable");
        return new DifferenceEvaluation(null, "comparison_undefined", false);
      }
      if (!spec.includeSampleOnly()) {
        reasons.add("sample_only_excluded");
        return new DifferenceEvaluation(null, "sample_only", false);
      }
      return new DifferenceEvaluation(null, "sample_only", true);
    }
    if (target.mean() == null || reference.mean() == null || reference.mean() <= 0d) {
      reasons.add("fold_change_unavailable");
      return new DifferenceEvaluation(null, "comparison_undefined", false);
    }
    final double foldChange = target.mean() / reference.mean();
    if (!Double.isFinite(foldChange)) {
      reasons.add("fold_change_unavailable");
      return new DifferenceEvaluation(null, "comparison_undefined", false);
    }
    if (foldChange < spec.minimumFoldChange()) {
      reasons.add("fold_change_below_minimum");
      return new DifferenceEvaluation(foldChange, "not_increased", false);
    }
    return new DifferenceEvaluation(foldChange, "increased", true);
  }

  private static @NotNull BlankEvaluation evaluateBlank(@NotNull GroupSummary target,
      @Nullable GroupSummary blank, @NotNull FeatureComparisonSpec spec,
      @NotNull List<String> reasons) {
    if (spec.minimumBlankRatio() == null) {
      return new BlankEvaluation(null, "not_requested", true);
    }
    if (blank == null || target.mean() == null || blank.mean() == null || blank.mean() <= 0d) {
      // decision: no blank, nondetection, and a zero denominator are unassessed rather than infinity.
      reasons.add("blank_ratio_unavailable");
      return new BlankEvaluation(null, "unassessed", false);
    }
    final double blankRatio = target.mean() / blank.mean();
    if (!Double.isFinite(blankRatio)) {
      reasons.add("blank_ratio_unavailable");
      return new BlankEvaluation(null, "unassessed", false);
    }
    if (blankRatio < spec.minimumBlankRatio()) {
      reasons.add("blank_ratio_below_minimum");
      return new BlankEvaluation(blankRatio, "blank_ratio_below_minimum", false);
    }
    return new BlankEvaluation(blankRatio, "blank_ratio_passed", true);
  }

  private static @NotNull List<Observation> select(@NotNull List<Observation> observations,
      @NotNull List<Integer> indexes) {
    return indexes.stream().map(observations::get).toList();
  }

  private static void validateIndexesInRange(@NotNull RowInput row,
      @NotNull FeatureComparisonSpec spec) {
    final int samples = row.samples().size();
    for (final int index : allIndexes(spec)) {
      if (index >= samples) {
        throw new IllegalArgumentException("Sample index " + index + " is outside row " + row.rowId());
      }
    }
  }

  private static @NotNull List<Integer> allIndexes(@NotNull FeatureComparisonSpec spec) {
    final List<Integer> indexes = new ArrayList<>(spec.targetIndexes());
    indexes.addAll(spec.referenceIndexes());
    indexes.addAll(spec.blankIndexes());
    return indexes;
  }

  private static @Nullable Double calculateCv(double @NotNull [] values, @Nullable Double mean) {
    if (values.length < 2 || mean == null || mean <= 0d) {
      return null;
    }
    // decision: report descriptive sample CV only when it has a non-zero finite mean.
    final double cv = MathUtils.calcRelativeStd(values);
    return Double.isFinite(cv) ? cv : null;
  }

  private static @NotNull Comparator<FeatureComparisonResult> resultComparator() {
    return Comparator.comparing((FeatureComparisonResult result) -> !result.differenceStatus()
            .equals("sample_only"))
        .thenComparing(FeatureComparison::descendingFoldChange)
        .thenComparing(FeatureComparison::descendingTargetMean)
        .thenComparingInt(FeatureComparisonResult::rowId);
  }

  private static double descendingFoldChange(@NotNull FeatureComparisonResult result) {
    return result.foldChange() == null ? Double.POSITIVE_INFINITY : -result.foldChange();
  }

  private static double descendingTargetMean(@NotNull FeatureComparisonResult result) {
    return result.target().mean() == null ? Double.POSITIVE_INFINITY : -result.target().mean();
  }

  private static void checkCanceled(@NotNull BooleanSupplier canceled) {
    if (canceled.getAsBoolean()) {
      throw new CancellationException("Feature comparison was canceled");
    }
  }

  private record DifferenceEvaluation(@Nullable Double foldChange, @NotNull String status,
                                      boolean passes) {
  }

  private record BlankEvaluation(@Nullable Double blankRatio, @NotNull String status,
                                 boolean passes) {
  }
}
