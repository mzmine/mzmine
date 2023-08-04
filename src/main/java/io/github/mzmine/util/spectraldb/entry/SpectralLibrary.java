/*
 * Copyright (c) 2004-2023 The MZmine Development Team
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

package io.github.mzmine.util.spectraldb.entry;

import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.util.MemoryMapStorage;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Stream;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public class SpectralLibrary {

  private final @NotNull String name;
  private final @NotNull File path;
  // spectra
  private final @NotNull List<SpectralLibraryEntry> entries = new ArrayList<>();

  // internals
  @Nullable
  private final MemoryMapStorage storage;
  private final ObservableMap<Class<? extends DataType>, DataType> types = FXCollections.observableMap(
      new LinkedHashMap<>());

  public SpectralLibrary(@Nullable MemoryMapStorage storage, @NotNull File path) {
    this(storage, path.getName(), path);
  }

  public SpectralLibrary(@Nullable MemoryMapStorage storage, @NotNull String name,
      @NotNull File path) {
    this.storage = storage;
    this.path = path;
    this.name = name;
  }

  @NotNull
  public List<SpectralLibraryEntry> getEntries() {
    return Collections.unmodifiableList(entries);
  }

  public void addEntry(SpectralLibraryEntry entry) {
    entry.setLibrary(this);
    entries.add(entry);
  }

  public void addEntries(Collection<SpectralLibraryEntry> entries) {
    entries.forEach(this::addEntry);
  }

  @NotNull
  public File getPath() {
    return path;
  }

  @NotNull
  public String getName() {
    return String.format("%s (%d spectra)", name, size());
  }

  public int size() {
    return entries.size();
  }

  @Override
  public String toString() {
    return getName();
  }

  public MemoryMapStorage getStorage() {
    return storage;
  }

  public void addType(Collection<DataType> newTypes) {
    for (DataType<?> type : newTypes) {
      if (!types.containsKey(type.getClass())) {
        // add row type - all rows will automatically generate a default property for this type in
        // their data map
        types.put(type.getClass(), type);
      }
    }
  }

  public void addType(@NotNull DataType<?>... types) {
    addType(Arrays.asList(types));
  }

  /**
   * Test for equal resourse paths
   *
   * @param lib other library
   * @return true if the path of both libraries equal
   */
  public boolean equalSources(SpectralLibrary lib) {
    return lib != null && lib.getPath().equals(this.getPath());
  }

  public int getNumEntries() {
    return getEntries().size();
  }

  public Stream<SpectralLibraryEntry> stream() {
    return getEntries().stream();
  }
}
