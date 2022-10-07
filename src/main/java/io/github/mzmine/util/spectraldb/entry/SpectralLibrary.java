/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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
