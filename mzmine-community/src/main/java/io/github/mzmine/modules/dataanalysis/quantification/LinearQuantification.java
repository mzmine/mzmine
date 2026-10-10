package io.github.mzmine.modules.dataanalysis.quantification;

import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A small, immutable calculation core for explicitly configured linear quantitative methods.
 *
 * <p>This class deliberately does not choose calibration models, acceptance limits, or LOD/LOQ values.
 * Callers retain those method decisions and can persist the returned back-calculations for review.</p>
 */
public final class LinearQuantification {

  public enum Mode { EXTERNAL, INTERNAL_STANDARD, SURROGATE_REFERENCE_EQUIVALENT }
  public enum Weighting { UNWEIGHTED, ONE_OVER_X, ONE_OVER_X_SQUARED }
  public enum Intercept { FREE, FIXED_ZERO }
  public enum Status { AVAILABLE, INSUFFICIENT_POINTS, INVALID_POINT, DEGENERATE, NONFINITE }

  /** A calibration measurement. For internal-standard mode, both IS fields are required. */
  public record CalibrationPoint(@NotNull String sampleId, @Nullable Double analyteConcentration,
                                 @Nullable Double internalStandardConcentration,
                                 @Nullable Double analyteResponse,
                                 @Nullable Double internalStandardResponse) { }

  public record BackCalculation(@NotNull String sampleId, @Nullable Double suppliedConcentration,
                                @Nullable Double fittedConcentration, @Nullable Double residual,
                                @Nullable Double relativeError) { }

  public record Fit(@NotNull Status status, @NotNull Mode mode, @NotNull Weighting weighting,
                    @NotNull Intercept interceptMode, int usedPointCount, int rejectedPointCount,
                    @Nullable Double slope, @Nullable Double intercept, @Nullable Double rSquared,
                    @Nullable Double minimumCalibratedConcentration,
                    @Nullable Double maximumCalibratedConcentration,
                    @Nullable Double minimumRegressionX, @Nullable Double maximumRegressionX,
                    @NotNull List<BackCalculation> backCalculations) {
    public boolean available() { return status == Status.AVAILABLE; }

    /**
     * Calculates analyte concentration. Internal-standard mode requires the unknown's actual IS amount;
     * a varying IS amount is never silently replaced by a calibrator amount.
     */
    public @Nullable Estimate estimate(@Nullable Double analyteResponse,
        @Nullable Double internalStandardResponse, @Nullable Double internalStandardConcentration,
        @Nullable Double relativeResponseFactor) {
      if (!available() || !finiteNonnegative(analyteResponse)) return Estimate.unavailable("missing_or_invalid_analyte_response");
      final double response;
      if (mode == Mode.INTERNAL_STANDARD) {
        if (!finitePositive(internalStandardResponse) || !finitePositive(internalStandardConcentration)) {
          return Estimate.unavailable("missing_or_invalid_internal_standard");
        }
        response = analyteResponse / internalStandardResponse;
      } else response = analyteResponse;
      if (!Double.isFinite(response) || slope == null || intercept == null || slope == 0d) {
        return Estimate.unavailable("numerical_unavailable");
      }
      final double regressionX = (response - intercept) / slope;
      double concentration = regressionX;
      if (mode == Mode.INTERNAL_STANDARD) concentration *= internalStandardConcentration;
      if (mode == Mode.SURROGATE_REFERENCE_EQUIVALENT) {
        // Convention: factor = unknown response / reference response at equal concentration.
        if (!finitePositive(relativeResponseFactor)) return Estimate.unavailable("missing_or_invalid_relative_response_factor");
        concentration /= relativeResponseFactor;
      }
      if (!Double.isFinite(concentration)) return Estimate.unavailable("numerical_unavailable");
      final boolean xOutside = minimumRegressionX != null && maximumRegressionX != null
          && (regressionX < minimumRegressionX || regressionX > maximumRegressionX);
      final double comparableConcentration = mode == Mode.SURROGATE_REFERENCE_EQUIVALENT ? regressionX : concentration;
      final boolean concentrationOutside = minimumCalibratedConcentration != null && maximumCalibratedConcentration != null
          && (comparableConcentration < minimumCalibratedConcentration || comparableConcentration > maximumCalibratedConcentration);
      final String range = xOutside || concentrationOutside ? "outside_calibrated_range" : "within_calibrated_range";
      return new Estimate(concentration, range, null);
    }
  }

  public record Estimate(@Nullable Double concentration, @NotNull String rangeFlag,
                         @Nullable String unavailableReason) {
    static @NotNull Estimate unavailable(final @NotNull String reason) {
      return new Estimate(null, "unavailable", reason);
    }
  }

  private LinearQuantification() { }

  public static @NotNull Fit fit(final @NotNull Mode mode, final @NotNull Weighting weighting,
      final @NotNull Intercept interceptMode, final @NotNull List<CalibrationPoint> points) {
    final List<Point> usable = new ArrayList<>();
    int rejected = 0;
    for (final CalibrationPoint point : points) {
      final Point transformed = transform(mode, weighting, point);
      if (transformed == null) rejected++; else usable.add(transformed);
    }
    // Even a fixed-zero calibration needs two distinct levels; one point is a response factor, not a validated curve.
    if (usable.size() < 2 || usable.stream().map(point -> point.x).distinct().count() < 2) return unavailable(Status.INSUFFICIENT_POINTS, mode, weighting, interceptMode,
        usable.size(), rejected, points);
    final Regression regression = regression(usable, interceptMode);
    if (regression == null) return unavailable(Status.DEGENERATE, mode, weighting, interceptMode,
        usable.size(), rejected, points);
    final List<BackCalculation> backCalculations = new ArrayList<>();
    double min = Double.POSITIVE_INFINITY;
    double max = Double.NEGATIVE_INFINITY;
    double minX = Double.POSITIVE_INFINITY;
    double maxX = Double.NEGATIVE_INFINITY;
    for (final Point point : usable) {
      final double fittedX = (point.y - regression.intercept) / regression.slope;
      final double fittedConcentration = mode == Mode.INTERNAL_STANDARD ? fittedX * point.isConcentration : fittedX;
      final double supplied = point.concentration;
      final double residual = fittedConcentration - supplied;
      final Double relativeError = supplied == 0d ? null : residual / supplied;
      backCalculations.add(new BackCalculation(point.sampleId, supplied, finite(fittedConcentration), finite(residual),
          finite(relativeError)));
      min = Math.min(min, supplied);
      max = Math.max(max, supplied);
      minX = Math.min(minX, point.x);
      maxX = Math.max(maxX, point.x);
    }
    final Double rSquared = rSquared(usable, regression);
    return new Fit(Status.AVAILABLE, mode, weighting, interceptMode, usable.size(), rejected,
        regression.slope, regression.intercept, rSquared, min, max, minX, maxX, List.copyOf(backCalculations));
  }

  private static @Nullable Point transform(final @NotNull Mode mode, final @NotNull Weighting weighting,
      final @NotNull CalibrationPoint point) {
    if (!finiteNonnegative(point.analyteConcentration) || !finiteNonnegative(point.analyteResponse)) return null;
    final double concentration = point.analyteConcentration;
    final double x;
    final double y;
    final double isConcentration;
    if (mode == Mode.INTERNAL_STANDARD) {
      if (!finitePositive(point.internalStandardConcentration) || !finitePositive(point.internalStandardResponse)) return null;
      isConcentration = point.internalStandardConcentration;
      x = concentration / isConcentration;
      y = point.analyteResponse / point.internalStandardResponse;
    } else {
      isConcentration = 1d;
      x = concentration;
      y = point.analyteResponse;
    }
    if (!Double.isFinite(x) || !Double.isFinite(y) || y < 0d) return null;
    final double weight = switch (weighting) {
      case UNWEIGHTED -> 1d;
      case ONE_OVER_X -> x > 0d ? 1d / x : Double.NaN;
      case ONE_OVER_X_SQUARED -> x > 0d ? 1d / (x * x) : Double.NaN;
    };
    return Double.isFinite(weight) && weight > 0d ? new Point(point.sampleId, concentration, isConcentration, x, y, weight) : null;
  }

  private static @Nullable Regression regression(final @NotNull List<Point> points,
      final @NotNull Intercept interceptMode) {
    double sumW = 0d, sumX = 0d, sumY = 0d, sumXX = 0d, sumXY = 0d;
    for (final Point point : points) {
      sumW += point.weight;
      sumX += point.weight * point.x;
      sumY += point.weight * point.y;
      sumXX += point.weight * point.x * point.x;
      sumXY += point.weight * point.x * point.y;
    }
    if (!Double.isFinite(sumW) || !Double.isFinite(sumX) || !Double.isFinite(sumY) || !Double.isFinite(sumXX)
        || !Double.isFinite(sumXY)) return null;
    final double slope;
    final double intercept;
    if (interceptMode == Intercept.FIXED_ZERO) {
      if (sumXX <= 0d) return null;
      slope = sumXY / sumXX;
      intercept = 0d;
    } else {
      final double denominator = sumW * sumXX - sumX * sumX;
      if (denominator == 0d || !Double.isFinite(denominator)) return null;
      slope = (sumW * sumXY - sumX * sumY) / denominator;
      intercept = (sumY - slope * sumX) / sumW;
    }
    // A non-positive response slope is not a usable quantitative calibration model.
    return Double.isFinite(slope) && slope > 0d && Double.isFinite(intercept) ? new Regression(slope, intercept) : null;
  }

  private static @Nullable Double rSquared(final @NotNull List<Point> points, final @NotNull Regression regression) {
    double weight = 0d, mean = 0d;
    for (final Point point : points) { weight += point.weight; mean += point.weight * point.y; }
    if (weight <= 0d || !Double.isFinite(mean /= weight)) return null;
    double ssTotal = 0d, ssResidual = 0d;
    for (final Point point : points) {
      ssTotal += point.weight * Math.pow(point.y - mean, 2d);
      ssResidual += point.weight * Math.pow(point.y - (regression.intercept + regression.slope * point.x), 2d);
    }
    return ssTotal == 0d || !Double.isFinite(ssResidual) ? null : finite(1d - ssResidual / ssTotal);
  }

  private static @NotNull Fit unavailable(final @NotNull Status status, final @NotNull Mode mode,
      final @NotNull Weighting weighting, final @NotNull Intercept interceptMode, final int used, final int rejected,
      final @NotNull List<CalibrationPoint> ignored) {
    return new Fit(status, mode, weighting, interceptMode, used, rejected, null, null, null, null, null, null, null, List.of());
  }

  private static boolean finitePositive(final @Nullable Double value) { return value != null && Double.isFinite(value) && value > 0d; }
  private static boolean finiteNonnegative(final @Nullable Double value) { return value != null && Double.isFinite(value) && value >= 0d; }
  private static @Nullable Double finite(final double value) { return Double.isFinite(value) ? value : null; }
  private record Point(@NotNull String sampleId, double concentration, double isConcentration, double x, double y, double weight) { }
  private record Regression(double slope, double intercept) { }
}
