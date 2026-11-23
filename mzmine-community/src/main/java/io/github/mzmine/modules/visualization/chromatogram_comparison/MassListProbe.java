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

package io.github.mzmine.modules.visualization.chromatogram_comparison;

import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.collections.BinarySearch.DefaultTo;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Finds the data point of the mass list of a scan that a chromatogram could have used. Only gaps
 * with such a data point are holes: without data point in the mass list, the builder could not fill
 * the gap.
 */
@FunctionalInterface
public interface MassListProbe {

  /**
   * @param scans       the {@link ComparisonScans} in the same order
   * @param mzTolerance maximum distance of the data point
   */
  static @NotNull MassListProbe of(@NotNull List<? extends Scan> scans,
      @NotNull MZTolerance mzTolerance) {
    return (scan, mz) -> {
      final MassList massList = scans.get(scan).getMassList();
      if (massList == null || massList.getNumberOfDataPoints() == 0) {
        return Double.NaN;
      }
      final int closest = massList.binarySearch(mz, DefaultTo.CLOSEST_VALUE);
      if (closest < 0) {
        return Double.NaN;
      }
      final double found = massList.getMzValue(closest);
      return mzTolerance.checkWithinTolerance(mz, found) ? found : Double.NaN;
    };
  }

  /**
   * @param scan index into the {@link ComparisonScans}
   * @return the m/z of the closest data point of the mass list within the tolerance of the m/z, NaN
   * if there is none
   */
  double findMz(int scan, double mz);
}
