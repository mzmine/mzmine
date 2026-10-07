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
import io.github.mzmine.datamodel.featuredata.IonMobilogramTimeSeries;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.featuredata.impl.SummedIntensityMobilitySeries;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.types.numbers.AsymmetryFactorType;
import io.github.mzmine.datamodel.features.types.numbers.FwhmType;
import io.github.mzmine.datamodel.features.types.numbers.MobilityFwhmType;
import io.github.mzmine.datamodel.features.types.numbers.TailingFactorType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Calculates quality parameters for each peak in a feature list: - Full width at half maximum
 * (FWHM) - Tailing Factor - Asymmetry factor
 * <p>
 * All three parameters are derived from the same {@link ThresholdCrossings}, so a peak only has to
 * be scanned once, see {@link #findThresholdCrossings(float[], double[])}. They are returned
 * together as a {@link PeakQuality}, in which a parameter that cannot be determined is null.
 */
public class QualityParameters {

  private static final double FWHM_HEIGHT_FRACTION = 0.5;
  private static final double TAILING_HEIGHT_FRACTION = 0.05;
  private static final double ASYMMETRY_HEIGHT_FRACTION = 0.1;
  /**
   * All height fractions the quality parameters need, searched in a single walk per flank. The
   * order matches the indices below.
   */
  private static final double[] HEIGHT_FRACTIONS = {FWHM_HEIGHT_FRACTION, ASYMMETRY_HEIGHT_FRACTION,
      TAILING_HEIGHT_FRACTION};
  private static final int FWHM_INDEX = 0;
  private static final int ASYMMETRY_INDEX = 1;
  private static final int TAILING_INDEX = 2;
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

  /**
   * @throws IllegalArgumentException if a feature of the list is not initialized
   */
  public static void calculateAndSetModularQualityParameters(
      @NotNull final ModularFeatureList flist) {
    // add quality columns to flist - feature columns
    flist.addFeatureType(new FwhmType(), new AsymmetryFactorType(), new TailingFactorType());
    flist.streamFeatures().forEach(QualityParameters::calculateAndSetQualityParameters);
  }

  /**
   * Calculates FWHM, asymmetry factor and tailing factor in a single pass over the feature data and
   * sets all three.
   * <p>
   * decision: a parameter that cannot be determined is set to null instead of being skipped, so
   * that the value of an earlier run is removed rather than kept next to the new ones.
   *
   * @throws IllegalArgumentException if the feature values are not initialized
   */
  public static void calculateAndSetQualityParameters(@NotNull final ModularFeature feature) {
    final PeakQuality quality = calculateQualityParameters(feature);
    feature.set(FwhmType.class, quality.fwhm());
    feature.set(AsymmetryFactorType.class, quality.asymmetry());
    feature.set(TailingFactorType.class, quality.tailing());

    if (feature.getFeatureData() instanceof IonMobilogramTimeSeries imts) {
      final SummedIntensityMobilitySeries summedMobilogram = imts.getSummedMobilogram();
      // since version 4.10, null removes the value of an earlier run
      feature.set(MobilityFwhmType.class, QualityParameters.calculateFWHM(summedMobilogram));
    }
  }

  /**
   * @return all quality parameters of the feature, {@link PeakQuality#EMPTY} if its trace holds too
   * few data points
   * @throws IllegalArgumentException if the feature values are not initialized
   */
  @NotNull
  public static PeakQuality calculateQualityParameters(@NotNull final Feature feature) {
    final IonTimeSeries<? extends Scan> series = requireInitializedTrace(feature);
    return series == null ? PeakQuality.EMPTY
        : calculateQualityParameters(extractX(series), extractIntensities(series));
  }

  /**
   * @param x           x values (retention time), ascending with index
   * @param intensities intensities, same length as {@code x}
   * @return all quality parameters of the peak, {@link PeakQuality#EMPTY} if the trace is too short
   * or carries no intensity
   */
  @NotNull
  public static PeakQuality calculateQualityParameters(final float @NotNull [] x,
      final double @NotNull [] intensities) {
    final ThresholdCrossings c = findThresholdCrossings(x, intensities);
    return c == null ? PeakQuality.EMPTY
        : new PeakQuality(calculateFWHM(c), calculateAsymmetryFactor(c), calculateTailingFactor(c));
  }

  /**
   * The FWHM is the only quality parameter that is meaningful for a mobilogram.
   *
   * @return the full width at half maximum in mobility units, or null if it cannot be determined
   */
  @Nullable
  public static Float calculateFWHM(@Nullable final SummedIntensityMobilitySeries series) {
    if (series == null || series.getNumberOfValues() < 3) {
      return null;
    }
    // assumption: mobility values are ascending with index, guaranteed by the series
    final float[] mobilities = new float[series.getNumberOfValues()];
    for (int i = 0; i < mobilities.length; i++) {
      mobilities[i] = (float) series.getMobility(i);
    }
    final double[] intensities = series.getIntensityValues(new double[series.getNumberOfValues()]);
    return calculateFWHM(findThresholdCrossings(mobilities, intensities));
  }

  /**
   * Full width at half maximum.
   * <p>
   * decision: a side that never drops below half the apex intensity, e.g. because the peak is cut
   * off or because a co-eluting peak fills up one flank, is mirrored from the opposite side. The
   * mirrored width is never narrower than what was actually observed on that side. If neither side
   * drops below half maximum, both flanks are extrapolated down to the threshold, see
   * {@link #extrapolateToThreshold(double, float[], double[], int, int)}.
   * <p>
   * The result never exceeds twice the observed x range of the peak.
   *
   * @param c the crossings of the peak, or null if it has none
   * @return the width or null if it cannot be determined
   */
  @Nullable
  public static Float calculateFWHM(@Nullable final ThresholdCrossings c) {
    if (c == null) {
      return null;
    }
    final float[] bounds = resolveFwhmBoundsWithFallbacks(c);
    final float fwhm = bounds[1] - bounds[0];
    return fwhm > 0 && !Float.isInfinite(fwhm) ? fwhm : null;
  }

  /**
   * Tailing factor at 5 % of the apex intensity.
   * <p>
   * decision: unlike the FWHM, a missing side is not mirrored here. Mirroring would force the
   * factor to exactly 1 and thus claim a perfectly symmetric peak, which is the opposite of what
   * this parameter is meant to detect. An undetermined side yields null instead.
   *
   * @param c the crossings of the peak, or null if it has none
   * @return the tailing factor or null if it cannot be determined
   */
  @Nullable
  public static Float calculateTailingFactor(@Nullable final ThresholdCrossings c) {
    if (c == null || c.leftX5() == null || c.rightX5() == null) {
      return null;
    }
    final float left = c.leftX5();
    final float tf = (c.rightX5() - left) / (2 * (c.apexX() - left));
    return tf > 0 && !Float.isInfinite(tf) ? tf : null;
  }

  /**
   * Asymmetry factor at 10 % of the apex intensity. See
   * {@link #calculateTailingFactor(ThresholdCrossings)} for why a missing side is not mirrored.
   *
   * @param c the crossings of the peak, or null if it has none
   * @return the asymmetry factor or null if it cannot be determined
   */
  @Nullable
  public static Float calculateAsymmetryFactor(@Nullable final ThresholdCrossings c) {
    if (c == null || c.leftX10() == null || c.rightX10() == null) {
      return null;
    }
    final float apex = c.apexX();
    final float af = (c.rightX10() - apex) / (apex - c.leftX10());
    return af > 0 && !Float.isInfinite(af) ? af : null;
  }

  /**
   * Searches the x values at which the signal crosses every height fraction the quality parameters
   * need, i.e. 50 % for the FWHM, 10 % for the asymmetry factor and 5 % for the tailing factor.
   * <p>
   * decision: the search starts at both outer edges and walks inwards towards the apex, stopping at
   * the first crossing of each threshold. The whole trace is treated as a single peak because peak
   * picking already decided its boundaries. Interior valleys of a split peak and single data points
   * that drop to zero, e.g. because of detector defects, therefore do not truncate the peak.
   * <p>
   * decision: all thresholds are collected in the same walk, so each flank is visited once no
   * matter how many quality parameters are calculated from the result.
   *
   * @param x           x values, ascending with index
   * @param intensities intensities, same length as {@code x}
   * @return the crossings or null if the trace is too short or carries no intensity
   */
  @Nullable
  public static ThresholdCrossings findThresholdCrossings(final float @NotNull [] x,
      final double @NotNull [] intensities) {
    if (x.length < 3) {
      return null;
    }
    if (x.length != intensities.length) {
      throw new IllegalArgumentException("x and intensities must have the same length");
    }

    final int apex = indexOfMax(intensities);
    if (intensities[apex] <= 0) {
      return null;
    }
    final double[] thresholds = new double[HEIGHT_FRACTIONS.length];
    for (int t = 0; t < thresholds.length; t++) {
      thresholds[t] = intensities[apex] * HEIGHT_FRACTIONS[t];
    }
    final int last = x.length - 1;

    final Float[] left = findCrossings(thresholds, x, intensities, 0, apex);
    final Float[] right = findCrossings(thresholds, x, intensities, last, apex);

    // only the FWHM falls back to an extrapolated flank, so the lower thresholds are not fitted
    final double halfMaximum = thresholds[FWHM_INDEX];
    return new ThresholdCrossings(x[apex], x[0], x[last], //
        left[FWHM_INDEX], right[FWHM_INDEX], //
        left[ASYMMETRY_INDEX], right[ASYMMETRY_INDEX], //
        left[TAILING_INDEX], right[TAILING_INDEX], //
        extrapolateToThreshold(halfMaximum, x, intensities, 0, apex), //
        extrapolateToThreshold(halfMaximum, x, intensities, last, apex));
  }

  /**
   * Walks from the outer data point {@code edge} towards the apex (both inclusive) and interpolates
   * the first crossing of every threshold. A single walk covers all of them because the data point
   * that first reaches a low threshold never lies further inside than the one that first reaches a
   * higher threshold.
   *
   * @return one crossing per threshold, in the order of {@code thresholds}, null for a threshold
   * that the trace already exceeds at the edge, i.e., a side that is cut off
   */
  @NotNull
  private static Float[] findCrossings(final double @NotNull [] thresholds,
      final float @NotNull [] x, final double @NotNull [] intensities, final int edge,
      final int apex) {
    final Float @Nullable [] crossings = new Float[thresholds.length];

    final boolean[] crossed = new boolean[thresholds.length];
    int open = thresholds.length;

    final int step = edge < apex ? 1 : -1;
    for (int i = edge; open > 0; i += step) {
      for (int t = 0; t < thresholds.length; t++) {
        if (crossed[t] || intensities[i] < thresholds[t]) {
          continue;
        }
        crossed[t] = true;
        open--;
        if (i == edge) {
          // the outermost data point is already above the threshold, this side is cut off
          crossings[t] = intensities[i] == thresholds[t] ? x[i] : null;
        } else {
          // intensities[outer] < threshold <= intensities[i], so this always interpolates
          final int outer = i - step;
          crossings[t] = interpolateX(x[outer], intensities[outer], x[i], intensities[i],
              thresholds[t]);
        }
      }
      if (i == apex) {
        // the apex is at or above every threshold, so all of them are resolved by now
        break;
      }
    }
    return crossings;
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
   * @return the estimated crossing, or null if the flank holds no data points, is too flat, or does
   * not rise towards the apex
   */
  @Nullable
  private static Float extrapolateToThreshold(final double threshold, final float @NotNull [] x,
      final double @NotNull [] intensities, final int edge, final int apex) {
    if (edge == apex) {
      return null;
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
      return null;
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
      return null;
    }
    final double slope = sumXy / sumXx;
    // the fitted line must rise towards the apex, no matter which flank it describes
    if (slope * (x[apex] - x[edge]) <= 0) {
      return null;
    }

    final double apexX = x[apex];
    final double observedHalfWidth = Math.abs(apexX - x[edge]);
    final double fittedHalfWidth = Math.abs(meanX + (threshold - meanY) / slope - apexX);
    // the flank stays above the threshold, so the crossing cannot lie inside the observed data
    final double halfWidth = Math.clamp(fittedHalfWidth, observedHalfWidth,
        MAX_EXTRAPOLATION_FACTOR * observedHalfWidth);
    return (float) (edge < apex ? apexX - halfWidth : apexX + halfWidth);
  }

  /**
   * Resolves the half maximum crossings into a left and a right bound, mirroring a side that never
   * reaches the threshold.
   * <p>
   * decision: a mirrored side is at least as wide as the side that did cross and at least as wide
   * as the data that was actually observed on the missing side, whichever is larger.
   */
  private static float @NotNull [] resolveFwhmBoundsWithFallbacks(
      @NotNull final ThresholdCrossings c) {
    final float apex = c.apexX();
    final Float left = c.leftX50();
    final Float right = c.rightX50();
    if (left != null && right != null) {
      return new float[]{left, right};
    }
    if (left != null) {
      final float halfWidth = Math.max(apex - left, c.lastX() - apex);
      return new float[]{left, apex + halfWidth};
    }
    if (right != null) {
      final float halfWidth = Math.max(right - apex, apex - c.firstX());
      return new float[]{apex - halfWidth, right};
    }
    // neither side reaches the threshold: extrapolate both outer flanks and fall back to the
    // observed edges if a flank does not rise towards the apex
    final Float leftFit = c.leftExtrapolatedX50();
    final Float rightFit = c.rightExtrapolatedX50();
    return new float[]{leftFit != null ? leftFit : c.firstX(),
        rightFit != null ? rightFit : c.lastX()};
  }

  /**
   * @return the x value at which the line through both points reaches {@code targetY}
   */
  private static float interpolateX(final float x1, final double y1, final float x2,
      final double y2, final double targetY) {
    return (float) (x1 + (targetY - y1) * (x2 - x1) / (y2 - y1));
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

  private static float @NotNull [] extractX(@NotNull final IonTimeSeries<? extends Scan> series) {
    final float[] x = new float[series.getNumberOfValues()];
    for (int i = 0; i < x.length; i++) {
      x[i] = series.getRetentionTime(i);
    }
    return x;
  }

  private static double @NotNull [] extractIntensities(
      @NotNull final IonTimeSeries<? extends Scan> series) {
    return series.getIntensityValues(new double[series.getNumberOfValues()]);
  }

  /**
   * @return the feature data, or null if it holds too few values to calculate quality parameters
   * @throws IllegalArgumentException if the feature values are not initialized
   */
  @Nullable
  private static IonTimeSeries<? extends Scan> requireInitializedTrace(
      @NotNull final Feature feature) {
    final IonTimeSeries<? extends Scan> series = feature.getFeatureData();
    if (series == null || feature.getHeight() == null || feature.getRT() == null
        || feature.getRawDataFile() == null || feature.getScanNumbers().isEmpty()) {
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
