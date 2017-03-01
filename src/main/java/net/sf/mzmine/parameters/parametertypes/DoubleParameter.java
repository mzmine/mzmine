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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.parameters.parametertypes;

import java.text.NumberFormat;
import java.util.Collection;

import javax.swing.BorderFactory;

import org.w3c.dom.Element;

import net.sf.mzmine.parameters.UserParameter;

/**
 * Number parameter. Note that we prefer to use JTextField rather than
 * JFormattedTextField, because JFormattedTextField sometimes has odd behavior.
 * For example, value reported by getValue() may be different than value
 * actually typed in the text box, because it has not been committed yet. Also,
 * when formatter is set to 1 decimal digit, it becomes impossible to enter 2
 * decimals etc.
 */
public class DoubleParameter implements UserParameter<Double, DoubleComponent> {

    // Text field width.
    private static final int WIDTH = 100;

    private final NumberFormat format;

    private Double value;
    private final String name;
    private final String description;
    private final Double minimum;
    private final Double maximum;

    public DoubleParameter(final String aName, final String aDescription) {

        this(aName, aDescription, NumberFormat.getNumberInstance(), null, null,
                null);
    }

    public DoubleParameter(final String aName, final String aDescription,
            final NumberFormat numberFormat) {

        this(aName, aDescription, numberFormat, null, null, null);
    }

    public DoubleParameter(final String aName, final String aDescription,
            final NumberFormat numberFormat, final Double defaultValue) {

        this(aName, aDescription, numberFormat, defaultValue, null, null);
    }

    public DoubleParameter(final String aName, final String aDescription,
            final NumberFormat numberFormat, final Double defaultValue,
            final Double min, final Double max) {
        name = aName;
        description = aDescription;
        format = numberFormat;
        value = defaultValue;
        minimum = min;
        maximum = max;
    }

    @Override
    public String getDescription() {

        return description;
    }

    @Override
    public DoubleComponent createEditingComponent() {

        DoubleComponent doubleComponent = new DoubleComponent(WIDTH, minimum,
                maximum, format);
        doubleComponent.setBorder(
                BorderFactory.createCompoundBorder(doubleComponent.getBorder(),
                        BorderFactory.createEmptyBorder(0, 3, 0, 0)));
        return doubleComponent;
    }

    @Override
    public void setValueFromComponent(final DoubleComponent component) {
        try {

            value = format.parse(component.getText()).doubleValue();
        } catch (Exception e) {

            value = null;
        }
    }

    @Override
    public void setValue(final Double newValue) {
        value = newValue;
    }

    @Override
    public DoubleParameter cloneParameter() {
        return new DoubleParameter(name, description, format, value, minimum,
                maximum);
    }

    @Override
    public void setValueToComponent(final DoubleComponent component,
            final Double newValue) {
        component.setText(format.format(newValue));
    }

    @Override
    public Double getValue() {
        return value;
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
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean checkValue(final Collection<String> errorMessages) {

        final boolean check;
        if (value == null) {

            errorMessages.add(name + " is not set properly");
            check = false;

        } else if (!checkBounds(value)) {

            errorMessages.add(name + " lies outside its bounds: (" + minimum
                    + " ... " + maximum + ')');
            check = false;

        } else {

            check = true;
        }

        return check;
    }

    private boolean checkBounds(final double number) {

        return (minimum == null || number >= minimum)
                && (maximum == null || number <= maximum);
    }

}
