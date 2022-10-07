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
