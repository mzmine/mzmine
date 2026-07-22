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
import io.github.mzmine.util.SortingDirection;
import io.github.mzmine.util.SortingProperty;
import io.github.mzmine.util.collections.BinarySearch.DefaultTo;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

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
  // how many offsets beyond the detected range to probe for signals resolved only in other scans
  private static final int EXTRA_RECOVERY_OFFSETS = 4;

  private CrossScanRefiner() {
  }

  /**
   * @param detected        the pattern detected on the most intense scan.
   * @param scans           the scans (mass lists) within the FWHM to refine across.
   * @param tol             m/z tolerance for matching signals across scans.
   * @param aggregation     how to aggregate the per-scan ratios.
   * @param minScansPresent a recovered (previously absent) offset must appear in at least this many
   *                        scans to be added.
   * @return the refined pattern (same charge/description), or the original if refinement is not
   * possible.
   */
  public static @NotNull IsotopePattern refine(@NotNull final IsotopePattern detected,
      @NotNull final List<? extends MassSpectrum> scans, @NotNull final MZTolerance tol,
      @NotNull final RatioAggregation aggregation, final int minScansPresent) {
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

    // map detected offsets to their m/z
    final Map<Integer, Double> detectedMz = new HashMap<>();
    int maxOffset = 0;
    for (int i = 0; i < n; i++) {
      final int offset = (int) Math.round((detected.getMzValue(i) - minMz) / spacing);
      detectedMz.put(offset, detected.getMzValue(i));
      maxOffset = Math.max(maxOffset, offset);
    }

    final List<DataPoint> refined = new ArrayList<>();
    // probe a few offsets beyond the detected range to recover signals resolved only in other scans
    for (int offset = 0; offset <= maxOffset + EXTRA_RECOVERY_OFFSETS; offset++) {
      final boolean inDetected = detectedMz.containsKey(offset);
      final double targetMz = inDetected ? detectedMz.get(offset) : minMz + offset * spacing;

      final List<Double> ratios = new ArrayList<>();
      int presentCount = 0;
      double weightedMzSum = 0d;
      double weightSum = 0d;
      for (final MassSpectrum scan : scans) {
        final double baseInScan = closestIntensity(scan, baseMz, tol);
        if (baseInScan <= 0) {
          continue; // this scan does not contain the base peak -> skip
        }
        final double offsetInScan = closestIntensity(scan, targetMz, tol);
        ratios.add(offsetInScan / baseInScan);
        if (offsetInScan > 0) {
          presentCount++;
          final double foundMz = closestMz(scan, targetMz, tol);
          if (!Double.isNaN(foundMz)) {
            weightedMzSum += foundMz * offsetInScan;
            weightSum += offsetInScan;
          }
        }
      }

      if (ratios.isEmpty()) {
        // no scan had the base peak -> keep the detection-scan value if present
        if (inDetected) {
          refined.add(
              new SimpleDataPoint(targetMz, intensityForOffset(detected, offset, spacing, minMz)));
        }
        continue;
      }

      // keep existing offsets; only add new offsets that recur across enough scans
      if (!inDetected && presentCount < minScansPresent) {
        continue;
      }

      final double aggRatio = aggregate(ratios, aggregation);
      final double refinedIntensity = aggRatio * baseIntensity;
      if (refinedIntensity <= 0) {
        continue;
      }
      final double refinedMz = weightSum > 0 ? weightedMzSum / weightSum : targetMz;
      refined.add(new SimpleDataPoint(refinedMz, refinedIntensity));
    }

    if (refined.isEmpty()) {
      return detected;
    }
    refined.sort(MZ_SORTER);
    // preserve the detection score so refined patterns keep their charge ranking
    return new SimpleIsotopePattern(refined.toArray(new DataPoint[0]), charge, detected.getScore(),
        IsotopePatternStatus.DETECTED, detected.getDescription());
  }

  private static double intensityForOffset(final IsotopePattern pattern, final int offset,
      final double spacing, final double minMz) {
    for (int i = 0; i < pattern.getNumberOfDataPoints(); i++) {
      if ((int) Math.round((pattern.getMzValue(i) - minMz) / spacing) == offset) {
        return pattern.getIntensityValue(i);
      }
    }
    return 0d;
  }

  private static double closestIntensity(final MassSpectrum scan, final double mz,
      final MZTolerance tol) {
    if (scan.getNumberOfDataPoints() == 0) {
      return 0d;
    }
    final int idx = scan.binarySearch(mz, DefaultTo.CLOSEST_VALUE);
    if (idx < 0) {
      return 0d;
    }
    return tol.checkWithinTolerance(mz, scan.getMzValue(idx)) ? scan.getIntensityValue(idx) : 0d;
  }

  private static double closestMz(final MassSpectrum scan, final double mz, final MZTolerance tol) {
    if (scan.getNumberOfDataPoints() == 0) {
      return Double.NaN;
    }
    final int idx = scan.binarySearch(mz, DefaultTo.CLOSEST_VALUE);
    if (idx < 0) {
      return Double.NaN;
    }
    return tol.checkWithinTolerance(mz, scan.getMzValue(idx)) ? scan.getMzValue(idx) : Double.NaN;
  }

  private static double aggregate(@NotNull final List<Double> values,
      @NotNull final RatioAggregation aggregation) {
    if (values.isEmpty()) {
      return 0d;
    }
    return switch (aggregation) {
      case MEAN -> {
        double sum = 0d;
        for (final double v : values) {
          sum += v;
        }
        yield sum / values.size();
      }
      case MEDIAN -> {
        final List<Double> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        final int mid = sorted.size() / 2;
        yield sorted.size() % 2 == 1 ? sorted.get(mid)
            : (sorted.get(mid - 1) + sorted.get(mid)) / 2d;
      }
    };
  }
}
