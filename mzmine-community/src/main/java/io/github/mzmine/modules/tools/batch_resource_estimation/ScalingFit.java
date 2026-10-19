package io.github.mzmine.modules.tools.batch_resource_estimation;

import org.jetbrains.annotations.NotNull;

/**
 * Scaling of a resource with the dataset size: {@code y = coefficient * x^exponent}. x is the
 * dataset size in file equivalents (raw data bytes divided by the mean file size), y the resource
 * without the constant baseline of the application.
 * <p>
 * Fitted by least squares in log-log space, which captures linear (per file) and superlinear
 * (alignment, gap filling: rows x files) growth. Values that are zero in any point, like temp files
 * of steps that do not create any, are fitted proportional to x instead.
 *
 * @param coefficient y at x = 1
 * @param exponent    1 is linear growth, 2 quadratic
 */
public record ScalingFit(double coefficient, double exponent) {

  /**
   * Smaller values are treated as zero (GB or seconds) and cannot be fitted in log space
   */
  static final double MIN_POSITIVE = 1e-3;
  // assumption: no batch step scales worse than quadratic with the number of files, steeper fits
  // of noisy small subsets would explode on extrapolation
  static final double MAX_EXPONENT = 2;

  public static final ScalingFit ZERO = new ScalingFit(0, 1);

  /**
   * @param x dataset sizes in file equivalents, ascending and positive
   * @param y measured values without baseline, same length as x
   */
  public static @NotNull ScalingFit fit(final double @NotNull [] x, final double @NotNull [] y) {
    if (x.length != y.length) {
      throw new IllegalArgumentException("x and y differ in length");
    }
    if (x.length == 0) {
      return ZERO;
    }

    boolean allPositive = true;
    for (final double v : y) {
      allPositive &= v > MIN_POSITIVE;
    }
    // decision: a single point or zero values cannot define an exponent, assume linear scaling
    if (x.length == 1 || !allPositive) {
      return fitWithExponent(x, y, 1);
    }

    double meanLx = 0;
    double meanLy = 0;
    for (int i = 0; i < x.length; i++) {
      meanLx += Math.log(x[i]);
      meanLy += Math.log(y[i]);
    }
    meanLx /= x.length;
    meanLy /= x.length;
    double covariance = 0;
    double varianceX = 0;
    for (int i = 0; i < x.length; i++) {
      final double dx = Math.log(x[i]) - meanLx;
      covariance += dx * (Math.log(y[i]) - meanLy);
      varianceX += dx * dx;
    }
    if (varianceX == 0) {
      return fitWithExponent(x, y, 1);
    }
    final double exponent = covariance / varianceX;
    if (exponent < 0 || exponent > MAX_EXPONENT) {
      return fitWithExponent(x, y, Math.clamp(exponent, 0, MAX_EXPONENT));
    }
    return new ScalingFit(Math.exp(meanLy - exponent * meanLx), exponent);
  }

  /**
   * Extrapolates from the end of the measured curve: the fit passes through the last point with the
   * exponent of the last segment. Steps that level off, like thread limited steps or gap filling,
   * grow fast for small subsets and slower later. The last segment describes the growth beyond the
   * largest subset better than a fit over all points.
   *
   * @param x dataset sizes, ascending and positive
   * @param y measured values, same length as x
   */
  public static @NotNull ScalingFit fitLastSegment(final double @NotNull [] x,
      final double @NotNull [] y) {
    if (x.length != y.length) {
      throw new IllegalArgumentException("x and y differ in length");
    }
    if (x.length == 0) {
      return ZERO;
    }
    final int last = x.length - 1;
    final double lastY = Math.max(0, y[last]);
    int previous = last - 1;
    // thread limited steps have the same x for all runs above the thread count
    while (previous >= 0 && x[previous] == x[last]) {
      previous--;
    }
    if (previous < 0) {
      // decision: without a second x the value is kept constant
      return new ScalingFit(lastY, 0);
    }
    final double exponent = segmentExponent(x[previous], y[previous], x[last], y[last]);
    if (Double.isNaN(exponent)) {
      // zero values cannot define an exponent, assume linear scaling through the last point
      return new ScalingFit(lastY / x[last], 1);
    }
    final double clamped = Math.clamp(exponent, 0, MAX_EXPONENT);
    return new ScalingFit(lastY / Math.pow(x[last], clamped), clamped);
  }

  /**
   * @return the exponent between two points in log-log space, NaN if a value is too small or both
   * x are equal
   */
  public static double segmentExponent(final double x1, final double y1, final double x2,
      final double y2) {
    if (y1 <= MIN_POSITIVE || y2 <= MIN_POSITIVE || x1 <= 0 || x2 <= 0 || x1 == x2) {
      return Double.NaN;
    }
    return Math.log(y2 / y1) / Math.log(x2 / x1);
  }

  /**
   * Least squares coefficient for a fixed exponent, never negative.
   */
  private static @NotNull ScalingFit fitWithExponent(final double @NotNull [] x,
      final double @NotNull [] y, final double exponent) {
    double sumXY = 0;
    double sumXX = 0;
    for (int i = 0; i < x.length; i++) {
      final double xe = Math.pow(x[i], exponent);
      sumXY += xe * y[i];
      sumXX += xe * xe;
    }
    final double coefficient = sumXX == 0 ? 0 : Math.max(0, sumXY / sumXX);
    return new ScalingFit(coefficient, exponent);
  }

  public double predict(final double x) {
    return coefficient * Math.pow(x, exponent);
  }
}
