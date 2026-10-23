/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 */
package io.github.mzmine.modules.dataprocessing.norm_intensity;

import java.util.Arrays;
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
import org.apache.commons.math3.linear.RealVector;
import org.jetbrains.annotations.NotNull;

/**
 * QC-RLSC local-polynomial smoother following the candidate-selection approach described by Dunn
 * et al. (2011). The fit is intentionally independent of feature-list state so it can be applied
 * separately to each feature and batch.
 *
 * <p>Candidate neighborhoods are selected by complete leave-one-out cross validation. The number
 * of robust bisquare reweighting iterations is configurable; Dunn et al. describe robust fitting
 * but do not prescribe a universal iteration count, so callers choose it explicitly.
 */
public final class DunnQcRlsc {

  private static final double WEIGHT_EPSILON = 1e-14;
  private static final double MAX_RESPONSE = Math.sqrt(Double.MAX_VALUE) / 4d;
  // Candidate scores are squared normalized responses. This is several thousand squared ulps,
  // enough to collapse numerical exact-fit noise without merging scientifically distinct errors.
  private static final double MSE_TIE_ABSOLUTE_TOLERANCE = 4096d * Math.ulp(1d) * Math.ulp(1d);

  private DunnQcRlsc() {
  }

  /** Result of selecting and fitting one QC-RLSC curve. */
  public record Fit(double[] coordinates, @NotNull UnivariateFunction curve, int degree,
                    int neighborhoodSize, double span, double crossValidationMse) {

    public Fit {
      coordinates = coordinates.clone();
    }

    @Override
    public double[] coordinates() {
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
   * @throws IllegalArgumentException if the input is invalid or no candidate has a complete LOOCV
   *                                  score
   * @throws CancellationException if {@code canceled} becomes true
   */
  public static @NotNull Fit fit(@NotNull final double[] x, @NotNull final double[] y,
      final int robustnessIterations, @NotNull final BooleanSupplier canceled) {
    validate(x, y, robustnessIterations, canceled);
    // The median is stable in the presence of a single extreme QC and keeps ordinary responses
    // close to one. It affects neither local-polynomial predictions nor candidate ordering.
    final double responseScale = median(y);
    final double[] normalizedY = Arrays.stream(y).map(value -> value / responseScale).toArray();
    if (Arrays.stream(normalizedY).anyMatch(value -> !Double.isFinite(value))) {
      throw new IllegalArgumentException("QC-RLSC response range is too large for finite fitting");
    }
    final Geometry geometry = Geometry.create(x);
    Candidate best = null;
    for (int degree = 1; degree <= 2; degree++) {
      // A k-nearest tricube neighborhood has at most one zero-weight boundary point at a query.
      // degree + 2 therefore leaves degree + 1 positive-weight observations in the normal case.
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

    final double[] fittedAtKnots = smoothAtKnots(geometry, normalizedY, best.degree,
        best.neighborhoodSize,
        robustnessIterations, -1, canceled);
    final PolynomialSplineFunction spline;
    try {
      spline = new SplineInterpolator().interpolate(geometry.x, fittedAtKnots);
    } catch (final MathIllegalArgumentException exception) {
      throw new IllegalArgumentException("Cannot construct QC-RLSC spline", exception);
    }
    final double rawMse = best.mse * responseScale * responseScale;
    if (!Double.isFinite(rawMse)) {
      throw new IllegalArgumentException("QC-RLSC cross-validation error exceeds finite range");
    }
    return new Fit(x, coordinate -> responseScale * spline.value(coordinate), best.degree,
        best.neighborhoodSize, (double) best.neighborhoodSize / x.length, rawMse);
  }

  private static Double leaveOneOutMse(final Geometry geometry, final double[] y, final int degree,
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

  private static double[] smoothAtKnots(final Geometry geometry, final double[] y, final int degree,
      final int neighborhoodSize, final int robustnessIterations, final int excluded,
      final @NotNull BooleanSupplier canceled) {
    final double[] robustWeights = robustWeights(geometry, y, degree, neighborhoodSize,
        robustnessIterations, excluded, canceled);
    final double[] fitted = new double[geometry.x.length];
    for (int index = 0; index < geometry.x.length; index++) {
      checkCanceled(canceled);
      if (index == excluded) {
        continue;
      }
      fitted[index] = localPrediction(geometry, y, index, degree, neighborhoodSize, excluded,
          robustWeights, canceled);
    }
    return fitted;
  }

  private static double[] robustWeights(final Geometry geometry, final double[] y, final int degree,
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

  private static double[] smoothAtKnotsWithoutRobustUpdate(final Geometry geometry,
      final double[] y, final int degree, final int neighborhoodSize, final int excluded,
      final double[] robust, final @NotNull BooleanSupplier canceled) {
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

  private static double localPrediction(final Geometry geometry, final double[] y,
      final int queryIndex,
      final int degree, final int neighborhoodSize, final int excluded, final double[] robust,
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
  private static double solveCenteredMoments(final double[] moments, final double[] response,
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

  private static double qrFallbackPrediction(final Geometry geometry, final double[] y,
      final int queryIndex, final int degree, final int neighborCount, final int excluded,
      final double[] robust, final @NotNull BooleanSupplier canceled,
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

  private static double median(final double[] values) {
    final double[] sorted = values.clone();
    Arrays.sort(sorted);
    final int middle = sorted.length / 2;
    return sorted.length % 2 == 0 ? (sorted[middle - 1] + sorted[middle]) / 2d : sorted[middle];
  }

  private static double trainingResponseScale(final double[] y, final int excluded) {
    final double[] training = new double[y.length - (excluded >= 0 ? 1 : 0)];
    int trainingIndex = 0;
    for (int index = 0; index < y.length; index++) {
      if (index != excluded) {
        training[trainingIndex++] = Math.abs(y[index]);
      }
    }
    return median(training);
  }

  private static void validate(final double[] x, final double[] y, final int robustnessIterations,
      final @NotNull BooleanSupplier canceled) {
    if (x.length != y.length || x.length < 3) {
      throw new IllegalArgumentException(
          "QC-RLSC requires at least three paired QC coordinates and responses");
    }
    if (robustnessIterations < 0) {
      throw new IllegalArgumentException("Robustness iterations must not be negative");
    }
    for (int index = 0; index < x.length; index++) {
      checkCanceled(canceled);
      if (!Double.isFinite(x[index]) || !Double.isFinite(y[index]) || y[index] <= 0d) {
        throw new IllegalArgumentException(
            "QC-RLSC requires finite coordinates and positive finite responses");
      }
      if (y[index] > MAX_RESPONSE) {
        throw new IllegalArgumentException("QC-RLSC response is too large for finite squared "
            + "cross-validation errors");
      }
      if (index > 0 && x[index] <= x[index - 1]) {
        throw new IllegalArgumentException("QC-RLSC coordinates must be strictly increasing");
      }
    }
  }

  private static void checkCanceled(final @NotNull BooleanSupplier canceled) {
    if (canceled.getAsBoolean()) {
      throw new CancellationException("QC-RLSC fitting was canceled");
    }
  }

  /** Cached coordinate geometry shared by every candidate and leave-one-out fold. */
  private record Geometry(double[] x, int[][] neighborOrder) {

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
