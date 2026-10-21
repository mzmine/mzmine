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

package io.github.mzmine.modules.dataprocessing.norm_intensity;

import io.github.mzmine.util.MathUtils;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Shared scalar QC normalization calculation. Adapters resolve samples, metrics and interpolation
 * weights, while this class calculates the reference levels, within-batch factors and optional
 * inter-batch scales.
 */
public final class ScalarQcBatchNormalization {

  private ScalarQcBatchNormalization() {
  }

  public record ReferenceFactors<T>(double referenceLevel, @NotNull Map<T, Double> factors) {

    public ReferenceFactors {
      factors = immutableOrderedMap(factors);
    }
  }

  /** A pre-resolved interpolation between one or two reference samples. */
  public record ResolvedInterpolation<T>(@NotNull T previousReference, double previousWeight,
                                         @Nullable T nextReference, double nextWeight) {

    public static <T> @NotNull ResolvedInterpolation<T> single(@NotNull final T reference) {
      return new ResolvedInterpolation<>(reference, 1d, null, 0d);
    }
  }

  public record InterBatchScales<T>(double interBatchReference,
                                    @NotNull Map<T, Double> scales) {

    public InterBatchScales {
      scales = immutableOrderedMap(scales);
    }
  }

  /** Calculates a shared reference level and factors for each reference metric. */
  public static <T> @NotNull ReferenceFactors<T> calculateReferenceFactors(
      @NotNull final Map<T, Double> referenceMetrics) {
    if (referenceMetrics.isEmpty()) {
      return new ReferenceFactors<>(Double.NaN, Map.of());
    }
    final double referenceLevel = MathUtils.calcMedian(
        referenceMetrics.values().stream().mapToDouble(Double::doubleValue).toArray());
    final Map<T, Double> factors = new LinkedHashMap<>();
    referenceMetrics.forEach(
        (reference, metric) -> factors.put(reference, referenceLevel / metric));
    return new ReferenceFactors<>(referenceLevel, factors);
  }

  /**
   * Produces the correction factors for a batch from its reference factors and resolved sample
   * interpolation weights. Weights are deliberately used as supplied; they are not normalized.
   */
  public static <T> @NotNull Map<T, Double> calculateWithinBatchFactors(
      @NotNull final Map<T, Double> referenceFactors,
      @NotNull final Map<T, ResolvedInterpolation<T>> interpolations) {
    final Map<T, Double> factors = new LinkedHashMap<>(referenceFactors);
    for (final Map.Entry<T, ResolvedInterpolation<T>> entry : interpolations.entrySet()) {
      if (factors.containsKey(entry.getKey())) {
        throw new IllegalArgumentException(
            "Reference samples must not have interpolation weights.");
      }
      final ResolvedInterpolation<T> interpolation = entry.getValue();
      final Double previous = referenceFactors.get(interpolation.previousReference());
      if (previous == null) {
        throw new IllegalArgumentException("Interpolation references an unknown previous QC.");
      }
      final double factor;
      if (interpolation.nextReference() == null) {
        if (Double.compare(interpolation.previousWeight(), 1d) != 0
            || Double.compare(interpolation.nextWeight(), 0d) != 0) {
          throw new IllegalArgumentException(
              "Single-reference interpolation must have weights 1 and 0.");
        }
        factor = previous;
      } else {
        final Double next = referenceFactors.get(interpolation.nextReference());
        if (next == null || interpolation.nextReference().equals(interpolation.previousReference())) {
          throw new IllegalArgumentException(
              "Interpolation references an unknown or duplicate next QC.");
        }
        factor = previous * interpolation.previousWeight() + next * interpolation.nextWeight();
      }
      factors.put(entry.getKey(), factor);
    }
    return immutableOrderedMap(factors);
  }

  /** Calculates the common inter-batch reference and the optional scale per batch. */
  public static <T> @NotNull InterBatchScales<T> calculateInterBatchScales(
      @NotNull final Map<T, Double> batchReferenceLevels, final boolean applyCorrection) {
    if (batchReferenceLevels.isEmpty()) {
      return new InterBatchScales<>(Double.NaN, Map.of());
    }
    final double interBatchReference = MathUtils.calcMedian(
        batchReferenceLevels.values().stream().mapToDouble(Double::doubleValue).toArray());
    final Map<T, Double> scales = new LinkedHashMap<>();
    batchReferenceLevels.forEach((batch, referenceLevel) -> scales.put(batch,
        applyCorrection ? interBatchReference / referenceLevel : 1d));
    return new InterBatchScales<>(interBatchReference, scales);
  }

  private static @NotNull <K, V> Map<K, V> immutableOrderedMap(@NotNull final Map<K, V> map) {
    return Collections.unmodifiableMap(new LinkedHashMap<>(map));
  }
}
