/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.modules.visualization.projectmetadata.table.columns;

import io.github.mzmine.modules.visualization.projectmetadata.ProjectMetadataColumnParameters.AvailableTypes;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Abstract parameter column sealed class, afterwards it will be inherited by the specific
 * parameters types.
 *
 * @param <T> datatype of the project parameter
 */
public abstract sealed class MetadataColumn<T> permits StringMetadataColumn, DoubleMetadataColumn,
    DateMetadataColumn {

  public static final String FILENAME_HEADER = "filename";
  public static final String DATE_HEADER = "run_date";
  // renamed this from sample_type to mz_sample_type because too standard name that may be overwritten by users metadata
  public static final String SAMPLE_TYPE_HEADER = "mzmine_sample_type";

  /**
   * Title (name) of the parameter.
   */
  private final @NotNull String title;

  /**
   * Description (optional) of the parameter.
   */
  private final @NotNull String description;

  public MetadataColumn(String title) {
    this.title = (title != null && !title.isBlank()) ? title : "None";
    this.description = "";
  }

  public MetadataColumn(String title, String description) {
    this.title = (title != null && !title.isBlank()) ? title : "None";
    this.description = (description != null) ? description : "";
  }

  /**
   * Factory method for creating the MetadataColumn instances according to their type and name.
   *
   * @param type        type of the parameter
   * @param name        name of the parameter
   * @param description description of the parameter
   * @return instance of the MetadataColumn
   */
  public static MetadataColumn forType(AvailableTypes type, String name, String description) {
    return switch (type) {
      case TEXT -> new StringMetadataColumn(name, description);
      case NUMBER -> new DoubleMetadataColumn(name, description);
      case DATETIME -> new DateMetadataColumn(name, description);
    };
  }

  /**
   * Get the project parameter title.
   *
   * @return project parameter title
   */
  public @NotNull String getTitle() {
    return title;
  }

  /**
   * Validate input depending on it's type.
   *
   * @param value input to be validated
   * @return true if input is valid, false otherwise
   */
  public abstract boolean checkInput(Object value);

  /**
   * Get type of the parameter in this column.
   *
   * @return type of the parameter
   */
  public abstract AvailableTypes getType();

  /**
   * Factory method for creating the MetadataColumn instances according to their type and name.
   *
   * @param type type of the parameter
   * @param name name of the parameter
   * @return instance of the MetadataColumn
   */
  public static MetadataColumn forType(AvailableTypes type, String name) {
    return forType(type, name, null);
  }

  /**
   * Get the project parameter description.
   *
   * @return project parameter description.
   */
  public @NotNull String getDescription() {
    return description;
  }

  /**
   * Convert input string to the specific type of the parameter.
   *
   * @param input        input string
   * @param defaultValue default value if input is null or fails to cast
   * @return converted value of the specific type
   */
  @Nullable
  public abstract T convertOrElse(@Nullable String input, @Nullable T defaultValue);

  /**
   * Convert input string to the specific type of the parameter.
   *
   * @param input input string
   * @return converted value of the specific type or error
   */
  public abstract T convertOrThrow(@NotNull String input);

  /**
   * Returns the default value for the columns of such type.
   *
   * @return the value of the column type T.
   */
  public abstract T defaultValue();

  /**
   * Returns the example value for the columns of such type.
   *
   * @return the value of the column type T.
   */
  public abstract T exampleValue();

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof MetadataColumn<?> that)) {
      return false;
    }
    return title.equals(that.title) && description.equals(that.description);
  }

  @Override
  public int hashCode() {
    return Objects.hash(title, description);
  }

  @Override
  public String toString() {
    return getTitle();
  }
}
