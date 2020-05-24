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

package io.github.mzmine.datamodel.data.types.numbers.abstr;

import java.text.NumberFormat;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import io.github.mzmine.datamodel.data.types.DataType;
import javafx.beans.property.Property;

public abstract class NumberType<T extends Property<?>> extends DataType<T> {
  protected final NumberFormat DEFAULT_FORMAT;

  protected NumberType(NumberFormat defaultFormat) {
    DEFAULT_FORMAT = defaultFormat;
  }

  public abstract NumberFormat getFormatter();

  /**
   * A formatted string representation of the value
   * 
   * @return the formatted representation of the value (or an empty String)
   */
  @Override
  @Nonnull
  public String getFormattedString(@Nullable Object value) {
    if (value != null) {
      if (value instanceof Double || value instanceof Float)
        return getFormatter().format(((Number) value).doubleValue());
      else if (value instanceof Integer || value instanceof Long)
        return getFormatter().format(((Number) value).longValue());
      else
        return getFormatter().format(value);
    } else
      return "";
  }
}
