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

package io.github.mzmine.datamodel.features.types.numbers;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.GroupType;
import io.github.mzmine.datamodel.features.types.fx.DataTypeCellFactory;
import io.github.mzmine.datamodel.features.types.fx.DataTypeCellValueFactory;
import io.github.mzmine.datamodel.features.types.modifiers.SubColumnsFactory;
import io.github.mzmine.datamodel.features.types.numbers.abstr.NumberType;
import io.github.mzmine.datamodel.features.types.numbers.stats.MaximumType;
import io.github.mzmine.datamodel.features.types.numbers.stats.MeanType;
import io.github.mzmine.datamodel.features.types.numbers.stats.MinimumType;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.TreeTableColumn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class SimpleStatisticsType extends NumberType<SimpleStatistics> implements
    SubColumnsFactory {


  private final DataType[] columns = new DataType[]{new MinimumType(DEFAULT_FORMAT),
      new MeanType(DEFAULT_FORMAT), new MaximumType(DEFAULT_FORMAT), new GroupType()};
  private final NumberFormat guiFormat;
  private final NumberFormat exportFormat;

  protected SimpleStatisticsType(NumberFormat guiFormat, NumberFormat exportFormat) {
    super(guiFormat);
    this.guiFormat = guiFormat;
    this.exportFormat = exportFormat;
  }

  @Override
  public NumberFormat getFormat() {
    return guiFormat;
  }

  @Override
  public NumberFormat getExportFormat() {
    return exportFormat;
  }

  @Override
  public @NotNull DataType<?> getType(int subcolumn) {
    if (subcolumn < 0 || subcolumn >= columns.length) {
      throw new IndexOutOfBoundsException(subcolumn);
    }
    return columns[subcolumn];
  }

  @Override
  @NotNull
  public String getFormattedString(SimpleStatistics value, boolean export) {
    if (value == null) {
      return "";
    } else {
      NumberFormat format = getFormat(export);
      return value.toString(format);
    }
  }

  @Override
  public ObjectProperty<SimpleStatistics> createProperty() {
    return new SimpleObjectProperty<>();
  }

  @Override
  public Class<SimpleStatistics> getValueClass() {
    return SimpleStatistics.class;
  }

  @NotNull
  @Override
  public int getNumberOfSubColumns() {
    return columns.length;
  }

  @Nullable
  @Override
  public String getHeader(int subcolumn) {
    return getType(subcolumn).getHeaderString();
  }

  @Override
  @Nullable
  public String getUniqueID(int subcolumn) {
    // do not change unique ID
    return getType(subcolumn).getUniqueID();
  }

  @Override
  @NotNull
  public List<TreeTableColumn<ModularFeatureListRow, Object>> createSubColumns(
      @Nullable RawDataFile raw, @Nullable SubColumnsFactory parentType) {
    List<TreeTableColumn<ModularFeatureListRow, Object>> cols = new ArrayList<>();

    // e.g. FloatType for FloatRangeType etc
    // create column per name
    for (int index = 0; index < getNumberOfSubColumns(); index++) {
      DataType subColType = getType(index);
      TreeTableColumn<ModularFeatureListRow, Object> col = new TreeTableColumn<>(getHeader(index));
      DataTypeCellValueFactory cvFactoryMin = new DataTypeCellValueFactory(raw, subColType, this,
          index);
      col.setCellValueFactory(cvFactoryMin);
      col.setCellFactory(new DataTypeCellFactory(raw, subColType, this, index));
      // add column
      cols.add(col);
    }
    return cols;
  }

  @Override
  @Nullable
  public String getFormattedSubColValue(int subcolumn, Object value, boolean export) {
    if (value == null) {
      return "";
    }
    if (!(value instanceof SimpleStatistics stats)) {
      throw new IllegalArgumentException(
          "Value was not of type SimpleStatistics but of type: " + value.getClass());
    }
    return switch (subcolumn) {
      case 0 -> getFormat(export).format(stats.min());
      case 1 -> getFormat(export).format(stats.mean());
      case 2 -> getFormat(export).format(stats.max());
      case 3 -> stats.group();
      default -> "";
    };
  }

  @Override
  public @Nullable Object getSubColValue(@NotNull final DataType sub, final Object value) {
    if (value == null) {
      return null;
    }
    if (!(value instanceof SimpleStatistics stats)) {
      throw new IllegalArgumentException(
          "Value was not of type SimpleStatistics but of type: " + value.getClass());
    }
    return switch (sub) {
      case MinimumType ignored -> stats.min();
      case MaximumType ignored -> stats.max();
      case MeanType ignored -> stats.mean();
      case GroupType ignored -> stats.group();
      default -> throw new IllegalArgumentException(
          "Cannot handle column in SumpleStatisticsType: " + sub);
    };
  }

  @Override
  public @Nullable Object getSubColValue(int subcolumn, Object value) {
    if (value == null) {
      return null;
    }
    if (!(value instanceof SimpleStatistics stats)) {
      throw new IllegalArgumentException(
          "Value was not of type SimpleStatistics but of type: " + value.getClass());
    }

    return switch (subcolumn) {
      case 0 -> stats.min();
      case 1 -> stats.mean();
      case 2 -> stats.max();
      case 3 -> stats.group();
      default -> null;
    };
  }

}
