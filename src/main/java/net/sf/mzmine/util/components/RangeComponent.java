/*
 * Copyright 2006-2012 The MZmine 2 Development Team
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

package net.sf.mzmine.util.components;

import java.beans.PropertyChangeListener;
import java.text.NumberFormat;

import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.NumberFormatter;

import net.sf.mzmine.util.Range;

/**
 * Component with two textboxes to specify a range
 */
public class RangeComponent extends GridBagPanel {

	public static final int TEXTFIELD_COLUMNS = 8;

	private JFormattedTextField minTxtField, maxTxtField;

	public RangeComponent(NumberFormat format) {
		minTxtField = new JFormattedTextField(format);
		maxTxtField = new JFormattedTextField(format);
		minTxtField.setColumns(TEXTFIELD_COLUMNS);
		maxTxtField.setColumns(TEXTFIELD_COLUMNS);
		add(minTxtField, 0, 0, 1, 1, 1, 0);
		add(new JLabel(" - "), 1, 0, 1, 1, 0, 0);
		add(maxTxtField, 2, 0, 1, 1, 1, 0);
	}

	/**
	 * @return Returns the current values
	 */
	public Range getRangeValue() {
		double minValue = ((Number) minTxtField.getValue()).doubleValue();
		double maxValue = ((Number) maxTxtField.getValue()).doubleValue();
		return new Range(minValue, maxValue);
	}

	public void setRangeValue(Range value) {
		minTxtField.setValue(value.getMin());
		maxTxtField.setValue(value.getMax());
	}

	public void addPropertyChangeListener(String property,
			PropertyChangeListener listener) {
		minTxtField.addPropertyChangeListener(property, listener);
		maxTxtField.addPropertyChangeListener(property, listener);
	}

	public void removePropertyChangeListener(String property,
			PropertyChangeListener listener) {
		minTxtField.removePropertyChangeListener(property, listener);
		maxTxtField.removePropertyChangeListener(property, listener);
	}

	public void setNumberFormat(NumberFormat format) {
		DefaultFormatterFactory fac = new DefaultFormatterFactory(
				new NumberFormatter(format));
		minTxtField.setFormatterFactory(fac);
		maxTxtField.setFormatterFactory(fac);
	}

}
