/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.modules.io.export_scans_modular;

import io.github.mzmine.datamodel.utils.UniqueIdSupplier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public enum Ms1ScanSelection implements UniqueIdSupplier {
  MS1, CORRELATED, MS1_AND_CORRELATED;

  public Ms1ScanSelection parseOrElse(@Nullable String toParse,
      @Nullable Ms1ScanSelection defaultValue) {
    return UniqueIdSupplier.parseOrElse(toParse, values(), defaultValue);
  }

  @Override
  public String toString() {
    return switch (this) {
      case MS1 -> "MS1";
      case CORRELATED -> "Correlated";
      case MS1_AND_CORRELATED -> "MS1 & Correlated";
    };
  }

  @Override
  public @NotNull String getUniqueID() {
    return switch (this) {
      case CORRELATED -> "correlated";
      case MS1 -> "ms1";
      case MS1_AND_CORRELATED -> "ms1_and_correlated";
    };
  }

  public boolean includesCorrelated() {
    return this == CORRELATED || this == MS1_AND_CORRELATED;
  }

  public boolean includesMs1() {
    return this == MS1 || this == MS1_AND_CORRELATED;
  }
}
