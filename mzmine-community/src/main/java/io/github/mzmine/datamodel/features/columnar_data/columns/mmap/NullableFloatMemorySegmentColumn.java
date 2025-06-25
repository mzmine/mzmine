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

import io.github.mzmine.datamodel.features.columnar_data.columns.NullableFloatDataColumn;
import io.github.mzmine.util.MemoryMapStorage;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import org.jetbrains.annotations.NotNull;

public class NullableFloatMemorySegmentColumn extends AbstractMemorySegmentColumn<Float> implements
    NullableFloatDataColumn {

  public NullableFloatMemorySegmentColumn(final @NotNull MemoryMapStorage storage, int initialCapacity) {
    super(storage, initialCapacity);
  }

  @Override
  protected @NotNull ValueLayout getValueLayout() {
    return ValueLayout.JAVA_FLOAT;
  }

  @Override
  protected void set(final @NotNull MemorySegment data, final int index, final Float value) {
    data.setAtIndex(ValueLayout.JAVA_FLOAT, index, value != null ? value : Float.NaN);
  }

  @Override
  public float getFloat(final int index) {
    return data.getAtIndex(ValueLayout.JAVA_FLOAT, index);
  }

  @Override
  public float setFloat(final int index, final float value) {
    float old = getFloat(index);
    data.setAtIndex(ValueLayout.JAVA_FLOAT, index, value);
    return old;
  }

}
