/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

import io.github.mzmine.parameters.ValuePropertyComponent;
import java.text.NumberFormat;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.util.converter.DoubleStringConverter;
import javafx.util.converter.NumberStringConverter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DoubleComponent extends FlowPane implements ValuePropertyComponent<Double> {

  private final Double minimum;
  private final Double maximum;
  private final TextField textField;
  private final ObjectProperty<Double> value = new SimpleObjectProperty<>();

  public DoubleComponent(int inputsize, @Nullable Double minimum, @Nullable Double maximum,
      @NotNull NumberFormat format, @Nullable Double defvalue) {
    this.minimum = minimum;
    this.maximum = maximum;

    textField = new TextField();
    textField.setTextFormatter(new TextFormatter<>(new NumberStringConverter(format)));
    textField.setPrefWidth(inputsize);
    textField.textProperty().bindBidirectional(value, new DoubleStringConverter());

    getChildren().add(textField);
    if (defvalue != null) {
      // could also just set the value.set but by setting the text the number will be rounded
      // maybe more reproducible
      textField.setText(format.format(defvalue));
    }
  }

  public String getText() {
    return textField.getText().trim();
  }

  public void setText(String text) {
    textField.setText(text);
  }

  public void setToolTipText(String toolTip) {
    textField.setTooltip(new Tooltip(toolTip));
  }

  private boolean checkBounds(final double number) {
    return (minimum == null || number >= minimum) && (maximum == null || number <= maximum);
  }

  /**
   * Input verifier used when minimum or maximum bounds are defined.
   */
  /*
   * private class MinMaxVerifier extends InputVerifier {
   *
   * @Override public boolean shouldYieldFocus(final JComponent input) {
   *
   * final boolean yield = super.shouldYieldFocus(input); if (!yield) {
   *
   * // Beep and highlight. Toolkit.getDefaultToolkit().beep(); ((JTextComponent)
   * input).selectAll(); }
   *
   * return yield; }
   *
   * @Override public boolean verify(final JComponent input) {
   *
   * boolean verified = false; try {
   *
   * verified = checkBounds(format.parse(((JTextComponent) input).getText()).doubleValue()); } catch
   * (ParseException e) {
   *
   * // Not a number. } return verified; } }
   */
  public TextField getTextField() {
    return textField;
  }

  @Override
  public Property<Double> valueProperty() {
    return value;
  }

  /*
   * public void addDocumentListener(DelayedDocumentListener dl) {
   * textField.getDocument().addDocumentListener(dl); }
   */
}
