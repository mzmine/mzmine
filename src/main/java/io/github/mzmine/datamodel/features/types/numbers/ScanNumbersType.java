/*
 * Copyright 2006-2020 The MZmine Development Team
 * 
 * This file is part of MZmine.
 * 
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.datamodel.features.types.numbers;

import io.github.mzmine.datamodel.features.types.modifiers.NullColumnType;
import io.github.mzmine.datamodel.features.types.numbers.abstr.ListDataType;
import javafx.beans.property.ListProperty;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class ScanNumbersType extends ListDataType<Integer> {

  @Override
  public String getHeaderString() {
    return "Scans";
  }


  @Nonnull
  @Override
  public String getFormattedString(@Nonnull ListProperty<Integer> property) {
    return property.getValue() != null ? String.valueOf(property.getValue().size()) : "";
  }

  @Nonnull
  @Override
  public String getFormattedString(@Nullable Object value) {
    if(value==null || !(value instanceof List))
      return "";
    return String.valueOf(((List)value).size());
  }
}
