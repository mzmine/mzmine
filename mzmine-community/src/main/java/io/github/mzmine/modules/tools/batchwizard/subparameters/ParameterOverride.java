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

import java.io.Serializable;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a single parameter override for a specific module parameter. This class is
 * serializable so it can be saved/loaded with wizard presets.
 */
public class ParameterOverride implements Serializable {

  private final String moduleClassName;
  private final String moduleName;
  private final String parameterName;
  private final String valueAsString;
  private final String valueType;

  public ParameterOverride(@NotNull String moduleClassName, @NotNull String moduleName,
      @NotNull String parameterName, @NotNull Object value) {
    this.moduleClassName = moduleClassName;
    this.moduleName = moduleName;
    this.parameterName = parameterName;
    this.valueAsString = value == null ? "null" : value.toString();
    this.valueType = value == null ? "null" : value.getClass().getName();
  }

  public ParameterOverride(@NotNull String moduleClassName, @NotNull String moduleName,
      @NotNull String parameterName, @NotNull String valueAsString, @NotNull String valueType) {
    this.moduleClassName = moduleClassName;
    this.moduleName = moduleName;
    this.parameterName = parameterName;
    this.valueAsString = valueAsString;
    this.valueType = valueType;
  }

  public String getModuleClassName() {
    return moduleClassName;
  }

  public String getModuleName() {
    return moduleName;
  }

  public String getParameterName() {
    return parameterName;
  }

  public String getValueAsString() {
    return valueAsString;
  }

  public String getValueType() {
    return valueType;
  }

  /**
   * Gets a display string for the value (truncated if too long)
   */
  public String getDisplayValue() {
    if (valueAsString.length() > 50) {
      return valueAsString.substring(0, 47) + "...";
    }
    return valueAsString;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ParameterOverride that = (ParameterOverride) o;
    return Objects.equals(moduleClassName, that.moduleClassName) && Objects.equals(parameterName,
        that.parameterName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(moduleClassName, parameterName);
  }

  @Override
  public String toString() {
    return moduleName + "." + parameterName + " = " + getDisplayValue();
  }
}