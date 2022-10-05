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

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.fx.DataTypeCellFactory;
import io.github.mzmine.datamodel.features.types.fx.DataTypeCellValueFactory;
import io.github.mzmine.datamodel.features.types.modifiers.SubColumnsFactory;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.TreeTableColumn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class NumberRangeType<T extends Number & Comparable<?>>
    extends NumberType<Range<T>> implements SubColumnsFactory {

  protected NumberRangeType(NumberFormat defaultFormat) {
    super(defaultFormat);
  }

  @Override
  public @NotNull DataType<?> getType(int subcolumn) {
    return this;
  }

  @Override
  public abstract NumberFormat getFormatter();

  @Override
  @NotNull
  public String getFormattedString(Range<T> value) {
    return value == null ? ""
        : getFormatter().format(value.lowerEndpoint()) + "-"
          + getFormatter().format(value.upperEndpoint());
  }

  @NotNull
  public String getFormattedString(T value) {
    return value == null ? "" : getFormatter().format(value);
  }

  @Override
  public ObjectProperty<Range<T>> createProperty() {
    return new SimpleObjectProperty<Range<T>>();
  }

  @NotNull
  @Override
  public int getNumberOfSubColumns() {
    return 2;
  }

  @Nullable
  @Override
  public String getHeader(int subcolumn) {
    // is also used as unique ID - do not change or make sure that unique ID is min / max
    switch (subcolumn) {
      case 0:
        return "min";
      case 1:
        return "max";
    }
    if (subcolumn < getNumberOfSubColumns()) {
      throw new IllegalArgumentException("Sub column index is not handled: " + subcolumn);
    } else {
      throw new IndexOutOfBoundsException(
          "Sub column index " + subcolumn + " is out of range " + getNumberOfSubColumns());
    }
  }

  @Override
  @Nullable
  public String getUniqueID(int subcolumn) {
    // do not change unique ID
    return getHeader(subcolumn);
  }

  @Override
  @NotNull
  public List<TreeTableColumn<ModularFeatureListRow, Object>> createSubColumns(
      @Nullable RawDataFile raw, @Nullable SubColumnsFactory parentType) {
    List<TreeTableColumn<ModularFeatureListRow, Object>> cols = new ArrayList<>();

    // e.g. FloatType for FloatRangeType etc
    DataType subColType = getType(0);
    // create column per name
    for (int index = 0; index < getNumberOfSubColumns(); index++) {
      TreeTableColumn<ModularFeatureListRow, Object> min = new TreeTableColumn<>(getHeader(index));
      DataTypeCellValueFactory cvFactoryMin = new DataTypeCellValueFactory(raw, subColType, this,
          index);
      min.setCellValueFactory(cvFactoryMin);
      min.setCellFactory(new DataTypeCellFactory(raw, subColType, this, index));
      // add column
      cols.add(min);
    }
    return cols;
  }

  @Override
  @Nullable
  public String getFormattedSubColValue(int subcolumn, Object value) {
    if (value == null) {
      return "";
    }
    switch (subcolumn) {
      case 0:
        return getFormatter().format(((Range) value).lowerEndpoint());
      case 1:
        return getFormatter().format(((Range) value).upperEndpoint());
    }
    return "";
  }

  @Override
  public @Nullable Object getSubColValue(int subcolumn, Object value) {
    if (value == null) {
      return null;
    }
    switch (subcolumn) {
      case 0:
        return ((Range) value).lowerEndpoint();
      case 1:
        return ((Range) value).upperEndpoint();
    }
    return null;
  }

}
