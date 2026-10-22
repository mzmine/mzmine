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

import io.github.mzmine.datamodel.RawDataFile;
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

  public record Sample(@NotNull RawDataFile rawDataFile, @NotNull String batch, double order, boolean reference,
                       double metric) {

  }

  public record Result(@NotNull Map<RawDataFile, Double> factors,
                       @NotNull Map<String, Double> batchReferenceMedians,
                       double interBatchReference, @NotNull Set<RawDataFile> edgeRawDataFiles) {

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
   * Calculates factors using precomputed native interpolation weights for non-reference samples.
   *
   * <p>This overload is intended for callers that resolved {@link MetadataTableUtils}-equivalent
   * weights against the current project metadata during preparation. It never reads raw files or
   * metadata after the weights have been captured.
   */
  public static @NotNull Result calculate(@NotNull final List<Sample> samples,
      @NotNull final Map<RawDataFile, ScalarQcBatchNormalization.ResolvedInterpolation<RawDataFile>>
          precomputedInterpolations,
      final boolean correctBetweenBatches) {
    if (samples.isEmpty()) {
      throw new IllegalArgumentException("At least one sample is required for QC normalization.");
    }

    final Map<String, List<Sample>> batches = new LinkedHashMap<>();
    final Set<RawDataFile> rawDataFiles = new LinkedHashSet<>();
    for (final Sample sample : samples) {
      validateSample(sample, rawDataFiles);
      batches.computeIfAbsent(sample.batch(), ignored -> new ArrayList<>()).add(sample);
    }
    validatePrecomputedInterpolations(samples, precomputedInterpolations);

    final Map<String, Double> batchMedians = new LinkedHashMap<>();
    final Map<RawDataFile, Double> factors = new LinkedHashMap<>();
    final Set<RawDataFile> edgeRawDataFiles = new LinkedHashSet<>();
    for (final Map.Entry<String, List<Sample>> entry : batches.entrySet()) {
      final List<Sample> batchSamples = entry.getValue();
      validateDistinctOrders(entry.getKey(), batchSamples);
      final List<Sample> references = batchSamples.stream().filter(Sample::reference)
          .sorted(Comparator.comparingDouble(Sample::order)).toList();
      if (references.isEmpty()) {
        throw new IllegalArgumentException("Batch '%s' has no reference sample.".formatted(entry.getKey()));
      }

      final Map<RawDataFile, Double> referenceMetrics = new LinkedHashMap<>();
      references.forEach(reference -> referenceMetrics.put(reference.rawDataFile(), reference.metric()));
      final ScalarQcBatchNormalization.ReferenceFactors<RawDataFile> referenceFactors =
          ScalarQcBatchNormalization.calculateReferenceFactors(referenceMetrics);
      batchMedians.put(entry.getKey(), referenceFactors.referenceLevel());

      final Map<RawDataFile, ScalarQcBatchNormalization.ResolvedInterpolation<RawDataFile>> interpolations =
          new LinkedHashMap<>();
      for (final Sample sample : batchSamples) {
        if (!sample.reference()) {
          interpolations.put(sample.rawDataFile(), resolveInterpolation(sample, references,
              edgeRawDataFiles, precomputedInterpolations.get(sample.rawDataFile()),
              precomputedInterpolations.isEmpty()));
        }
      }
      factors.putAll(ScalarQcBatchNormalization.calculateWithinBatchFactors(
          referenceFactors.factors(), interpolations));
    }

    final ScalarQcBatchNormalization.InterBatchScales<String> interBatchScales =
        ScalarQcBatchNormalization.calculateInterBatchScales(batchMedians, correctBetweenBatches);
    if (correctBetweenBatches) {
      for (final Map.Entry<String, List<Sample>> entry : batches.entrySet()) {
        final double interBatchFactor = interBatchScales.scales().get(entry.getKey());
        for (final Sample sample : entry.getValue()) {
          factors.compute(sample.rawDataFile(), (file, factor) -> factor * interBatchFactor);
        }
      }
    }

    validateFactors(factors);

    final Map<RawDataFile, Double> factorsInSampleOrder = new LinkedHashMap<>();
    final Set<RawDataFile> edgesInSampleOrder = new LinkedHashSet<>();
    for (final Sample sample : samples) {
      factorsInSampleOrder.put(sample.rawDataFile(), factors.get(sample.rawDataFile()));
      if (edgeRawDataFiles.contains(sample.rawDataFile())) {
        edgesInSampleOrder.add(sample.rawDataFile());
      }
    }
    return new Result(immutableOrderedMap(factorsInSampleOrder), immutableOrderedMap(batchMedians),
        interBatchScales.interBatchReference(), immutableOrderedSet(edgesInSampleOrder));
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

  private static void validateSample(@NotNull final Sample sample,
      @NotNull final Set<RawDataFile> rawDataFiles) {
    if (!rawDataFiles.add(sample.rawDataFile())) {
      throw new IllegalArgumentException("Duplicate raw data file: " + sample.rawDataFile().getName());
    }
    if (!Double.isFinite(sample.order())) {
      throw new IllegalArgumentException("Sample order must be finite: " + sample.rawDataFile().getName());
    }
    if (sample.batch() == null || sample.batch().isBlank()) {
      throw new IllegalArgumentException("Sample batch must not be blank: " + sample.rawDataFile().getName());
    }
    if (sample.reference() && (!Double.isFinite(sample.metric()) || sample.metric() <= 0d)) {
      throw new IllegalArgumentException(
          "Reference sample metric must be positive and finite: " + sample.rawDataFile().getName());
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

  private static void validatePrecomputedInterpolations(@NotNull final List<Sample> samples,
      @NotNull final Map<RawDataFile, ScalarQcBatchNormalization.ResolvedInterpolation<RawDataFile>>
          precomputedInterpolations) {
    if (precomputedInterpolations.isEmpty()) return;
    final Set<RawDataFile> expected = samples.stream().filter(sample -> !sample.reference())
        .map(Sample::rawDataFile).collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    if (!precomputedInterpolations.keySet().equals(expected)) {
      throw new IllegalArgumentException(
          "Precomputed interpolations must cover exactly all non-reference samples.");
    }
  }

  private static @NotNull ScalarQcBatchNormalization.ResolvedInterpolation<RawDataFile> resolveInterpolation(
      @NotNull final Sample sample, @NotNull final List<Sample> references,
      @NotNull final Set<RawDataFile> edgeRawDataFiles,
      @Nullable final ScalarQcBatchNormalization.ResolvedInterpolation<RawDataFile> precomputed,
      final boolean calculateWeights) {
    if (!calculateWeights) {
      if (precomputed == null) {
        throw new IllegalArgumentException("Missing precomputed interpolation for sample: "
            + sample.rawDataFile().getName());
      }
      return resolvePrecomputedInterpolation(sample, references, edgeRawDataFiles, precomputed);
    }

    final Sample first = references.getFirst();
    final Sample last = references.getLast();
    if (sample.order() < first.order()) {
      edgeRawDataFiles.add(sample.rawDataFile());
      return ScalarQcBatchNormalization.ResolvedInterpolation.single(first.rawDataFile());
    }
    if (sample.order() > last.order()) {
      edgeRawDataFiles.add(sample.rawDataFile());
      return ScalarQcBatchNormalization.ResolvedInterpolation.single(last.rawDataFile());
    }

    for (int index = 1; index < references.size(); index++) {
      final Sample previous = references.get(index - 1);
      final Sample next = references.get(index);
      if (sample.order() < next.order()) {
        final double nextWeight = (sample.order() - previous.order()) / (next.order()
            - previous.order());
        return new ScalarQcBatchNormalization.ResolvedInterpolation<>(previous.rawDataFile(),
            1d - nextWeight, next.rawDataFile(), nextWeight);
      }
    }
    throw new IllegalStateException("Could not determine QC factor for sample: "
        + sample.rawDataFile().getName());
  }

  private static @NotNull ScalarQcBatchNormalization.ResolvedInterpolation<RawDataFile> resolvePrecomputedInterpolation(
      @NotNull final Sample sample, @NotNull final List<Sample> references,
      @NotNull final Set<RawDataFile> edgeRawDataFiles,
      @NotNull final ScalarQcBatchNormalization.ResolvedInterpolation<RawDataFile> precomputed) {
    final Map<RawDataFile, Sample> referencesByFile = new LinkedHashMap<>();
    references.forEach(reference -> referencesByFile.put(reference.rawDataFile(), reference));
    final Sample previous = referencesByFile.get(precomputed.previousReference());
    if (previous == null) {
      throw new IllegalArgumentException("Precomputed previous QC is not in sample batch: "
          + sample.rawDataFile().getName());
    }
    if (!Double.isFinite(precomputed.previousWeight()) || precomputed.previousWeight() < 0d
        || !Double.isFinite(precomputed.nextWeight()) || precomputed.nextWeight() < 0d) {
      throw new IllegalArgumentException("Precomputed interpolation weights must be finite and nonnegative: "
          + sample.rawDataFile().getName());
    }
    if (precomputed.nextReference() == null) {
      if (Double.compare(precomputed.previousWeight(), 1d) != 0
          || Double.compare(precomputed.nextWeight(), 0d) != 0) {
        throw new IllegalArgumentException("Single precomputed interpolation must have weights 1 and 0: "
            + sample.rawDataFile().getName());
      }
      edgeRawDataFiles.add(sample.rawDataFile());
      return ScalarQcBatchNormalization.ResolvedInterpolation.single(previous.rawDataFile());
    }
    final Sample next = referencesByFile.get(precomputed.nextReference());
    if (next == null || next == previous) {
      throw new IllegalArgumentException("Precomputed next QC is not a distinct sample-batch reference: "
          + sample.rawDataFile().getName());
    }
    return new ScalarQcBatchNormalization.ResolvedInterpolation<>(previous.rawDataFile(),
        precomputed.previousWeight(), next.rawDataFile(), precomputed.nextWeight());
  }

  private static void validateFactors(@NotNull final Map<RawDataFile, Double> factors) {
    for (final Map.Entry<RawDataFile, Double> entry : factors.entrySet()) {
      if (entry.getValue() == null || !Double.isFinite(entry.getValue()) || entry.getValue() <= 0d) {
        throw new IllegalArgumentException("QC normalization produced an invalid factor for "
            + entry.getKey().getName());
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
