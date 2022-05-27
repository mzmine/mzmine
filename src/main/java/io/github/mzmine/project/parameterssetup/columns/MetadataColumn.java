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

package io.github.mzmine.project.parameterssetup.columns;

import java.util.Objects;
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
   * Convert input string to the specific type of the parameter.
   *
   * @param input input string
   * @return converted value of the specific type
   */
  public abstract T convert(String input, T defaultValue);

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
