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

import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 */
public class MZToleranceComponent extends JPanel {

	private static final String toleranceTypes[] = { "m/z", "ppm" };

	private JTextField toleranceField;
	private JComboBox toleranceType;

	public MZToleranceComponent() {

		super(new BorderLayout());

		toleranceField = new JTextField();
		toleranceField.setColumns(6);
		add(toleranceField, BorderLayout.CENTER);

		toleranceType = new JComboBox(toleranceTypes);
		add(toleranceType, BorderLayout.EAST);

	}

	public void setValue(MZTolerance value) {
		if (value.isAbsolute())
			toleranceType.setSelectedIndex(0);
		else
			toleranceType.setSelectedIndex(1);
		toleranceField.setText(String.valueOf(value.getTolerance()));

	}

	public MZTolerance getValue() {

		try {
			int index = toleranceType.getSelectedIndex();
			double tol = Double.parseDouble(toleranceField.getText());
			MZTolerance value = new MZTolerance(index <= 0, tol);
			return value;
		} catch (NumberFormatException e) {
			return null;
		}

	}

}
