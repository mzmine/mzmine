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

package io.github.mzmine.modules.dataprocessing.norm_intensity;

import io.github.mzmine.util.ArrayUtils;
import io.github.mzmine.util.MathUtils;
import io.github.mzmine.modules.visualization.projectmetadata.table.MetadataTableUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Calculates QC-reference factors for an ordered batch sequence.
 *
 * <p>The same factor arithmetic is used by the native intensity normalizer. Orders are arbitrary
 * finite coordinates, such as elapsed acquisition seconds.
 */
public final class QcBatchNormalization {

  private QcBatchNormalization() {
  }

  public record Sample(@NotNull String id, @NotNull String batch, double order, boolean reference,
                       double metric) {

  }

  public record Result(@NotNull Map<String, Double> factors,
                       @NotNull Map<String, Double> batchReferenceMedians,
                       double interBatchReference, @NotNull Set<String> edgeSampleIds) {

  }

  /**
   * Frozen native interpolation weights, identified only by stable sample IDs. A missing next
   * reference represents native nearest-QC boundary interpolation.
   */
  public record FrozenInterpolation(@NotNull String sampleId, @NotNull String previousReferenceId,
                                    double previousWeight, @Nullable String nextReferenceId,
                                    double nextWeight) {

    public static @NotNull FrozenInterpolation single(@NotNull final String sampleId,
        @NotNull final String referenceId) {
      return new FrozenInterpolation(sampleId, referenceId, 1d, null, 0d);
    }
  }

  /**
   * Calculates per-sample QC drift factors and, optionally, aligns batches to the median batch QC
   * level. The resulting maps and set retain input/batch encounter order and are immutable.
   *
   * @throws IllegalArgumentException if the samples cannot define an unambiguous QC correction
   */
  public static @NotNull Result calculate(@NotNull final List<Sample> samples,
      final boolean correctBetweenBatches) {
    return calculate(samples, Map.of(), correctBetweenBatches);
  }

  /**
   * Calculates factors using frozen native interpolation weights for non-reference samples.
   *
   * <p>This overload is intended for callers that resolved {@link MetadataTableUtils}-equivalent
   * weights while live metadata was available. It never requires raw files or metadata after the
   * weights have been frozen.
   */
  public static @NotNull Result calculate(@NotNull final List<Sample> samples,
      @NotNull final Map<String, FrozenInterpolation> frozenInterpolations,
      final boolean correctBetweenBatches) {
    if (samples.isEmpty()) {
      throw new IllegalArgumentException("At least one sample is required for QC normalization.");
    }

    final Map<String, List<Sample>> batches = new LinkedHashMap<>();
    final Set<String> ids = new LinkedHashSet<>();
    for (final Sample sample : samples) {
      validateSample(sample, ids);
      batches.computeIfAbsent(sample.batch(), ignored -> new ArrayList<>()).add(sample);
    }
    validateFrozenInterpolations(samples, frozenInterpolations);

    final Map<String, Double> batchMedians = new LinkedHashMap<>();
    final Map<String, Double> factors = new LinkedHashMap<>();
    final Set<String> edgeSampleIds = new LinkedHashSet<>();
    for (final Map.Entry<String, List<Sample>> entry : batches.entrySet()) {
      final List<Sample> batchSamples = entry.getValue();
      validateDistinctOrders(entry.getKey(), batchSamples);
      final List<Sample> references = batchSamples.stream().filter(Sample::reference)
          .sorted(Comparator.comparingDouble(Sample::order)).toList();
      if (references.isEmpty()) {
        throw new IllegalArgumentException("Batch '%s' has no reference sample.".formatted(entry.getKey()));
      }

      final double median = MathUtils.calcMedian(references.stream().mapToDouble(Sample::metric).toArray());
      batchMedians.put(entry.getKey(), median);
      for (final Sample sample : batchSamples) {
        final FrozenInterpolation frozen = frozenInterpolations.get(sample.id());
        final double factor = factorForSample(sample, references, median, edgeSampleIds, frozen,
            frozenInterpolations.isEmpty());
        factors.put(sample.id(), factor);
      }
    }

    final double interBatchReference = MathUtils.calcMedian(
        batchMedians.values().stream().mapToDouble(Double::doubleValue).toArray());
    if (correctBetweenBatches) {
      for (final Map.Entry<String, List<Sample>> entry : batches.entrySet()) {
        final double interBatchFactor = interBatchFactor(interBatchReference,
            batchMedians.get(entry.getKey()));
        for (final Sample sample : entry.getValue()) {
          factors.compute(sample.id(), (id, factor) -> factor * interBatchFactor);
        }
      }
    }

    validateFactors(factors);

    final Map<String, Double> factorsInSampleOrder = new LinkedHashMap<>();
    final Set<String> edgesInSampleOrder = new LinkedHashSet<>();
    for (final Sample sample : samples) {
      factorsInSampleOrder.put(sample.id(), factors.get(sample.id()));
      if (edgeSampleIds.contains(sample.id())) {
        edgesInSampleOrder.add(sample.id());
      }
    }
    return new Result(immutableOrderedMap(factorsInSampleOrder), immutableOrderedMap(batchMedians),
        interBatchReference, immutableOrderedSet(edgesInSampleOrder));
  }

  /** Calculates the feature-intensity metric with the native normalizer's arithmetic. */
  public static double metric(@NotNull final double[] values,
      @NotNull final FeatureIntensityNormalizationMode mode) {
    return switch (mode) {
      case MEDIAN -> MathUtils.calcMedian(values);
      case AVERAGE -> MathUtils.calcAvg(values);
      case SUM_TIC -> ArrayUtils.sum(values);
      case MAX -> ArrayUtils.max(values).orElse(0d);
    };
  }

  static double referenceFactor(final double batchReferenceMedian, final double referenceMetric) {
    return batchReferenceMedian / referenceMetric;
  }

  static double interpolateFactor(final double previousFactor, final double previousWeight,
      final double nextFactor, final double nextWeight) {
    return previousFactor * previousWeight + nextFactor * nextWeight;
  }

  static double interBatchFactor(final double interBatchReference,
      final double batchReferenceMedian) {
    return interBatchReference / batchReferenceMedian;
  }

  private static void validateSample(@NotNull final Sample sample, @NotNull final Set<String> ids) {
    if (sample.id() == null || sample.id().isBlank()) {
      throw new IllegalArgumentException("Sample IDs must not be blank.");
    }
    if (!ids.add(sample.id())) {
      throw new IllegalArgumentException("Duplicate sample ID: " + sample.id());
    }
    if (!Double.isFinite(sample.order())) {
      throw new IllegalArgumentException("Sample order must be finite: " + sample.id());
    }
    if (sample.batch() == null || sample.batch().isBlank()) {
      throw new IllegalArgumentException("Sample batch must not be blank: " + sample.id());
    }
    if (sample.reference() && (!Double.isFinite(sample.metric()) || sample.metric() <= 0d)) {
      throw new IllegalArgumentException(
          "Reference sample metric must be positive and finite: " + sample.id());
    }
  }

  private static void validateDistinctOrders(@NotNull final String batch,
      @NotNull final List<Sample> samples) {
    final Set<Double> orders = new LinkedHashSet<>();
    for (final Sample sample : samples) {
      if (!orders.add(sample.order())) {
        throw new IllegalArgumentException(
            "Duplicate sample order in batch '%s': %s".formatted(batch, sample.order()));
      }
    }
  }

  private static void validateFrozenInterpolations(@NotNull final List<Sample> samples,
      @NotNull final Map<String, FrozenInterpolation> frozenInterpolations) {
    if (frozenInterpolations.isEmpty()) return;
    final Set<String> expected = samples.stream().filter(sample -> !sample.reference())
        .map(Sample::id).collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    if (!frozenInterpolations.keySet().equals(expected)) {
      throw new IllegalArgumentException("Frozen interpolations must cover exactly all non-reference samples.");
    }
  }

  private static double factorForSample(@NotNull final Sample sample,
      @NotNull final List<Sample> references, final double median,
      @NotNull final Set<String> edgeSampleIds, @Nullable final FrozenInterpolation frozen,
      final boolean calculateWeights) {
    if (sample.reference()) {
      if (frozen != null) {
        throw new IllegalArgumentException("Reference sample must not have frozen interpolation: "
            + sample.id());
      }
      return referenceFactor(median, sample.metric());
    }

    if (!calculateWeights) {
      if (frozen == null || !sample.id().equals(frozen.sampleId())) {
        throw new IllegalArgumentException("Missing frozen interpolation for sample: " + sample.id());
      }
      return frozenFactor(sample, references, median, edgeSampleIds, frozen);
    }

    final Sample first = references.getFirst();
    final Sample last = references.getLast();
    if (sample.order() < first.order()) {
      edgeSampleIds.add(sample.id());
      return referenceFactor(median, first.metric());
    }
    if (sample.order() > last.order()) {
      edgeSampleIds.add(sample.id());
      return referenceFactor(median, last.metric());
    }

    for (int index = 1; index < references.size(); index++) {
      final Sample previous = references.get(index - 1);
      final Sample next = references.get(index);
      if (sample.order() < next.order()) {
        final double nextWeight = (sample.order() - previous.order()) / (next.order()
            - previous.order());
        return interpolateFactor(referenceFactor(median, previous.metric()), 1d - nextWeight,
            referenceFactor(median, next.metric()), nextWeight);
      }
    }
    throw new IllegalStateException("Could not determine QC factor for sample: " + sample.id());
  }

  private static double frozenFactor(@NotNull final Sample sample,
      @NotNull final List<Sample> references, final double median,
      @NotNull final Set<String> edgeSampleIds, @NotNull final FrozenInterpolation frozen) {
    final Map<String, Sample> referencesById = new LinkedHashMap<>();
    references.forEach(reference -> referencesById.put(reference.id(), reference));
    final Sample previous = referencesById.get(frozen.previousReferenceId());
    if (previous == null) {
      throw new IllegalArgumentException("Frozen previous QC is not in sample batch: " + sample.id());
    }
    if (!Double.isFinite(frozen.previousWeight()) || frozen.previousWeight() < 0d
        || !Double.isFinite(frozen.nextWeight()) || frozen.nextWeight() < 0d) {
      throw new IllegalArgumentException("Frozen interpolation weights must be finite and nonnegative: "
          + sample.id());
    }
    if (frozen.nextReferenceId() == null) {
      if (Double.compare(frozen.previousWeight(), 1d) != 0 || Double.compare(frozen.nextWeight(), 0d) != 0) {
        throw new IllegalArgumentException("Single frozen interpolation must have weights 1 and 0: "
            + sample.id());
      }
      edgeSampleIds.add(sample.id());
      return referenceFactor(median, previous.metric());
    }
    final Sample next = referencesById.get(frozen.nextReferenceId());
    if (next == null || next == previous) {
      throw new IllegalArgumentException("Frozen next QC is not a distinct sample-batch reference: "
          + sample.id());
    }
    return interpolateFactor(referenceFactor(median, previous.metric()), frozen.previousWeight(),
        referenceFactor(median, next.metric()), frozen.nextWeight());
  }

  private static void validateFactors(@NotNull final Map<String, Double> factors) {
    for (final Map.Entry<String, Double> entry : factors.entrySet()) {
      if (entry.getValue() == null || !Double.isFinite(entry.getValue()) || entry.getValue() <= 0d) {
        throw new IllegalArgumentException("QC normalization produced an invalid factor for "
            + entry.getKey());
      }
    }
  }

  private static @NotNull <K, V> Map<K, V> immutableOrderedMap(@NotNull final Map<K, V> map) {
    return Collections.unmodifiableMap(new LinkedHashMap<>(map));
  }

  private static @NotNull <T> Set<T> immutableOrderedSet(@NotNull final Set<T> set) {
    return Collections.unmodifiableSet(new LinkedHashSet<>(set));
  }
}
