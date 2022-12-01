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

package io.github.mzmine.parameters.parametertypes.ranges;

import java.text.NumberFormat;
import java.util.Collection;
import com.google.common.collect.Range;

public class MobilityRangeParameter extends DoubleRangeParameter {

  public MobilityRangeParameter() {
    super("Mobility", "Mobility depends on acquisition technique, e.g. drift tube in ms", null, true,
        null);
  }

  public MobilityRangeParameter(boolean valueRequired) {
    super("Mobility", "Mobility depends on acquisition technique, e.g. drift tube in ms", null,
        valueRequired, null);
  }

  public MobilityRangeParameter(String name, String description, boolean valueRequired,
      Range<Double> defaultValue) {
    super(name, description, null, valueRequired, defaultValue);
  }

  public MobilityRangeParameter(String name, String description, NumberFormat format,
      boolean valueRequired, Range<Double> defaultValue) {
    super(name, description, format, valueRequired, defaultValue);
  }

  @Override
  public boolean checkValue(Collection<String> errorMessages) {
    if (valueRequired && (this.getValue() == null)) {
      errorMessages.add(this.getName() + " is not set properly");
      return false;
    }
    if ((this.getValue() != null) && (this.getValue().lowerEndpoint() < 0.0)) {
      errorMessages.add("Mobility lower end point must not be negative");
      return false;
    }
    if ((this.getValue() != null)
        && (this.getValue().upperEndpoint() <= this.getValue().lowerEndpoint())) {
      errorMessages.add("Mobility lower end point must be less than upper end point");
      return false;
    }

    return true;
  }

  @Override
  public MobilityRangeComponent createEditingComponent() {
    return new MobilityRangeComponent();
  }

  @Override
  public MobilityRangeParameter cloneParameter() {
    MobilityRangeParameter copy =
        new MobilityRangeParameter(getName(), getDescription(), isValueRequired(), getValue());
    return copy;
  }

}
