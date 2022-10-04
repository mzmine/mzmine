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
