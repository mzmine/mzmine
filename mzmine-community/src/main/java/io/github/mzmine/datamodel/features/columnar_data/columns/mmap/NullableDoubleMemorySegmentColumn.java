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

import io.github.mzmine.datamodel.features.columnar_data.columns.NullableDoubleDataColumn;
import io.github.mzmine.util.MemoryMapStorage;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import org.jetbrains.annotations.NotNull;

public class NullableDoubleMemorySegmentColumn extends
    AbstractMemorySegmentColumn<Double> implements NullableDoubleDataColumn {

  public NullableDoubleMemorySegmentColumn(final @NotNull MemoryMapStorage storage, int initialCapacity) {
    super(storage, initialCapacity);
  }

  @Override
  protected @NotNull ValueLayout getValueLayout() {
    return ValueLayout.JAVA_DOUBLE;
  }

  @Override
  protected void set(final @NotNull MemorySegment data, final int index, final Double value) {
    data.setAtIndex(ValueLayout.JAVA_DOUBLE, index, value != null ? value : Double.NaN);
  }

  @Override
  public double getDouble(final int index) {
    return data.getAtIndex(ValueLayout.JAVA_DOUBLE, index);
  }

  @Override
  public double setDouble(final int index, final double value) {
    double old = getDouble(index);
    data.setAtIndex(ValueLayout.JAVA_DOUBLE, index, value);
    return old;
  }

}
