/*
 * Copyright 2006-2022 The MZmine Development Team
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

package io.github.mzmine.modules.visualization.projectmetadata.table.columns;

import io.github.mzmine.modules.visualization.projectmetadata.ProjectMetadataParameters.AvailableTypes;
import io.github.mzmine.util.DateTimeUtils;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

/**
 * Specific Date-type implementation of the project parameter.
 */
public final class DateMetadataColumn extends MetadataColumn<LocalDateTime> {

  public DateMetadataColumn(String title) {
    super(title);
  }

  public DateMetadataColumn(String title, String description) {
    super(title, description);
  }

  @Override
  public boolean checkInput(Object value) {
    return value instanceof LocalDateTime;
  }

  @Override
  public AvailableTypes getType() {
    return AvailableTypes.DATETIME;
  }

  // todo: make it clear with the logic of covert; should it ever return null?
  @Override
  public LocalDateTime convert(String input, LocalDateTime defaultValue) {
    try {
      //ISO-8601 format
      return input == null ? defaultValue : DateTimeUtils.parse(input.trim());
//      return LocalDateTime.parse(input.trim(), DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"));
    } catch (DateTimeParseException ignored) {
      return defaultValue;
    }
  }

  @Override
  public LocalDateTime defaultValue() {
    return null;
  }

  @Override
  public LocalDateTime exampleValue() {
    return LocalDateTime.of(2021, 6, 10, 22, 23);
  }
}
