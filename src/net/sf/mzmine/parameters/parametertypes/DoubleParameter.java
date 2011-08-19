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

import java.awt.Dimension;
import java.awt.Toolkit;
import java.util.Collection;

import javax.swing.InputVerifier;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JTextField;
import javax.swing.text.JTextComponent;

import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.UserParameter;

import org.w3c.dom.Element;

/**
 * Number parameter. Note that we prefer to use JTextField rather than
 * JFormattedTextField, because JFormattedTextField sometimes has odd behavior.
 * For example, value reported by getValue() may be different than value
 * actually typed in the text box, because it has not been committed yet. Also,
 * when formatter is set to 1 decimal digit, it becomes impossible to enter 2
 * decimals etc.
 */
public class DoubleParameter implements UserParameter<Double, JTextField> {

	// Text field width.
	private static final int WIDTH = 200;

	private Double value;
	private final String name;
	private final String description;
	private final Double minimum;
	private final Double maximum;

	public DoubleParameter(final String name, final String description) {
		this(name, description, null, null, null);
	}

	public DoubleParameter(final String name, final String description,
			final Double defaultValue) {
		this(name, description, defaultValue, null, null);
	}

	public DoubleParameter(final String name, final String description,
			final Double defaultValue, final Double min, final Double max) {
		this.name = name;
		this.description = description;
		value = defaultValue;
		minimum = min;
		maximum = max;
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
	public JTextField createEditingComponent() {
		final JTextField textField = new JTextField();
		textField.setPreferredSize(new Dimension(WIDTH, textField
				.getPreferredSize().height));

		// Add an input verifier if any bounds are specified.
		if (minimum != null || maximum != null) {
			textField.setInputVerifier(new MinMaxVerifier());
		}

		return textField;
	}

	@Override
	public void setValueFromComponent(final JTextField component) {
		String textValue = component.getText();
		try {
			value = Double.parseDouble(textValue);
		} catch (NumberFormatException e) {
			value = null;
		}
	}

	@Override
	public void setValue(final Double newValue) {
		value = newValue;
	}

	@Override
	public DoubleParameter clone() {
		return new DoubleParameter(name, description, value, minimum, maximum);
	}

	@Override
	public void setValueToComponent(final JTextField component,
			final Double newValue) {
		component.setText(String.valueOf(newValue));
	}

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
	public String toString() {
		return name;
	}

	@Override
	public boolean checkValue(final Collection<String> errorMessages) {
		if (value == null) {
			errorMessages.add(name + " is not set");
			return false;
		}
		if (!checkBounds(value)) {
			errorMessages.add(name + " lies outside its bounds: (" + minimum
					+ " ... " + maximum + ')');
			return false;
		}
		return true;
	}

	private boolean checkBounds(final double number) {
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
				// Beep, reset value and highlight.
				Toolkit.getDefaultToolkit().beep();
				((JFormattedTextField) input).setValue(value);
				((JTextComponent) input).selectAll();
			}
			return yield;
		}

		@Override
		public boolean verify(final JComponent input) {
			boolean verified = false;
			try {
				JTextField textField = (JTextField) input;
				verified = checkBounds(Double.parseDouble(textField.getText()));
			} catch (final NumberFormatException e) {
				// not a number.
			}
			return verified;
		}
	}
}
