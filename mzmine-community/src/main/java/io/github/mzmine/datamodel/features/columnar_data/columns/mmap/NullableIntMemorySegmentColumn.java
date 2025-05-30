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

import io.github.mzmine.datamodel.features.columnar_data.columns.NullableIntDataColumn;
import io.github.mzmine.util.MemoryMapStorage;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import org.jetbrains.annotations.NotNull;

public class NullableIntMemorySegmentColumn extends AbstractMemorySegmentColumn<Integer> implements
    NullableIntDataColumn {

  public NullableIntMemorySegmentColumn(final @NotNull MemoryMapStorage storage, int initialCapacity) {
    super(storage, initialCapacity);
  }

  @Override
  protected @NotNull ValueLayout getValueLayout() {
    return ValueLayout.JAVA_INT;
  }

  @Override
  protected void set(final @NotNull MemorySegment data, final int index, final Integer value) {
    data.setAtIndex(ValueLayout.JAVA_INT, index, value != null ? value : NULL_VALUE);
  }

  @Override
  public int getInt(final int index) {
    return data.getAtIndex(ValueLayout.JAVA_INT, index);
  }

  @Override
  public int setInt(final int index, final int value) {
    data.setAtIndex(ValueLayout.JAVA_INT, index, value);
    return index;
  }

}
