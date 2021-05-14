/*
 *  Copyright 2006-2020 The MZmine Development Team
 *
 *  This file is part of MZmine.
 *
 *  MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 *  General Public License as published by the Free Software Foundation; either version 2 of the
 *  License, or (at your option) any later version.
 *
 *  MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 *  the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 *  Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with MZmine; if not,
 *  write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 *  USA
 */

package io.github.mzmine.datamodel.featuredata.impl;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.util.MemoryMapStorage;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ScanList<T extends Scan> implements List<T> {

  private static Logger logger = Logger.getLogger(ScanList.class.getName());

  public static final IntBuffer EMPTY_BUFFER = IntBuffer.wrap(new int[0]);

  private final IntBuffer indices;
  private final RawDataFile file;

  public ScanList(@Nonnull final List<T> scans, @Nullable final MemoryMapStorage storage) {
    if (scans.isEmpty()) {
      throw new IllegalArgumentException("Cannot create a ScanList for 0 scans.");
    }

    file = scans.get(0).getDataFile();
    final int[] indices = new int[scans.size()];

    final List<T> allScans = (List<T>) file.getScans();
    int rawScansIndex = allScans.indexOf(scans.get(0));
    int featureScansIndex = 0;
    indices[featureScansIndex] = rawScansIndex;

    while (featureScansIndex < scans.size() && rawScansIndex < allScans.size()) {
      if (scans.get(featureScansIndex)
          .equals(allScans.get(rawScansIndex))) { // don't compare identity to make robin happy
        indices[featureScansIndex] = rawScansIndex;
        featureScansIndex++;
      }
      rawScansIndex++;
    }

    this.indices = StorageUtils.storeValuesToIntBuffer(storage, indices);
  }

  @Nonnull
  public List<T> load() {
    final List<T> list = new ArrayList<>();
    final int[] buffer = new int[indices.capacity()];
    indices.get(0, buffer);

    for(int b : buffer) {
      list.add((T) file.getScan(b));
    }
    return list;
  }

  @Override
  public int size() {
    return indices.capacity();
  }

  @Override
  public boolean isEmpty() {
    return indices.capacity() == 0;
  }

  @Override
  public boolean contains(Object o) {
    return false;
  }

  @Nonnull
  @Override
  public Iterator<T> iterator() {
    return load().listIterator();
  }

  @Nonnull
  @Override
  public Object[] toArray() {
    return new Object[0];
  }

  @Nonnull
  @Override
  public <T1> T1[] toArray(@Nonnull T1[] a) {
    return null;
  }

  @Override
  public boolean add(T e) {
    throw new UnsupportedOperationException("Immutable list");
  }

  @Override
  public boolean remove(Object o) {
    throw new UnsupportedOperationException("Immutable list");
  }

  @Override
  public boolean containsAll(@Nonnull Collection<?> c) {
    return load().containsAll(c);
  }

  @Override
  public boolean addAll(@Nonnull Collection<? extends T> c) {
    throw new UnsupportedOperationException("Immutable list");
  }

  @Override
  public boolean addAll(int index, @Nonnull Collection<? extends T> c) {
    throw new UnsupportedOperationException("Immutable list");
  }

  @Override
  public boolean removeAll(@Nonnull Collection<?> c) {
    throw new UnsupportedOperationException("Immutable list");
  }

  @Override
  public boolean retainAll(@Nonnull Collection<?> c) {
    throw new UnsupportedOperationException("Immutable list");
  }

  @Override
  public void clear() {
    throw new UnsupportedOperationException("Immutable list");
  }

  @Override
  public T get(int index) {
    return (T) file.getScan(indices.get(index));
  }

  @Override
  public T set(int index, T element) {
    throw new UnsupportedOperationException("Immutable list");
  }

  @Override
  public void add(int index, T element) {
    throw new UnsupportedOperationException("Immutable list");
  }

  @Override
  public T remove(int index) {
    throw new UnsupportedOperationException("Immutable list");
  }

  @Override
  public int indexOf(Object o) {
    return load().indexOf(o);
  }

  @Override
  public int lastIndexOf(Object o) {
    return load().lastIndexOf(o);
  }

  @Nonnull
  @Override
  public ListIterator<T> listIterator() {
    return load().listIterator();
  }

  @Nonnull
  @Override
  public ListIterator<T> listIterator(int index) {
    return load().listIterator(index);
  }

  @Nonnull
  @Override
  public List<T> subList(int fromIndex, int toIndex) {
    return load().subList(fromIndex, toIndex);
  }
}
