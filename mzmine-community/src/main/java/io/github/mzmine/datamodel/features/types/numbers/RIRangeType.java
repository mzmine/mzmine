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
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.datamodel.features.types.numbers;

import io.github.mzmine.datamodel.features.RowBinding;
import io.github.mzmine.datamodel.features.SimpleRowBinding;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.datamodel.features.types.modifiers.BindingsType;
import io.github.mzmine.datamodel.features.types.modifiers.ExpandableType;
import io.github.mzmine.datamodel.features.types.numbers.abstr.DoubleRangeType;
import io.github.mzmine.datamodel.features.types.numbers.abstr.FloatRangeType;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public class RIRangeType extends FloatRangeType implements ExpandableType {

  public RIRangeType() {
    super(new DecimalFormat("0.##"));
  }

  @Override
  public NumberFormat getFormat() {
    return DEFAULT_FORMAT;
  }

  @Override
  public NumberFormat getExportFormat() {
    return DEFAULT_FORMAT;
  }

  @Override
  public @NotNull String getUniqueID() {
    return "retention_index_range";
  }

  @Override
  public @NotNull String getHeaderString() {
    return "RI range";
  }

  @Override
  public @NotNull Class<? extends DataType<?>> getExpandedTypeClass() {
    return getClass();
  }

  @Override
  public @NotNull Class<? extends DataType<?>> getHiddenTypeClass() {
    return RIType.class;
  }

  @Override
  public @NotNull List<RowBinding> createDefaultRowBindings() {
    return List.of(new SimpleRowBinding(this, DataTypes.get(RIType.class), BindingsType.RANGE));
  }
}
