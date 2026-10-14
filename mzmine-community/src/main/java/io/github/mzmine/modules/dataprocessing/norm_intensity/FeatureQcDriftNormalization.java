/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 */
package io.github.mzmine.modules.dataprocessing.norm_intensity;

import io.github.mzmine.util.MathUtils;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.interpolation.LoessInterpolator;
import org.jetbrains.annotations.NotNull;

/**
 * Fits a separate log-response LOESS curve for every feature and QC batch.
 *
 * <p>This calculation deliberately returns unavailable rows instead of substituting a raw value or
 * silently clipping an implausible correction factor. It is independent of project state so callers
 * can retain and apply the exact frozen output.</p>
 */
public final class FeatureQcDriftNormalization {

  private FeatureQcDriftNormalization() {
  }

  public record Sample(@NotNull String id, @NotNull String batch, double order, boolean qc) {
  }

  public record Result(@NotNull Map<Integer, Map<String, Double>> rowFactors,
                       @NotNull Map<Integer, Set<String>> boundarySampleIds,
                       @NotNull Map<Integer, String> unavailableRows) {
  }

  /**
   * Calculates a factor for each row/sample pair. {@code qcValues} contains only the positive,
   * finite QC responses for a row. Each batch needs at least minimumQcs usable responses.
   */
  public static @NotNull Result calculate(@NotNull final List<Sample> samples,
      @NotNull final Map<Integer, Map<String, Double>> qcValues, final int minimumQcs,
      final double bandwidth, final int robustnessIterations, final double maxFactor,
      final boolean correctBetweenBatches) {
    return calculate(samples, qcValues, minimumQcs, bandwidth, robustnessIterations, maxFactor,
        correctBetweenBatches, () -> false);
  }

  public static @NotNull Result calculate(@NotNull final List<Sample> samples,
      @NotNull final Map<Integer, Map<String, Double>> qcValues, final int minimumQcs,
      final double bandwidth, final int robustnessIterations, final double maxFactor,
      final boolean correctBetweenBatches, final @NotNull BooleanSupplier canceled) {
    validateInputs(samples, minimumQcs, bandwidth, robustnessIterations, maxFactor);
    final Map<Integer, Map<String, Double>> factors = new LinkedHashMap<>();
    final Map<Integer, Set<String>> boundaries = new LinkedHashMap<>();
    final Map<Integer, String> unavailable = new LinkedHashMap<>();
    for (final Map.Entry<Integer, Map<String, Double>> entry : qcValues.entrySet()) {
      if (canceled.getAsBoolean()) throw new java.util.concurrent.CancellationException();
      try {
        final RowResult row = calculateRow(samples, entry.getValue(), minimumQcs, bandwidth,
            robustnessIterations, maxFactor, correctBetweenBatches, canceled);
        factors.put(entry.getKey(), row.factors());
        if (!row.boundaryIds().isEmpty()) boundaries.put(entry.getKey(), row.boundaryIds());
      } catch (final IllegalArgumentException exception) {
        unavailable.put(entry.getKey(), exception.getMessage());
      }
    }
    return new Result(immutableNestedMap(factors), immutableSetMap(boundaries), Map.copyOf(unavailable));
  }

  private static @NotNull RowResult calculateRow(@NotNull final List<Sample> samples,
      @NotNull final Map<String, Double> values, final int minimumQcs, final double bandwidth,
      final int robustnessIterations, final double maxFactor, final boolean correctBetweenBatches,
      final @NotNull BooleanSupplier canceled) {
    final Map<String, List<Sample>> batches = batches(samples);
    final Map<String, Double> targets = new LinkedHashMap<>();
    final Map<String, Map<String, Double>> perBatch = new LinkedHashMap<>();
    final Set<String> boundaries = new LinkedHashSet<>();
    for (final Map.Entry<String, List<Sample>> batch : batches.entrySet()) {
      if (canceled.getAsBoolean()) throw new java.util.concurrent.CancellationException();
      final List<Sample> qcs = batch.getValue().stream().filter(Sample::qc)
          .filter(sample -> values.get(sample.id()) != null && Double.isFinite(values.get(sample.id()))
              && values.get(sample.id()) > 0d)
          .sorted(Comparator.comparingDouble(Sample::order)).toList();
      if (qcs.size() < minimumQcs) throw new IllegalArgumentException("Batch '%s' has fewer than %d usable positive QC responses."
          .formatted(batch.getKey(), minimumQcs));
      final double[] x = new double[qcs.size()];
      final double[] logY = new double[qcs.size()];
      final double[] raw = new double[qcs.size()];
      for (int index = 0; index < qcs.size(); index++) {
        final Sample qc = qcs.get(index);
        final Double response = values.get(qc.id());
        if (response == null || !Double.isFinite(response) || response <= 0d) {
          throw new IllegalArgumentException("QC response is missing or nonpositive for " + qc.id() + ".");
        }
        x[index] = qc.order();
        raw[index] = response;
        logY[index] = Math.log(response);
      }
      final double target = MathUtils.calcMedian(raw);
      final UnivariateFunction model;
      try {
        model = new LoessInterpolator(bandwidth, robustnessIterations).interpolate(x, logY);
      } catch (final RuntimeException exception) {
        throw new IllegalArgumentException("LOESS fitting failed for batch '" + batch.getKey() + "'.", exception);
      }
      targets.put(batch.getKey(), target);
      final Map<String, Double> batchFactors = new LinkedHashMap<>();
      for (final Sample sample : batch.getValue()) {
        final double predictedLog;
        if (sample.order() <= x[0]) {
          predictedLog = model.value(x[0]);
          if (sample.order() < x[0]) boundaries.add(sample.id());
        } else if (sample.order() >= x[x.length - 1]) {
          predictedLog = model.value(x[x.length - 1]);
          if (sample.order() > x[x.length - 1]) boundaries.add(sample.id());
        } else {
          predictedLog = model.value(sample.order());
        }
        final double predicted = Math.exp(predictedLog);
        final double factor = target / predicted;
        validateFactor(factor, maxFactor);
        batchFactors.put(sample.id(), factor);
      }
      perBatch.put(batch.getKey(), batchFactors);
    }
    if (correctBetweenBatches) {
      final double interBatchTarget = MathUtils.calcMedian(targets.values().stream()
          .mapToDouble(Double::doubleValue).toArray());
      for (final Map.Entry<String, Map<String, Double>> entry : perBatch.entrySet()) {
        final double adjustment = interBatchTarget / targets.get(entry.getKey());
        for (final Map.Entry<String, Double> factor : entry.getValue().entrySet()) {
          final double adjusted = factor.getValue() * adjustment;
          validateFactor(adjusted, maxFactor);
          factor.setValue(adjusted);
        }
      }
    }
    final Map<String, Double> ordered = new LinkedHashMap<>();
    for (final Sample sample : samples) ordered.put(sample.id(), perBatch.get(sample.batch()).get(sample.id()));
    return new RowResult(Map.copyOf(ordered), Set.copyOf(boundaries));
  }

  private static void validateInputs(@NotNull final List<Sample> samples, final int minimumQcs,
      final double bandwidth, final int robustnessIterations, final double maxFactor) {
    if (samples.isEmpty()) throw new IllegalArgumentException("At least one sample is required.");
    if (minimumQcs < 5) throw new IllegalArgumentException("minimum_qcs must be at least 5.");
    if (!Double.isFinite(bandwidth) || bandwidth <= 0d || bandwidth > 1d)
      throw new IllegalArgumentException("bandwidth must be in (0, 1].");
    if (robustnessIterations < 0) throw new IllegalArgumentException("robustness_iterations must be nonnegative.");
    if (!Double.isFinite(maxFactor) || maxFactor <= 1d)
      throw new IllegalArgumentException("max_factor must be finite and greater than one.");
    final Set<String> ids = new LinkedHashSet<>();
    final Map<String, Set<Double>> orders = new LinkedHashMap<>();
    for (final Sample sample : samples) {
      if (sample.id() == null || sample.id().isBlank() || !ids.add(sample.id()))
        throw new IllegalArgumentException("Sample IDs must be nonblank and unique.");
      if (sample.batch() == null || sample.batch().isBlank() || !Double.isFinite(sample.order()))
        throw new IllegalArgumentException("Every sample needs a finite order and nonblank batch.");
      if (!orders.computeIfAbsent(sample.batch(), ignored -> new LinkedHashSet<>()).add(sample.order()))
        throw new IllegalArgumentException("Sample order is tied in batch '" + sample.batch() + "'.");
    }
  }

  private static @NotNull Map<String, List<Sample>> batches(@NotNull final List<Sample> samples) {
    final Map<String, List<Sample>> output = new LinkedHashMap<>();
    for (final Sample sample : samples) output.computeIfAbsent(sample.batch(), ignored -> new ArrayList<>()).add(sample);
    return output;
  }

  private static void validateFactor(final double factor, final double maxFactor) {
    // decision: reject a row outside the declared reciprocal factor range instead of changing it.
    if (!Double.isFinite(factor) || factor < 1d / maxFactor || factor > maxFactor)
      throw new IllegalArgumentException("A feature-specific factor exceeds the declared reciprocal max_factor range.");
  }

  private static @NotNull Map<Integer, Map<String, Double>> immutableNestedMap(
      @NotNull final Map<Integer, Map<String, Double>> input) {
    final Map<Integer, Map<String, Double>> output = new LinkedHashMap<>();
    input.forEach((key, value) -> output.put(key, Map.copyOf(value)));
    return Map.copyOf(output);
  }

  private static @NotNull Map<Integer, Set<String>> immutableSetMap(
      @NotNull final Map<Integer, Set<String>> input) {
    final Map<Integer, Set<String>> output = new LinkedHashMap<>();
    input.forEach((key, value) -> output.put(key, Set.copyOf(value)));
    return Map.copyOf(output);
  }

  private record RowResult(@NotNull Map<String, Double> factors, @NotNull Set<String> boundaryIds) {
  }
}
