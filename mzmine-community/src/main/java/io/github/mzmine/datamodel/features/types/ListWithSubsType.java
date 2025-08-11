/*
 * Copyright (c) 2004-2023 The MZmine Development Team
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

package io.github.mzmine.datamodel.features.types;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.modifiers.EditableColumnType;
import io.github.mzmine.datamodel.features.types.modifiers.NullColumnType;
import io.github.mzmine.datamodel.features.types.modifiers.SubColumnsFactory;
import io.github.mzmine.datamodel.features.types.numbers.abstr.ListDataType;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.scene.control.TreeTableColumn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public abstract class ListWithSubsType<T> extends ListDataType<T> implements SubColumnsFactory,
    EditableColumnType {

  private static final Logger logger = Logger.getLogger(ListWithSubsType.class.getName());

  /**
   * The unmodifiable list of sub data types. Order reflects the initial order of columns.
   *
   * @return list of sub data types
   */
  @NotNull
  public abstract List<DataType> getSubDataTypes();

  @Override
  @NotNull
  public List<TreeTableColumn<ModularFeatureListRow, Object>> createSubColumns(
      @Nullable RawDataFile raw, @Nullable SubColumnsFactory parentType) {
    // add column for each sub data type
    List<TreeTableColumn<ModularFeatureListRow, Object>> cols = new ArrayList<>();

    List<DataType> subTypes = getSubDataTypes();
    // create column per name
    for (int index = 0; index < getNumberOfSubColumns(); index++) {
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

  @Nullable
  @Override
  public String getHeader(int subcolumn) {
    List<DataType> list = getSubDataTypes();
    if (subcolumn >= 0 && subcolumn < list.size()) {
      return list.get(subcolumn).getHeaderString();
    } else {
      throw new IndexOutOfBoundsException(
          "Sub column index " + subcolumn + " is out of range " + list.size());
    }
  }

  @Override
  @Nullable
  public String getUniqueID(int subcolumn) {
    // do not change unique ID
    List<DataType> list = getSubDataTypes();
    if (subcolumn >= 0 && subcolumn < list.size()) {
      return list.get(subcolumn).getUniqueID();
    } else {
      throw new IndexOutOfBoundsException(
          "Sub column index " + subcolumn + " is out of range " + list.size());
    }
  }

  @Override
  public @NotNull DataType<?> getType(int index) {
    if (index < 0 || index >= getSubDataTypes().size()) {
      throw new IndexOutOfBoundsException(
          String.format("Sub column index %d is out of bounds %d", index,
              getSubDataTypes().size()));
    }
    return getSubDataTypes().get(index);
  }

  @Override
  @Nullable
  public String getFormattedSubColValue(int subcolumn, Object value, boolean export) {
    DataType sub = getType(subcolumn);
    if (sub == null) {
      return "";
    }
    if (value == null) {
      return sub.getFormattedString(sub.getDefaultValue(), export);
    }

    Object subvalue = null;
    try {
      List<T> list = ((List<T>) value);
      subvalue = list.isEmpty() ? sub.getDefaultValue() : getSubColValue(sub, list);
      return sub.getFormattedString(subvalue, export);
    } catch (Exception ex) {
      logger.log(Level.WARNING, String.format(
          "Error while formatting sub column value in type %s. Sub type %s cannot format value of %s",
          this.getClass().getName(), sub.getClass().getName(),
          (subvalue == null ? "null" : subvalue.getClass())), ex);
      return "";
    }
  }

  @Override
  public @Nullable Object getSubColValue(int subcolumn, Object value) {
    DataType sub = getType(subcolumn);
    if (sub == null) {
      return null;
    }
    if (value == null) {
      return sub.getDefaultValue();
    }

    Object subvalue = null;
    try {
      List<T> list = ((List<T>) value);
      return list.isEmpty() ? sub.getDefaultValue() : getSubColValue(sub, list);
    } catch (Exception ex) {
      logger.log(Level.WARNING, String.format(
          "Error while getting sub column value in type %s. Sub type %s cannot get value of %s",
          this.getClass().getName(), sub.getClass().getName(),
          (subvalue == null ? "" : subvalue.getClass())), ex);
      return "";
    }
  }

  /**
   * The sub column value for a specific subType in a value.
   *
   * @param subType the sub type
   * @param list    the list
   * @return the sub column value or null if value==null or if sub column empty.
   */
  protected <K> @Nullable K getSubColValue(@NotNull DataType<K> subType, @Nullable List<T> list) {
    if (list == null || list.isEmpty()) {
      return subType.getDefaultValue();
    } else {
      if (this.equals(subType)) {
        // all ions
        return (K) list;
      } else {
        // get value for first ion
        return (K) map(subType, list.get(0));
      }
    }
  }

  /**
   * This method extracts the sub column value K from the parentItem, which is usually an item with
   * the list of this {@link ListWithSubsType}. This could be one annotation of a list of
   * annotations.
   *
   * This way one can retrieve the formatted value of a sub columns like below:
   *
   * <pre>
   *   {@code
   *   // example with MZType column. listType is an arbitrary ListWithSubsType
   *   var mzType = DataTypes.get(MZType.class);
   *   var value = listType.map(mzType, aListItem);
   *   String formatted = mzType.getFormattedString(value, false);
   *   }
   * </pre>
   *
   * @param subType    the data type of the sub column. Defines what value should be extracted from
   *                   the parentItem
   * @param parentItem the parent item, which is usually an element of the list set to this
   *                   {@link ListWithSubsType}
   * @param <K>        the value of the subType
   * @return the value of the subType or null
   */
  public abstract <K> @Nullable K map(@NotNull DataType<K> subType, T parentItem);

  @Override
  public @Nullable Object getSubColValue(@NotNull final DataType sub, final Object value) {
    if (value instanceof List list) {
      return getSubColValue(sub, list);
    }
    return null;
  }

  @Override
  public boolean getDefaultVisibility() {
    return true;
  }
}
