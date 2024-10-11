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
 *
 */

package io.github.mzmine.util.javafx;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import javafx.collections.ListChangeListener;

/**
 * Passes the changes that are made to a list to a backing map.
 *
 * @param <ListContent>
 * @param <MapKey>
 */
public class ListToMapListener<ListContent, MapKey> implements ListChangeListener<ListContent> {


  private final Function<ListContent, MapKey> mapper;
  private final Map<MapKey, ListContent> map;

  public ListToMapListener(Function<ListContent, MapKey> mapper, Map<MapKey, ListContent> map) {
    this.mapper = mapper;
    this.map = map;
  }

  @Override
  public void onChanged(Change<? extends ListContent> change) {
    while (change.next()) {
      if (change.wasRemoved()) {
        final List<? extends ListContent> removed = change.getRemoved();
        for (ListContent model : removed) {
          map.remove(mapper.apply(model));
        }
      }

      if (change.wasAdded()) {
        final List<? extends ListContent> added = change.getAddedSubList();
        map.putAll(added.stream().collect(Collectors.toMap(mapper, model -> model)));
      }
    }
  }
}
