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
import io.github.mzmine.util.collections.BinarySearch.DefaultTo;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Refines a detected isotope pattern across multiple scans (e.g. within the feature FWHM) instead
 * of pre-merging them. For each isotope offset it recomputes the relative intensity as a robust
 * aggregate of the per-scan {@code offset/base} ratios, and recovers offsets that are resolved in
 * several scans but were absent in the single detection scan. This addresses split/merged fine
 * structure that varies between consecutive scans without blurring it through merging.
 */
public final class CrossScanRefiner {

  private static final DataPointSorter MZ_SORTER = new DataPointSorter(SortingProperty.MZ,
      SortingDirection.Ascending);
  // how many offsets beyond the detected range to probe (in BOTH directions) for signals resolved
  // only in other scans
  private static final int EXTRA_RECOVERY_OFFSETS = 4;
  // a recovered (previously absent) signal must also reach this fraction of the base peak, so
  // persistent low-level background at the probed m/z is not added just because it recurs. The
  // detected signals themselves are never subject to this - refinement must not drop real peaks.
  private static final double MIN_RECOVERED_REL_INTENSITY = 0.001;
  // a recovered offset must additionally be one the predicted envelope allows a peak at, i.e. its
  // plausible upper bound must reach this relative intensity (the same cutoff the engine's
  // termination uses). Only applied when the caller supplies the envelope anchor.
  private static final double MIN_RECOVERED_PREDICTED_BOUND = IsotopeEnvelope.SUPPORT_CUTOFF;

  private CrossScanRefiner() {
  }

  /**
   * Refine without an envelope anchor, i.e. without the predicted-offset plausibility check on
   * recovered offsets.
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
   * @param scans           the scans (mass lists) within the FWHM to refine across.
   * @param tol             m/z tolerance for matching signals across scans.
   * @param aggregation     how to aggregate the per-scan ratios.
   * @param minScansPresent a recovered (previously absent) offset must appear in at least this many
   *                        scans to be added.
   * @param anchor          how the pattern maps to its predicted envelope, or null when unknown. When
   *                        given, a recovered offset must also be one the envelope predicts a peak
   *                        at - recurring in enough scans alone does not make a persistent
   *                        background signal part of the isotope pattern.
   * @return the refined pattern (same charge/description), or the original if refinement is not
   * possible.
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

    double minMz = Double.MAX_VALUE;
    int baseIndex = 0;
    for (int i = 0; i < n; i++) {
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

    final List<DataPoint> refined = new ArrayList<>();
    // decision: refine every detected signal at its OWN m/z rather than one signal per nominal
    // offset. Keying by offset collapsed isotopic fine structure (e.g. 13C2 vs 34S, which the engine
    // deliberately keeps resolved) down to a single point, so enabling refinement REDUCED pattern
    // completeness on high-resolution data.
    final Set<Integer> occupied = new HashSet<>();
    int maxOffset = 0;
    for (int i = 0; i < n; i++) {
      final double mz = detected.getMzValue(i);
      final int offset = (int) Math.round((mz - minMz) / spacing);
      occupied.add(offset);
      maxOffset = Math.max(maxOffset, offset);

      final ScanAggregate agg = aggregateAcrossScans(mz, baseMz, scans, tol, aggregation);
      if (agg == null) {
        // no scan contained the base peak -> keep the detection-scan values unchanged
        refined.add(new SimpleDataPoint(mz, detected.getIntensityValue(i)));
        continue;
      }
      final double intensity = agg.ratio() * baseIntensity;
      refined.add(intensity > 0d ? new SimpleDataPoint(agg.mz(), intensity)
          : new SimpleDataPoint(mz, detected.getIntensityValue(i)));
    }

    // probe unoccupied offsets on BOTH sides to recover signals resolved only in other scans.
    // decision: downward too - the monoisotopic can be missing from the single detection scan, and
    // an upward-only probe could never recover it.
    for (int offset = -EXTRA_RECOVERY_OFFSETS; offset <= maxOffset + EXTRA_RECOVERY_OFFSETS;
        offset++) {
      if (occupied.contains(offset)) {
        continue;
      }
      final double targetMz = minMz + offset * spacing;
      if (targetMz <= 0d) {
        continue;
      }
      // envelope plausibility: only recover where a peak is predicted at all, mirroring the
      // termination check the engine applies to the detected offsets
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

  /**
   * Aggregate the {@code targetMz / baseMz} intensity ratio of one signal across all scans that
   * contain the base peak.
   *
   * @param targetMz    the m/z to measure.
   * @param baseMz      the m/z of the pattern's base peak (the ratio denominator).
   * @param scans       the scans to aggregate over.
   * @param tol         m/z tolerance for matching signals across scans.
   * @param aggregation how to aggregate the per-scan ratios.
   * @return the aggregate, or {@code null} when no scan contained the base peak.
   */
  private static @Nullable ScanAggregate aggregateAcrossScans(final double targetMz,
      final double baseMz, @NotNull final List<? extends MassSpectrum> scans,
      @NotNull final MZTolerance tol, @NotNull final RatioAggregation aggregation) {
    final DoubleArrayList ratios = new DoubleArrayList();
    int presentCount = 0;
    double weightedMzSum = 0d;
    double weightSum = 0d;
    for (final MassSpectrum scan : scans) {
      final MatchedSignal base = matchWithinTolerance(scan, baseMz, tol);
      if (base.intensity() <= 0) {
        continue; // this scan does not contain the base peak -> skip
      }
      final MatchedSignal target = matchWithinTolerance(scan, targetMz, tol);
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
   * Match a probed m/z in one scan, summing ALL data points within the tolerance rather than taking
   * the single nearest one.
   * <p>
   * decision: a split centroid (one peak reported as two adjacent points, common on FT data and
   * after mass-list recalibration) otherwise contributes only part of its intensity, which biases
   * every ratio it takes part in - and asymmetrically, since the base peak may be split in one scan
   * and the target in another. The returned m/z is intensity-weighted over the matched points.
   *
   * @return the summed intensity and intensity-weighted m/z; intensity 0 (m/z {@code NaN}) when no
   * data point falls within the tolerance.
   */
  private static @NotNull MatchedSignal matchWithinTolerance(@NotNull final MassSpectrum scan,
      final double mz, @NotNull final MZTolerance tol) {
    final int n = scan.getNumberOfDataPoints();
    if (n == 0) {
      return MatchedSignal.ABSENT;
    }
    final int idx = scan.binarySearch(mz, DefaultTo.CLOSEST_VALUE);
    if (idx < 0 || !tol.checkWithinTolerance(mz, scan.getMzValue(idx))) {
      return MatchedSignal.ABSENT;
    }
    double sum = 0d;
    double weightedMz = 0d;
    // data points are sorted by m/z, so the matches form a contiguous run around idx
    for (int i = idx; i >= 0 && tol.checkWithinTolerance(mz, scan.getMzValue(i)); i--) {
      sum += scan.getIntensityValue(i);
      weightedMz += scan.getMzValue(i) * scan.getIntensityValue(i);
    }
    for (int i = idx + 1; i < n && tol.checkWithinTolerance(mz, scan.getMzValue(i)); i++) {
      sum += scan.getIntensityValue(i);
      weightedMz += scan.getMzValue(i) * scan.getIntensityValue(i);
    }
    return sum > 0d ? new MatchedSignal(sum, weightedMz / sum)
        : new MatchedSignal(0d, scan.getMzValue(idx));
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
