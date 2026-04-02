/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.datamodel.features.types.abstr;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularDataRecord;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.modifiers.NullColumnType;
import io.github.mzmine.datamodel.features.types.modifiers.SubColumnsFactory;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.control.TreeTableColumn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class SimpleSubColumnsType<T> extends DataType<T> implements SubColumnsFactory {

  @SuppressWarnings("rawtypes")
  public abstract @NotNull List<DataType> getSubDataTypes();

  @Override
  public @NotNull List<TreeTableColumn<ModularFeatureListRow, Object>> createSubColumns(
      @Nullable RawDataFile raw, @Nullable SubColumnsFactory parentType) {
    // add column for each sub data type
    List<TreeTableColumn<ModularFeatureListRow, Object>> cols = new ArrayList<>();

    List<DataType> subTypes = getSubDataTypes();
    // create column per name
    for (int index = 0; index < subTypes.size(); index++) {
      DataType type = subTypes.get(index);
      if (type instanceof NullColumnType) {
        continue;
      }
      if (this.equals(type)) {
        // create a special column for this type that actually represents the list of data
        cols.add(DataType.createStandardColumn(type, raw, this, index));
      } else {
        // create all other columns
        var col = type.createColumn(raw, this, index);
        // override type in CellValueFactory with this parent type
        cols.add(col);
      }
    }
    return cols;
  }

  @Override
  public int getNumberOfSubColumns() {
    return getSubDataTypes().size();
  }

  @Override
  public @Nullable String getHeader(int subcolumn) {
    return getType(subcolumn).getHeaderString();
  }

  @Override
  public @Nullable String getUniqueID(int subcolumn) {
    return getType(subcolumn).getUniqueID();
  }

  @Override
  public @NotNull DataType<?> getType(int index) {
    var list = getSubDataTypes();
    if (index < 0 || index >= list.size()) {
      throw new IndexOutOfBoundsException(
          String.format("Sub column index %d is out of bounds %d", index, list.size()));
    }
    return list.get(index);
  }

  @Override
  public @Nullable Object getSubColValue(int subcolumn, Object cellData) {
    DataType sub = getType(subcolumn);
    return sub == null ? null : getSubColValue(sub, cellData);
  }

  @Override
  public @Nullable Object getSubColValue(DataType sub, Object value) {
    if (value == null) {
      return null;
    } else if (value instanceof ModularDataRecord record) {
      return record.getValue(sub);
    } else {
      throw new IllegalArgumentException(
          String.format("value of type %s needs to be of type ModularDataRecord",
              value.getClass().getName()));
    }
  }
}
