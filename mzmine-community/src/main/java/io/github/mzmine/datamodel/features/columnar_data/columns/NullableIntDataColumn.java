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

package io.github.mzmine.datamodel.features.columnar_data.columns;

import io.github.mzmine.datamodel.features.columnar_data.columns.general.NullableInteger;
import org.jetbrains.annotations.Nullable;

public non-sealed interface NullableIntDataColumn extends DataColumn<Integer>, NullableInteger {

  /**
   * @param index row index
   * @return the primitive double value or {@link #nullValue()} for null
   */
  int getInt(final int index);

  /**
   * @param index row index
   * @param value the primitive value or {@link #nullValue()} for null
   */
  int setInt(final int index, final int value);

  default void clear(final int index) {
    setInt(index, nullValue());
  }

  @Override
  default @Nullable Integer set(final int index, final @Nullable Integer value) {
    return setInt(index, value == null ? nullValue() : value);
  }

  @Override
  default @Nullable Integer get(final int index) {
    var value = getInt(index);
    return isNull(value) ? null : value;
  }

}
