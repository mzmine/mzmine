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

public enum SpectralLibrarySelectionType implements UniqueIdSupplier {

  ALL_IMPORTED("All imported libraries"), //
  SPECIFIC("Specific libraries"),
  AS_SELECTED_IN_MAIN_WINDOW("As selected in main window");

  private final String stringValue;

  SpectralLibrarySelectionType(String stringValue) {
    this.stringValue = stringValue;
  }

  @Override
  public String toString() {
    return stringValue;
  }

  @Override
  public @NotNull String getUniqueID() {
    return switch (this) {
      case ALL_IMPORTED -> "ALL_IMPORTED";
      case SPECIFIC -> "SPECIFIC";
      case AS_SELECTED_IN_MAIN_WINDOW -> "selected_in_main_window";
    };
  }
}
