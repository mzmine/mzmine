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

import io.github.mzmine.parameters.parametertypes.combowithinput.ComboWithStringInputValue;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ExtraColumnHandler {

  private final HandleExtraColumnsOptions selectedOption;
  private final @NotNull Set<String> extraColumns;

  public ExtraColumnHandler(ComboWithStringInputValue<HandleExtraColumnsOptions> value) {
    selectedOption = value.getSelectedOption();
    final String columnsList = value.getEmbeddedValue();
    if (columnsList == null) {
      extraColumns = Collections.emptySet();
      return;
    }

    final String[] columns = columnsList.strip().split(",");
    extraColumns = Arrays.stream(columns).map(String::strip).filter(s -> !s.isBlank())
        .map(String::toLowerCase).collect(Collectors.toSet());
  }

  public HandleExtraColumnsOptions getSelectedOption() {
    return selectedOption;
  }

  public @NotNull Set<String> getExtraColumns() {
    return extraColumns;
  }

  /**
   * @return true if the column shall be imported. Always true if {@link #getSelectedOption()} is
   * {@link HandleExtraColumnsOptions#IMPORT_ALL} unless the given column name is null.
   */
  public boolean isImportedColumn(@Nullable final String column) {
    if (column == null) {
      return false;
    }
    return switch (selectedOption) {
      case IGNORE -> false;
      case IMPORT_SPECIFIC -> extraColumns.contains(column.toLowerCase());
      case IMPORT_ALL -> true;
    };
  }

  /**
   * @return True if either specific or all other columns will be imported.
   */
  public boolean isImportExtraColumns() {
    return switch (selectedOption) {
      case IGNORE -> false;
      case IMPORT_SPECIFIC, IMPORT_ALL -> true;
    };
  }
}
