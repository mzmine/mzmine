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
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package io.github.mzmine.util.spectraldb.entry;

import java.io.File;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public class SpectralLibrary {

  private final @NotNull String name;
  private final @NotNull File path;
  private final @NotNull List<SpectralDBEntry> entries;

  public SpectralLibrary(@NotNull File path, @NotNull List<SpectralDBEntry> entries) {
    this(path.getName(), path, entries);
  }

  public SpectralLibrary(@NotNull String name, @NotNull File path,
      @NotNull List<SpectralDBEntry> entries) {
    this.path = path;
    this.entries = entries;
    this.name = String.format("%s (%d spectra)", name, size());
  }

  @NotNull
  public List<SpectralDBEntry> getEntries() {
    return entries;
  }

  @NotNull
  public File getPath() {
    return path;
  }

  @NotNull
  public String getName() {
    return name;
  }

  public int size() {
    return entries.size();
  }

  @Override
  public String toString() {
    return getName();
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
}
