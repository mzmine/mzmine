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

package io.github.mzmine.datamodel.features.types.numbers.abstr;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.modifiers.SubColumnsFactory;
import io.github.mzmine.javafx.components.factories.TableColumns;
import io.github.mzmine.javafx.components.util.TextLabelMeasurementUtil;
import java.text.NumberFormat;
import javafx.scene.control.TreeTableColumn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A DataType that has a NumberFormat to format its content. May be a simple number or Range or
 * complex object resolving to numbers.
 *
 * @param <T> any
 */
public abstract class NumberFormatType<T> extends DataType<T> {

  protected final NumberFormat DEFAULT_FORMAT;

  protected NumberFormatType(NumberFormat defaultFormat) {
    DEFAULT_FORMAT = defaultFormat;
  }

  public abstract NumberFormat getFormat();

  public abstract NumberFormat getExportFormat();

  public NumberFormat getFormat(boolean export) {
    return export ? getExportFormat() : getFormat();
  }

  @Override
  public @NotNull String getFormattedString(final T value, final boolean export) {
    return value != null ? getFormat(export).format(value) : "";
  }

  @Override
  public @Nullable TreeTableColumn<ModularFeatureListRow, Object> createColumn(
      @Nullable RawDataFile raw, @Nullable SubColumnsFactory parentType, int subColumnIndex) {
    final TreeTableColumn<ModularFeatureListRow, Object> column = super.createColumn(raw,
        parentType, subColumnIndex);

    return column;
  }

  /**
   * @return a large number that represents the expected value range for the preferred width
   * estimation
   */
  public double getRepresentativeLargeNumber() {
    return 99999;
  }

  @Override
  public double getPrefColumnWidth() {
    final NumberFormat format = getFormat();
    final String longNumberString = format.format(getRepresentativeLargeNumber());
    return TextLabelMeasurementUtil.measureWidth(longNumberString)
        + TableColumns.EXTRA_WIDTH_MARGIN;
  }
}
