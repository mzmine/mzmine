/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 */
package io.github.mzmine.modules.dataprocessing.norm_intensity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.apache.commons.math3.exception.MathIllegalArgumentException;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.DecompositionSolver;
import org.apache.commons.math3.linear.QRDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** One QC-RLSC pipeline for every feature and batch, with automatic or fixed smoothing. */
public final class FeatureQcDriftNormalization {

  private static final double WEIGHT_EPSILON = 1e-14;
  private static final double MAX_RESPONSE = Math.sqrt(Double.MAX_VALUE) / 4d;
  // Candidate scores are squared normalized responses. This is several thousand squared ulps,
  // enough to collapse numerical exact-fit noise without merging scientifically distinct errors.
  private static final double MSE_TIE_ABSOLUTE_TOLERANCE = 4096d * Math.ulp(1d) * Math.ulp(1d);

  private FeatureQcDriftNormalization() {
  }

  /** The order coordinate establishes acquisition sequence; fitting uses ordinal positions per batch. */
  public record Sample(@NotNull String id, @NotNull String batch, double order, boolean qc) {
  }

  public static @NotNull FeatureQcResult calculate(@NotNull final List<Sample> samples,
      @NotNull final Map<Integer, Map<String, Double>> allValues,
      @NotNull final FeatureQcSettings settings) {
    return calculate(samples, allValues, settings, () -> false);
  }

  /**
   * Calculates multiplicative correction factors. Inputs include QC and study abundances at the same upstream stage;
   * only positive finite values are modeled or normalized.
   */
  public static @NotNull FeatureQcResult calculate(@NotNull final List<Sample> samples,
      @NotNull final Map<Integer, Map<String, Double>> allValues,
      @NotNull final FeatureQcSettings settings, @NotNull final BooleanSupplier canceled) {
    validateInputs(samples, allValues, canceled);
    return calculatePrepared(prepareSamples(samples, settings, canceled), allValues, settings, canceled);
  }

  private static @NotNull FeatureQcResult calculatePrepared(@NotNull final List<Sample> samples,
      @NotNull final Map<Integer, Map<String, Double>> allValues,
      @NotNull final FeatureQcSettings settings, @NotNull final BooleanSupplier canceled) {
    final Map<String, List<Sample>> batches = batches(samples, canceled);
    final int totalQcs = countQcs(samples, canceled);

    final Map<Integer, Map<String, Double>> factors = new LinkedHashMap<>();
    final Map<Integer, Set<String>> boundaries = new LinkedHashMap<>();
    final Map<Integer, FeatureQcUnavailableReason> unavailableRows = new LinkedHashMap<>();
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
      final Map<String, Double> rowFactors = new LinkedHashMap<>();
      final Set<String> rowBoundaries = new LinkedHashSet<>();
      final Map<String, FeatureQcBatchDiagnostic> batchDiagnostics = new LinkedHashMap<>();
      for (final Map.Entry<String, List<Sample>> batch : batches.entrySet()) {
        checkCanceled(canceled);
        final BatchResult result = calculateBatch(batch.getKey(), batch.getValue(), values,
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
    final FeatureQcDiagnostics diagnostics = new FeatureQcDiagnostics(
        "Tricube local linear/quadratic regression; bisquare residual reweighting; natural cubic "
            + "spline; raw response; " + (settings.automaticSmoothing()
            ? "Dunn 2011 LOOCV span/degree selection" : "fixed span and degree, no LOOCV search"),
        "ordinal acquisition order within each batch; nearest QC boundary outside fitted range",
        "divide by fitted QC response, rescale to arithmetic mean of usable QCs in each batch",
        settings, availability, rowDiagnostics);
    return new FeatureQcResult(samples, factors, boundaries, unavailableRows, diagnostics);
  }

  private static @NotNull BatchResult calculateBatch(@NotNull final String batch,
      @NotNull final List<Sample> samples, @NotNull final Map<String, Double> values,
      @NotNull final FeatureQcSettings settings, @NotNull final BooleanSupplier canceled) {
    final List<Sample> qcs = usableQcs(samples, values, canceled);
    final int totalQcs = countQcs(samples, canceled);
    final int usableQcs = qcs.size();
    if (usableQcs < settings.minimumUsableQcs()) {
      return unavailableBatch(batch, qcs, totalQcs, usableQcs,
          FeatureQcUnavailableReason.BATCH_INSUFFICIENT_USABLE_QCS);
    }
    if (1d - (double) usableQcs / totalQcs > .5d) {
      return unavailableBatch(batch, qcs, totalQcs, usableQcs,
          FeatureQcUnavailableReason.BATCH_MISSING_QC_THRESHOLD);
    }
    final double[] x = new double[usableQcs];
    final double[] y = new double[usableQcs];
    double target = 0d;
    for (int i = 0; i < usableQcs; i++) {
      checkCanceled(canceled);
      x[i] = qcs.get(i).order();
      y[i] = values.get(qcs.get(i).id());
      target += y[i] / usableQcs;
    }
    final Fit fit;
    try {
      fit = fit(x, y, settings, canceled);
    } catch (final CancellationException exception) {
      throw exception;
    } catch (final RuntimeException exception) {
      return unavailableBatch(batch, qcs, totalQcs, usableQcs, FeatureQcUnavailableReason.FIT_FAILED);
    }

    final Map<String, Double> factors = new LinkedHashMap<>();
    final Set<String> boundaries = new LinkedHashSet<>();
    final List<FeatureQcFitPoint> points;
    try {
      for (final Sample sample : samples) {
        checkCanceled(canceled);
        final Prediction prediction = predict(fit, sample.order());
        if (!Double.isFinite(prediction.value()) || prediction.value() <= 0d) {
          return unavailableBatch(batch, qcs, totalQcs, usableQcs,
              FeatureQcUnavailableReason.INVALID_PREDICTION);
        }
        final double factor = target / prediction.value();
        if (!validFactor(factor, settings.maxFactor())) {
          return unavailableBatch(batch, qcs, totalQcs, usableQcs,
              FeatureQcUnavailableReason.FACTOR_OUT_OF_BOUNDS);
        }
        factors.put(sample.id(), factor);
        if (prediction.boundary()) boundaries.add(sample.id());
      }
      points = fitPoints(fit, qcs, values, samples, canceled);
    } catch (final CancellationException exception) {
      throw exception;
    } catch (final RuntimeException exception) {
      return unavailableBatch(batch, qcs, totalQcs, usableQcs,
          FeatureQcUnavailableReason.INVALID_PREDICTION);
    }
    final double[] after = new double[y.length];
    for (int i = 0; i < y.length; i++) {
      checkCanceled(canceled);
      after[i] = y[i] * factors.get(qcs.get(i).id());
    }
    final double[] factorValues = factors.values().stream().mapToDouble(Double::doubleValue).toArray();
    final FeatureQcBatchDiagnostic diagnostic = new FeatureQcBatchDiagnostic(batch, totalQcs,
        usableQcs, x[0], x[x.length - 1], boundaries.size(), factors.size(), null, rsd(y),
        rsd(after), y.length, fit.crossValidationMse(), settings.automaticSmoothing() ? y.length : 0,
        0, settings.automaticSmoothing() ? null : "fixed smoothing; cross-validation not performed",
        minimum(factorValues), maximum(factorValues), points, fit.degree(), fit.neighborhoodSize(),
        fit.span(), target, "raw response; " + (settings.automaticSmoothing()
        ? "model-selection LOOCV error, not independent validation" : "fixed smoothing, no LOOCV score"));
    return new BatchResult(factors, boundaries, diagnostic);
  }

  private static @NotNull List<Sample> prepareSamples(@NotNull final List<Sample> samples,
      @NotNull final FeatureQcSettings settings, @NotNull final BooleanSupplier canceled) {
    final Map<String, Sample> prepared = new LinkedHashMap<>();
    for (final List<Sample> batch : batches(samples, canceled).values()) {
      final List<Sample> ordered = new ArrayList<>(batch);
      ordered.sort(Comparator.comparingDouble(Sample::order));
      int qcs = 0;
      for (int index = 0; index < ordered.size(); index++) {
        checkCanceled(canceled);
        final Sample sample = ordered.get(index);
        final boolean reference = sample.qc() && ++qcs > settings.initialQcsToExclude();
        final double coordinate = index + 1d;
        prepared.put(sample.id(), new Sample(sample.id(), sample.batch(), coordinate, reference));
      }
    }
    return samples.stream().map(sample -> prepared.get(sample.id())).toList();
  }

  private static @NotNull BatchResult unavailableBatch(@NotNull final String batch,
      @NotNull final List<Sample> usableQcs, final int totalQcs, final int usableQcCount,
      @NotNull final FeatureQcUnavailableReason reason) {
    final Double minimum = usableQcs.isEmpty() ? null : usableQcs.getFirst().order();
    final Double maximum = usableQcs.isEmpty() ? null : usableQcs.getLast().order();
    return new BatchResult(Map.of(), Set.of(), new FeatureQcBatchDiagnostic(batch, totalQcs,
        usableQcCount, minimum, maximum, 0, 0, reason, null, null, 0, null, 0, 0, null, null,
        null, List.of(), null, null, null, null, "not fitted"));
  }

  private static @NotNull Prediction predict(@NotNull final Fit fit, final double order) {
    if (order < fit.coordinates[0]) return new Prediction(fit.curve().value(fit.coordinates[0]), true);
    final int last = fit.coordinates.length - 1;
    if (order > fit.coordinates[last]) return new Prediction(fit.curve().value(fit.coordinates[last]), true);
    return new Prediction(fit.curve().value(order), false);
  }

  private static @NotNull List<FeatureQcFitPoint> fitPoints(@NotNull final Fit fit,
      @NotNull final List<Sample> qcs, @NotNull final Map<String, Double> values,
      @NotNull final List<Sample> batchSamples,
      @NotNull final BooleanSupplier canceled) {
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
      addFitPoint(points, fit, qc.order(), values.get(qc.id()), canceled);
      if (index + 1 < qcs.size()) {
        addFitPoint(points, fit, (qc.order() + qcs.get(index + 1).order()) / 2d, null, canceled);
      }
    }
    return List.copyOf(points.values());
  }

  private static void addFitPoint(@NotNull final Map<Double, FeatureQcFitPoint> points,
      @NotNull final Fit fit, final double time, @Nullable final Double observedQc,
      @NotNull final BooleanSupplier canceled) {
    checkCanceled(canceled);
    final FeatureQcFitPoint existing = points.get(time);
    final Double observed = observedQc == null && existing != null ? existing.observedQc()
        : observedQc;
    final double fitted = predict(fit, time).value();
    if (!Double.isFinite(fitted)) throw new IllegalArgumentException("Non-finite fit-curve prediction.");
    points.put(time, new FeatureQcFitPoint(time, observed, fitted));
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

  private static boolean positiveFinite(@Nullable final Double value) {
    return value != null && Double.isFinite(value) && value > 0d;
  }

  private static boolean validFactor(final double factor, final double maxFactor) {
    return Double.isFinite(factor) && factor >= 1d / maxFactor && factor <= maxFactor;
  }

  private static @Nullable Double rsd(@NotNull final double[] values) {
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

  private static @Nullable Double minimum(@NotNull final double[] values) {
    return values.length == 0 ? null : java.util.Arrays.stream(values).min().orElseThrow();
  }

  private static @Nullable Double maximum(@NotNull final double[] values) {
    return values.length == 0 ? null : java.util.Arrays.stream(values).max().orElseThrow();
  }

  private static void checkCanceled(@NotNull final BooleanSupplier canceled) {
    if (canceled.getAsBoolean()) throw new CancellationException();
  }

  private record Prediction(double value, boolean boundary) {
  }
  private record BatchResult(@NotNull Map<String, Double> factors, @NotNull Set<String> boundaryIds,
                             @NotNull FeatureQcBatchDiagnostic diagnostic) {
  }

  /** Result of selecting and fitting one QC-RLSC curve. */
  record Fit(@NotNull double[] coordinates, @NotNull UnivariateFunction curve, int degree,
             int neighborhoodSize, double span, @Nullable Double crossValidationMse) {

    Fit {
      coordinates = coordinates.clone();
    }

    @Override
    public @NotNull double[] coordinates() {
      return coordinates.clone();
    }

    /** Predicts at a coordinate, clamping production predictions to the fitted QC boundaries. */
    public double predictClamped(final double coordinate) {
      return curve.value(Math.max(coordinates[0], Math.min(coordinate,
          coordinates[coordinates.length - 1])));
    }
  }

  /**
   * Fits positive finite responses at strictly increasing QC coordinates.
   *
   * @throws IllegalArgumentException if the inputs or fixed neighborhood are invalid, or automatic
   *                                  selection finds no complete LOOCV score
   * @throws CancellationException if {@code canceled} becomes true
   */
  static @NotNull Fit fit(@NotNull final double[] x, @NotNull final double[] y,
      @NotNull final FeatureQcSettings settings, @NotNull final BooleanSupplier canceled) {
    final int robustnessIterations = settings.robustnessIterations();
    validateFitInputs(x, y, settings, canceled);
    // The median is stable in the presence of a single extreme QC and keeps ordinary responses
    // close to one. It affects neither local-polynomial predictions nor candidate ordering.
    final double responseScale = median(y);
    final double[] normalizedY = Arrays.stream(y).map(value -> value / responseScale).toArray();
    if (Arrays.stream(normalizedY).anyMatch(value -> !Double.isFinite(value))) {
      throw new IllegalArgumentException("QC-RLSC response range is too large for finite fitting");
    }
    final Geometry geometry = Geometry.create(x);
    Candidate best = null;
    // decision: fixed smoothing bypasses the entire candidate/fold search.
    if (settings.automaticSmoothing()) {
      for (int degree = 1; degree <= 2; degree++) {
        // degree + 2 allows a zero-weight boundary neighbor. Distance ties can remove more
        // support; each local fit still checks the actual rank and positive weights.
        for (int neighborhoodSize = degree + 2; neighborhoodSize <= x.length; neighborhoodSize++) {
          checkCanceled(canceled);
          final Double mse = leaveOneOutMse(geometry, normalizedY, degree, neighborhoodSize,
              robustnessIterations,
              canceled);
          if (mse == null) {
            continue;
          }
          final Candidate candidate = new Candidate(degree, neighborhoodSize, mse);
          if (best == null || candidate.isBetterThan(best)) {
            best = candidate;
          }
        }
      }
      if (best == null) {
        throw new IllegalArgumentException("No QC-RLSC candidate produced a complete finite "
            + "leave-one-out score");
      }
    } else {
      // Avoid promoting k/n to k+1 neighbors because of multiplication roundoff (e.g. 7/25).
      final int neighbors = (int) Math.ceil(Math.nextDown(settings.bandwidth() * x.length));
      if (neighbors < settings.polynomialDegree() + 2) {
        throw new IllegalArgumentException("Fixed smoothing needs at least degree + 2 neighbors");
      }
      best = new Candidate(settings.polynomialDegree(), neighbors, Double.NaN);
    }

    final double[] fittedAtKnots = smoothAtKnots(geometry, normalizedY, best.degree,
        best.neighborhoodSize,
        robustnessIterations, -1, canceled);
    final PolynomialSplineFunction spline;
    try {
      spline = new SplineInterpolator().interpolate(geometry.x, fittedAtKnots);
    } catch (final MathIllegalArgumentException exception) {
      throw new IllegalArgumentException("Cannot construct QC-RLSC spline", exception);
    }
    final Double rawMse = settings.automaticSmoothing()
        ? best.mse * responseScale * responseScale : null;
    if (rawMse != null && !Double.isFinite(rawMse)) {
      throw new IllegalArgumentException("QC-RLSC cross-validation error exceeds finite range");
    }
    return new Fit(x, coordinate -> responseScale * spline.value(coordinate), best.degree,
        best.neighborhoodSize, (double) best.neighborhoodSize / x.length, rawMse);
  }

  private static @Nullable Double leaveOneOutMse(@NotNull final Geometry geometry, @NotNull final double[] y, final int degree,
      final int neighborhoodSize, final int robustnessIterations,
      final @NotNull BooleanSupplier canceled) {
    double squaredError = 0d;
    for (int leftOut = 0; leftOut < geometry.x.length; leftOut++) {
      checkCanceled(canceled);
      try {
        final double[] robustWeights = robustWeights(geometry, y, degree, neighborhoodSize,
            robustnessIterations, leftOut, canceled);
        final double predicted = localPrediction(geometry, y, leftOut, degree, neighborhoodSize,
            leftOut, robustWeights, canceled);
        final double error = y[leftOut] - predicted;
        if (!Double.isFinite(error)) {
          return null;
        }
        squaredError += error * error;
      } catch (final SingularFitException exception) {
        // A candidate must predict every omitted QC; partial scores are deliberately not compared.
        return null;
      }
    }
    return Double.isFinite(squaredError) ? squaredError / geometry.x.length : null;
  }

  private static @NotNull double[] smoothAtKnots(@NotNull final Geometry geometry, @NotNull final double[] y, final int degree,
      final int neighborhoodSize, final int robustnessIterations, final int excluded,
      final @NotNull BooleanSupplier canceled) {
    final double[] robustWeights = robustWeights(geometry, y, degree, neighborhoodSize,
        robustnessIterations, excluded, canceled);
    return smoothAtKnotsWithoutRobustUpdate(geometry, y, degree, neighborhoodSize, excluded,
        robustWeights, canceled);
  }

  private static @NotNull double[] robustWeights(@NotNull final Geometry geometry, @NotNull final double[] y, final int degree,
      final int neighborhoodSize, final int robustnessIterations, final int excluded,
      final @NotNull BooleanSupplier canceled) {
    final double[] robust = new double[geometry.x.length];
    Arrays.fill(robust, 1d);
    if (excluded >= 0) {
      robust[excluded] = 0d;
    }
    for (int iteration = 0; iteration < robustnessIterations; iteration++) {
      checkCanceled(canceled);
      final double[] fitted = smoothAtKnotsWithoutRobustUpdate(geometry, y, degree,
          neighborhoodSize, excluded, robust, canceled);
      final double[] residuals = new double[geometry.x.length - (excluded >= 0 ? 1 : 0)];
      int residualIndex = 0;
      for (int index = 0; index < geometry.x.length; index++) {
        if (index != excluded) {
          residuals[residualIndex++] = Math.abs(y[index] - fitted[index]);
        }
      }
      final double median = median(residuals);
      // In a LOOCV fold, derive the scale only from training responses. Including y[excluded]
      // would let an extreme held-out QC alter the robust fit used to predict itself.
      final double relativeTolerance = WEIGHT_EPSILON * trainingResponseScale(y, excluded);
      if (!Double.isFinite(median) || median <= relativeTolerance) {
        break;
      }
      final double scale = 6d * median;
      for (int index = 0; index < geometry.x.length; index++) {
        if (index == excluded) {
          continue;
        }
        final double u = Math.abs(y[index] - fitted[index]) / scale;
        final double complement = 1d - u * u;
        robust[index] = u >= 1d ? 0d : complement * complement;
      }
    }
    return robust;
  }

  private static @NotNull double[] smoothAtKnotsWithoutRobustUpdate(@NotNull final Geometry geometry,
      @NotNull final double[] y, final int degree, final int neighborhoodSize, final int excluded,
      @NotNull final double[] robust, final @NotNull BooleanSupplier canceled) {
    final double[] fitted = new double[geometry.x.length];
    for (int index = 0; index < geometry.x.length; index++) {
      checkCanceled(canceled);
      if (index != excluded) {
        fitted[index] = localPrediction(geometry, y, index, degree, neighborhoodSize, excluded,
            robust, canceled);
      }
    }
    return fitted;
  }

  private static double localPrediction(@NotNull final Geometry geometry, @NotNull final double[] y,
      final int queryIndex,
      final int degree, final int neighborhoodSize, final int excluded, @NotNull final double[] robust,
      final @NotNull BooleanSupplier canceled) {
    final int[] neighborOrder = geometry.neighborOrder[queryIndex];
    int neighborCount = 0;
    int lastNeighbor = -1;
    for (final int index : neighborOrder) {
      if (index != excluded) {
        lastNeighbor = index;
        if (++neighborCount == neighborhoodSize) {
          break;
        }
      }
    }
    if (neighborCount < degree + 1) {
      throw new SingularFitException();
    }
    final double query = geometry.x[queryIndex];
    final double maximumDistance = Math.abs(geometry.x[lastNeighbor] - query);
    if (maximumDistance <= WEIGHT_EPSILON) {
      throw new SingularFitException();
    }
    final double center = query;
    final double scale = maximumDistance;
    final double[] moments = new double[2 * degree + 1];
    final double[] responseMoments = new double[degree + 1];
    int positiveWeights = 0;
    int row = 0;
    for (final int index : neighborOrder) {
      if (index == excluded) {
        continue;
      }
      checkCanceled(canceled);
      final double distanceRatio = Math.abs(geometry.x[index] - query) / maximumDistance;
      final double tricube = tricubeWeight(distanceRatio);
      final double weight = tricube * robust[index];
      if (weight > WEIGHT_EPSILON) {
        positiveWeights++;
      }
      final double normalized = (geometry.x[index] - center) / scale;
      double power = 1d;
      for (int moment = 0; moment <= 2 * degree; moment++) {
        moments[moment] += weight * power;
        if (moment <= degree) {
          responseMoments[moment] += weight * y[index] * power;
        }
        power *= normalized;
      }
      if (++row == neighborCount) {
        break;
      }
    }
    if (positiveWeights < degree + 1) {
      throw new SingularFitException();
    }
    final double momentPrediction = solveCenteredMoments(moments, responseMoments, degree);
    if (Double.isFinite(momentPrediction)) {
      return momentPrediction;
    }
    return qrFallbackPrediction(geometry, y, queryIndex, degree, neighborCount, excluded, robust,
        canceled, maximumDistance);
  }

  /** Fast centered weighted solves; QR remains the fallback for ill-conditioned neighborhoods. */
  private static double solveCenteredMoments(@NotNull final double[] moments, @NotNull final double[] response,
      final int degree) {
    if (degree == 1) {
      final double determinant = moments[0] * moments[2] - moments[1] * moments[1];
      final double scale = Math.max(Math.abs(moments[0] * moments[2]), moments[1] * moments[1]);
      return Math.abs(determinant) <= 1e-12 * scale ? Double.NaN
          : (response[0] * moments[2] - response[1] * moments[1]) / determinant;
    }
    final double[][] matrix = {{moments[0], moments[1], moments[2]},
        {moments[1], moments[2], moments[3]}, {moments[2], moments[3], moments[4]}};
    final double[] vector = response.clone();
    double largest = 0d;
    for (final double[] matrixRow : matrix) {
      for (final double value : matrixRow) {
        largest = Math.max(largest, Math.abs(value));
      }
    }
    for (int pivot = 0; pivot < 3; pivot++) {
      int selected = pivot;
      for (int row = pivot + 1; row < 3; row++) {
        if (Math.abs(matrix[row][pivot]) > Math.abs(matrix[selected][pivot])) {
          selected = row;
        }
      }
      if (Math.abs(matrix[selected][pivot]) <= 1e-12 * largest) {
        return Double.NaN;
      }
      final double[] swap = matrix[pivot];
      matrix[pivot] = matrix[selected];
      matrix[selected] = swap;
      final double vectorSwap = vector[pivot];
      vector[pivot] = vector[selected];
      vector[selected] = vectorSwap;
      for (int row = pivot + 1; row < 3; row++) {
        final double factor = matrix[row][pivot] / matrix[pivot][pivot];
        for (int column = pivot; column < 3; column++) {
          matrix[row][column] -= factor * matrix[pivot][column];
        }
        vector[row] -= factor * vector[pivot];
      }
    }
    for (int row = 2; row >= 0; row--) {
      for (int column = row + 1; column < 3; column++) {
        vector[row] -= matrix[row][column] * vector[column];
      }
      vector[row] /= matrix[row][row];
    }
    return Double.isFinite(vector[0]) ? vector[0] : Double.NaN;
  }

  private static double qrFallbackPrediction(@NotNull final Geometry geometry, @NotNull final double[] y,
      final int queryIndex, final int degree, final int neighborCount, final int excluded,
      @NotNull final double[] robust, final @NotNull BooleanSupplier canceled,
      final double maximumDistance) {
    final double query = geometry.x[queryIndex];
    final int[] neighborOrder = geometry.neighborOrder[queryIndex];
    final double[][] design = new double[neighborCount][degree + 1];
    final double[] response = new double[neighborCount];
    int row = 0;
    for (final int index : neighborOrder) {
      if (index == excluded) {
        continue;
      }
      checkCanceled(canceled);
      final double distanceRatio = Math.abs(geometry.x[index] - query) / maximumDistance;
      final double tricube = tricubeWeight(distanceRatio);
      final double rootWeight = Math.sqrt(tricube * robust[index]);
      final double normalized = (geometry.x[index] - query) / maximumDistance;
      double power = 1d;
      for (int column = 0; column <= degree; column++) {
        design[row][column] = rootWeight * power;
        power *= normalized;
      }
      response[row] = rootWeight * y[index];
      if (++row == neighborCount) {
        break;
      }
    }
    try {
      final RealMatrix matrix = new Array2DRowRealMatrix(design, false);
      final DecompositionSolver solver = new QRDecomposition(matrix, 1e-12).getSolver();
      if (!solver.isNonSingular()) {
        throw new SingularFitException();
      }
      final double prediction = solver.solve(new ArrayRealVector(response, false)).getEntry(0);
      if (!Double.isFinite(prediction)) {
        throw new SingularFitException();
      }
      return prediction;
    } catch (final RuntimeException exception) {
      throw new SingularFitException();
    }
  }


  private static double tricubeWeight(final double distanceRatio) {
    if (distanceRatio >= 1d) return 0d;
    final double complement = 1d - distanceRatio * distanceRatio * distanceRatio;
    return complement * complement * complement;
  }

  private static double median(@NotNull final double[] values) {
    final double[] sorted = values.clone();
    Arrays.sort(sorted);
    final int middle = sorted.length / 2;
    return sorted.length % 2 == 0 ? (sorted[middle - 1] + sorted[middle]) / 2d : sorted[middle];
  }

  private static double trainingResponseScale(@NotNull final double[] y, final int excluded) {
    final double[] training = new double[y.length - (excluded >= 0 ? 1 : 0)];
    int trainingIndex = 0;
    for (int index = 0; index < y.length; index++) {
      if (index != excluded) {
        training[trainingIndex++] = Math.abs(y[index]);
      }
    }
    return median(training);
  }

  private static void validateFitInputs(@NotNull final double[] x, @NotNull final double[] y,
      @NotNull final FeatureQcSettings settings, @NotNull final BooleanSupplier canceled) {
    if (x.length != y.length || x.length < 3) {
      throw new IllegalArgumentException(
          "QC-RLSC requires at least three paired QC coordinates and responses");
    }
    for (int index = 0; index < x.length; index++) {
      checkCanceled(canceled);
      if (!Double.isFinite(x[index]) || !Double.isFinite(y[index]) || y[index] <= 0d) {
        throw new IllegalArgumentException(
            "QC-RLSC requires finite coordinates and positive finite responses");
      }
      if (settings.automaticSmoothing() && y[index] > MAX_RESPONSE) {
        throw new IllegalArgumentException("QC-RLSC response is too large for finite squared "
            + "cross-validation errors");
      }
      if (index > 0 && x[index] <= x[index - 1]) {
        throw new IllegalArgumentException("QC-RLSC coordinates must be strictly increasing");
      }
    }
  }

  /** Cached coordinate geometry shared by every candidate and leave-one-out fold. */
  private record Geometry(@NotNull double[] x, @NotNull int[][] neighborOrder) {

    private static @NotNull Geometry create(@NotNull final double[] x) {
      final int[][] order = new int[x.length][];
      for (int query = 0; query < x.length; query++) {
        final int queryIndex = query;
        order[query] = java.util.stream.IntStream.range(0, x.length).boxed()
            .sorted((left, right) -> {
              final int comparison = Double.compare(Math.abs(x[left] - x[queryIndex]),
                  Math.abs(x[right] - x[queryIndex]));
              return comparison != 0 ? comparison : Integer.compare(left, right);
            }).mapToInt(Integer::intValue).toArray();
      }
      return new Geometry(x, order);
    }
  }

  private record Candidate(int degree, int neighborhoodSize, double mse) {

    private boolean isBetterThan(@NotNull final Candidate other) {
      final double tolerance = Math.max(MSE_TIE_ABSOLUTE_TOLERANCE,
          64d * Math.ulp(Math.max(Math.abs(mse), Math.abs(other.mse))));
      if (mse < other.mse - tolerance) {
        return true;
      }
      if (mse > other.mse + tolerance) {
        return false;
      }
      // Deterministic ties prefer the smoother curve, then the lower polynomial degree.
      if (neighborhoodSize != other.neighborhoodSize) {
        return neighborhoodSize > other.neighborhoodSize;
      }
      return degree < other.degree;
    }
  }

  private static final class SingularFitException extends RuntimeException {
  }
}
