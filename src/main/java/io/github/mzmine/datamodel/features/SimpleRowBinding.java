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

import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.modifiers.BindingsType;
import org.jetbrains.annotations.NotNull;

/**
 * Create standard RowBindings for AVERAGE, SUM, MAX, MIN, RANGES, etc Specific {@link DataType}s
 * define the binding
 *
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public class SimpleRowBinding implements RowBinding {

  private final @NotNull DataType rowType;
  private final @NotNull DataType featureType;
  private final @NotNull BindingsType bindingType;

  public SimpleRowBinding(@NotNull DataType<?> featureAndRowType,
      @NotNull BindingsType bindingType) {
    this(featureAndRowType, featureAndRowType, bindingType);
  }

  public SimpleRowBinding(@NotNull DataType<?> rowType, @NotNull DataType<?> featureType,
      @NotNull BindingsType bindingType) {
    super();
    this.rowType = rowType;
    this.featureType = featureType;
    this.bindingType = bindingType;
  }

  @Override
  public void apply(FeatureListRow row) {
    // row might be null if the feature was not yet added
    if (row != null) {
      Object value = featureType.evaluateBindings(bindingType, row.getFeatures());
      row.set(rowType, value);
    }
  }

  @Override
  public DataType getRowType() {
    return rowType;
  }

  @Override
  public DataType getFeatureType() {
    return featureType;
  }

  @Override
  public void valueChanged(ModularDataModel dataModel, DataType type, Object oldValue,
      Object newValue) {
    if (dataModel instanceof Feature feature) {
      // change in feature applied to its row
      apply(feature.getRow());
    } else {
      throw new UnsupportedOperationException(
          "Cannot apply a SimpleRowBinding if the changed data model is not a Feature");
    }
  }
}
