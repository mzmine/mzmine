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

package io.github.mzmine.modules.visualization.projectmetadata.table;

import io.github.mzmine.datamodel.RawDataFile;
import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This allows nullable value for grouping by
 */
public final class SamplesGroupedBy<T> {

  private final @Nullable T value;
  private final @NotNull List<RawDataFile> files;
  private final double doubleValue;

  /**
   * @param value       the value grouped by
   * @param files
   * @param doubleValue a double value to enable comparison. Date and numbers are converted and
   *                    strings usually just follow the group order as integers.
   */
  public SamplesGroupedBy(@Nullable T value, @NotNull List<RawDataFile> files, double doubleValue) {
    this.value = value;
    this.files = files;
    this.doubleValue = doubleValue;
  }

  /**
   * @return a double value to enable comparison. Date and numbers are converted and strings usually
   * just follow the group order as integers.
   */
  public double doubleValue() {
    return doubleValue;
  }

  public boolean isNullValue() {
    return value == null;
  }

  public int size() {
    return files.size();
  }

  public @Nullable T value() {
    return value;
  }

  public @NotNull List<RawDataFile> files() {
    return files;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj == null || obj.getClass() != this.getClass()) {
      return false;
    }
    var that = (SamplesGroupedBy) obj;
    return Objects.equals(this.value, that.value) && Objects.equals(this.files, that.files)
        && Objects.equals(this.doubleValue, that.doubleValue);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value, files);
  }

  @Override
  public String toString() {
    return "SamplesGroupedBy[" + "value=" + value + ", " + "files=" + files + ']';
  }


  /**
   * @return N/A for null or toString otherwise
   */
  public String valueString() {
    if (value == null) {
      return "unnamed";
    }
    return value.toString();
  }
}
