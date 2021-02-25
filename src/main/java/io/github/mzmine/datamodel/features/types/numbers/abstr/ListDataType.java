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

package io.github.mzmine.datamodel.features.types.numbers.abstr;

import java.util.ArrayList;
import java.util.List;

import io.github.mzmine.datamodel.features.types.DataType;
import javafx.beans.property.ListProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class ListDataType<T> extends DataType<ListProperty<T>> {

  @Override
  public ListProperty<T> createProperty() {
    return new SimpleListProperty<T>(FXCollections.observableList(new ArrayList<T>()));
  }

  @Nonnull
  @Override
  public String getFormattedString(@Nullable Object value) {
    if(value==null)
      return "";
    if(value instanceof List)
      return (String) ((List)value).stream().map(Object::toString).findFirst().orElse("");
    if(value instanceof ListProperty)
      return (String) ((ListProperty)value).stream().map(Object::toString).findFirst().orElse("");
    return value.toString();
  }

  @Nonnull
  @Override
  public String getFormattedString(@Nonnull ListProperty<T> property) {
    return property.stream().map(Object::toString).findFirst().orElse("");
  }
}
