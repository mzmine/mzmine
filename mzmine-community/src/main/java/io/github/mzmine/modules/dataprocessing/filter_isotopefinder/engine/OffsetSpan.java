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

import io.github.mzmine.util.collections.IndexRange;
import org.jetbrains.annotations.NotNull;

/**
 * An inclusive span of 13C-grid offsets, walked in steps of {@code step} (1 = every offset, 2 =
 * every second).
 * <p>
 * decision: not {@link IndexRange}. Offsets are SIGNED - the observed base is 0 and the
 * monoisotopic side negative - while {@code IndexRange} reserves -1 as its empty sentinel, so a
 * span reaching offset -1 would silently collapse to empty.
 */
record OffsetSpan(int min, int maxInclusive, int step) {

  OffsetSpan {
    if (step < 1) {
      throw new IllegalArgumentException("Offset span step must be >= 1 but was " + step);
    }
  }

  static @NotNull OffsetSpan of(final int min, final int maxInclusive) {
    return new OffsetSpan(min, maxInclusive, 1);
  }

  int size() {
    return (maxInclusive - min) / step + 1;
  }

  boolean contains(final int offset) {
    return offset >= min && offset <= maxInclusive;
  }

  @NotNull OffsetSpan extendTo(final int offset) {
    return contains(offset) ? this
        : new OffsetSpan(Math.min(min, offset), Math.max(maxInclusive, offset), step);
  }
}
