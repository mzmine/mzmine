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

package io.github.mzmine.datamodel.features.columnar_data.arrays;

import io.github.mzmine.datamodel.features.columnar_data.AbstractDataColumn;
import io.github.mzmine.datamodel.features.columnar_data.NullableIntDataColumn;
import java.util.Arrays;

public class NullableIntArrayColumn extends AbstractDataColumn<Integer> implements
    NullableIntDataColumn {

  public int[] data;

  public NullableIntArrayColumn(int initialSize) {
    data = new int[initialSize];
    Arrays.fill(data, NULL_VALUE);
  }

  @Override
  public int getInt(final int index) {
    return data[index];
  }

  @Override
  public int setInt(final int index, final int value) {
    int oldValue = data[index];
    data[index] = value;
    return oldValue;
  }

  @Override
  protected boolean resizeTo(final int finalSize) {
    var oldSize = data.length;
    data = Arrays.copyOf(data, finalSize);
    Arrays.fill(data, oldSize-1, finalSize, NULL_VALUE);
    return true;
  }

  @Override
  public int capacity() {
    return data == null ? 0 : data.length;
  }
}
