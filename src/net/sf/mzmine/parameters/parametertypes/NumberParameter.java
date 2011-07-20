/*
 * Copyright 2006-2011 The MZmine 2 Development Team
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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.parameters.parametertypes;

import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.UserParameter;
import org.w3c.dom.Element;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Collection;

/**
 * Simple Parameter implementation
 */
public class NumberParameter implements UserParameter<Number, JFormattedTextField> {

    // Text field width.
    private static final int WIDTH = 200;

    private Number value;
    private final String name;
    private final String description;
    private final Double minimum;
    private final Double maximum;
    private final NumberFormat format;

    public NumberParameter(final String name, final String description) {
        this(name, description, NumberFormat.getNumberInstance(), null, null, null);
    }

    public NumberParameter(final String name, final String description, final NumberFormat format) {
        this(name, description, format, null, null, null);
    }

    public NumberParameter(final String name,
                           final String description,
                           final NumberFormat format,
                           final Number defaultValue) {
        this(name, description, format, defaultValue, null, null);
    }

    public NumberParameter(final String name,
                           final String description,
                           final NumberFormat format,
                           final Number defaultValue,
                           final Number min,
                           final Number max) {
        this.name = name;
        this.description = description;
        this.format = format;
        value = defaultValue;
        minimum = min == null ? null : min.doubleValue();
        maximum = max == null ? null : max.doubleValue();
    }

    /**
     * @see Parameter#getName()
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * @see UserParameter#getDescription()
     */
    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public JFormattedTextField createEditingComponent() {
        final JFormattedTextField textField = new JFormattedTextField(format);
        textField.setPreferredSize(new Dimension(WIDTH, textField.getPreferredSize().height));

        // Add an input verifier if any bounds are specified.
        if (minimum != null || maximum != null) {
            textField.setInputVerifier(new MinMaxVerifier());
        }

        return textField;
    }

    @Override
    public void setValueFromComponent(final JFormattedTextField component) {
        value = (Number) component.getValue();
    }

    @Override
    public void setValue(final Number newValue) {
        value = newValue;
    }

    @Override
    public NumberParameter clone() {
        return new NumberParameter(name, description, format, value, minimum, maximum);
    }

    @Override
    public void setValueToComponent(final JFormattedTextField component,
                                    final Number newValue) {
        component.setValue(newValue);
    }

    @Override
    public Number getValue() {
        return value;
    }

    public Double getDouble() {
        return value == null ? null : value.doubleValue();
    }

    public Integer getInt() {
        return value == null ? null : value.intValue();
    }

    @Override
    public void loadValueFromXML(final Element xmlElement) {
        final String numString = xmlElement.getTextContent();
        if (numString.length() > 0) {
            value = Double.parseDouble(numString);
        }
    }

    @Override
    public void saveValueToXML(final Element xmlElement) {
        if (value != null) {
            xmlElement.setTextContent(value.toString());
        }
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean checkValue(final Collection<String> errorMessages) {
        boolean check = true;
        if (value == null) {
            errorMessages.add(name + " is not set");
            check = false;
        }
        if (!checkBounds(value)) {
            errorMessages.add(name + " lies outside its bound: (" + minimum + " ... " + maximum + ')');
            check = false;
        }
        return check;
    }

    private boolean checkBounds(final Number parse) {
        final double number = parse.doubleValue();
        return (minimum == null || number >= minimum) &&
               (maximum == null || number <= maximum);
    }

    /**
     * Input verifier used when minimum or maximum bounds are defined.
     */
    private class MinMaxVerifier extends InputVerifier {

        @Override public boolean shouldYieldFocus(final JComponent input) {
            final boolean yield = super.shouldYieldFocus(input);
            if (!yield) {
                // Beep, reset value and highlight.
                Toolkit.getDefaultToolkit().beep();
                ((JFormattedTextField) input).setValue(value);
                ((JTextComponent) input).selectAll();
            }
            return yield;
        }

        @Override public boolean verify(final JComponent input) {
            boolean verified = false;
            try {
                verified = checkBounds(format.parse(((JTextComponent) input).getText()));
            }
            catch (final ParseException e) {
                // not a number.
            }
            return verified;
        }
    }
}
