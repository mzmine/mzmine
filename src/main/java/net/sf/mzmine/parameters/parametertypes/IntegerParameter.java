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

import java.util.Collection;

import javax.swing.BorderFactory;

import org.w3c.dom.Element;

import net.sf.mzmine.parameters.UserParameter;

/**
 * Integer parameter. Note that we prefer to use JTextField rather than
 * JFormattedTextField, because JFormattedTextField sometimes has odd behavior.
 * For example, value reported by getValue() may be different than value
 * actually typed in the text box, because it has not been committed yet. Also,
 * when formatter is set to 1 decimal digit, it becomes impossible to enter 2
 * decimals etc.
 */
public class IntegerParameter implements UserParameter<Integer, IntegerComponent> {

    // Text field width.
    private static final int WIDTH = 100;

    private final String name, description;
    private final Integer minimum, maximum;
    private Integer value;
    private final boolean valueRequired;

    public IntegerParameter(final String aName, final String aDescription) {
        this(aName, aDescription, null, true, null, null);
    }

    public IntegerParameter(final String aName, final String aDescription,
            final Integer defaultValue) {
        this(aName, aDescription, defaultValue, true, null, null);
    }

    public IntegerParameter(final String aName, final String aDescription,
            final Integer defaultValue, final boolean valueRequired) {
        this(aName, aDescription, defaultValue, valueRequired, null, null);
    }

    public IntegerParameter(final String aName, final String aDescription,
            final Integer defaultValue, final Integer min, final Integer max) {
        this(aName, aDescription, defaultValue, true, min, max);
    }

    public IntegerParameter(final String aName, final String aDescription,
            final Integer defaultValue, final boolean valueRequired,
            final Integer min, final Integer max) {
        this.name = aName;
        this.description = aDescription;
        this.value = defaultValue;
        this.valueRequired = valueRequired;
        this.minimum = min;
        this.maximum = max;
    }

    @Override
    public String getName() {

        return name;
    }

    @Override
    public String getDescription() {

        return description;
    }

    @Override
    public IntegerComponent createEditingComponent() {
        IntegerComponent integerComponent = new IntegerComponent(WIDTH, minimum,
                maximum);
        integerComponent.setBorder(
                BorderFactory.createCompoundBorder(integerComponent.getBorder(),
                        BorderFactory.createEmptyBorder(0, 4, 0, 0)));
        return integerComponent;
    }

    @Override
    public void setValueFromComponent(final IntegerComponent component) {

        final String textValue = component.getText();
        try {

            value = Integer.parseInt(textValue);
        } catch (NumberFormatException e) {

            value = null;
        }
    }

    @Override
    public void setValue(final Integer newValue) {

        value = newValue;
    }

    @Override
    public IntegerParameter cloneParameter() {

        return new IntegerParameter(name, description, value, valueRequired,
                minimum, maximum);
    }

    @Override
    public void setValueToComponent(final IntegerComponent component,
            final Integer newValue) {

        component.setText(String.valueOf(newValue));
    }

    @Override
    public Integer getValue() {

        return value;
    }

    @Override
    public void loadValueFromXML(final Element xmlElement) {

        final String numString = xmlElement.getTextContent();
        if (numString.length() > 0) {

            value = Integer.parseInt(numString);
        }
    }

    @Override
    public void saveValueToXML(final Element xmlElement) {

        if (value != null) {

            xmlElement.setTextContent(value.toString());
        }
    }

    @Override
    public boolean checkValue(final Collection<String> errorMessages) {

        if (valueRequired && (value == null)) {
            errorMessages.add(name + " is not set properly");
            return false;
        }

        if ((value != null) && (!checkBounds(value))) {
            errorMessages.add(name + " lies outside its bounds: (" + minimum
                    + " ... " + maximum + ')');
            return false;
        }

        return true;
    }

    private boolean checkBounds(final int number) {
        return (minimum == null || number >= minimum)
                && (maximum == null || number <= maximum);
    }
}
