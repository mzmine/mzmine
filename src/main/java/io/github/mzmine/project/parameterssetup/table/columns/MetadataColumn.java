/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.project.parameterssetup.table.columns;

import io.github.mzmine.project.parameterssetup.ProjectMetadataParameters.AvailableTypes;
import java.util.Objects;
import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

/**
 * Abstract parameter column sealed class, afterwards it will be inherited by the specific
 * parameters types.
 *
 * @param <T> datatype of the project parameter
 */
public abstract sealed class MetadataColumn<T> permits StringMetadataColumn, DoubleMetadataColumn,
    DateMetadataColumn {

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
   * Get the project parameter title.
   *
   * @return project parameter title
   */
  public String getTitle() {
    return title;
  }

  /**
   * Get the project parameter description.
   *
   * @return project parameter description.
   */
  public String getDescription() {
    return description;
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
      case DOUBLE -> new DoubleMetadataColumn(name, description);
      case DATETIME -> new DateMetadataColumn(name, description);
    };
  }

  /**
   * Convert input string to the specific type of the parameter.
   *
   * @param input input string
   * @return converted value of the specific type
   */
  @Nullable
  public abstract T convert(@Nullable String input, @Nullable T defaultValue);

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
    if (!(o instanceof MetadataColumn)) {
      return false;
    }
    MetadataColumn<?> that = (MetadataColumn<?>) o;
    return title.equals(that.title) && description.equals(that.description);
  }

  @Override
  public int hashCode() {
    return Objects.hash(title, description);
  }
}
