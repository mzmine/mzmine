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

import java.util.HashMap;
import java.util.Optional;
import java.util.stream.Stream;
import io.github.mzmine.datamodel.data.types.DataType;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;

@SuppressWarnings({"rawtypes"})
public class DataTypeMap {
  private static final long serialVersionUID = 1L;

  private final ObservableMap<Class<? extends DataType>, DataType> map =
      FXCollections.observableMap(new HashMap<>());

  public <T extends DataType<?>> Optional<T> get(Class<T> type) {
    return Optional.ofNullable(map.get(type));
  }

  public void set(Class<? extends DataType> class1, DataType<?> data) {
    if (class1.isInstance(data))
      map.put(class1, data);
    // wrong data type. Check code that supplied this data
    else
      throw new WrongTypeException(class1, data);
  }

  public void remove(Class<? extends DataType<?>> key) {
    map.remove(key);
  }

  /**
   * TODO make this list immutable? to force the use of DataTypeMap (or extend Map type for this
   * class)
   * 
   * @return
   */
  public ObservableMap<Class<? extends DataType>, DataType> getObservableMap() {
    return map;
  }

  public Stream<DataType> stream() {
    return map.values().stream();
  }
}
