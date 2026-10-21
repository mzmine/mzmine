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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HeapPeakTrackerTest {

  private static final long GB = 1L << 30;

  @Test
  void distinguishesFullPartialAndMissingCollections() {
    final HeapPeakTracker full = new HeapPeakTracker(100);
    full.onGc(101, 4 * GB, GB, true);
    full.onGc(102, 5 * GB, 3 * GB, false);
    final HeapPeak fullPeak = full.stop();
    // A larger young-GC measurement must not replace the full-GC live heap sample.
    assertEquals(1, fullPeak.peakHeapAfterGcGB());
    assertEquals(HeapMeasurementQuality.FULL_GC, fullPeak.heapMeasurementQuality());

    final HeapPeakTracker partial = new HeapPeakTracker(100);
    partial.onGc(101, 4 * GB, 2 * GB, false);
    final HeapPeak partialPeak = partial.stop();
    assertEquals(2, partialPeak.peakHeapAfterGcGB());
    assertEquals(HeapMeasurementQuality.PARTIAL_GC, partialPeak.heapMeasurementQuality());

    final HeapPeakTracker missing = new HeapPeakTracker(100);
    // A full collection from the preceding step must not make this step reliable.
    missing.onGc(99, 4 * GB, GB, true);
    final HeapPeak missingPeak = missing.stop();
    assertNull(missingPeak.peakHeapAfterGcGB());
    assertEquals(HeapMeasurementQuality.NO_GC, missingPeak.heapMeasurementQuality());
  }

  @Test
  void onlyConfirmedFullCollectionActionsAreReliable() {
    assertTrue(HeapPeakTracker.isFullGc("end of major GC"));
    assertFalse(HeapPeakTracker.isFullGc("end of minor GC"));
    assertFalse(HeapPeakTracker.isFullGc("end of concurrent GC pause"));
    assertFalse(HeapPeakTracker.isFullGc("unknown"));
  }

  @Test
  void aggregateQualityDoesNotHideUnreliableSteps() {
    final HeapPeak full = new HeapPeak(3, 1.0, HeapMeasurementQuality.FULL_GC);
    final HeapPeak partial = new HeapPeak(4, 2.0, HeapMeasurementQuality.PARTIAL_GC);
    assertEquals(HeapMeasurementQuality.FULL_GC, HeapPeak.max(full, full).heapMeasurementQuality());
    assertEquals(HeapMeasurementQuality.PARTIAL_GC,
        HeapPeak.max(full, partial).heapMeasurementQuality());
    assertEquals(HeapMeasurementQuality.NO_GC,
        HeapPeak.max(full, new HeapPeak(5, null)).heapMeasurementQuality());
  }
}
