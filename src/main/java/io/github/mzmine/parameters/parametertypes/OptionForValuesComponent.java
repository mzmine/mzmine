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

import io.github.mzmine.util.maths.MathOperator;
import java.text.NumberFormat;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.util.converter.NumberStringConverter;

/**
 * Component will show OPTION OPERATOR VALUE, e.g., EXCLUDE > 1000
 */
public class OptionForValuesComponent extends HBox {

  private final Logger logger = Logger.getLogger(this.getClass().getName());

  private final ComboBox<ValueOption> comboOption;
  private final TextField txtValue;

  // combo is only used for multiple operator choices - otherwise text
  private final ComboBox<MathOperator> comboOperator;
  private final Label txtOperator;
  private final MathOperator fixedOperator;

  private NumberFormat format;

  public OptionForValuesComponent(ValueOption[] options, MathOperator[] operator,
      NumberFormat format) {
    super(3d);
    setAlignment(Pos.CENTER_LEFT);

    comboOption = new ComboBox<>(FXCollections.observableArrayList(options));
    comboOption.getSelectionModel().select(0);

    txtValue = new TextField();
    txtValue.setPrefColumnCount(8);

    if (operator.length == 1) {
      fixedOperator = operator[0];
      txtOperator = new Label(fixedOperator.toString());
      comboOperator = null;
      getChildren().addAll(comboOption, txtOperator, txtValue);
    } else {
      comboOperator = new ComboBox<>(FXCollections.observableArrayList(operator));
      comboOperator.getSelectionModel().select(0);
      txtOperator = null;
      fixedOperator = null;
      getChildren().addAll(comboOption, comboOperator, txtValue);
    }

    setMinWidth(200.0);
    // setStyle("-fx-border-color: red");

    setNumberFormat(format);
  }

  public void setNumberFormat(NumberFormat format) {
    this.format = format;
    NumberStringConverter formatConverter = new NumberStringConverter(format);
    txtValue.setTextFormatter(new TextFormatter<>(formatConverter));
  }

  public OptionForValues getValue() {
    try {
      MathOperator operator = fixedOperator != null ? fixedOperator : comboOperator.getValue();
      ValueOption option = comboOption.getValue();
      double value = Double.parseDouble(txtValue.getText());
      return new OptionForValues(option, operator, value);
    } catch (Exception ex) {
      logger.log(Level.WARNING, ex.getMessage(), ex);
      return null;
    }
  }

  public void setValue(OptionForValues value) {
    if (value == null) {
      return;
    }

    comboOption.getSelectionModel().select(value.option());
    txtValue.setText(format.format(value.value()));
    if (comboOperator != null) {
      comboOperator.getSelectionModel().select(value.operator());
    }
  }

  public void setToolTipText(String toolTip) {
    comboOption.setTooltip(new Tooltip(toolTip));
    txtValue.setTooltip(new Tooltip(toolTip));
    if (comboOperator != null) {
      comboOperator.setTooltip(new Tooltip(toolTip));
    }
    if (txtOperator != null) {
      txtOperator.setTooltip(new Tooltip(toolTip));
    }
  }

}
