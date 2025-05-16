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

import io.github.mzmine.javafx.components.factories.FxTextFields;
import io.github.mzmine.javafx.components.formatters.FormatDoubleStringConverter;
import io.github.mzmine.parameters.ValuePropertyComponent;
import java.text.NumberFormat;
import java.util.logging.Logger;
import javafx.beans.property.Property;
import javafx.geometry.Pos;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DoubleComponent extends FlowPane implements ValuePropertyComponent<Double> {

  private static final Logger logger = Logger.getLogger(DoubleComponent.class.getName());
  private final Double minimum;
  private final Double maximum;
  private final TextField textField;
  private final TextFormatter<Double> textFormatter;

  public DoubleComponent(int inputsize, @Nullable Double minimum, @Nullable Double maximum,
      @NotNull NumberFormat format, @Nullable Double defvalue) {
    this.minimum = minimum;
    this.maximum = maximum;

    textField = new TextField();
    textFormatter = new TextFormatter<>(new FormatDoubleStringConverter(format));
    FxTextFields.attachDelayedTextFormatter(textField, textFormatter);

    textField.setPrefWidth(inputsize);
    textFormatter.setValue(defvalue);

    getChildren().add(textField);
    setAlignment(Pos.CENTER_LEFT);
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
    return textFormatter.valueProperty();
  }

  /*
   * public void addDocumentListener(DelayedDocumentListener dl) {
   * textField.getDocument().addDocumentListener(dl); }
   */
}
