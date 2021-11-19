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

package io.github.mzmine.util.javafx;

import java.util.ArrayList;
import java.util.List;
import javafx.collections.ModifiableObservableListBase;

/**
 * A simple ObservableList implementation.
 */
public class ArrayObservableList<E> extends ModifiableObservableListBase<E> {

  private final List<E> delegate = new ArrayList<>();

  @Override
  public E get(int index) {
    return delegate.get(index);
  }

  @Override
  public int size() {
    return delegate.size();
  }

  @Override
  protected void doAdd(int index, E element) {
    delegate.add(index, element);
  }

  @Override
  protected E doSet(int index, E element) {
    return delegate.set(index, element);
  }

  @Override
  protected E doRemove(int index) {
    return delegate.remove(index);
  }

}


