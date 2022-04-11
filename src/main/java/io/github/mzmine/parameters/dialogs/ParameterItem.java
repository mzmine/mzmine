/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
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
