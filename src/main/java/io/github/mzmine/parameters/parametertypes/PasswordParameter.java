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
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.util.exceptions.DecryptionException;
import io.github.mzmine.util.exceptions.EncryptionException;
import javafx.scene.control.PasswordField;

/**
 * Password parameter
 *
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public class PasswordParameter implements UserParameter<String, PasswordField> {

  private String name, description, value;
  private int inputsize = 20;
  private boolean valueRequired = true;

  public PasswordParameter(String name, String description) {
    this(name, description, null);
  }

  public PasswordParameter(String name, String description, int inputsize) {
    this.name = name;
    this.description = description;
    this.inputsize = inputsize;
  }

  public PasswordParameter(String name, String description, String defaultValue) {
    this.name = name;
    this.description = description;
    this.value = defaultValue;
  }

  public PasswordParameter(String name, String description, boolean valueRequired) {
    this.name = name;
    this.description = description;
    this.valueRequired = valueRequired;
  }

  public PasswordParameter(String name, String description, String defaultValue,
      boolean valueRequired) {
    this.name = name;
    this.description = description;
    this.value = defaultValue;
    this.valueRequired = valueRequired;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getValue() {
    return value;
  }

  @Override
  public void setValue(String newValue) {
    this.value = newValue;
  }

  @Override
  public boolean checkValue(Collection<String> errorMessages) {
    if (!valueRequired)
      return true;
    if ((value == null) || (value.trim().isEmpty())) {
      errorMessages.add(name + " is not set properly");
      return false;
    }
    return true;
  }

  @Override
  public void loadValueFromXML(Element xmlElement) {
    try {
      final String nuValue = xmlElement.getTextContent();
      if (nuValue == null || nuValue.trim().isEmpty())
        return;

      value = MZmineCore.getConfiguration().getEncrypter().decrypt(nuValue);
    } catch (DecryptionException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void saveValueToXML(Element xmlElement) {
    if (value == null || value.isEmpty())
      return;
    try {
      xmlElement.setTextContent(MZmineCore.getConfiguration().getEncrypter().encrypt(value));
    } catch (EncryptionException e) {
      e.printStackTrace();
    }
  }

  @Override
  public boolean isSensitive() {
    return true;
  }

  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public PasswordField createEditingComponent() {
    PasswordField passwordComponent = new PasswordField();
    passwordComponent.setPrefColumnCount(inputsize);
    // passwordComponent.setBorder(BorderFactory.createCompoundBorder(passwordComponent.getBorder(),
    // BorderFactory.createEmptyBorder(0, 4, 0, 0)));
    return passwordComponent;
  }

  @Override
  public void setValueFromComponent(PasswordField component) {
    value = component.getText().toString();
  }

  @Override
  public void setValueToComponent(PasswordField component, String newValue) {
    component.setText(newValue);
  }

  @Override
  public PasswordParameter cloneParameter() {
    PasswordParameter copy = new PasswordParameter(name, description);
    copy.setValue(this.getValue());
    return copy;
  }

}
