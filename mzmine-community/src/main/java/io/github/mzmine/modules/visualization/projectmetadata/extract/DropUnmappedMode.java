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

import io.github.mzmine.datamodel.utils.UniqueIdSupplier;
import org.jetbrains.annotations.NotNull;

/**
 * Controls what happens to extracted values that are not listed in the value-mapping table.
 */
public enum DropUnmappedMode implements UniqueIdSupplier {

  /**
   * Values not found in the mapping list are passed through unchanged.
   */
  KEEP_UNMAPPED("Keep unmapped values"),
  /**
   * Values not found in the mapping list are left empty.
   */
  DROP_UNMAPPED("Drop unmapped values");

  private final String label;

  DropUnmappedMode(final String label) {
    this.label = label;
  }

  @Override
  public String toString() {
    return label;
  }

  @Override
  public @NotNull String getUniqueID() {
    return switch (this) {
      case KEEP_UNMAPPED -> "KEEP_UNMAPPED";
      case DROP_UNMAPPED -> "DROP_UNMAPPED";
    };
  }
}
