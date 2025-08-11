/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

import io.github.mzmine.parameters.ValueChangeDecorator;
import io.github.mzmine.parameters.ValuePropertyComponent;
import javafx.beans.property.Property;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.util.converter.IntegerStringConverter;

public class IntegerComponent extends FlowPane implements ValueChangeDecorator,
    ValuePropertyComponent<Integer> {


  private final Integer minimum, maximum;
  private final TextField textField;
  private final TextFormatter<Integer> textFormatter;

  public IntegerComponent(int inputsize, Integer minimum, Integer maximum) {
    this.minimum = minimum;
    this.maximum = maximum;

    textField = new TextField();
    textField.setPrefWidth(inputsize);
    textFormatter = new TextFormatter<>(new IntegerStringConverter());
    textField.setTextFormatter(textFormatter);

    getChildren().add(textField);
  }

  public void setText(String text) {
    textField.setText(text);
  }

  public String getText() {
    return textField.getText().trim();
  }

  public void setToolTipText(String toolTip) {
    textField.setTooltip(new Tooltip(toolTip));
  }

  private boolean checkBounds(final int number) {
    return (minimum == null || number >= minimum) && (maximum == null || number <= maximum);
  }

  /**
   * Sets the number of columns in this TextField.
   */
  public void setColumns(int columns) {
    textField.setPrefColumnCount(columns);
  }

  public TextField getTextField() {
    return textField;
  }

  @Override
  public void addValueChangedListener(final Runnable onChange) {
    textField.textProperty().addListener((observable, oldValue, newValue) -> onChange.run());
  }

  @Override
  public Property<Integer> valueProperty() {
    return textFormatter.valueProperty();
  }

}
