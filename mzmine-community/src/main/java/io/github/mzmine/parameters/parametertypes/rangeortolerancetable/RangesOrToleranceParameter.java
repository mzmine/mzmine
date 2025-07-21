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

package io.github.mzmine.parameters.parametertypes.rangeortolerancetable;

import io.github.mzmine.parameters.UserParameter;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.stream.Collectors;
import javafx.scene.Node;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;

/**
 * @param <T>
 * @param <ToleranceParam>
 * @param <ToleranceComponent>
 */
public abstract class RangesOrToleranceParameter<T extends Number & Comparable<T>, //
    TolType extends Tolerance<T>, //
    ToleranceParam extends UserParameter<TolType, ToleranceComponent>, //
    ToleranceComponent extends Node> implements //
    UserParameter<RangeOrValueResult<T>, RangesOrToleranceComponent<T, ToleranceComponent>> {

  protected final String name;
  protected final String description;
  protected final ToleranceParam toleranceParameter;
  protected final NumberFormat numberFormat;
  protected final String unit;
  protected @Nullable RangeOrValueResult<T> value;

  public RangesOrToleranceParameter(String name, String description, String unit,
      NumberFormat format, ToleranceParam toleranceParameter) {
    this.name = name;
    this.description = description;
    this.toleranceParameter = toleranceParameter;
    this.unit = unit;
    this.numberFormat = format;
  }

  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public void setValueFromComponent(
      RangesOrToleranceComponent<T, ToleranceComponent> rangesOrToleranceComponent) {
    value = rangesOrToleranceComponent.getValue();
    toleranceParameter.setValueFromComponent(rangesOrToleranceComponent.toleranceComponent);
  }

  @Override
  public void setValueToComponent(
      RangesOrToleranceComponent<T, ToleranceComponent> rangesOrToleranceComponent,
      @Nullable RangeOrValueResult<T> newValue) {
    rangesOrToleranceComponent.setValue(newValue);
    toleranceParameter.setValue((TolType) newValue.tolerance());
    toleranceParameter.setValueToComponent(rangesOrToleranceComponent.toleranceComponent,
        toleranceParameter.getValue());
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public RangeOrValueResult<T> getValue() {
    return value;
  }

  @Override
  public void setValue(RangeOrValueResult<T> newValue) {
    this.value = newValue;
    toleranceParameter.setValue((TolType) newValue.tolerance());
  }

  @Override
  public boolean checkValue(Collection<String> errorMessages) {
    boolean failed = !toleranceParameter.checkValue(errorMessages);
    if (value == null) {
      errorMessages.add("Parameter %s has no value.".formatted(name));
      failed = true;
    }
    String invalid = value.ranges().stream().filter(v -> !v.isValid()).map(RangeOrValue::toString)
        .collect(Collectors.joining(", "));
    if (!invalid.isBlank()) {
      errorMessages.add("Ranges " + invalid + " are invalid.");
      failed = true;
    }
    return !failed;
  }
}
