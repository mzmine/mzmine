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

import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;

public class IntegerComponent extends FlowPane {


  private final Integer minimum, maximum;
  private final TextField textField;

  public IntegerComponent(int inputsize, Integer minimum, Integer maximum) {
    this.minimum = minimum;
    this.maximum = maximum;

    textField = new TextField();
    textField.setPrefWidth(inputsize);
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

  private boolean checkBounds(final int number) {
    return (minimum == null || number >= minimum) && (maximum == null || number <= maximum);
  }

  /**
   * Sets the number of columns in this TextField.
   */
  public void setColumns(int columns) {
    textField.setPrefColumnCount(columns);
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
   * verified = checkBounds(Integer.parseInt(((JTextComponent) input).getText())); } catch (final
   * NumberFormatException e) {
   * 
   * // not a number. }
   * 
   * return verified; } }
   */

  public TextField getTextField() {
    return textField;
  }

  /**
   * Add a document listener to the underlying textfield (see {@link DelayedDocumentListener}
   * 
   * @param dl
   */
  /*
   * public void addDocumentListener(DelayedDocumentListener dl) {
   * textField.getDocument().addDocumentListener(dl); }
   */
}
