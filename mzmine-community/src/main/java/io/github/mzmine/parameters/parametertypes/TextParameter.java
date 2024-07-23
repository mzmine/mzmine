/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

import static java.util.Objects.requireNonNullElse;

import io.github.mzmine.parameters.UserParameter;
import java.util.Collection;
import javafx.scene.control.TextArea;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;

public class TextParameter implements UserParameter<String, TextArea> {

  private final String name;
  private final String description;
  private final boolean valueRequired;
  protected String value;

  public TextParameter(String name, String description) {
    this(name, description, null);
  }

  public TextParameter(String name, String description, String defaultValue) {
    this(name, description, defaultValue, true);
  }

  public TextParameter(String name, String description, String defaultValue, boolean valueRequired) {
    this.name = name;
    this.description = description;
    this.value = defaultValue;
    this.valueRequired = valueRequired;
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
  public TextArea createEditingComponent() {
    return new TextArea();
  }

  @Override
  public String getValue() {
    return value;
  }

  @Override
  public void setValue(String value) {
    this.value = value;
  }

  @Override
  public TextParameter cloneParameter() {
    TextParameter copy = new TextParameter(name, description);
    copy.setValue(this.getValue());
    return copy;
  }

  @Override
  public String toString() {
    return name;
  }

  @Override
  public void setValueFromComponent(TextArea component) {
    value = component.getText();
  }

  @Override
  public void setValueToComponent(TextArea component, @Nullable String newValue) {
    component.setText(requireNonNullElse(newValue, ""));
  }

  @Override
  public void loadValueFromXML(Element xmlElement) {
    value = xmlElement.getTextContent();
  }

  @Override
  public void saveValueToXML(Element xmlElement) {
    if (value == null)
      return;
    xmlElement.setTextContent(value);
  }

  @Override
  public boolean checkValue(Collection<String> errorMessages) {
    if (valueRequired && ((value == null) || (value.trim().length() == 0))) {
      errorMessages.add(name + " is not set properly");
      return false;
    }
    return true;
  }
}
