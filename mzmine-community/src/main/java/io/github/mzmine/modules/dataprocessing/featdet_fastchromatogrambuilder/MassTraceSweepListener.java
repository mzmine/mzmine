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

package io.github.mzmine.modules.dataprocessing.featdet_fastchromatogrambuilder;

import org.jetbrains.annotations.NotNull;

/**
 * Receives the events of a {@link MassTraceSweeper}. Slots are reused after a trace was closed, the
 * trace id is unique within one sweep and can be read with {@link MassTraceSweeper#getTraceId(int)}
 * while the slot is in use.
 */
interface MassTraceSweepListener {

  /**
   * A new trace was started in the current scan. Called before the first
   * {@link #onDataPointAssigned(MassTraceSweeper, int, int, double, double)} of this trace.
   */
  default void onTraceCreated(@NotNull MassTraceSweeper sweeper, int slot) {
  }

  /**
   * A data point of the current scan was added to the trace in slot.
   */
  default void onDataPointAssigned(@NotNull MassTraceSweeper sweeper, int slot, int scanIndex,
      double mz, double intensity) {
  }

  /**
   * Two traces that existed before the current scan and whose centers are within the m/z tolerance
   * both received a data point in the current scan. Only called if
   * {@link #isCollisionTrackingEnabled()}.
   */
  default void onCollision(@NotNull MassTraceSweeper sweeper, int slotA, int slotB) {
  }

  /**
   * The trace in slot is closed, its statistics are still readable from the sweeper during this
   * call. The slot is reused afterwards.
   */
  default void onTraceClosed(@NotNull MassTraceSweeper sweeper, int slot) {
  }

  /**
   * All data points of the scan were assigned and stale traces were closed.
   */
  default void onScanFinished(@NotNull MassTraceSweeper sweeper, int scanIndex) {
  }

  /**
   * @return true to receive {@link #onCollision(MassTraceSweeper, int, int)} events
   */
  default boolean isCollisionTrackingEnabled() {
    return false;
  }
}
