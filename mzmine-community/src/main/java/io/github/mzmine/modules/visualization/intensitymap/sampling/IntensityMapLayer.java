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

package io.github.mzmine.modules.visualization.intensitymap.sampling;

import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.gui.preferences.NumberFormats;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.modules.visualization.intensitymap.data.IntensityMapGrid;
import io.github.mzmine.modules.visualization.intensitymap.data.IntensityMapSeries;
import java.util.List;
import javafx.scene.paint.Color;
import org.jetbrains.annotations.NotNull;

/**
 * A raw sample and extraction range with stable identity and color across edits and resampling.
 *
 * @param fullRange true for the default layer that covers the complete selected m/z range
 */
public record IntensityMapLayer(@NotNull String id, @NotNull RawDataFile file,
                      @NotNull Range<Double> mzRange, @NotNull Color color, boolean fullRange) {

  public IntensityMapLayer {
    if (!validRange(mzRange)) {
      throw new IllegalArgumentException("m/z ranges must have finite, nonnegative bounds");
    }
  }

  /**
   * Merges overlapping ranges, e.g. of the same feature in several samples, so that every ion is
   * shown once.
   *
   * @return disjoint ranges sorted by m/z
   */
  public static @NotNull List<Range<Double>> mergeOverlapping(
      @NotNull final List<Range<Double>> ranges) {
    final RangeSet<Double> merged = TreeRangeSet.create();
    ranges.stream().filter(IntensityMapLayer::validRange).forEach(merged::add);
    return List.copyOf(merged.asRanges());
  }

  public static boolean validRange(@NotNull final Range<Double> range) {
    return range.hasLowerBound() && range.hasUpperBound() && !range.isEmpty()
        && Double.isFinite(range.lowerEndpoint()) && Double.isFinite(range.upperEndpoint())
        && range.lowerEndpoint() >= 0;
  }

  @NotNull String mzDescription() {
    final NumberFormats formats = ConfigService.getGuiFormats();
    final double lower = mzRange.lowerEndpoint();
    final double upper = mzRange.upperEndpoint();
    if (upper - lower < 1) {
      // narrow extraction windows read best as center and tolerance
      final double center = (lower + upper) / 2;
      final double ppm = (upper - center) / center * 1e6;
      return "m/z " + formats.mz(center) + " ± " + formats.ppm(ppm) + " ppm";
    }
    return "m/z " + formats.mz(mzRange);
  }

  public @NotNull IntensityMapSeries toSeries(@NotNull final IntensityMapGrid data) {
    return new IntensityMapSeries(id, file.getName(), fullRange ? "all m/z" : mzDescription(), data,
        color);
  }
}
