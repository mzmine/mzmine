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

package io.github.mzmine.modules.dataprocessing.featdet_fastchromatogrambuilder;

import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

/**
 * Explains resolved features without a match in the other feature list by looking at the
 * chromatograms of the other builder at the m/z and retention time of the feature.
 */
final class ResolvedFeatureDifferences {

  private ResolvedFeatureDifferences() {
  }

  /**
   * @param unmatched          features without a feature in the other list
   * @param otherLacksSignal   the other chromatograms within the tolerance lack the peak, less than
   *                           half of the feature height in its scan range
   * @param otherHasMoreHoles  the other chromatogram has the peak but more missing scans in the
   *                           feature range
   * @param otherResolvedApart the other chromatogram has the peak without more holes, the resolver
   *                           made a different decision
   * @param medianHeight       median height of the unmatched features
   * @param medianHeightAll    median height of all features
   * @param examples           a few unmatched features per category
   */
  record Result(int unmatched, int otherLacksSignal, int otherHasMoreHoles, int otherResolvedApart,
                double medianHeight, double medianHeightAll, @NotNull List<String> examples) {

  }

  @NotNull
  static Result explain(@NotNull FeatureList resolved, @NotNull FeatureList otherResolved,
      @NotNull List<EvaluatedChromatogram> otherChromatograms, @NotNull Scan[] scans,
      @NotNull MZTolerance tolerance, float rtTolerance) {
    final Map<Scan, Integer> scanIndex = new IdentityHashMap<>(scans.length);
    for (int i = 0; i < scans.length; i++) {
      scanIndex.put(scans[i], i);
    }
    final double[] otherMzs = new double[otherChromatograms.size()];
    for (int c = 0; c < otherMzs.length; c++) {
      otherMzs[c] = otherChromatograms.get(c).weightedMz();
    }
    final List<FeatureListRow> otherRows = otherResolved.getRows();

    int unmatched = 0;
    int lacksSignal = 0;
    int moreHoles = 0;
    int resolvedApart = 0;
    final DoubleArrayList unmatchedHeights = new DoubleArrayList();
    final DoubleArrayList allHeights = new DoubleArrayList();
    final List<String> examples = new ArrayList<>();
    final int[] examplesPerCategory = new int[3];

    for (final FeatureListRow row : resolved.getRows()) {
      final Feature feature = row.getFeatures().getFirst();
      final double mz = feature.getMZ();
      final float rt = feature.getRT();
      final double height = feature.getHeight();
      allHeights.add(height);
      final double tol = tolerance.getMzToleranceForMass(mz);
      boolean matched = false;
      for (final FeatureListRow other : otherRows) {
        if (Math.abs(other.getAverageMZ() - mz) <= tol
            && Math.abs(other.getAverageRT() - rt) <= rtTolerance) {
          matched = true;
          break;
        }
      }
      if (matched) {
        continue;
      }
      unmatched++;
      unmatchedHeights.add(height);

      // detected scan range of the feature
      final IonTimeSeries<? extends Scan> series = feature.getFeatureData();
      int first = Integer.MAX_VALUE;
      int last = -1;
      int detected = 0;
      for (int i = 0; i < series.getNumberOfValues(); i++) {
        if (series.getIntensity(i) > 0) {
          final int s = scanIndex.get(series.getSpectrum(i));
          first = Math.min(first, s);
          last = Math.max(last, s);
          detected++;
        }
      }
      if (last < 0) {
        continue;
      }

      double bestMax = 0;
      int bestDetected = 0;
      for (int c = 0; c < otherChromatograms.size(); c++) {
        if (Math.abs(otherMzs[c] - mz) > tol) {
          continue;
        }
        final EvaluatedChromatogram chrom = otherChromatograms.get(c);
        double max = 0;
        int inRange = 0;
        for (int i = 0; i < chrom.size(); i++) {
          if (chrom.scans()[i] >= first && chrom.scans()[i] <= last) {
            max = Math.max(max, chrom.intensities()[i]);
            inRange++;
          }
        }
        if (max > bestMax) {
          bestMax = max;
          bestDetected = inRange;
        }
      }

      final int category;
      if (bestMax < 0.5 * height) {
        lacksSignal++;
        category = 0;
      } else if (bestDetected < detected) {
        moreHoles++;
        category = 1;
      } else {
        resolvedApart++;
        category = 2;
      }
      if (examplesPerCategory[category]++ < 5) {
        examples.add(
            "%s mz %.5f rt %.3f height %.3g scans %d-%d detected %d, other max %.3g detected %d".formatted(
                switch (category) {
                  case 0 -> "lacks signal";
                  case 1 -> "more holes";
                  default -> "resolved apart";
                }, mz, rt, height, first, last, detected, bestMax, bestDetected));
      }
    }
    return new Result(unmatched, lacksSignal, moreHoles, resolvedApart, median(unmatchedHeights),
        median(allHeights), examples);
  }

  private static double median(@NotNull DoubleArrayList values) {
    if (values.isEmpty()) {
      return 0;
    }
    final double[] sorted = values.toDoubleArray();
    Arrays.sort(sorted);
    return sorted[sorted.length / 2];
  }
}
