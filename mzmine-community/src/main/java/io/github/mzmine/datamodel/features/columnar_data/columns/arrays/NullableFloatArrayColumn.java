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

package io.github.mzmine.datamodel.features.columnar_data.columns.arrays;

import io.github.mzmine.datamodel.features.columnar_data.columns.AbstractDataColumn;
import io.github.mzmine.datamodel.features.columnar_data.columns.NullableFloatDataColumn;
import java.util.Arrays;

public class NullableFloatArrayColumn extends AbstractDataColumn<Float> implements
    NullableFloatDataColumn {

  public volatile float[] data;

  public NullableFloatArrayColumn(int initialSize) {
    data = new float[initialSize];
    Arrays.fill(data, Float.NaN);
  }

  @Override
  public float getFloat(final int index) {
    return data[index];
  }

  @Override
  public float setFloat(final int index, final float value) {
    float oldValue = data[index];
    data[index] = value;
    return oldValue;
  }

  @Override
  protected boolean resizeTo(final int finalSize) {
    var oldSize = data.length;
    var copy = Arrays.copyOf(data, finalSize);
    Arrays.fill(copy, oldSize, finalSize, Float.NaN);
    data = copy;
    return true;
  }

  @Override
  public int capacity() {
    return data == null ? 0 : data.length;
  }
}
