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

package io.github.mzmine.parameters.dialogs;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.UserParameter;
import java.util.Optional;
import javafx.beans.value.ObservableValue;
import org.controlsfx.control.PropertySheet;
import org.controlsfx.control.PropertySheet.Item;
import org.controlsfx.property.editor.PropertyEditor;

/**
 * Wraps a parameter to be used by {@link PropertySheet} from ControlsFX
 *
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public class ParameterItem implements PropertySheet.Item {

  protected final String category;
  protected final Parameter parameter;

  public ParameterItem(Parameter parameter, String category) {
    this.parameter = parameter;
    this.category = category;
  }

  public ParameterItem(Parameter parameter) {
    this(parameter, null);
  }

  public Parameter getParameter() {
    return parameter;
  }

  @Override
  public Class<?> getType() {
    final Object value = parameter.getValue();
    return value == null ? null : parameter.getValue().getClass();
  }

  @Override
  public String getCategory() {
    return category;
  }

  @Override
  public String getName() {
    return parameter.getName();
  }

  @Override
  public String getDescription() {
    if (parameter instanceof UserParameter up) {
      return up.getDescription();
    } else {
      return null;
    }
  }

  @Override
  public Object getValue() {
    return parameter.getValue();
  }

  @Override
  public void setValue(Object value) {
    parameter.setValue(value);
  }

  @Override
  public Optional<ObservableValue<? extends Object>> getObservableValue() {
    return Optional.empty();
  }

  @Override
  public Optional<Class<? extends PropertyEditor<?>>> getPropertyEditorClass() {
    return Item.super.getPropertyEditorClass();
  }

  @Override
  public boolean isEditable() {
    return true;
  }
}
