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

package io.github.mzmine.parameters.parametertypes.selectors;

import io.github.mzmine.datamodel.utils.UniqueIdSupplier;
import org.jetbrains.annotations.NotNull;

public enum RawDataFilesSelectionType implements UniqueIdSupplier {

  GUI_SELECTED_FILES("As selected in main window"), //
  ALL_FILES("All raw data files"), //
  SPECIFIC_FILES("Specific raw data files"), //
  NAME_PATTERN("File name pattern"), //
  BY_METADATA("By metadata"), //
  BATCH_LAST_FILES("Those created by previous batch step");

  private final String stringValue;

  RawDataFilesSelectionType(String stringValue) {
    this.stringValue = stringValue;
  }

  @Override
  public String toString() {
    return stringValue;
  }

  @Override
  public @NotNull String getUniqueID() {
    return switch (this) {
      case GUI_SELECTED_FILES -> "GUI_SELECTED_FILES";
      case ALL_FILES -> "ALL_FILES";
      case SPECIFIC_FILES -> "SPECIFIC_FILES";
      case NAME_PATTERN -> "NAME_PATTERN";
      case BY_METADATA -> "BY_METADATA";
      case BATCH_LAST_FILES -> "BATCH_LAST_FILES";
    };
  }

  /**
   * @return true if requires additional user input
   */
  public boolean hasAdditionalUserInput() {
    return switch (this) {
      case GUI_SELECTED_FILES, ALL_FILES, BATCH_LAST_FILES -> false;
      case SPECIFIC_FILES, NAME_PATTERN, BY_METADATA -> true;
    };
  }
}
