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

package io.github.mzmine.parameters.parametertypes;

import io.github.mzmine.parameters.UserParameter;
import java.text.NumberFormat;
import java.util.Collection;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;

/**
 * Number parameter. Note that we prefer to use JTextField rather than JFormattedTextField, because
 * JFormattedTextField sometimes has odd behavior. For example, value reported by getValue() may be
 * different than value actually typed in the text box, because it has not been committed yet. Also,
 * when formatter is set to 1 decimal digit, it becomes impossible to enter 2 decimals etc.
 */
public class DoubleParameter implements UserParameter<Double, DoubleComponent> {

  // Text field width.
  private static final int WIDTH = 100;

  private final NumberFormat format;

  private Double value;
  private final String name;
  private final String description;
  private final Double minimum;
  private final Double maximum;

  public DoubleParameter(final String aName, final String aDescription) {

    this(aName, aDescription, NumberFormat.getNumberInstance(), null, null, null);
  }

  public DoubleParameter(final String aName, final String aDescription,
      final NumberFormat numberFormat) {

    this(aName, aDescription, numberFormat, null, null, null);
  }

  public DoubleParameter(final String aName, final String aDescription,
      final NumberFormat numberFormat, final Double defaultValue) {

    this(aName, aDescription, numberFormat, defaultValue, null, null);
  }

  public DoubleParameter(final String aName, final String aDescription,
      final NumberFormat numberFormat, final Double defaultValue, final Double min,
      final Double max) {
    name = aName;
    description = aDescription;
    format = numberFormat;
    value = defaultValue;
    minimum = min;
    maximum = max;
  }

  @Override
  public String getDescription() {

    return description;
  }

  @Override
  public DoubleComponent createEditingComponent() {

    DoubleComponent doubleComponent = new DoubleComponent(WIDTH, minimum, maximum, format, value);
    // doubleComponent.setBorder(BorderFactory.createCompoundBorder(doubleComponent.getBorder(),
    // BorderFactory.createEmptyBorder(0, 3, 0, 0)));
    return doubleComponent;
  }

  @Override
  public void setValueFromComponent(final DoubleComponent component) {
    try {
      value = format.parse(component.getText()).doubleValue();
    } catch (Exception e) {
      value = null;
    }
  }

  @Override
  public void setValue(final Double newValue) {
    value = newValue;
  }

  @Override
  public DoubleParameter cloneParameter() {
    return new DoubleParameter(name, description, format, value, minimum, maximum);
  }

  @Override
  public void setValueToComponent(final DoubleComponent component, final @Nullable Double newValue) {
    component.setText(newValue!=null? format.format(newValue) : "");
  }

  @Override
  public Double getValue() {
    return value;
  }

  @Override
  public void loadValueFromXML(final Element xmlElement) {

    final String numString = xmlElement.getTextContent();
      if (numString.length() > 0) {

        value = Double.parseDouble(numString);
      }
  }

  @Override
  public void saveValueToXML(final Element xmlElement) {

    if (value != null) {

      xmlElement.setTextContent(value.toString());
    }
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String toString() {
    return name;
  }

  @Override
  public boolean checkValue(final Collection<String> errorMessages) {

    final boolean check;
    if (value == null) {

      errorMessages.add(name + " is not set properly");
      check = false;

    } else if (!checkBounds(value)) {

      errorMessages.add(name + " lies outside its bounds: (" + minimum + " ... " + maximum + ')');
      check = false;

    } else {

      check = true;
    }

    return check;
  }

  private boolean checkBounds(final double number) {

    return (minimum == null || number >= minimum) && (maximum == null || number <= maximum);
  }

}
