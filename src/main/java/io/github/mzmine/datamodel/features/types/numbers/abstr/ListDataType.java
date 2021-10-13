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

package io.github.mzmine.datamodel.features.types.numbers.abstr;

import io.github.mzmine.datamodel.features.types.DataType;
import java.util.ArrayList;
import java.util.List;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import org.jetbrains.annotations.NotNull;

public abstract class ListDataType<T> extends DataType<List<T>> {

  @Override
  public Property<List<T>> createProperty() {
    return new SimpleObjectProperty<>(FXCollections.observableList(new ArrayList<>()));
  }

  @Override
  public Class<List<T>> getValueClass() {
    return (Class) List.class;
  }

  @NotNull
  @Override
  public String getFormattedString(@NotNull List<T> property) {
    return property.stream().map(Object::toString).findFirst().orElse("");
  }
}
