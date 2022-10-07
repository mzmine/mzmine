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

import java.util.Collection;
import org.w3c.dom.Element;
import io.github.mzmine.parameters.UserParameter;

/**
 * Integer parameter. Note that we prefer to use JTextField rather than JFormattedTextField, because
 * JFormattedTextField sometimes has odd behavior. For example, value reported by getValue() may be
 * different than value actually typed in the text box, because it has not been committed yet. Also,
 * when formatter is set to 1 decimal digit, it becomes impossible to enter 2 decimals etc.
 */
public class IntegerParameter implements UserParameter<Integer, IntegerComponent> {

  // Text field width.
  private static final int WIDTH = 100;

  private final String name, description;
  private final Integer minimum, maximum;
  private Integer value;
  private final boolean valueRequired;
  private final boolean sensitive;

  public IntegerParameter(final String aName, final String aDescription) {
    this(aName, aDescription, null, true, null, null);
  }

  public IntegerParameter(final String aName, final String aDescription, boolean isSensitive) {
    this(aName, aDescription, null, true, null, null, isSensitive);
  }

  public IntegerParameter(final String aName, final String aDescription,
      final Integer defaultValue) {
    this(aName, aDescription, defaultValue, true, null, null);
  }

  public IntegerParameter(final String aName, final String aDescription, final Integer defaultValue,
      final boolean valueRequired) {
    this(aName, aDescription, defaultValue, valueRequired, null, null);
  }

  public IntegerParameter(final String aName, final String aDescription, final Integer defaultValue,
      final Integer min, final Integer max) {
    this(aName, aDescription, defaultValue, true, min, max);
  }

  public IntegerParameter(final String aName, final String aDescription, final Integer defaultValue,
      final boolean valueRequired, final Integer min, final Integer max) {
    this(aName, aDescription, defaultValue, valueRequired, min, max, false);
  }

  public IntegerParameter(final String aName, final String aDescription, final Integer defaultValue,
      final boolean valueRequired, final Integer min, final Integer max, boolean isSensitive) {
    this.name = aName;
    this.description = aDescription;
    this.value = defaultValue;
    this.valueRequired = valueRequired;
    this.minimum = min;
    this.maximum = max;
    this.sensitive = isSensitive;
  }

  @Override
  public String getName() {

    return name;
  }

  @Override
  public String getDescription() {

    return description;
  }

  @Override
  public IntegerComponent createEditingComponent() {
    IntegerComponent integerComponent = new IntegerComponent(WIDTH, minimum, maximum);
    // integerComponent.setBorder(BorderFactory.createCompoundBorder(integerComponent.getBorder(),
    // BorderFactory.createEmptyBorder(0, 4, 0, 0)));
    return integerComponent;
  }

  @Override
  public void setValueFromComponent(final IntegerComponent component) {

    final String textValue = component.getText();
    try {

      value = Integer.parseInt(textValue);
    } catch (NumberFormatException e) {

      value = null;
    }
  }

  @Override
  public void setValue(final Integer newValue) {

    value = newValue;
  }

  @Override
  public IntegerParameter cloneParameter() {

    return new IntegerParameter(name, description, value, valueRequired, minimum, maximum);
  }

  @Override
  public void setValueToComponent(final IntegerComponent component, final Integer newValue) {

    component.setText(String.valueOf(newValue));
  }

  @Override
  public Integer getValue() {

    return value;
  }

  @Override
  public void loadValueFromXML(final Element xmlElement) {

    final String numString = xmlElement.getTextContent();
    if (numString.length() > 0) {

      value = Integer.parseInt(numString);
    }
  }

  @Override
  public void saveValueToXML(final Element xmlElement) {

    if (value != null) {

      xmlElement.setTextContent(value.toString());
    }
  }

  @Override
  public boolean checkValue(final Collection<String> errorMessages) {

    if (valueRequired && (value == null)) {
      errorMessages.add(name + " is not set properly");
      return false;
    }

    if ((value != null) && (!checkBounds(value))) {
      errorMessages.add(name + " lies outside its bounds: (" + minimum + " ... " + maximum + ')');
      return false;
    }

    return true;
  }

  private boolean checkBounds(final int number) {
    return (minimum == null || number >= minimum) && (maximum == null || number <= maximum);
  }

  @Override
  public boolean isSensitive() {
    return sensitive;
  }
}
