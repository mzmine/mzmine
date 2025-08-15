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

package io.github.mzmine.parameters.parametertypes;

import io.github.mzmine.datamodel.features.compoundannotations.CompoundDBAnnotation;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.DataTypes;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ImportType<T> {

  private final BooleanProperty selected;
  private final StringProperty csvColumnName;
  private final ObjectProperty<DataType<T>> dataType;
  private final @NotNull Function<@Nullable String, @Nullable T> mapper;

  private int columnIndex = -1;

  public ImportType(boolean selected, @NotNull final DataType<T> dataType) {
    if (dataType.getMapper() == null) {
      throw new IllegalArgumentException(
          "DataType %s has no string mapper. Cannot use in ImportType".formatted(
              dataType.getUniqueID()));
    }
    this(selected, dataType.getUniqueID(), dataType, dataType.getMapper());
  }

  public ImportType(boolean selected, @NotNull final DataType<T> dataType,
      @NotNull final Function<@Nullable String, @Nullable T> customMapper) {
    this(selected, dataType.getUniqueID(), dataType, customMapper);
  }

  public ImportType(Boolean selected, @NotNull final String csvColumnName,
      @NotNull final DataType<T> dataType) {
    if (dataType.getMapper() == null) {
      throw new IllegalArgumentException(
          "DataType %s has no string mapper. Cannot use in ImportType".formatted(
              dataType.getUniqueID()));
    }
    this(selected, csvColumnName, dataType, dataType.getMapper());
  }

  public ImportType(Boolean selected, @NotNull final String csvColumnName,
      @NotNull final DataType<T> dataType,
      @NotNull final Function<@Nullable String, @Nullable T> customMapper) {
    this.selected = new SimpleBooleanProperty(selected);
    this.csvColumnName = new SimpleStringProperty(csvColumnName);
    this.dataType = new SimpleObjectProperty<>(dataType);
    this.mapper = customMapper;
  }

  /**
   * @param importTypes A list of import types
   * @param type        the type to search for
   * @return true if the type is in the list and selected, false if the type is not in the list or
   * in the list but not selected.
   */
  public static boolean isDataTypeSelectedInImportTypes(
      @Nullable final List<@Nullable ImportType<?>> importTypes,
      @NotNull final Class<? extends DataType<?>> type) {
    if (importTypes == null) {
      return false;
    }
    final DataType<?> instance = DataTypes.get(type);
    return importTypes.stream().filter(t -> t != null && t.getDataType().equals(instance))
        .findFirst().map(ImportType::isSelected).orElse(false);
  }

  @Override
  public String toString() {
    return "ImportType{" + selected.get() + ", in csv=" + csvColumnName.get() + ", type="
        + dataType.get() + ", index=" + columnIndex + '}';
  }

  public boolean isSelected() {
    return selected.get();
  }

  public void setSelected(boolean selected) {
    this.selected.set(selected);
  }

  public BooleanProperty selectedProperty() {
    return selected;
  }

  public String getCsvColumnName() {
    return csvColumnName.get();
  }

  public void setCsvColumnName(String csvColumnName) {
    this.csvColumnName.set(csvColumnName);
  }

  public StringProperty csvColumnName() {
    return csvColumnName;
  }

  public DataType<T> getDataType() {
    return dataType.get();
  }

  public void setDataType(DataType<T> dataType) {
    this.dataType.set(dataType);
  }

  public ObjectProperty<DataType<T>> dataTypeProperty() {
    return dataType;
  }

  /**
   * @return The column index if specified. This value is not set in the gui and has to be
   * determined from the file. -1 if the column index was not found.
   */
  public int getColumnIndex() {
    return columnIndex;
  }

  /**
   * @param columnIndex The column index. This value is not set in the gui and has to be determined
   *                    from the file.
   */
  public void setColumnIndex(int columnIndex) {
    this.columnIndex = columnIndex;
  }

  public @NotNull Function<String, T> getMapper() {
    return mapper;
  }

  public T apply(@Nullable String[] csvValues) {
    return apply(csvValues[columnIndex]);
  }

  public T apply(@Nullable String csvValue) {
    return mapper.apply(csvValue);
  }

  public T apply(Map<DataType<?>, @Nullable String> csvValues) {
    final String s = csvValues.get(dataType);
    return apply(s);
  }

  public T applyAndRemove(Map<@NotNull String, @Nullable String> csvValues) {
    final String value = csvValues.remove(csvColumnName.get());
    return apply(value);
  }

  /**
   * Checks if the {@link ImportType#csvColumnName()} is present in the csvValues map. If yes, gets
   * the value, removes it and maps it into the compound annotation. This method is a type save way
   * to circumvent using non-templated code.
   */
  public void applyRemoveAndPut(@NotNull Map<@NotNull String, @Nullable String> csvValues,
      @NotNull CompoundDBAnnotation annotation) {
    final T value = applyAndRemove(csvValues);
    annotation.put(dataType.get(), value);
  }
}
