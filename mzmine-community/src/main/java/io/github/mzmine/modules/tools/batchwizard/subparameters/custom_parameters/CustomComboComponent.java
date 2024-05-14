/*
 * Copyright (c) 2004-2023 The MZmine Development Team
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

package io.github.mzmine.modules.tools.batchwizard.subparameters.custom_parameters;

import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;

/**
 * Combo is on the top and center is free for additional controls
 *
 * @param <E>
 */
public abstract class CustomComboComponent<E extends Enum<?>, V extends CustomComboValue<E>> extends
    BorderPane {

  protected final ComboBox<E> comboBox;

  protected V value;

  public CustomComboComponent(E[] options) {
    comboBox = new ComboBox<>(FXCollections.observableArrayList(options));
    comboBox.getSelectionModel().select(0);

    setTop(comboBox);
  }

  public CustomComboComponent(E[] options, CustomComboValue<E> defaultValue) {
    this(options);
    comboBox.setValue(defaultValue.getValueType());
  }

  public abstract V getValue();

  public void setValue(V value) {
    if (value == null) {
      comboBox.setValue(null);
      return;
    }
    comboBox.setValue(value.getValueType());
  }

  public void setToolTipText(String toolTip) {
    comboBox.setTooltip(new Tooltip(toolTip));
  }

}
