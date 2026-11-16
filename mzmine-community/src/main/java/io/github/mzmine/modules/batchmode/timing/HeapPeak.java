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

package io.github.mzmine.modules.batchmode.timing;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Peak heap usage observed by a {@link HeapPeakTracker} during a time window, e.g. a batch step.
 * Both values are in GiB (2^30 bytes) like -Xmx and
 * {@link io.github.mzmine.main.MZmineConfiguration#getUsedMemoryGB()}.
 *
 * @param peakHeapGB        highest used heap, taken right before each garbage collection and at
 *                          the end of the window. This depends on -Xmx as the garbage collector lets
 *                          the heap fill up before it collects, so it is an upper bound of what the
 *                          window needs.
 * @param peakHeapAfterGcGB highest used heap right after a full garbage collection, which is the
 *                          peak of live data, or after any collection if there was no full one. A
 *                          young collection keeps the garbage of the old generation, so this is an
 *                          upper bound without full collections. The better basis to estimate the
 *                          required heap. Null if no garbage collection happened during the window
 *                          or GC notifications are not available.
 * @param heapMeasurementQuality whether the value is based on an observed full collection
 */
public record HeapPeak(double peakHeapGB, @Nullable Double peakHeapAfterGcGB,
                       @NotNull HeapMeasurementQuality heapMeasurementQuality) {

  /**
   * Without collection type information, an after-GC value is not confirmed as a full GC sample.
   */
  public HeapPeak(final double peakHeapGB, @Nullable final Double peakHeapAfterGcGB) {
    this(peakHeapGB, peakHeapAfterGcGB, peakHeapAfterGcGB == null ? HeapMeasurementQuality.NO_GC
        : HeapMeasurementQuality.PARTIAL_GC);
  }

  /**
   * @return the larger value of both peaks with the weaker measurement quality; null values are
   * ignored
   */
  public static @NotNull HeapPeak max(@NotNull final HeapPeak a, @NotNull final HeapPeak b) {
    final Double afterGc;
    if (a.peakHeapAfterGcGB == null) {
      afterGc = b.peakHeapAfterGcGB;
    } else if (b.peakHeapAfterGcGB == null) {
      afterGc = a.peakHeapAfterGcGB;
    } else {
      afterGc = Math.max(a.peakHeapAfterGcGB, b.peakHeapAfterGcGB);
    }
    final HeapMeasurementQuality quality =
        a.heapMeasurementQuality.ordinal() >= b.heapMeasurementQuality.ordinal()
            ? a.heapMeasurementQuality : b.heapMeasurementQuality;
    return new HeapPeak(Math.max(a.peakHeapGB, b.peakHeapGB), afterGc, quality);
  }
}
