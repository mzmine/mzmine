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

import io.github.mzmine.gui.preferences.MZminePreferences;
import io.github.mzmine.parameters.UserParameter;
import javafx.scene.Node;
import org.controlsfx.control.PropertySheet;
import org.controlsfx.property.editor.PropertyEditor;

/**
 * Wraps a parameter's component to be used in a {@link PropertySheet} (see ControlsFX). Example
 * usage: {@link GroupedParameterSetupDialog} and {@link MZminePreferences#showSetupDialog(boolean)}
 *
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public class ParameterEditorWrapper implements PropertyEditor<Object> {

  private final UserParameter parameter;
  private final Node editor;

  public ParameterEditorWrapper(UserParameter parameter, Node editor) {
    this.parameter = parameter;
    this.editor = editor;
  }

  @Override
  public Node getEditor() {
    return editor;
  }

  @Override
  public Object getValue() {
    // clone the parameter as we do not want to change the value of the original parameter.
    // its only changed once the user hits OK-button
    final UserParameter tmp = parameter.cloneParameter();
    try {
      tmp.setValueFromComponent(editor);
      return tmp.getValue();
    } catch (Exception ex) {
      return null;
    }
  }

  @Override
  public void setValue(Object value) {
    parameter.setValueToComponent(editor, value);
  }

}
