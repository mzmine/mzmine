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

package io.github.mzmine.modules.tools.qualityparameters;

import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.featuredata.impl.SummedIntensityMobilitySeries;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.types.numbers.AsymmetryFactorType;
import io.github.mzmine.datamodel.features.types.numbers.FwhmType;
import io.github.mzmine.datamodel.features.types.numbers.TailingFactorType;
import io.github.mzmine.util.DataPointUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Calculates quality parameters for each peak in a feature list: - Full width at half maximum
 * (FWHM) - Tailing Factor - Asymmetry factor
 */
public class QualityParameters {

  private static final double FWHM_HEIGHT_FRACTION = 0.5;
  private static final double TAILING_HEIGHT_FRACTION = 0.05;
  private static final double ASYMMETRY_HEIGHT_FRACTION = 0.1;
  /**
   * A flank whose minimum intensity stays above this fraction of the apex intensity is too flat to
   * tell where the peak would reach the threshold. Such a flank is limited to the observed data
   * instead of being extrapolated.
   */
  private static final double FLAT_FLANK_FRACTION = 0.85;
  /**
   * Upper limit for the estimated half width of a flank that never reaches the threshold, as a
   * multiple of the half width that was actually observed on that flank. A gaussian peak that is
   * cut off at 84 % of its apex intensity is twice as wide as the observed part, which is roughly
   * where {@link #FLAT_FLANK_FRACTION} already stops the extrapolation. This cap therefore mostly
   * catches flanks that are not gaussian or whose fit is dominated by noise. It also bounds the
   * FWHM to twice the observed peak width.
   */
  private static final double MAX_EXTRAPOLATION_FACTOR = 2.0;

  public static void calculateAndSetModularQualityParameters(
      @NotNull final ModularFeatureList flist) {
    // add quality columns to flist - feature columns
    flist.addFeatureType(new FwhmType(), new AsymmetryFactorType(), new TailingFactorType());

    flist.streamFeatures().forEach(feature -> {
      final IonTimeSeries<? extends Scan> series = feature.getFeatureData();
      if (!isInitialized(feature) || series == null || series.getNumberOfValues() < 3) {
        return;
      }
      final double[] rts = extractX(series);
      final double[] intensities = extractIntensities(series);

      final double fwhm = calculateFWHM(rts, intensities);
      if (!Double.isNaN(fwhm)) {
        feature.set(FwhmType.class, (float) fwhm);
      }

      final double tf = calculateTailingFactor(rts, intensities);
      if (!Double.isNaN(tf)) {
        feature.set(TailingFactorType.class, (float) tf);
      }

      final double af = calculateAsymmetryFactor(rts, intensities);
      if (!Double.isNaN(af)) {
        feature.set(AsymmetryFactorType.class, (float) af);
      }
    });
  }

  public static float calculateFWHM(@Nullable final Feature feature) {
    final IonTimeSeries<? extends Scan> series = requireInitializedTrace(feature);
    if (series == null) {
      return Float.NaN;
    }
    return (float) calculateFWHM(extractX(series), extractIntensities(series));
  }

  public static float calculateTailingFactor(@Nullable final Feature feature) {
    final IonTimeSeries<? extends Scan> series = requireInitializedTrace(feature);
    if (series == null) {
      return Float.NaN;
    }
    return (float) calculateTailingFactor(extractX(series), extractIntensities(series));
  }

  public static float calculateAsymmetryFactor(@Nullable final Feature feature) {
    final IonTimeSeries<? extends Scan> series = requireInitializedTrace(feature);
    if (series == null) {
      return Float.NaN;
    }
    return (float) calculateAsymmetryFactor(extractX(series), extractIntensities(series));
  }

  public static float calculateFWHM(@Nullable final SummedIntensityMobilitySeries series) {
    if (series == null || series.getNumberOfValues() < 3) {
      return Float.NaN;
    }
    // assumption: mobility values are ascending with index, guaranteed by the series
    final double[] mobilities = DataPointUtils.getDoubleBufferAsArray(series.getMobilityValues());
    final double[] intensities = DataPointUtils.getDoubleBufferAsArray(
        series.getIntensityValueBuffer());
    return (float) calculateFWHM(mobilities, intensities);
  }

  /**
   * Full width at half maximum.
   * <p>
   * decision: a side that never drops below half the apex intensity, e.g. because the peak is cut
   * off or because a co-eluting peak fills up one flank, is mirrored from the opposite side. The
   * mirrored width is never narrower than what was actually observed on that side. If neither side
   * drops below half maximum, both flanks are extrapolated down to the threshold, see
   * {@link #extrapolateToThreshold(double, double[], double[], int, int)}.
   * <p>
   * The result never exceeds twice the observed x range of the peak.
   *
   * @param x           x values (retention time), ascending with index
   * @param intensities intensities, same length as {@code x}
   * @return the width or {@link Double#NaN} if it cannot be determined
   */
  public static double calculateFWHM(final double @NotNull [] x,
      final double @NotNull [] intensities) {
    final ThresholdCrossings crossings = findThresholdCrossings(FWHM_HEIGHT_FRACTION, x,
        intensities);
    if (crossings == null) {
      return Double.NaN;
    }
    final double[] bounds = resolveWithFallbacks(crossings);
    final double fwhm = bounds[1] - bounds[0];
    return fwhm > 0 && !Double.isInfinite(fwhm) ? fwhm : Double.NaN;
  }

  /**
   * Tailing factor at 5 % of the apex intensity.
   * <p>
   * decision: unlike the FWHM, a missing side is not mirrored here. Mirroring would force the
   * factor to exactly 1 and thus claim a perfectly symmetric peak, which is the opposite of what
   * this parameter is meant to detect. An undetermined side yields NaN instead.
   *
   * @param x           x values (retention time), ascending with index
   * @param intensities intensities, same length as {@code x}
   * @return the tailing factor or {@link Double#NaN} if it cannot be determined
   */
  public static double calculateTailingFactor(final double @NotNull [] x,
      final double @NotNull [] intensities) {
    final ThresholdCrossings c = findThresholdCrossings(TAILING_HEIGHT_FRACTION, x, intensities);
    if (c == null || !c.hasLeftCrossing() || !c.hasRightCrossing()) {
      return Double.NaN;
    }
    final double tf = (c.rightX() - c.leftX()) / (2 * (c.apexX() - c.leftX()));
    return tf > 0 && !Double.isInfinite(tf) ? tf : Double.NaN;
  }

  /**
   * Asymmetry factor at 10 % of the apex intensity. See
   * {@link #calculateTailingFactor(double[], double[])} for why a missing side is not mirrored.
   *
   * @param x           x values (retention time), ascending with index
   * @param intensities intensities, same length as {@code x}
   * @return the asymmetry factor or {@link Double#NaN} if it cannot be determined
   */
  public static double calculateAsymmetryFactor(final double @NotNull [] x,
      final double @NotNull [] intensities) {
    final ThresholdCrossings c = findThresholdCrossings(ASYMMETRY_HEIGHT_FRACTION, x, intensities);
    if (c == null || !c.hasLeftCrossing() || !c.hasRightCrossing()) {
      return Double.NaN;
    }
    final double af = (c.rightX() - c.apexX()) / (c.apexX() - c.leftX());
    return af > 0 && !Double.isInfinite(af) ? af : Double.NaN;
  }

  /**
   * Searches the x values at which the signal crosses {@code heightFraction} of the apex
   * intensity.
   * <p>
   * decision: the search starts at both outer edges and walks inwards towards the apex, stopping at
   * the first crossing. The whole trace is treated as a single peak because peak picking already
   * decided its boundaries. Interior valleys of a split peak and single data points that drop to
   * zero, e.g. because of detector defects, therefore do not truncate the peak.
   *
   * @param heightFraction fraction of the apex intensity, e.g. 0.5 for FWHM
   * @param x              x values, ascending with index
   * @param intensities    intensities, same length as {@code x}
   * @return the crossings or null if the trace is too short or carries no intensity
   */
  @Nullable
  static ThresholdCrossings findThresholdCrossings(final double heightFraction,
      final double @NotNull [] x, final double @NotNull [] intensities) {
    if (x.length < 3 || x.length != intensities.length) {
      return null;
    }
    final int apex = indexOfMax(intensities);
    final double threshold = intensities[apex] * heightFraction;
    if (threshold <= 0) {
      return null;
    }
    final int last = x.length - 1;

    return new ThresholdCrossings(x[apex], //
        findCrossing(threshold, x, intensities, 0, apex), //
        findCrossing(threshold, x, intensities, last, apex), //
        x[0], x[last], //
        extrapolateToThreshold(threshold, x, intensities, 0, apex), //
        extrapolateToThreshold(threshold, x, intensities, last, apex));
  }

  /**
   * Walks from the outer data point {@code from} towards the apex at {@code to} (both inclusive)
   * and interpolates the first crossing of the threshold.
   *
   * @return the crossing or NaN if the trace already starts above the threshold on that side
   */
  private static double findCrossing(final double threshold, final double @NotNull [] x,
      final double @NotNull [] intensities, final int from, final int to) {
    final int step = from < to ? 1 : -1;
    for (int i = from; i != to + step; i += step) {
      if (intensities[i] < threshold) {
        continue;
      }
      if (i == from) {
        // the outermost data point is already above the threshold, this side is cut off
        return intensities[i] == threshold ? x[i] : Double.NaN;
      }
      // intensities[outer] < threshold <= intensities[i], so this always interpolates
      final int outer = i - step;
      return interpolateX(x[outer], intensities[outer], x[i], intensities[i], threshold);
    }
    // unreachable for fractions < 1 because the apex is always at or above the threshold
    return Double.NaN;
  }

  /**
   * Estimates where one flank would cross the threshold if the peak were not cut off. Only used
   * when that flank never drops below the threshold within the data.
   * <p>
   * decision: the slope is a least squares fit over all data points of the flank rather than the
   * line through the two outermost points, so that a single noisy edge data point cannot dominate
   * the estimate.
   * <p>
   * decision: a flank that never descends below {@link #FLAT_FLANK_FRACTION} of the apex intensity
   * is not extrapolated at all. Such a flank carries no usable information about where the
   * threshold would be reached, and a linear extrapolation of it reaches arbitrarily far. The
   * caller then falls back to the observed data range for that flank.
   * <p>
   * decision: for all other flanks the estimated half width is capped at
   * {@link #MAX_EXTRAPOLATION_FACTOR} times the half width that was actually observed.
   *
   * @param edge index of the outermost data point of the flank
   * @param apex index of the apex
   * @return the estimated crossing, or NaN if the flank holds no data points, is too flat, or does
   * not rise towards the apex
   */
  private static double extrapolateToThreshold(final double threshold, final double @NotNull [] x,
      final double @NotNull [] intensities, final int edge, final int apex) {
    if (edge == apex) {
      return Double.NaN;
    }
    final int step = edge < apex ? 1 : -1;
    final int end = apex + step;

    int n = 0;
    double sumX = 0;
    double sumY = 0;
    double minIntensity = Double.MAX_VALUE;
    for (int i = edge; i != end; i += step) {
      sumX += x[i];
      sumY += intensities[i];
      minIntensity = Math.min(minIntensity, intensities[i]);
      n++;
    }
    // an almost flat flank never descended far enough to tell where it would reach the threshold
    if (minIntensity >= intensities[apex] * FLAT_FLANK_FRACTION) {
      return Double.NaN;
    }
    final double meanX = sumX / n;
    final double meanY = sumY / n;

    double sumXy = 0;
    double sumXx = 0;
    for (int i = edge; i != end; i += step) {
      final double dx = x[i] - meanX;
      sumXy += dx * (intensities[i] - meanY);
      sumXx += dx * dx;
    }
    if (sumXx == 0) {
      return Double.NaN;
    }
    final double slope = sumXy / sumXx;
    // the fitted line must rise towards the apex, no matter which flank it describes
    if (slope * (x[apex] - x[edge]) <= 0) {
      return Double.NaN;
    }

    final double apexX = x[apex];
    final double observedHalfWidth = Math.abs(apexX - x[edge]);
    final double fittedHalfWidth = Math.abs(meanX + (threshold - meanY) / slope - apexX);
    // the flank stays above the threshold, so the crossing cannot lie inside the observed data
    final double halfWidth = Math.clamp(fittedHalfWidth, observedHalfWidth,
        MAX_EXTRAPOLATION_FACTOR * observedHalfWidth);
    return edge < apex ? apexX - halfWidth : apexX + halfWidth;
  }

  /**
   * Resolves the crossings into a left and a right bound, mirroring a side that never reaches the
   * threshold.
   * <p>
   * decision: a mirrored side is at least as wide as the side that did cross and at least as wide
   * as the data that was actually observed on the missing side, whichever is larger.
   */
  private static double @NotNull [] resolveWithFallbacks(@NotNull final ThresholdCrossings c) {
    final double apex = c.apexX();
    if (c.hasLeftCrossing() && c.hasRightCrossing()) {
      return new double[]{c.leftX(), c.rightX()};
    }
    if (c.hasLeftCrossing()) {
      final double halfWidth = Math.max(apex - c.leftX(), c.rightEdgeX() - apex);
      return new double[]{c.leftX(), apex + halfWidth};
    }
    if (c.hasRightCrossing()) {
      final double halfWidth = Math.max(c.rightX() - apex, apex - c.leftEdgeX());
      return new double[]{apex - halfWidth, c.rightX()};
    }
    // neither side reaches the threshold: extrapolate both outer flanks and fall back to the
    // observed edges if a flank does not rise towards the apex
    final double left = Double.isNaN(c.leftExtrapolatedX()) ? c.leftEdgeX() : c.leftExtrapolatedX();
    final double right =
        Double.isNaN(c.rightExtrapolatedX()) ? c.rightEdgeX() : c.rightExtrapolatedX();
    return new double[]{left, right};
  }

  /**
   * @return the x value at which the line through both points reaches {@code targetY}
   */
  private static double interpolateX(final double x1, final double y1, final double x2,
      final double y2, final double targetY) {
    return x1 + (targetY - y1) * (x2 - x1) / (y2 - y1);
  }

  private static int indexOfMax(final double @NotNull [] values) {
    int maxIndex = 0;
    for (int i = 1; i < values.length; i++) {
      if (values[i] > values[maxIndex]) {
        maxIndex = i;
      }
    }
    return maxIndex;
  }

  private static double @NotNull [] extractX(@NotNull final IonTimeSeries<? extends Scan> series) {
    final double[] x = new double[series.getNumberOfValues()];
    for (int i = 0; i < x.length; i++) {
      x[i] = series.getRetentionTime(i);
    }
    return x;
  }

  private static double @NotNull [] extractIntensities(
      @NotNull final IonTimeSeries<? extends Scan> series) {
    return DataPointUtils.getDoubleBufferAsArray(series.getIntensityValueBuffer());
  }

  private static boolean isInitialized(@NotNull final Feature feature) {
    return feature.getHeight() != null && feature.getRT() != null
        && feature.getRawDataFile() != null && !feature.getScanNumbers().isEmpty();
  }

  /**
   * @return the feature data, or null if the feature is null or holds too few values to calculate
   * quality parameters
   * @throws IllegalArgumentException if the feature exists but its values are not initialized
   */
  @Nullable
  private static IonTimeSeries<? extends Scan> requireInitializedTrace(
      @Nullable final Feature feature) {
    if (feature == null) {
      return null;
    }
    final IonTimeSeries<? extends Scan> series = feature.getFeatureData();
    if (!isInitialized(feature) || series == null) {
      throw new IllegalArgumentException("Modular feature values are not initialized.");
    }
    return series.getNumberOfValues() < 3 ? null : series;
  }

  /**
   * Returns the number of sign changes (positive/negative) in the slope between the start and end
   * of a peak. 1 is subtracted from the sign changes because one change is expected at the top of
   * the peak. If there is no sign change, 0 is returned.
   *
   * @param startIndex        search start
   * @param endIndexExclusive search end
   * @param y                 intensities (or similar)
   * @return The number of sign changes in the slope of the signal. (n-1 as one is expected)
   */
  public static int getSignChanges(int startIndex, int endIndexExclusive, double[] y) {
    int lastSign = 1;
    int changes = 0;
//    final double tenPercentHeight = 0.1 * height; // tested with only changes above 10% height, but was not that useful

    for (int i = startIndex; i < endIndexExclusive - 1; i++) {
//      if(y[i] < tenPercentHeight) {
//        continue;
//      }
      if (y[i] < y[i + 1] && lastSign < 0) {
        changes++;
        lastSign = 1;
      } else if (y[i] > y[i + 1] && lastSign > 0) {
        changes++;
        lastSign = -1;
      }
    }

    return Math.max(0, changes - 1);
  }

  /**
   * The number of observed sign changes (-1, see {@link #getSignChanges(int, int, double[])} per
   * nPoints. Can be used as a quality parameter for peak jaggedness.
   * <br>
   * e.g. if a sign change is allowed every 5 points and the peak changes slope more often, the
   * value will be greater than 1 and could thus fail the quality check. In general, allowing a sign
   * change every 2 points already filters out a lot of noise without being too strict.
   *
   * @param startIndex        start search
   * @param endIndexExclusive end search
   * @param y                 intensity values
   * @param nPoints           the number of allowed points between every sign change
   * @return Fraction of the absolute sign changes divided by the points on the peak divided by the
   * sign changes.
   */
  public static double signChangesPerNPoints(int startIndex, int endIndexExclusive, double[] y,
      final double nPoints) {
    final int changes = getSignChanges(startIndex, endIndexExclusive, y);
    return (double) changes / ((endIndexExclusive - startIndex) / (double) nPoints);
  }
}
