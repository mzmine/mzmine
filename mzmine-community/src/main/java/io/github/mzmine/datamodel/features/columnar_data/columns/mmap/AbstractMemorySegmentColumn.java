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

import io.github.mzmine.datamodel.features.columnar_data.columns.AbstractDataColumn;
import io.github.mzmine.util.MathUtils;
import io.github.mzmine.util.MemoryMapStorage;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractMemorySegmentColumn<T> extends AbstractDataColumn<T> {

  protected static final int SIZE_MULTIPLIER = 4;

  @NotNull
  protected final MemoryMapStorage storage;
  protected volatile MemorySegment data;

  public AbstractMemorySegmentColumn(@NotNull final MemoryMapStorage storage,
      final int initialCapacity) {
    this.storage = storage;
    ensureCapacity(initialCapacity);
  }


  @Override
  public @Nullable T set(final int index, final @Nullable T value) {
    T old = get(index);
    set(data, index, value);
    return old;
  }

  /**
   * Internal method to actually set the value to a memory segment
   *
   * @param data a memory segment
   */
  protected abstract void set(final @NotNull MemorySegment data, final int index, final T value);

  @NotNull
  protected abstract MemoryLayout getValueLayout();

  /**
   * Set the initial value like Double.NaN or a blacklisted int
   *
   * @param data           a MemorySegment to be changed
   * @param startInclusive the start index
   * @param endExclusive   the end index excluded to be set
   */
  protected void clearRange(final @NotNull MemorySegment data, final int startInclusive,
      final int endExclusive) {
    for (int i = startInclusive; i < endExclusive; i++) {
      clear(data, i);
    }
  }

  /**
   * @param data  backing data to clear
   * @param index element to clear
   */
  protected void clear(final @NotNull MemorySegment data, final int index) {
    set(data, index, null);
  }

  @Override
  public boolean ensureCapacity(final int requiredCapacity) {
    if (requiredCapacity > capacity()) {
      // avoid int overflow
      return resizeTo(MathUtils.capMaxInt((long) requiredCapacity * SIZE_MULTIPLIER));
    }
    return false;
  }

  /**
   * @return true if resize happened, false otherwise
   */
  @Override
  protected boolean resizeTo(final int finalSize) {
    int capacity = capacity();
    if (capacity >= finalSize) {
      return false;
    }

    MemorySegment newData = storage.allocateMemorySegment(getValueLayout(), finalSize);
    clearRange(newData, capacity, finalSize);
    if (data != null) {
      final MemorySegment copy = newData.copyFrom(data);
      data = copy;
    } else {
      data = newData;
    }
    return true;
  }

  @Override
  public int capacity() {
    if (data == null) {
      return 0;
    }
    // should always be the same as a direct cast but the maximum capacity is always an int
    return MathUtils.capMaxInt(data.byteSize() / getValueLayout().byteSize());
  }
}
