/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.datamodel;

import io.github.mzmine.datamodel.features.ModularDataModel;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.numbers.AreaType;
import io.github.mzmine.datamodel.features.types.numbers.HeightType;
import java.util.Objects;
import org.jetbrains.annotations.Nullable;

/**
 * Used to define the abundance of features
 */
public enum AbundanceMeasure {
  Height(HeightType.class), Area(AreaType.class);

  final Class<? extends DataType<Float>> type;

  AbundanceMeasure(Class<? extends DataType<Float>> type) {
    this.type = type;
  }

  public Class<? extends DataType<Float>> type() {
    return type;
  }

  /**
   * @param featureOrRow The feature or row
   * @return The abundance or null if the feature/row is null or no abundance is set.
   */
  public Float get(@Nullable ModularDataModel featureOrRow) {
    if (featureOrRow == null) {
      return null;
    }
    return featureOrRow.get(type);
  }

  public Float getOrNaN(@Nullable ModularDataModel featureOrRow) {
    if (featureOrRow == null) {
      return Float.NaN;
    }
    return Objects.requireNonNullElse(featureOrRow.get(type), Float.NaN);
  }

}
