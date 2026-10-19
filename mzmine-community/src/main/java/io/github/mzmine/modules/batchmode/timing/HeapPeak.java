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
 */
public record HeapPeak(double peakHeapGB, @Nullable Double peakHeapAfterGcGB) {

  /**
   * @return the larger value of both peaks, null values are ignored
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
    return new HeapPeak(Math.max(a.peakHeapGB, b.peakHeapGB), afterGc);
  }
}
