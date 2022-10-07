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

import java.text.NumberFormat;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.util.converter.NumberStringConverter;

public class DoubleComponent extends FlowPane {

  private final Double minimum;
  private final Double maximum;
  private final TextField textField;

  public DoubleComponent(int inputsize, Double minimum, Double maximum, NumberFormat format, Double defvalue) {
    this.minimum = minimum;
    this.maximum = maximum;

    textField = new TextField();
    textField.setTextFormatter(new TextFormatter<>(new NumberStringConverter(format)));
    textField.setPrefWidth(inputsize);
    textField.setText(String.valueOf(defvalue)); // why not format.format(defValue)?

    // Add an input verifier if any bounds are specified.
    if (minimum != null || maximum != null) {
      // textField.setInputVerifier(new MinMaxVerifier());
    }

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

  /*
   * public void addDocumentListener(DelayedDocumentListener dl) {
   * textField.getDocument().addDocumentListener(dl); }
   */
}
