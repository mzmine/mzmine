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

package io.github.mzmine.modules.tools.batchwizard.subparameters;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a single parameter override for a specific module parameter. This class is
 * serializable so it can be saved/loaded with wizard presets.
 */
public record ParameterOverride(String moduleClassName, String moduleName,
                                Parameter<?> parameterWithValue) {

  public ParameterOverride(@NotNull String moduleClassName, @NotNull String moduleName,
      @NotNull final Parameter<?> parameterWithValue) {
    this.moduleClassName = moduleClassName;
    this.moduleName = moduleName;
    this.parameterWithValue = parameterWithValue;
  }

  /**
   * Gets a display string for the value (truncated if too long)
   */
  public String getDisplayValue() {
    String valueAsString = String.valueOf(parameterWithValue.getValue());
    if (parameterWithValue instanceof OptionalParameter<?> opt) {
      valueAsString += String.valueOf(opt.getEmbeddedParameter().getValue());
    }

    if (valueAsString.length() > 50) {
      return valueAsString.substring(0, 47) + "...";
    }
    return valueAsString;
  }

  @Override
  public @NotNull String toString() {
    return moduleName + "." + parameterWithValue.getName() + " = " + getDisplayValue();
  }
}