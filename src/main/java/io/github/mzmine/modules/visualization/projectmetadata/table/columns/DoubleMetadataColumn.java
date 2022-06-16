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

package io.github.mzmine.modules.visualization.projectmetadata.table.columns;

import io.github.mzmine.modules.visualization.projectmetadata.ProjectMetadataParameters.AvailableTypes;

/**
 * Specific Double-type implementation of the project parameter.
 */
public final class DoubleMetadataColumn extends MetadataColumn<Double> {

  public DoubleMetadataColumn(String title) {
    super(title);
  }

  public DoubleMetadataColumn(String title, String description) {
    super(title, description);
  }

  @Override
  public boolean checkInput(Object value) {
    return value instanceof Double;
  }

  @Override
  public AvailableTypes getType() {
    return AvailableTypes.DOUBLE;
  }

  @Override
  public Double convert(String input, Double defaultValue) {
    try {
      return input == null ? defaultValue : Double.parseDouble(input.trim());
    } catch (NumberFormatException ignored) {
      return defaultValue;
    }
  }

  @Override
  public Double defaultValue() {
    return null;
  }

  @Override
  public Double exampleValue() {
    return 19.21;
  }
}
