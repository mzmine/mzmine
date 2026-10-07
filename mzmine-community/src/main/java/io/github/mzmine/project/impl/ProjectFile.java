/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.project.impl;

import io.github.mzmine.datamodel.RawDataFile;
import java.io.File;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Describes a file in the current mzmine project. Since some data formats may contain multiple
 * samples (wiff2), we have to check for equality by name in the project + abs file path.
 *
 * @param fileOnDisk
 * @param nameInProject
 */
public record ProjectFile(@Nullable File fileOnDisk, String nameInProject) {

  ProjectFile(@NotNull RawDataFile file) {
    this(file.getAbsoluteFilePath(), file.getName());
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ProjectFile that = (ProjectFile) o;
    return Objects.equals(fileOnDisk, that.fileOnDisk) && Objects.equals(nameInProject,
        that.nameInProject);
  }

  @Override
  public int hashCode() {
    int result = Objects.hashCode(fileOnDisk);
    result = 31 * result + Objects.hashCode(nameInProject);
    return result;
  }
}
