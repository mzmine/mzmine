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

package io.github.mzmine.parameters.parametertypes;

import io.github.mzmine.parameters.UserParameter;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;

/**
 * Integer parameter with synchronized slider editor for bounded weight inputs.
 */
public class WeightSliderParameter implements UserParameter<Integer, WeightSliderComponent> {

  private final @NotNull String name;
  private final @NotNull String description;
  private final int minimum;
  private final int maximum;
  private final boolean valueRequired;
  private @Nullable Integer value;

  public WeightSliderParameter(final @NotNull String name, final @NotNull String description,
      final int defaultValue, final int minimum, final int maximum) {
    this(name, description, defaultValue, minimum, maximum, true);
  }

  public WeightSliderParameter(final @NotNull String name, final @NotNull String description,
      final int defaultValue, final int minimum, final int maximum,
      final boolean valueRequired) {
    this.name = name;
    this.description = description;
    this.minimum = minimum;
    this.maximum = maximum;
    this.valueRequired = valueRequired;
    this.value = defaultValue;
  }

  @Override
  public @NotNull String getName() {
    return name;
  }

  @Override
  public @NotNull String getDescription() {
    return description;
  }

  @Override
  public @NotNull WeightSliderComponent createEditingComponent() {
    final @NotNull WeightSliderComponent component = new WeightSliderComponent(minimum, maximum);
    component.setValue(value);
    return component;
  }

  @Override
  public @Nullable Integer getValue() {
    return value;
  }

  @Override
  public void setValue(final @Nullable Integer value) {
    this.value = value;
  }

  @Override
  public @NotNull WeightSliderParameter cloneParameter() {
    final @NotNull WeightSliderParameter copy = new WeightSliderParameter(name, description,
        value == null ? minimum : value, minimum, maximum, valueRequired);
    copy.setValue(value);
    return copy;
  }

  @Override
  public void setValueFromComponent(final @NotNull WeightSliderComponent component) {
    value = component.getValue();
  }

  @Override
  public void setValueToComponent(final @NotNull WeightSliderComponent component,
      final @Nullable Integer newValue) {
    component.setValue(newValue);
  }

  @Override
  public void loadValueFromXML(final @NotNull Element xmlElement) {
    final @NotNull String numString = xmlElement.getTextContent();
    if (!numString.isEmpty()) {
      value = Integer.parseInt(numString);
    }
  }

  @Override
  public void saveValueToXML(final @NotNull Element xmlElement) {
    if (value != null) {
      xmlElement.setTextContent(value.toString());
    }
  }

  @Override
  public boolean checkValue(final @NotNull Collection<String> errorMessages) {
    if (valueRequired && value == null) {
      errorMessages.add(name + " is not set properly");
      return false;
    }
    if (value != null && (value < minimum || value > maximum)) {
      errorMessages.add(name + " lies outside its bounds: (" + minimum + " ... " + maximum + ')');
      return false;
    }
    return true;
  }
}
