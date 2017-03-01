/*
 * Copyright 2006-2015 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */
package net.sf.mzmine.parameters.parametertypes;

import java.awt.Dimension;
import java.awt.Toolkit;

import javax.swing.InputVerifier;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.text.JTextComponent;

public class IntegerComponent extends JPanel {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private final Integer minimum, maximum;
    private final JTextField textField;

    public IntegerComponent(int inputsize, Integer minimum, Integer maximum) {
        this.minimum = minimum;
        this.maximum = maximum;

        textField = new JTextField();
        textField.setPreferredSize(
                new Dimension(inputsize, textField.getPreferredSize().height));
        // Add an input verifier if any bounds are specified.
        if (minimum != null || maximum != null) {
            textField.setInputVerifier(new MinMaxVerifier());
        }

        add(textField);
    }

    public void setText(String text) {
        textField.setText(text);
    }

    public String getText() {
        return textField.getText().trim();
    }

    @Override
    public void setToolTipText(String toolTip) {
        textField.setToolTipText(toolTip);
    }

    private boolean checkBounds(final int number) {
        return (minimum == null || number >= minimum)
                && (maximum == null || number <= maximum);
    }

    /**
     * Input verifier used when minimum or maximum bounds are defined.
     */
    private class MinMaxVerifier extends InputVerifier {

        @Override
        public boolean shouldYieldFocus(final JComponent input) {

            final boolean yield = super.shouldYieldFocus(input);
            if (!yield) {

                // Beep and highlight.
                Toolkit.getDefaultToolkit().beep();
                ((JTextComponent) input).selectAll();
            }

            return yield;
        }

        @Override
        public boolean verify(final JComponent input) {

            boolean verified = false;
            try {

                verified = checkBounds(
                        Integer.parseInt(((JTextComponent) input).getText()));
            } catch (final NumberFormatException e) {

                // not a number.
            }

            return verified;
        }
    }
}
