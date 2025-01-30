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

package io.github.mzmine.datamodel.features.columnar_data.mmap;

import io.github.mzmine.datamodel.features.columnar_data.AbstractDataColumn;
import io.github.mzmine.util.MemoryMapStorage;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

public abstract class AbstractMemorySegmentColumn<T> extends AbstractDataColumn<T> {

  protected static final int SIZE_MULTIPLIER = 10;
  protected final MemoryMapStorage storage;
  protected MemorySegment data;

  public AbstractMemorySegmentColumn(final MemoryMapStorage storage, final int initialCapacity) {
    this.storage = storage;
    ensureCapacity(initialCapacity);
  }

  protected abstract ValueLayout getValueLayout();

  /**
   * Set the initial value like Double.NaN or a blacklisted int
   *
   * @param newData        the new memorySegment
   * @param startInclusive the start index
   * @param endExclusive   the end index excluded to be set
   */
  protected void setInitialValue(final MemorySegment newData, final int startInclusive,
      final int endExclusive) {
    // do nothing by default
  }

  @Override
  public boolean ensureCapacity(final int requiredCapacity) {
    if (requiredCapacity > capacity()) {
      return resizeTo(requiredCapacity * SIZE_MULTIPLIER);
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
    setInitialValue(newData, capacity, finalSize);
    if (data != null) {
      data = newData.copyFrom(data);
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
    return (int) (data.byteSize() / getValueLayout().byteSize());
  }
}
