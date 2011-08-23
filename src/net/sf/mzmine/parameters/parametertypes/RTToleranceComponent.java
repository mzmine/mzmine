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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.parameters.parametertypes;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;

import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JPanel;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.NumberFormatter;

import net.sf.mzmine.main.MZmineCore;

/**
 */
public class RTToleranceComponent extends JPanel implements ActionListener {

	private static final String toleranceTypes[] = { "absolute", "relative (%)" };

	private JFormattedTextField toleranceField;
	private JComboBox toleranceType;

	public RTToleranceComponent() {

		super(new BorderLayout());

		toleranceField = new JFormattedTextField(MZmineCore.getRTFormat());
		toleranceField.setColumns(6);
		add(toleranceField, BorderLayout.CENTER);

		toleranceType = new JComboBox(toleranceTypes);
		toleranceType.addActionListener(this);
		add(toleranceType, BorderLayout.EAST);

	}

	public void setValue(RTTolerance value) {
		if (value.isAbsolute()) {
			toleranceType.setSelectedIndex(0);
			DefaultFormatterFactory fact = new DefaultFormatterFactory(
					new NumberFormatter(MZmineCore.getRTFormat()));
			toleranceField.setFormatterFactory(fact);
			toleranceField.setValue(value.getTolerance());
		} else {
			toleranceType.setSelectedIndex(1);
			DefaultFormatterFactory fact = new DefaultFormatterFactory(
					new NumberFormatter(NumberFormat.getNumberInstance()));
			toleranceField.setFormatterFactory(fact);
			toleranceField.setValue(value.getTolerance() * 100);
		}
	}

	public RTTolerance getValue() {

		int index = toleranceType.getSelectedIndex();

		Number tol = (Number) toleranceField.getValue();
		if (tol == null)
			return null;

		double toleranceValue = tol.doubleValue();
		if (index == 1)
			toleranceValue /= 100;

		RTTolerance value = new RTTolerance(index <= 0, toleranceValue);
		return value;
	}

	@Override
	public void actionPerformed(ActionEvent event) {

		Object src = event.getSource();
		if (src == toleranceType) {
			if (toleranceType.getSelectedIndex() <= 0) {
				DefaultFormatterFactory fact = new DefaultFormatterFactory(
						new NumberFormatter(MZmineCore.getRTFormat()));
				toleranceField.setFormatterFactory(fact);
			} else {
				DefaultFormatterFactory fact = new DefaultFormatterFactory(
						new NumberFormatter(NumberFormat.getNumberInstance()));
				toleranceField.setFormatterFactory(fact);

			}
		}
	}
	
	@Override
	public void setToolTipText(String toolTip) {
		toleranceField.setToolTipText(toolTip);
	}

}
