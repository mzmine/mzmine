/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

import javafx.scene.control.TextField;
import org.jetbrains.annotations.Nullable;

public class StringParameter extends StringValueParameter<TextField> {

  public StringParameter(String name, String description) {
    this(name, description, "");
  }

  public StringParameter(String name, String description, boolean isSensitive) {
    this(name, description, "", true, isSensitive);
  }

  public StringParameter(String name, String description, int inputsize) {
    this(name, description, "", true, false, inputsize);
  }

  public StringParameter(String name, String description, @Nullable String defaultValue) {
    this(name, description, defaultValue, true, false);
  }

  public StringParameter(String name, String description, @Nullable String defaultValue,
      boolean valueRequired) {
    this(name, description, defaultValue, valueRequired, false);
  }

  public StringParameter(String name, String description, @Nullable String defaultValue,
      boolean valueRequired, boolean isSensitive) {
    this(name, description, defaultValue, valueRequired, isSensitive, 20);
  }

  public StringParameter(String name, String description, @Nullable String defaultValue,
      boolean valueRequired, boolean isSensitive, int inputsize) {
    super(name, description, defaultValue, valueRequired, isSensitive, inputsize);
  }

  @Override
  public TextField createEditingComponent() {
    TextField stringComponent = new TextField();
    stringComponent.setPrefColumnCount(inputsize);
    return stringComponent;
  }

  @Override
  public void setValueFromComponent(TextField component) {
    value = component.getText();
  }

  @Override
  public void setValueToComponent(TextField component, @Nullable String newValue) {
    component.setText(requireNonNullElse(newValue, ""));
  }

  @Override
  public StringParameter cloneParameter() {
    StringParameter copy = new StringParameter(name, description, getValue(), valueRequired,
        isSensitive());
    copy.setValue(this.getValue());

    return copy;
  }

}
