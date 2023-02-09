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

package io.github.mzmine.datamodel.features.types.modifiers;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularDataModel;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.DataType;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.scene.control.TreeTableColumn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This data type contains sub columns. Master column is not visualized. Only sub columns
 *
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 */
public interface SubColumnsFactory {

  Logger logger = Logger.getLogger(SubColumnsFactory.class.getName());

  /**
   * Creates sub columns which are then added to the parent column by the parent datatype
   *
   * @return list of sub columns
   */
  @NotNull List<TreeTableColumn<ModularFeatureListRow, Object>> createSubColumns(
      final @Nullable RawDataFile raw, final @Nullable SubColumnsFactory parentType);

  int getNumberOfSubColumns();

  @Nullable String getHeader(int subcolumn);

  /**
   * The unique ID in a machine readable format
   *
   * @param subcolumn
   * @return parsable format of ID
   */
  @Nullable String getUniqueID(int subcolumn);

  /**
   * The data type of the subcolumn
   *
   * @param subcolumn index of subcolumn
   * @return datatype of subcolumn
   */
  @NotNull DataType<?> getType(int subcolumn);

  /**
   * Formatted string for GUI and other uses in MZMine. Use
   * {@link #getFormattedSubColExportValue(int, Object)} for export value formatting
   *
   * @param subcolumn sub column
   * @param value     main value that contains the sub values
   * @return
   */
  default @Nullable String getFormattedSubColValue(int subcolumn, Object value, boolean export) {
    DataType sub = getType(subcolumn);
    if (sub == null) {
      return "";
    }
    if (value == null) {
      return sub.getFormattedString(sub.getDefaultValue(), export);
    }

    Object subvalue = null;
    try {
      subvalue = getSubColValue(sub, value);
      return sub.getFormattedString(subvalue == null ? sub.getDefaultValue() : subvalue, export);
    } catch (Exception ex) {
      logger.log(Level.WARNING, String.format(
          "Error while formatting sub column value in type %s. Sub type %s cannot format value of %s",
          this.getClass().getName(), sub.getClass().getName(),
          (subvalue == null ? "null" : subvalue.getClass())), ex);
      return "";
    }
  }

  /**
   * Formatted string for export - usually with more precision. Best override
   * {@link #getFormattedSubColValue(int, Object, boolean)} instead of this method.
   *
   * @param subcolumn the subcolumn index
   * @param value     the main value of this type that contains sub values
   * @return the formatted string or null or empty strings
   */
  default @Nullable String getFormattedSubColExportValue(int subcolumn, Object value) {
    return getFormattedSubColValue(subcolumn, value, true);
  }

  /**
   * Formatted string for GUI and other uses in MZMine. Use
   * {@link #getFormattedSubColExportValue(int, Object)} for export value formatting. Best override
   * * {@link #getFormattedSubColValue(int, Object, boolean)} instead of this method.
   *
   * @param subcolumn sub column
   * @param value     main value that contains the sub values
   * @return
   */
  default @Nullable String getFormattedSubColValue(int subcolumn, Object value) {
    return getFormattedSubColValue(subcolumn, value, false);
  }

  /**
   * Get sub column value from main value
   *
   * @param sub   sub column
   * @param value main value that contains the sub value
   * @return The sub value
   */
  @Nullable Object getSubColValue(DataType sub, Object value);

  /**
   * Get sub column value from main value
   *
   * @param subcolumn sub column index
   * @param cellData  main value that contains the sub value
   * @return The sub value
   */
  @Nullable Object getSubColValue(int subcolumn, Object cellData);

  /**
   * Handle value change in this parent type
   *
   * @param model          original data model that holds the parent Type (this)
   * @param subType        the sub type that was changed
   * @param subColumnIndex the index of the sub column that was changed in this parent type
   * @param newValue       the new value for the subType in this parent type
   * @param <T>            value type of DataType
   */
  default <T> void valueChanged(ModularDataModel model, DataType<T> subType, int subColumnIndex,
      T newValue) {
  }

}
