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

import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;

public class ComboFieldComponent<E extends Enum<?>> extends HBox {

  private final TextField inputField;
  private final ComboBox<E> comboBox;

  public ComboFieldComponent(Class<E> options) {
    inputField = new TextField();
    comboBox = new ComboBox<>(FXCollections.observableArrayList(options.getEnumConstants()));
    comboBox.getSelectionModel().select(0);

    super.getChildren().addAll(inputField, comboBox);
    super.setSpacing(5d);
    super.setAlignment(Pos.CENTER_LEFT);
  }

  public ComboFieldComponent(Class<E> options, ComboFieldValue<E> defaultValue) {
    this(options);
    inputField.setText(defaultValue.getFieldText());
    comboBox.setValue(defaultValue.getValueType());
  }

  public ComboFieldValue<E> getValue() {
    return new ComboFieldValue<>(inputField.getText(), comboBox.getValue());
  }

  public void setValue(ComboFieldValue<E> value) {
    inputField.setText(value.getFieldText());
    comboBox.setValue(value.getValueType());
  }

  public void setToolTipText(String toolTip) {
    inputField.setTooltip(new Tooltip(toolTip));
  }

}
