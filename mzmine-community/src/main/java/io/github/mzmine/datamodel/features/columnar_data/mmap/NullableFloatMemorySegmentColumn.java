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

import io.github.mzmine.datamodel.features.columnar_data.NullableFloatDataColumn;
import io.github.mzmine.util.MemoryMapStorage;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

public class NullableFloatMemorySegmentColumn extends AbstractMemorySegmentColumn<Float> implements
    NullableFloatDataColumn {

  public NullableFloatMemorySegmentColumn(final MemoryMapStorage storage, int initialCapacity) {
    super(storage, initialCapacity);
  }

  @Override
  protected ValueLayout getValueLayout() {
    return ValueLayout.JAVA_FLOAT;
  }

  @Override
  public float getFloat(final int index) {
    return data.getAtIndex(ValueLayout.JAVA_FLOAT, index);
  }

  @Override
  public void setFloat(final int index, final float value) {
    data.setAtIndex(ValueLayout.JAVA_FLOAT, index, value);
  }

  @Override
  protected void setInitialValue(final MemorySegment newData, final int startInclusive,
      final int endExclusive) {
    for (int i = startInclusive; i < endExclusive; i++) {
      newData.setAtIndex(ValueLayout.JAVA_FLOAT, i, Float.NaN);
    }
  }

}
