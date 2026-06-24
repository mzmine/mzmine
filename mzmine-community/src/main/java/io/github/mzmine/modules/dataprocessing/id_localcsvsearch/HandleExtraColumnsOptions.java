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

package io.github.mzmine.modules.dataprocessing.id_localcsvsearch;

import io.github.mzmine.datamodel.utils.UniqueIdSupplier;
import io.github.mzmine.util.StringUtils;
import org.jetbrains.annotations.NotNull;

public enum HandleExtraColumnsOptions implements UniqueIdSupplier {
  IGNORE, IMPORT_SPECIFIC, IMPORT_ALL;

  public String getDescription() {
    return switch (this) {
      case IGNORE -> "Ignore all columns that are not selected and specified in the %s.".formatted(
          StringUtils.inQuotes("Columns"));
      case IMPORT_SPECIFIC -> """
          Import columns with specific headers as specified in the text field as a JSON string.
          Multiple columns can be specified by comma separation.""";
      case IMPORT_ALL -> "All additional columns will be imported as a JSON string.";
    };
  }

  @Override
  public @NotNull String getUniqueID() {
    return switch (this) {
      case IGNORE -> "ignore";
      case IMPORT_SPECIFIC -> "import_specific";
      case IMPORT_ALL -> "import_all";
    };
  }

  @Override
  public String toString() {
    return switch (this) {
      case IGNORE -> "Ignore all";
      case IMPORT_SPECIFIC -> "Import specific";
      case IMPORT_ALL -> "Import all";
    };
  }
}
