/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
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
