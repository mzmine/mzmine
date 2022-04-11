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
