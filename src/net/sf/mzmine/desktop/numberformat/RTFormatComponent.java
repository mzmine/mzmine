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

package net.sf.mzmine.desktop.numberformat;

import java.awt.BorderLayout;

import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 */
public class RTFormatComponent extends JPanel {

	private JComboBox formatTypeCombo;
	private JTextField formatField;

	public RTFormatComponent() {

		super(new BorderLayout());

		formatTypeCombo = new JComboBox(RTFormatterType.values());
		add(formatTypeCombo, BorderLayout.WEST);

		formatField = new JTextField();
		formatField.setColumns(8);
		add(formatField, BorderLayout.CENTER);

	}

	public void setValue(RTFormatter format) {
		formatTypeCombo.setSelectedItem(format.getType());
		formatField.setText(format.getPattern());
	}

	public RTFormatterType getType() {
		if ((formatTypeCombo == null)
				|| (formatTypeCombo.getSelectedItem() == null))
			return null;
		return (RTFormatterType) formatTypeCombo.getSelectedItem();
	}

	public String getPattern() {
		return formatField.getText();
	}

}
