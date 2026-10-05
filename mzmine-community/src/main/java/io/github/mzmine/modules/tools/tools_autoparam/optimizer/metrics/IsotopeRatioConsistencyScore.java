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

package io.github.mzmine.modules.tools.tools_autoparam.optimizer.metrics;

import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.PercentTolerance;
import io.github.mzmine.util.FeatureListRowSorter;
import io.github.mzmine.util.MathUtils;
import io.github.mzmine.util.collections.BinarySearch;
import io.github.mzmine.util.collections.BinarySearch.DefaultTo;
import io.github.mzmine.util.collections.IndexRange;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Checks the ratios of isotopic signals in a feature list across samples. This approach should
 * perfom similar to the SLAW-integration score but not rely on QC samples.
 *<br>
 * Initial idea for this score: Yasin El Abiedad
 */
public final class IsotopeRatioConsistencyScore implements SweepMetric {

  private final PercentTolerance areaTolerance = new PercentTolerance(0.2);

  @Override
  public @NotNull String name() {
    return "Isotope ratio consistency score";
  }

  @Override
  public @NotNull String getUniqueID() {
    return "isotope_ratio_consistency";
  }

  @Override
  public boolean higherIsBetter() {
    return true;
  }

  /**
   * decision: preferred for sorting and comparing results, as it rewards consistent integration
   * without relying on QC samples
   */
  @Override
  public boolean preferredForRanking() {
    return true;
  }

  @Override
  public @NotNull String toString() {
    return name();
  }

  @Override
  public double evaluate(@NotNull FeatureList featureList, @NotNull MetricContext context) {
    // discard low intensities, similar to IPO
    final double noise = MathUtils.calcQuantile(
        featureList.streamFeatures(false).mapToDouble(Feature::getHeight).toArray(), 0.03);
    final long totalFeatures = featureList.streamFeatures().count();

    long correctlyIntegrated = 0;
    long singleFileFeatures = 0;

    List<FeatureListRow> rowsByMz = featureList.getRowsCopy();
    rowsByMz.sort(FeatureListRowSorter.MZ_ASCENDING);

    for (FeatureListRow row : rowsByMz) {
      final Feature bestFeature = row.getBestFeature();
      if (bestFeature == null || bestFeature.getHeight() < noise) {
        continue;
      }
      final IsotopePattern bestIso = bestFeature.getIsotopePattern();
      if (bestIso == null) {
        continue;
      }
      final int mainFeatureIndex = bestIso.binarySearch(bestFeature.getMZ(),
          DefaultTo.CLOSEST_VALUE);
      if (mainFeatureIndex == bestIso.getNumberOfDataPoints() - 1) {
        // last peak in pattern
        continue;
      }

      final List<FeatureListRow> isotopeRows = new ArrayList<>(
          bestIso.getNumberOfDataPoints() - mainFeatureIndex);
      // only add higher mass isotopes
      for (int i = mainFeatureIndex + 1; i < bestIso.getNumberOfDataPoints(); i++) {
        IndexRange eligibleMzs = BinarySearch.indexRange(
            MZTolerance.FIFTEEN_PPM_OR_FIVE_MDA.getToleranceRange(bestIso.getMzValue(i)), rowsByMz,
            FeatureListRow::getAverageMZ);
        // todo: also check for noise?
        eligibleMzs.sublist(rowsByMz, false).stream()/*.filter(r -> r.getMaxHeight() > noise)*/.min(
                Comparator.comparingDouble(r -> Math.abs(r.getAverageRT() - row.getAverageRT())))
            .ifPresent(isotopeRows::add);
      }

      if (isotopeRows.isEmpty()) {
        continue;
      }

      final float bestMainPeakArea = bestFeature.getArea();
      final FloatArrayList benchmarkRatios = new FloatArrayList();
      for (int i = 0; i < isotopeRows.size(); i++) {
        final FeatureListRow isoRow = isotopeRows.get(i);
        final var isoFeature = isoRow.getFeature(bestFeature.getRawDataFile());
        if (isoFeature != null) {
          benchmarkRatios.add(bestMainPeakArea / isoFeature.getArea());
        } else {
          isotopeRows.remove(i);
          i--;
        }
      }

      final int foundCount[] = new int[isotopeRows.size()];
      for (final RawDataFile file : row.getRawDataFiles()) {
        final Feature feature = row.getFeature(file);
        if (feature == null || feature == bestFeature || feature.getHeight() < noise) {
          continue;
        }

        for (int i = 0; i < isotopeRows.size(); i++) {
          final FeatureListRow isotopeRow = isotopeRows.get(i);
          final Feature isotopeFeature = isotopeRow.getFeature(file);
          if (isotopeFeature == null) {
            continue;
          }
          // todo check for noise here?
          if (areaTolerance.matches(benchmarkRatios.getFloat(i),
              feature.getArea() / isotopeFeature.getArea())) {
            foundCount[i]++;
          }
        }
      }

      for (int i = 0; i < foundCount.length; i++) {
        if (foundCount[i] >= row.getRawDataFiles().size() * 0.5) {
          correctlyIntegrated += foundCount[i];
        } else if (foundCount[i] == 0) {
          singleFileFeatures++;
        }
      }
    }

    return (double) (correctlyIntegrated * correctlyIntegrated) / Math.max(
        totalFeatures/* + singleFileFeatures*/, 1);
  }
}
