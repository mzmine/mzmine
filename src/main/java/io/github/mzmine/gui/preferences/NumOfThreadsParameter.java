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

import static java.util.Objects.requireNonNullElse;

import io.github.mzmine.parameters.UserParameter;
import java.util.Collection;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;

/**
 * Simple Parameter implementation
 *
 */
public class NumOfThreadsParameter implements UserParameter<Integer, NumOfThreadsEditor> {

  private String name, description;
  private boolean automatic;
  private Integer value;

  public NumOfThreadsParameter() {
    this.name = "Number of concurrently running tasks";
    this.description = "Maximum number of tasks running simultaneously";
    this.value = Runtime.getRuntime().availableProcessors();
    this.automatic = true;
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
  public NumOfThreadsEditor createEditingComponent() {
    NumOfThreadsEditor editor = new NumOfThreadsEditor();
    editor.setValue(automatic, value);
    return editor;
  }

  @Override
  public Integer getValue() {
    return isAutomatic() ? Runtime.getRuntime().availableProcessors() : value;
  }

  public boolean isAutomatic() {
    return automatic;
  }

  public void setAutomatic(final boolean automatic) {
    this.automatic = automatic;
  }

  @Override
  public void setValue(Integer value) {
    assert value != null;
    this.value = value;
    automatic = false;
  }

  @Override
  public NumOfThreadsParameter cloneParameter() {
    return this;
  }

  @Override
  public void setValueFromComponent(NumOfThreadsEditor component) {
    automatic = component.isAutomatic();
    if (automatic) {
      value = Runtime.getRuntime().availableProcessors();
    } else {
      Number componentValue = component.getNumOfThreads();
      if (componentValue == null)
        value = null;
      else
        value = componentValue.intValue();
    }
  }

  @Override
  public void setValueToComponent(NumOfThreadsEditor component, @Nullable Integer newValue) {
    component.setValue(automatic, requireNonNullElse(newValue, 0));
  }

  @Override
  public void loadValueFromXML(Element xmlElement) {
    String attrValue = xmlElement.getAttribute("isautomatic");
    if (attrValue.length() > 0) {
      this.automatic = Boolean.parseBoolean(attrValue);
    }

    String textContent = xmlElement.getTextContent();
    if (textContent.length() > 0) {
      this.value = Integer.valueOf(textContent);
    }
  }

  @Override
  public void saveValueToXML(Element xmlElement) {
    xmlElement.setAttribute("isautomatic", String.valueOf(automatic));
    xmlElement.setTextContent(value.toString());
  }

  @Override
  public boolean checkValue(Collection<String> errorMessages) {
    if (value == null) {
      errorMessages.add(name + " is not set");
      return false;
    }
    return true;
  }

}
