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

package io.github.mzmine.modules.visualization.projectmetadata.color;

import io.github.mzmine.datamodel.utils.UniqueIdSupplier;
import org.jetbrains.annotations.NotNull;

public enum ColorByNumericOption implements UniqueIdSupplier {
  AUTO, GRADIENT, DISCRETE;

  public static final int AUTO_GRADIENT_ABOVE_N_SAMPLES = 6;

  @Override
  public @NotNull String getUniqueID() {
    return switch (this) {
      case AUTO -> "AUTO";
      case GRADIENT -> "GRADIENT";
      case DISCRETE -> "DISCRETE";
    };
  }

  @Override
  public String toString() {
    return switch (this) {
      case AUTO -> "Auto";
      case GRADIENT -> "Gradient";
      case DISCRETE -> "Discrete";
    };
  }

  public String getDescription() {
    return this + switch (this) {
      case AUTO -> ": Uses Gradient above %d samples".formatted(AUTO_GRADIENT_ABOVE_N_SAMPLES);
      case DISCRETE -> ": Uses separate colors from default colors";
      case GRADIENT -> ": Uses Gradient colors from default paint scale";
    };

  }

  public boolean isUseGradient(int size) {
    return switch (this) {
      case AUTO -> size > ColorByNumericOption.AUTO_GRADIENT_ABOVE_N_SAMPLES;
      case DISCRETE -> false;
      case GRADIENT -> size > 1;
    };
  }
}
