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

import io.github.mzmine.modules.visualization.projectmetadata.ProjectMetadataColumnParameters.AvailableTypes;
import org.jetbrains.annotations.Nullable;

/**
 * Target type of a metadata column created by the extraction. {@link #AUTO} lets the extraction
 * detect the most appropriate type from all extracted values of the whole column (see
 * {@link AvailableTypes#castToMostAppropriateType}).
 */
public enum ExtractColumnType {

  /**
   * Detect the type automatically from all extracted values of the column.
   */
  AUTO("Auto"),
  /**
   * Numbers parsed as double.
   */
  NUMBER("Number"),
  /**
   * Date/time parsed as {@link java.time.LocalDateTime}.
   */
  DATE("Date"),
  /**
   * Plain text.
   */
  TEXT("Text");

  private final String label;

  ExtractColumnType(final String label) {
    this.label = label;
  }

  /**
   * @return the matching {@link AvailableTypes} or {@code null} for {@link #AUTO} (type is detected
   * from the data).
   */
  public @Nullable AvailableTypes toAvailableType() {
    return switch (this) {
      case AUTO -> null;
      case NUMBER -> AvailableTypes.NUMBER;
      case DATE -> AvailableTypes.DATETIME;
      case TEXT -> AvailableTypes.TEXT;
    };
  }

  @Override
  public String toString() {
    return label;
  }
}
