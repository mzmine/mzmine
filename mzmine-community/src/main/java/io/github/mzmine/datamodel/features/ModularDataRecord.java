/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package io.github.mzmine.datamodel.features;

import static java.util.Objects.requireNonNullElse;

import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.DataTypes;
import org.jetbrains.annotations.NotNull;

public interface ModularDataRecord {

  /**
   * Provides data for data types
   *
   * @param sub data column
   * @return the score for this column
   */
  Object getValue(final @NotNull DataType<?> sub);

  /**
   * Provides data for data types
   *
   * @param sub data column
   * @return the score for this column
   */
  default <T> T getValue(final @NotNull Class<? extends DataType<T>> sub) {
    return (T) getValue(DataTypes.get(sub));
  }

  /**
   * Provides data for data types
   *
   * @param sub data column
   * @return the score for this column
   */
  default <T> T getOrElse(final @NotNull DataType<T> sub, T defaultValue) {
    return (T) requireNonNullElse(getValue(sub), defaultValue);
  }

  /**
   * Provides data for data types
   *
   * @param sub data column
   * @return the score for this column
   */
  default <T> T getOrElse(final @NotNull Class<? extends DataType<T>> sub, T defaultValue) {
    return getOrElse(DataTypes.get(sub), defaultValue);
  }
}
