/*
 * Copyright 2006-2018 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.datamodel.data;

import java.util.Optional;
import java.util.stream.Stream;
import io.github.mzmine.datamodel.data.types.DataType;
import javafx.collections.ObservableMap;

public interface ModularDataModel {

  public ObservableMap<Class<? extends DataType>, DataType> getMap();

  default <T extends DataType<?>> Optional<T> get(Class<T> type) {
    return Optional.ofNullable(getMap().get(type));
  }

  default void set(Class<? extends DataType> class1, DataType<?> data) {
    if (class1.isInstance(data))
      getMap().put(class1, data);
    // wrong data type. Check code that supplied this data
    else
      throw new WrongTypeException(class1, data);
  }

  default void remove(Class<? extends DataType<?>> key) {
    getMap().remove(key);
  }

  default Stream<DataType> stream() {
    return getMap().values().stream();
  }
}
