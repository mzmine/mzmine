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

package io.github.mzmine.modules.visualization.projectmetadata.extract;

import io.github.mzmine.datamodel.RawDataFile;
import java.io.File;
import org.jetbrains.annotations.NotNull;

/**
 * Defines which string of a {@link RawDataFile} is used as the regex input for metadata
 * extraction.
 */
public enum RegexInputSource {

  /**
   * The file name including the extension, e.g. {@code 20210610_blank_01.mzML}.
   */
  FILE_NAME("File name (with extension)"),
  /**
   * The file name without the extension, e.g. {@code 20210610_blank_01}.
   */
  FILE_NAME_WITHOUT_EXTENSION("File name (no extension)"),
  /**
   * The absolute path the file was loaded from.
   */
  ABSOLUTE_PATH("Absolute path"),
  /**
   * The name of the parent folder the file is located in.
   */
  PARENT_FOLDER("Parent folder name");

  private final String label;

  RegexInputSource(final String label) {
    this.label = label;
  }

  /**
   * Extracts the input string from the given file according to this source. Never returns null - an
   * empty string is returned if the information is not available.
   *
   * @param raw the raw data file
   * @return the input string used for regex matching
   */
  public @NotNull String extract(@NotNull final RawDataFile raw) {
    return switch (this) {
      case FILE_NAME -> raw.getFileName();
      case FILE_NAME_WITHOUT_EXTENSION -> stripExtension(raw.getFileName());
      case ABSOLUTE_PATH -> {
        final String path = raw.getAbsolutePath();
        yield path != null ? path : raw.getName();
      }
      case PARENT_FOLDER -> {
        final String path = raw.getAbsolutePath();
        if (path == null) {
          yield "";
        }
        final File parent = new File(path).getParentFile();
        yield parent != null ? parent.getName() : "";
      }
    };
  }

  private static @NotNull String stripExtension(@NotNull final String fileName) {
    final int dot = fileName.lastIndexOf('.');
    // decision: only strip a real extension, not a leading dot of a hidden file
    return dot > 0 ? fileName.substring(0, dot) : fileName;
  }

  @Override
  public String toString() {
    return label;
  }
}
