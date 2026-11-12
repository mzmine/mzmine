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

import io.github.mzmine.datamodel.features.Feature;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The detected data points of one chromatogram. The zero intensities that the builders add next to
 * each signal are removed.
 *
 * @param rowId       id of the feature list row
 * @param mz          m/z of the chromatogram
 * @param height      highest intensity
 * @param scans       ascending indices into the {@link ComparisonScans}
 * @param intensities intensity of each data point, all above zero
 * @param mzs         m/z of each data point
 * @param feature     the chromatogram for the charts, null in tests
 */
public record ComparedChromatogram(@NotNull ComparisonSide side, int rowId, double mz,
                                   double height, int @NotNull [] scans,
                                   double @NotNull [] intensities, double @NotNull [] mzs,
                                   @Nullable Feature feature) {

  public ComparedChromatogram {
    if (intensities.length != scans.length || mzs.length != scans.length) {
      throw new IllegalArgumentException("All arrays need one value per data point");
    }
  }

  public static @NotNull ComparedChromatogram create(@NotNull ComparisonSide side, int rowId,
      double mz, int @NotNull [] scans, double @NotNull [] intensities, double @NotNull [] mzs,
      @Nullable Feature feature) {
    double height = 0;
    for (final double intensity : intensities) {
      height = Math.max(height, intensity);
    }
    return new ComparedChromatogram(side, rowId, mz, height, scans, intensities, mzs, feature);
  }

  public int numDataPoints() {
    return scans.length;
  }
}
