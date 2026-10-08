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

package io.github.mzmine.modules.dataprocessing.gapfill_gc_ei;

import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.featuredata.FeatureDataUtils;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A feature re-detected by the resolver during GC-EI gap filling.
 *
 * @param series the resolved feature data
 * @param apexRt retention time of the most intense data point
 * @param height intensity of the most intense data point
 */
record GcEiResolvedPeak(@NotNull IonTimeSeries<? extends Scan> series, float apexRt,
                        double height) {

  static @NotNull GcEiResolvedPeak of(@NotNull final IonTimeSeries<? extends Scan> series) {
    final int apex = FeatureDataUtils.getMostIntenseIndex(series);
    return new GcEiResolvedPeak(series, series.getRetentionTime(apex), series.getIntensity(apex));
  }

  /**
   * @param peaks       candidates
   * @param rt          the reference retention time
   * @param rtTolerance maximum allowed apex distance to the reference
   * @return the peak with the apex closest to the reference retention time within the tolerance,
   * the higher peak on ties. Null if no peak is within the tolerance
   */
  static @Nullable GcEiResolvedPeak findClosestApex(@NotNull final List<GcEiResolvedPeak> peaks,
      final float rt, @NotNull final RTTolerance rtTolerance) {
    GcEiResolvedPeak best = null;
    float bestDistance = Float.MAX_VALUE;
    for (final GcEiResolvedPeak peak : peaks) {
      if (!rtTolerance.checkWithinTolerance(rt, peak.apexRt())) {
        continue;
      }
      final float distance = Math.abs(peak.apexRt() - rt);
      if (best == null || distance < bestDistance || (distance == bestDistance
          && peak.height() > best.height())) {
        best = peak;
        bestDistance = distance;
      }
    }
    return best;
  }
}
