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

package io.github.mzmine.modules.dataprocessing.filter_isotopefinder.engine;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.IsotopePattern.IsotopePatternStatus;
import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.datamodel.impl.SimpleIsotopePattern;
import io.github.mzmine.modules.tools.isotopeprediction.IsotopePatternCalculator;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.DataPointSorter;
import io.github.mzmine.util.MathUtils;
import io.github.mzmine.util.SortingDirection;
import io.github.mzmine.util.SortingProperty;
import io.github.mzmine.util.collections.BinarySearch;
import io.github.mzmine.util.collections.IndexRange;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Refines a detected pattern across several scans (the feature FWHM) instead of pre-merging them:
 * each offset's relative intensity becomes a robust aggregate of the per-scan {@code offset/base}
 * ratios, and offsets resolved in other scans but absent from the detection scan are recovered.
 * This handles fine structure that splits or merges between consecutive scans without blurring it.
 */
public final class CrossScanRefiner {

  private static final DataPointSorter MZ_SORTER = new DataPointSorter(SortingProperty.MZ,
      SortingDirection.Ascending);
  // offsets beyond the detected range to probe, in BOTH directions
  private static final int EXTRA_RECOVERY_OFFSETS = 4;
  // so persistent low-level background is not added just because it recurs. Applies to RECOVERED
  // signals only - refinement must never drop a detected peak.
  private static final double MIN_RECOVERED_REL_INTENSITY = 0.001;
  // a recovered offset must also be one the envelope allows a peak at, using the same cutoff as the
  // engine's termination. Only applied when the caller supplies an anchor.
  private static final double MIN_RECOVERED_PREDICTED_BOUND = IsotopeEnvelope.SUPPORT_CUTOFF;

  private CrossScanRefiner() {
  }

  /**
   * Refine without the predicted-offset plausibility check on recovered offsets.
   *
   * @see #refine(IsotopePattern, List, MZTolerance, RatioAggregation, int, PatternAnchor)
   */
  public static @NotNull IsotopePattern refine(@NotNull final IsotopePattern detected,
      @NotNull final List<? extends MassSpectrum> scans, @NotNull final MZTolerance tol,
      @NotNull final RatioAggregation aggregation, final int minScansPresent) {
    return refine(detected, scans, tol, aggregation, minScansPresent, null);
  }

  /**
   * @param detected        the pattern detected on the most intense scan.
   * @param scans           the mass lists within the FWHM to refine across.
   * @param minScansPresent scans a recovered offset must appear in before it is added.
   * @param anchor          how the pattern maps to its predicted envelope, or null when unknown.
   *                        Given one, a recovered offset must also be predicted: recurring in enough
   *                        scans alone does not make background part of the pattern.
   * @return the refined pattern, or the original when refinement is not possible.
   */
  public static @NotNull IsotopePattern refine(@NotNull final IsotopePattern detected,
      @NotNull final List<? extends MassSpectrum> scans, @NotNull final MZTolerance tol,
      @NotNull final RatioAggregation aggregation, final int minScansPresent,
      @Nullable final PatternAnchor anchor) {
    final int n = detected.getNumberOfDataPoints();
    if (n == 0 || scans.isEmpty()) {
      return detected;
    }

    final int charge = detected.getCharge() > 0 ? detected.getCharge() : 1;
    final double spacing = IsotopePatternCalculator.THIRTHEEN_C_DISTANCE / charge;

    final List<DataPoint> points = new ArrayList<>(n);
    double minMz = Double.MAX_VALUE;
    int baseIndex = 0;
    for (int i = 0; i < n; i++) {
      points.add(new SimpleDataPoint(detected.getMzValue(i), detected.getIntensityValue(i)));
      minMz = Math.min(minMz, detected.getMzValue(i));
      if (detected.getIntensityValue(i) > detected.getIntensityValue(baseIndex)) {
        baseIndex = i;
      }
    }
    final double baseMz = detected.getMzValue(baseIndex);
    final double baseIntensity = detected.getIntensityValue(baseIndex);
    if (baseIntensity <= 0) {
      return detected;
    }

    // decision: indexed through the shared CarbonLadder rather than a local round((mz - minMz) /
    // spacing), so the refiner's idea of "which offset is this" cannot drift from the engine's.
    // Offsets are relative to the LOWEST detected m/z, hence all >= 0.
    final CarbonLadder ladder = CarbonLadder.build(points, minMz, spacing, tol);
    final TreeMap<Integer, OffsetPeak> occupied = ladder.collapsed();
    final int maxOffset = occupied.isEmpty() ? 0 : occupied.lastKey();

    final List<DataPoint> refined = new ArrayList<>();
    // decision: every detected signal at its OWN m/z, not one per nominal offset. Keying by offset
    // collapsed fine structure the engine deliberately keeps resolved into a single point, so
    // enabling refinement REDUCED pattern completeness on high-resolution data.
    for (final DataPoint dp : points) {
      final double mz = dp.getMZ();
      final ScanAggregate agg = aggregateAcrossScans(mz, baseMz, scans, tol, aggregation);
      if (agg == null) {
        // no scan contained the base peak -> keep the detection-scan values unchanged
        refined.add(dp);
        continue;
      }
      final double intensity = agg.ratio() * baseIntensity;
      refined.add(intensity > 0d ? new SimpleDataPoint(agg.mz(), intensity) : dp);
    }

    // decision: probed downward as well as up - the monoisotopic can be missing from the single
    // detection scan, and an upward-only probe could never recover it
    for (int offset = -EXTRA_RECOVERY_OFFSETS; offset <= maxOffset + EXTRA_RECOVERY_OFFSETS;
        offset++) {
      if (occupied.containsKey(offset)) {
        continue;
      }
      final double targetMz = ladder.exactMzAt(offset);
      if (targetMz <= 0d) {
        continue;
      }
      // only recover where a peak is predicted at all, mirroring the engine's termination check
      if (anchor != null && anchor.env().upperBoundAt(anchor.predictedOffsetOf(targetMz))
          < MIN_RECOVERED_PREDICTED_BOUND) {
        continue;
      }
      final ScanAggregate agg = aggregateAcrossScans(targetMz, baseMz, scans, tol, aggregation);
      if (agg == null || agg.presentCount() < minScansPresent) {
        continue;
      }
      final double intensity = agg.ratio() * baseIntensity;
      if (intensity <= 0d || intensity < MIN_RECOVERED_REL_INTENSITY * baseIntensity) {
        continue;
      }
      refined.add(new SimpleDataPoint(agg.mz(), intensity));
    }

    if (refined.isEmpty()) {
      return detected;
    }
    refined.sort(MZ_SORTER);
    // preserve the detection score so refined patterns keep their charge ranking
    return new SimpleIsotopePattern(refined.toArray(new DataPoint[0]), charge, detected.getScore(),
        IsotopePatternStatus.DETECTED, detected.getDescription());
  }

  private static @Nullable ScanAggregate aggregateAcrossScans(final double targetMz,
      final double baseMz, @NotNull final List<? extends MassSpectrum> scans,
      @NotNull final MZTolerance tol, @NotNull final RatioAggregation aggregation) {
    // the two m/z windows do not depend on the scan, so they are built once here: each scan then
    // costs two binary searches plus a sum over the matched index range, with no per-data-point
    // tolerance checks at all
    final Range<Double> baseWindow = tol.getToleranceRange(baseMz);
    final Range<Double> targetWindow = tol.getToleranceRange(targetMz);
    final DoubleArrayList ratios = new DoubleArrayList();
    int presentCount = 0;
    double weightedMzSum = 0d;
    double weightSum = 0d;
    for (final MassSpectrum scan : scans) {
      final MatchedSignal base = matchWithinTolerance(scan, baseWindow);
      if (base.intensity() <= 0) {
        continue; // this scan does not contain the base peak -> skip
      }
      final MatchedSignal target = matchWithinTolerance(scan, targetWindow);
      ratios.add(target.intensity() / base.intensity());
      if (target.intensity() > 0) {
        presentCount++;
        weightedMzSum += target.mz() * target.intensity();
        weightSum += target.intensity();
      }
    }
    if (ratios.isEmpty()) {
      return null;
    }
    return new ScanAggregate(aggregate(ratios.toDoubleArray(), aggregation),
        weightSum > 0 ? weightedMzSum / weightSum : targetMz, presentCount);
  }

  /**
   * Match a probed m/z window in one scan, summing ALL data points inside it rather than taking the
   * single nearest one.
   * <p>
   * decision: a split centroid (one peak reported as two adjacent points, common on FT data and
   * after mass-list recalibration) otherwise contributes only part of its intensity, which biases
   * every ratio it takes part in - and asymmetrically, since the base peak may be split in one scan
   * and the target in another. The returned m/z is intensity-weighted over the matched points.
   *
   * @param mzWindow the tolerance window of the probed m/z, built once by the caller.
   * @return the summed intensity and intensity-weighted m/z; intensity 0 (m/z {@code NaN}) when no
   * data point falls inside the window.
   */
  private static @NotNull MatchedSignal matchWithinTolerance(@NotNull final MassSpectrum scan,
      @NotNull final Range<Double> mzWindow) {
    final int n = scan.getNumberOfDataPoints();
    if (n == 0) {
      return MatchedSignal.ABSENT;
    }
    // data points are sorted by m/z, so the matches are exactly one contiguous index range
    final IndexRange matched = BinarySearch.indexRange(mzWindow, n, scan::getMzValue);
    if (matched.isEmpty()) {
      return MatchedSignal.ABSENT;
    }
    double sum = 0d;
    double weightedMz = 0d;
    for (int i = matched.min(); i <= matched.maxInclusive(); i++) {
      final double intensity = scan.getIntensityValue(i);
      sum += intensity;
      weightedMz += scan.getMzValue(i) * intensity;
    }
    return sum > 0d ? new MatchedSignal(sum, weightedMz / sum)
        : new MatchedSignal(0d, scan.getMzValue(matched.min()));
  }

  /**
   * The cross-scan aggregate of one probed m/z.
   *
   * @param ratio        aggregated {@code target / base} intensity ratio.
   * @param mz           intensity-weighted mean m/z of the matched signals, or the probed m/z when
   *                     none were found.
   * @param presentCount number of scans in which the signal was actually present.
   */
  private record ScanAggregate(double ratio, double mz, int presentCount) {

  }

  /**
   * One probed m/z matched in a single scan.
   *
   * @param intensity summed intensity of every data point within the tolerance (0 = absent).
   * @param mz        intensity-weighted m/z of those points, {@code NaN} when absent.
   */
  private record MatchedSignal(double intensity, double mz) {

    private static final MatchedSignal ABSENT = new MatchedSignal(0d, Double.NaN);
  }

  private static double aggregate(final double @NotNull [] values,
      @NotNull final RatioAggregation aggregation) {
    if (values.length == 0) {
      return 0d;
    }
    return switch (aggregation) {
      case MEAN -> MathUtils.calcAvg(values);
      case MEDIAN -> MathUtils.calcMedian(values);
    };
  }
}
