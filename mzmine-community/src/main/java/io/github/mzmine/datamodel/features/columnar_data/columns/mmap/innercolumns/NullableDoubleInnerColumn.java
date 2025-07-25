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

package io.github.mzmine.datamodel.features.columnar_data.columns.mmap.innercolumns;

import io.github.mzmine.datamodel.features.columnar_data.columns.general.NullableDouble;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.StructLayout;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NullableDoubleInnerColumn extends AbstractInnerColumn<Double> implements
    NullableDouble {

  public NullableDoubleInnerColumn(StructLayout layout, String varHandleName) {
    super(layout, varHandleName);
  }

  @Override
  @Nullable
  public Double get(MemorySegment data, int index) {
    final double v = (double) varHandle.get(data, 0L, index);
    return isNull(v) ? null : v;
  }

  @Override
  public void clear(@NotNull MemorySegment data, int index) {
    set(data, index, nullValue());
  }

  @Override
  public void set(@NotNull MemorySegment data, int index, @Nullable Double value) {
    set(data, index, value == null ? nullValue() : value);
  }

  public void set(@NotNull MemorySegment data, int index, double value) {
    varHandle.set(data, 0L, index, value);
  }
}
