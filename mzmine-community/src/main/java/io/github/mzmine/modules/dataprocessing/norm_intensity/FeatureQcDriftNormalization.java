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
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.interpolation.LoessInterpolator;
import org.jetbrains.annotations.NotNull;

/** Fits a separate log-response LOESS curve for every feature and QC batch. */
public final class FeatureQcDriftNormalization {

  private static final String SOLVER = "Apache Commons Math 3 LoessInterpolator; local linear regression, "
      + "robust bisquare reweighting, cubic-spline interpolation of fitted knots; natural log";
  private static final String COORDINATE_POLICY = "elapsed acquisition seconds with fractional precision; nearest QC boundary; "
      + "curve samples QC knots, adjacent midpoints, and clamped batch endpoints";
  private static final String SCALE_POLICY = "global median of log positive finite input values";

  private FeatureQcDriftNormalization() {
  }

  /** {@code order} is the exact elapsed acquisition-time coordinate supplied by the adapter. */
  public record Sample(@NotNull String id, @NotNull String batch, double order, boolean qc) {
  }

  public static @NotNull FeatureQcResult calculate(@NotNull final List<Sample> samples,
      @NotNull final Map<Integer, Map<String, Double>> allValues,
      @NotNull final FeatureQcSettings settings) {
    return calculate(samples, allValues, settings, () -> false);
  }

  /**
   * Calculates version 2 factors. Inputs include QC and study abundances at the same upstream stage;
   * only positive finite values are modeled or normalized.
   */
  public static @NotNull FeatureQcResult calculate(@NotNull final List<Sample> samples,
      @NotNull final Map<Integer, Map<String, Double>> allValues,
      @NotNull final FeatureQcSettings settings, @NotNull final BooleanSupplier canceled) {
    validateInputs(samples, allValues, canceled);
    final Map<String, List<Sample>> batches = batches(samples, canceled);
    final int totalQcs = countQcs(samples, canceled);

    final Map<Integer, Map<String, Double>> factors = new LinkedHashMap<>();
    final Map<Integer, Set<String>> boundaries = new LinkedHashMap<>();
    final Map<Integer, FeatureQcUnavailableReason> unavailableRows = new LinkedHashMap<>();
    final Map<Integer, Double> globalTargets = new LinkedHashMap<>();
    final Map<Integer, FeatureQcRowDiagnostic> rowDiagnostics = new LinkedHashMap<>();
    for (final Map.Entry<Integer, Map<String, Double>> row : allValues.entrySet()) {
      checkCanceled(canceled);
      final int rowId = row.getKey();
      final Map<String, Double> values = row.getValue();
      final int usableQcs = usableQcs(samples, values, canceled).size();
      if (totalQcs == 0) {
        unavailableRows.put(rowId, FeatureQcUnavailableReason.NO_QC_REFERENCES);
        rowDiagnostics.put(rowId, new FeatureQcRowDiagnostic(rowId, 0, 0,
            FeatureQcUnavailableReason.NO_QC_REFERENCES, Map.of()));
        continue;
      }
      final double missingFraction = 1d - (double) usableQcs / totalQcs;
      if (missingFraction > settings.globalMissingQcThreshold()) {
        unavailableRows.put(rowId, FeatureQcUnavailableReason.GLOBAL_MISSING_QC_THRESHOLD);
        rowDiagnostics.put(rowId, new FeatureQcRowDiagnostic(rowId, totalQcs, usableQcs,
            FeatureQcUnavailableReason.GLOBAL_MISSING_QC_THRESHOLD, Map.of()));
        continue;
      }
      final double globalTarget = medianLogs(values, canceled);
      if (!Double.isFinite(globalTarget)) {
        unavailableRows.put(rowId, FeatureQcUnavailableReason.NO_POSITIVE_FINITE_INPUT);
        rowDiagnostics.put(rowId, new FeatureQcRowDiagnostic(rowId, totalQcs, usableQcs,
            FeatureQcUnavailableReason.NO_POSITIVE_FINITE_INPUT, Map.of()));
        continue;
      }
      globalTargets.put(rowId, globalTarget);
      final Map<String, Double> rowFactors = new LinkedHashMap<>();
      final Set<String> rowBoundaries = new LinkedHashSet<>();
      final Map<String, FeatureQcBatchDiagnostic> batchDiagnostics = new LinkedHashMap<>();
      for (final Map.Entry<String, List<Sample>> batch : batches.entrySet()) {
        checkCanceled(canceled);
        final BatchResult result = calculateBatch(batch.getKey(), batch.getValue(), values, globalTarget,
            settings, canceled);
        batchDiagnostics.put(batch.getKey(), result.diagnostic());
        if (result.diagnostic().successful()) {
          rowFactors.putAll(result.factors());
          rowBoundaries.addAll(result.boundaryIds());
        }
      }
      if (!rowFactors.isEmpty()) factors.put(rowId, rowFactors);
      if (!rowBoundaries.isEmpty()) boundaries.put(rowId, rowBoundaries);
      rowDiagnostics.put(rowId, new FeatureQcRowDiagnostic(rowId, totalQcs, usableQcs, null,
          batchDiagnostics));
    }
    final FeatureQcAvailability availability = availability(allValues.keySet(), samples, factors,
        unavailableRows, canceled);
    final FeatureQcDiagnostics diagnostics = new FeatureQcDiagnostics(SOLVER, COORDINATE_POLICY,
        SCALE_POLICY, FeatureQcSettings.METHOD_VERSION, settings, globalTargets, availability,
        rowDiagnostics);
    return new FeatureQcResult(samples, factors, boundaries, unavailableRows, settings, diagnostics,
        globalTargets, availability);
  }

  private static @NotNull BatchResult calculateBatch(@NotNull final String batch,
      @NotNull final List<Sample> samples, @NotNull final Map<String, Double> values,
      final double globalTarget, @NotNull final FeatureQcSettings settings,
      @NotNull final BooleanSupplier canceled) {
    final List<Sample> qcs = usableQcs(samples, values, canceled);
    final int totalQcs = countQcs(samples, canceled);
    final int usableQcs = qcs.size();
    final double missingFraction = 1d - (double) usableQcs / totalQcs;
    if (usableQcs < settings.minimumUsableQcs()) {
      return unavailableBatch(batch, qcs, totalQcs, usableQcs,
          FeatureQcUnavailableReason.BATCH_INSUFFICIENT_USABLE_QCS);
    }
    if (missingFraction >= .5d) {
      return unavailableBatch(batch, qcs, totalQcs, usableQcs,
          FeatureQcUnavailableReason.BATCH_MISSING_QC_THRESHOLD);
    }
    final Fit fit;
    try {
      fit = fit(qcs, values, settings, canceled);
    } catch (final CancellationException exception) {
      throw exception;
    } catch (final RuntimeException exception) {
      return unavailableBatch(batch, qcs, totalQcs, usableQcs,
          FeatureQcUnavailableReason.FIT_FAILED);
    }

    final Map<String, Double> factors = new LinkedHashMap<>();
    final Set<String> boundaries = new LinkedHashSet<>();
    try {
      for (final Sample sample : samples) {
        checkCanceled(canceled);
        final Prediction prediction = predict(fit, sample.order());
        if (!Double.isFinite(prediction.logValue())) {
          return unavailableBatch(batch, qcs, totalQcs, usableQcs,
              FeatureQcUnavailableReason.INVALID_PREDICTION);
        }
        final double factor = Math.exp(globalTarget - prediction.logValue());
        if (!validFactor(factor, settings.maxFactor())) {
          return unavailableBatch(batch, qcs, totalQcs, usableQcs,
              FeatureQcUnavailableReason.FACTOR_OUT_OF_BOUNDS);
        }
        if (prediction.boundary()) boundaries.add(sample.id());
        factors.put(sample.id(), factor);
      }
    } catch (final CancellationException exception) {
      throw exception;
    } catch (final RuntimeException exception) {
      return unavailableBatch(batch, qcs, totalQcs, usableQcs,
          FeatureQcUnavailableReason.INVALID_PREDICTION);
    }
    final double[] before = new double[qcs.size()];
    final double[] after = new double[qcs.size()];
    for (int index = 0; index < qcs.size(); index++) {
      checkCanceled(canceled);
      final double response = values.get(qcs.get(index).id());
      before[index] = response;
      after[index] = response * factors.get(qcs.get(index).id());
    }
    final Validation validation = leaveOneOut(qcs, values, settings, canceled);
    final double[] factorValues = new double[factors.size()];
    int factorIndex = 0;
    for (final double factor : factors.values()) {
      checkCanceled(canceled);
      factorValues[factorIndex++] = factor;
    }
    final List<FeatureQcFitPoint> fitPoints;
    try {
      fitPoints = fitPoints(fit, qcs, values, samples, canceled);
    } catch (final CancellationException exception) {
      throw exception;
    } catch (final RuntimeException exception) {
      return unavailableBatch(batch, qcs, totalQcs, usableQcs,
          FeatureQcUnavailableReason.INVALID_PREDICTION);
    }
    final FeatureQcBatchDiagnostic diagnostic = new FeatureQcBatchDiagnostic(batch, totalQcs,
        usableQcs, fit.x()[0], fit.x()[fit.x().length - 1], boundaries.size(), factors.size(), null,
        rsd(before), rsd(after), before.length, validation.mse(), validation.predictionCount(),
        validation.failureCount(), validation.reason(), minimum(factorValues), maximum(factorValues),
        fitPoints);
    return new BatchResult(factors, boundaries, diagnostic);
  }

  private static @NotNull BatchResult unavailableBatch(@NotNull final String batch,
      @NotNull final List<Sample> usableQcs, final int totalQcs, final int usableQcCount,
      @NotNull final FeatureQcUnavailableReason reason) {
    final Double minimum = usableQcs.isEmpty() ? null : usableQcs.getFirst().order();
    final Double maximum = usableQcs.isEmpty() ? null : usableQcs.getLast().order();
    return new BatchResult(Map.of(), Set.of(), new FeatureQcBatchDiagnostic(batch, totalQcs,
        usableQcCount, minimum, maximum, 0, 0, reason, null, null, 0, null, 0, 0, null, null,
        null, List.of()));
  }

  private static @NotNull Fit fit(@NotNull final List<Sample> qcs,
      @NotNull final Map<String, Double> values, @NotNull final FeatureQcSettings settings,
      @NotNull final BooleanSupplier canceled) {
    final double[] x = new double[qcs.size()];
    final double[] logY = new double[qcs.size()];
    for (int index = 0; index < qcs.size(); index++) {
      checkCanceled(canceled);
      final Sample qc = qcs.get(index);
      x[index] = qc.order();
      logY[index] = Math.log(values.get(qc.id()));
    }
    return new Fit(x, new LoessInterpolator(settings.bandwidth(), settings.robustnessIterations())
        .interpolate(x, logY));
  }

  private static @NotNull Validation leaveOneOut(@NotNull final List<Sample> qcs,
      @NotNull final Map<String, Double> values, @NotNull final FeatureQcSettings settings,
      @NotNull final BooleanSupplier canceled) {
    if (qcs.size() - 1 < settings.minimumUsableQcs()) {
      return new Validation(null, 0, qcs.size(), "fewer than minimum usable QCs after omission");
    }
    double squaredError = 0d;
    int predictions = 0;
    int failures = 0;
    for (int omitted = 0; omitted < qcs.size(); omitted++) {
      checkCanceled(canceled);
      final List<Sample> retained = new ArrayList<>(qcs);
      final Sample leftOut = retained.remove(omitted);
      try {
        final Prediction prediction = predict(fit(retained, values, settings, canceled), leftOut.order());
        if (!Double.isFinite(prediction.logValue())) {
          failures++;
          continue;
        }
        final double error = Math.log(values.get(leftOut.id())) - prediction.logValue();
        squaredError += error * error;
        predictions++;
      } catch (final CancellationException exception) {
        throw exception;
      } catch (final RuntimeException exception) {
        failures++;
      }
    }
    // A partial-fold mean is not comparable with candidates evaluated on all observations.
    return failures != 0 || predictions == 0 || !Double.isFinite(squaredError)
        ? new Validation(null, predictions, failures, "leave-one-out validation incomplete or nonfinite")
        : new Validation(squaredError / predictions, predictions, 0, null);
  }

  private static @NotNull Prediction predict(@NotNull final Fit fit, final double order) {
    if (order < fit.x()[0]) return new Prediction(fit.model().value(fit.x()[0]), true);
    final int last = fit.x().length - 1;
    if (order > fit.x()[last]) return new Prediction(fit.model().value(fit.x()[last]), true);
    return new Prediction(fit.model().value(order), false);
  }

  private static @NotNull List<FeatureQcFitPoint> fitPoints(@NotNull final Fit fit,
      @NotNull final List<Sample> qcs, @NotNull final Map<String, Double> values,
      @NotNull final List<Sample> batchSamples, @NotNull final BooleanSupplier canceled) {
    final Map<Double, FeatureQcFitPoint> points = new java.util.TreeMap<>();
    double batchMinimum = Double.POSITIVE_INFINITY;
    double batchMaximum = Double.NEGATIVE_INFINITY;
    for (final Sample sample : batchSamples) {
      checkCanceled(canceled);
      batchMinimum = Math.min(batchMinimum, sample.order());
      batchMaximum = Math.max(batchMaximum, sample.order());
    }
    addFitPoint(points, fit, batchMinimum, null, canceled);
    addFitPoint(points, fit, batchMaximum, null, canceled);
    for (int index = 0; index < qcs.size(); index++) {
      checkCanceled(canceled);
      final Sample qc = qcs.get(index);
      addFitPoint(points, fit, qc.order(), Math.log(values.get(qc.id())), canceled);
      if (index + 1 < qcs.size()) {
        addFitPoint(points, fit, (qc.order() + qcs.get(index + 1).order()) / 2d, null, canceled);
      }
    }
    return List.copyOf(points.values());
  }

  private static void addFitPoint(@NotNull final Map<Double, FeatureQcFitPoint> points,
      @NotNull final Fit fit, final double time, final Double observedLogQc,
      @NotNull final BooleanSupplier canceled) {
    checkCanceled(canceled);
    final FeatureQcFitPoint existing = points.get(time);
    final Double observed = observedLogQc == null && existing != null ? existing.observedLogQc()
        : observedLogQc;
    final double fitted = predict(fit, time).logValue();
    if (!Double.isFinite(fitted)) throw new IllegalArgumentException("Non-finite fit-curve prediction.");
    points.put(time, new FeatureQcFitPoint(time, observed, fitted));
  }

  private static double medianLogs(@NotNull final Map<String, Double> values,
      @NotNull final BooleanSupplier canceled) {
    final List<Double> logs = new ArrayList<>();
    for (final Double value : values.values()) {
      checkCanceled(canceled);
      if (positiveFinite(value)) logs.add(Math.log(value));
    }
    if (logs.isEmpty()) return Double.NaN;
    final double[] valuesAsArray = new double[logs.size()];
    for (int index = 0; index < logs.size(); index++) {
      checkCanceled(canceled);
      valuesAsArray[index] = logs.get(index);
    }
    return MathUtils.calcMedian(valuesAsArray);
  }

  private static @NotNull List<Sample> usableQcs(@NotNull final List<Sample> samples,
      @NotNull final Map<String, Double> values, @NotNull final BooleanSupplier canceled) {
    final List<Sample> result = new ArrayList<>();
    for (final Sample sample : samples) {
      checkCanceled(canceled);
      if (sample.qc() && positiveFinite(values.get(sample.id()))) result.add(sample);
    }
    result.sort(Comparator.comparingDouble(Sample::order));
    return result;
  }

  private static @NotNull Map<String, List<Sample>> batches(@NotNull final List<Sample> samples,
      @NotNull final BooleanSupplier canceled) {
    final Map<String, List<Sample>> output = new LinkedHashMap<>();
    for (final Sample sample : samples) {
      checkCanceled(canceled);
      output.computeIfAbsent(sample.batch(), ignored -> new ArrayList<>()).add(sample);
    }
    return output;
  }

  private static @NotNull FeatureQcAvailability availability(@NotNull final Set<Integer> rowIds,
      @NotNull final List<Sample> samples, @NotNull final Map<Integer, Map<String, Double>> factors,
      @NotNull final Map<Integer, FeatureQcUnavailableReason> unavailableRows,
      @NotNull final BooleanSupplier canceled) {
    int fully = 0, partial = 0, uncorrected = 0, skipped = 0;
    long correctedPairs = 0, unavailablePairs = 0;
    for (final Integer rowId : rowIds) {
      checkCanceled(canceled);
      if (unavailableRows.containsKey(rowId)) {
        skipped++;
        unavailablePairs += samples.size();
        continue;
      }
      final int count = factors.getOrDefault(rowId, Map.of()).size();
      correctedPairs += count;
      unavailablePairs += samples.size() - count;
      if (count == samples.size()) fully++;
      else if (count == 0) uncorrected++;
      else partial++;
    }
    return new FeatureQcAvailability(fully, partial, uncorrected, skipped, correctedPairs,
        unavailablePairs);
  }

  private static int countQcs(@NotNull final List<Sample> samples,
      @NotNull final BooleanSupplier canceled) {
    int result = 0;
    for (final Sample sample : samples) {
      checkCanceled(canceled);
      if (sample.qc()) result++;
    }
    return result;
  }

  private static void validateInputs(@NotNull final List<Sample> samples,
      @NotNull final Map<Integer, Map<String, Double>> allValues,
      @NotNull final BooleanSupplier canceled) {
    if (samples.isEmpty()) throw new IllegalArgumentException("At least one sample is required.");
    final Set<String> ids = new LinkedHashSet<>();
    final Map<String, Set<Double>> orders = new LinkedHashMap<>();
    for (final Sample sample : samples) {
      checkCanceled(canceled);
      if (sample.id() == null || sample.id().isBlank() || !ids.add(sample.id())) {
        throw new IllegalArgumentException("Sample IDs must be nonblank and unique.");
      }
      if (sample.batch() == null || sample.batch().isBlank() || !Double.isFinite(sample.order())) {
        throw new IllegalArgumentException("Every sample needs a finite order and nonblank batch.");
      }
      if (!orders.computeIfAbsent(sample.batch(), ignored -> new LinkedHashSet<>()).add(sample.order())) {
        throw new IllegalArgumentException("Sample order is tied in batch '" + sample.batch() + "'.");
      }
    }
    for (final Map.Entry<Integer, Map<String, Double>> row : allValues.entrySet()) {
      checkCanceled(canceled);
      if (row.getKey() == null || row.getKey() <= 0 || row.getValue() == null) {
        throw new IllegalArgumentException("Row IDs must be positive and row abundance maps nonnull.");
      }
      for (final String sampleId : row.getValue().keySet()) {
        checkCanceled(canceled);
        if (!ids.contains(sampleId)) {
          throw new IllegalArgumentException("Abundance input references unknown sample '" + sampleId + "'.");
        }
      }
    }
  }

  private static boolean positiveFinite(final Double value) {
    return value != null && Double.isFinite(value) && value > 0d;
  }

  private static boolean validFactor(final double factor, final double maxFactor) {
    return Double.isFinite(factor) && factor >= 1d / maxFactor && factor <= maxFactor;
  }

  private static Double rsd(final double[] values) {
    if (values.length < 2) return null;
    double sum = 0d;
    for (final double value : values) sum += value;
    final double mean = sum / values.length;
    if (!Double.isFinite(mean) || mean == 0d) return null;
    double variance = 0d;
    for (final double value : values) variance += Math.pow(value - mean, 2d);
    final double result = Math.sqrt(variance / (values.length - 1)) / Math.abs(mean);
    return Double.isFinite(result) ? result : null;
  }

  private static Double minimum(final double[] values) {
    return values.length == 0 ? null : java.util.Arrays.stream(values).min().orElseThrow();
  }

  private static Double maximum(final double[] values) {
    return values.length == 0 ? null : java.util.Arrays.stream(values).max().orElseThrow();
  }

  private static void checkCanceled(@NotNull final BooleanSupplier canceled) {
    if (canceled.getAsBoolean()) throw new CancellationException();
  }

  private record Fit(@NotNull double[] x, @NotNull UnivariateFunction model) {
  }
  private record Prediction(double logValue, boolean boundary) {
  }
  private record Validation(Double mse, int predictionCount, int failureCount, String reason) {
  }
  private record BatchResult(@NotNull Map<String, Double> factors, @NotNull Set<String> boundaryIds,
                             @NotNull FeatureQcBatchDiagnostic diagnostic) {
  }
}
