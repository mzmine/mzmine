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
import java.util.List;
import java.util.TreeMap;
import org.jetbrains.annotations.NotNull;

/**
 * Collapses candidate signals that fall within the same nominal isotope offset into a single
 * per-offset peak (summed intensity, intensity-weighted mean m/z). This handles isotopic fine
 * structure (e.g. 13C vs 15N at the same nominal offset) for scoring, while the raw signals are
 * retained elsewhere for the stored pattern.
 */
public final class FineStructureCollapser {

  private FineStructureCollapser() {
  }

  /**
   * @param candidates   detected signals.
   * @param anchorMz     the m/z mapped to {@code anchorOffset}.
   * @param anchorOffset the integer offset of the anchor (typically the predicted base-peak
   *                     offset).
   * @param spacingDa    m/z spacing between consecutive offsets (13C distance / charge).
   * @return offset -> collapsed peak, sorted by offset.
   */
  public static @NotNull TreeMap<Integer, OffsetPeak> collapse(
      @NotNull final List<DataPoint> candidates, final double anchorMz, final int anchorOffset,
      final double spacingDa) {
    // accumulate summed intensity and intensity-weighted m/z per offset
    final TreeMap<Integer, double[]> acc = new TreeMap<>(); // offset -> {sumIntensity, sumMz*intensity}
    for (final DataPoint dp : candidates) {
      final int offset = anchorOffset + (int) Math.round((dp.getMZ() - anchorMz) / spacingDa);
      final double[] v = acc.computeIfAbsent(offset, k -> new double[2]);
      v[0] += dp.getIntensity();
      v[1] += dp.getMZ() * dp.getIntensity();
    }
    final TreeMap<Integer, OffsetPeak> result = new TreeMap<>();
    for (final var entry : acc.entrySet()) {
      final double sumIntensity = entry.getValue()[0];
      final double mz = sumIntensity > 0 ? entry.getValue()[1] / sumIntensity : Double.NaN;
      result.put(entry.getKey(), new OffsetPeak(entry.getKey(), mz, sumIntensity));
    }
    return result;
  }
}
