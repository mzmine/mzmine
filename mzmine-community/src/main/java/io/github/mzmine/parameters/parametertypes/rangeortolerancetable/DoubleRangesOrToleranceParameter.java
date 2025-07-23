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
import java.text.ParseException;
import java.util.Collection;
import java.util.stream.Collectors;
import javafx.scene.Node;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;

/**
 * @param <TolType>
 * @param <ToleranceParam>
 * @param <ToleranceComponent>
 */
public class DoubleRangesOrToleranceParameter<TolType extends Tolerance<Double>, //
    ToleranceParam extends UserParameter<TolType, ToleranceComponent>, //
    ToleranceComponent extends Node> extends
    RangesOrToleranceParameter<Double, TolType, ToleranceParam, ToleranceComponent> {


  public DoubleRangesOrToleranceParameter(String name, String description, String unit,
      NumberFormat format, ToleranceParam toleranceParameter) {
    super(name, description, unit, format, toleranceParameter);
  }

  @Override
  public RangesOrToleranceComponent<Double, ToleranceComponent> createEditingComponent() {
    return new DoubleRangesOrToleranceComponent<>(toleranceParameter, unit, numberFormat);
  }

  @Override
  public UserParameter<RangeOrValueResult<Double>, RangesOrToleranceComponent<Double, ToleranceComponent>> cloneParameter() {
    final var clone = new DoubleRangesOrToleranceParameter<>(name, description, unit, numberFormat,
        toleranceParameter.cloneParameter());
    final RangeOrValueResult<Double> value = this.value != null ? this.value.copy() : null;
    clone.setValue(value);
    return clone;
  }

  @Override
  protected Double parseFromString(@Nullable String string) {
    if (string == null) {
      return null;
    }

    try {
      return numberFormat.parse(string).doubleValue();
    } catch (NumberFormatException | ParseException e) {
      return null;
    }
  }
}
