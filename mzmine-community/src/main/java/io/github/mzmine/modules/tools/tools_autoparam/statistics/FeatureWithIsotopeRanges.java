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

package io.github.mzmine.modules.tools.tools_autoparam.statistics;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.modules.tools.isotopeprediction.IsotopePatternCalculator;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.scans.ScanUtils;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record FeatureWithIsotopeRanges(double initialMz, @NotNull ModularFeature mostIntenseIsotope,
                                       @NotNull List<@NotNull Range<Double>> isotopeMzsSorted) {

  private static final int MAX_ISOTOPES = 5;

  /**
   * @param mostIntenseIsotope
   * @param maxTolerance
   * @return null if no potential isotope peaks were found in the given range
   */
  @Nullable
  public static FeatureWithIsotopeRanges of(final double initialMz,
      @NotNull ModularFeature mostIntenseIsotope, MZTolerance maxTolerance) {
    final Scan scan = mostIntenseIsotope.getRepresentativeScan();
    final double mainMz = mostIntenseIsotope.getMZ();

    final List<DataPoint> isotopePeaks = new ArrayList<>();

    for (int i = 1; i < MAX_ISOTOPES; i++) {
      final DataPoint basePeak = ScanUtils.findBasePeak(scan, maxTolerance.getToleranceRange(
          mainMz + i * IsotopePatternCalculator.THIRTHEEN_C_DISTANCE));
      if (basePeak == null) {
        break;
      }

      isotopePeaks.add(basePeak);
    }

    if (isotopePeaks.isEmpty()) {
      return null;
    }

    return new FeatureWithIsotopeRanges(initialMz, mostIntenseIsotope,
        isotopePeaks.stream().map(dp -> maxTolerance.getToleranceRange(dp.getMZ())).toList());
  }
}
