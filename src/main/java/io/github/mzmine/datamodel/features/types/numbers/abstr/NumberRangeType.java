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
import io.github.mzmine.datamodel.features.types.numbers.MZType;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.TreeTableColumn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class NumberRangeType<T extends Number & Comparable<?>> extends
    NumberType<Range<T>> implements SubColumnsFactory {

  // this is a trick, we need a datatype to get the sub column value
  // we use this as the first column and any other for the second
  private static final DataType<Double> MAX_REF_TYPE = new MZType();

  protected NumberRangeType(NumberFormat defaultFormat) {
    super(defaultFormat);
  }

  @Override
  public @NotNull DataType<?> getType(int subcolumn) {
    return switch (subcolumn) {
      case 0 -> this;
      case 1 -> MAX_REF_TYPE;
      default ->
          throw new IndexOutOfBoundsException("Index out of range 2 with value " + subcolumn);
    };
  }

  @Override
  public abstract NumberFormat getFormat();

  @Override
  @NotNull
  public String getFormattedString(Range<T> value, boolean export) {
    if (value == null) {
      return "";
    } else {
      NumberFormat format = getFormat(export);
      return format.format(value.lowerEndpoint()) + "-" + format.format(value.upperEndpoint());
    }
  }

  @NotNull
  public String getFormattedString(T value, boolean export) {
    return value == null ? "" : getFormat(export).format(value);
  }

  @NotNull
  public String getFormattedString(T value) {
    return getFormattedString(value, false);
  }

  @NotNull
  public String getFormattedExportString(T value) {
    return getFormattedString(value, true);
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
    return switch (subcolumn) {
      case 0 -> "min";
      case 1 -> "max";
      default -> throw new IndexOutOfBoundsException(
          "Range index out of bounds 2 with value " + subcolumn);
    };
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
  public String getFormattedSubColValue(int subcolumn, Object value, boolean export) {
    if (value == null) {
      return "";
    }
    return switch (subcolumn) {
      case 0 -> getFormat(export).format(((Range) value).lowerEndpoint());
      case 1 -> getFormat(export).format(((Range) value).upperEndpoint());
      default -> "";
    };
  }

  @Override
  public @Nullable Object getSubColValue(@NotNull final DataType sub, final Object value) {
    // uses a trick to identify the two different columsn
    // first type is this
    // second is randomly MAX_REF_TYPE
    if (this.equals(sub)) {
      return getExportFormat().format(((Range) value).lowerEndpoint());
    } else {
      return getExportFormat().format(((Range) value).upperEndpoint());
    }
  }

  @Override
  public @Nullable Object getSubColValue(int subcolumn, Object value) {
    if (value == null) {
      return null;
    }
    return switch (subcolumn) {
      case 0 -> ((Range) value).lowerEndpoint();
      case 1 -> ((Range) value).upperEndpoint();
      default -> null;
    };
  }

}
