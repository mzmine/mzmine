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

package io.github.mzmine.gui.preferences;

import java.text.DecimalFormat;
import java.util.Collection;

import org.w3c.dom.Element;

import io.github.mzmine.parameters.UserParameter;

/**
 * Simple Parameter implementation
 * 
 * 
 */
public class NumberFormatParameter implements UserParameter<DecimalFormat, NumberFormatEditor> {

  private String name, description;
  private boolean showExponentOption;
  private DecimalFormat value;

  public NumberFormatParameter(String name, String description, boolean showExponentOption,
      DecimalFormat defaultValue) {

    assert defaultValue != null;

    this.name = name;
    this.description = description;
    this.showExponentOption = showExponentOption;
    this.value = defaultValue;
  }

  /**
   * @see io.github.mzmine.data.Parameter#getName()
   */
  @Override
  public String getName() {
    return name;
  }

  /**
   * @see io.github.mzmine.data.Parameter#getDescription()
   */
  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public NumberFormatEditor createEditingComponent() {
    NumberFormatEditor editor = new NumberFormatEditor(showExponentOption);
    return editor;
  }

  public DecimalFormat getValue() {
    return value;
  }

  @Override
  public void setValue(DecimalFormat value) {
    assert value != null;
    this.value = value;
  }

  @Override
  public NumberFormatParameter cloneParameter() {
    NumberFormatParameter copy =
        new NumberFormatParameter(name, description, showExponentOption, value);
    copy.setValue(this.getValue());
    return copy;
  }

  @Override
  public void setValueFromComponent(NumberFormatEditor component) {
    final int decimals = component.getDecimals();
    final boolean showExponent = component.getShowExponent();
    String pattern = "0";

    if (decimals > 0) {
      pattern += ".";
      for (int i = 0; i < decimals; i++)
        pattern += "0";
    }
    if (showExponent) {
      pattern += "E0";
    }
    value.applyPattern(pattern);
  }

  @Override
  public void setValueToComponent(NumberFormatEditor component, DecimalFormat newValue) {
    final int decimals = newValue.getMinimumFractionDigits();
    boolean showExponent = newValue.toPattern().contains("E");
    component.setValue(decimals, showExponent);
  }

  @Override
  public void loadValueFromXML(Element xmlElement) {
    String newPattern = xmlElement.getTextContent();
    value.applyPattern(newPattern);
  }

  @Override
  public void saveValueToXML(Element xmlElement) {
    xmlElement.setTextContent(value.toPattern());
  }

  @Override
  public boolean checkValue(Collection<String> errorMessages) {
    return true;
  }

}
