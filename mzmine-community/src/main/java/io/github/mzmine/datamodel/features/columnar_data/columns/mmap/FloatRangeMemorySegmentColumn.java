/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

package io.github.mzmine.datamodel.features.columnar_data.columns.mmap;

import com.google.common.collect.Range;
import io.github.mzmine.util.MemoryMapStorage;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.StructLayout;
import java.lang.foreign.ValueLayout;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FloatRangeMemorySegmentColumn extends AbstractMemorySegmentColumn<Range<Float>> {

  private static final StructLayout LAYOUT = MemoryLayout.structLayout(
      ValueLayout.JAVA_FLOAT.withoutName(), ValueLayout.JAVA_FLOAT.withoutName());

  public FloatRangeMemorySegmentColumn(@NotNull final MemoryMapStorage storage, int initialCapacity) {
    super(storage, initialCapacity);
  }

  @Override
  protected @NotNull MemoryLayout getValueLayout() {
    return LAYOUT;
  }

  public @Nullable Float getLowerBound(final int index) {
    return getAtIndex(index * 2L);
  }

  public @Nullable Float getUpperBound(final int index) {
    return getAtIndex(index * 2L + 1L);
  }

  private @Nullable Float getAtIndex(final long index) {
    var value = data.getAtIndex(ValueLayout.JAVA_FLOAT, index);
    return Float.isNaN(value) ? null : value;
  }

  @Override
  public @Nullable Range<Float> get(final int index) {
    var lower = getLowerBound(index);
    if (lower == null) {
      return null;
    }
    var upper = getUpperBound(index);
    if (upper == null) {
      return null;
    }
    return Range.closed(lower, upper);
  }

  public void set(final @NotNull MemorySegment data, final int index, @Nullable final Float lower,
      @Nullable final Float upper) {
    data.setAtIndex(ValueLayout.JAVA_FLOAT, index * 2L, lower == null ? Float.NaN : lower);
    data.setAtIndex(ValueLayout.JAVA_FLOAT, index * 2L + 1L, upper == null ? Float.NaN : upper);
  }

  @Override
  public void set(final @NotNull MemorySegment data, final int index, final Range<Float> value) {
    if (value == null) {
      set(data, index, null, null);
      return;
    }
    set(data, index, value.lowerEndpoint(), value.upperEndpoint());
  }
}
