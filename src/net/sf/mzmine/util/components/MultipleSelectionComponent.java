/*
 * Copyright 2006-2010 The MZmine 2 Development Team
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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

/**
 * Component with multiple checkboxes in a list
 */
public class MultipleSelectionComponent extends JPanel {

	private ExtendedCheckBox<Object> checkBoxes[];

	@SuppressWarnings("unchecked")
	public MultipleSelectionComponent(Object multipleValues[]) {

		super(new BorderLayout());
		
		assert multipleValues != null;

		JPanel checkBoxesPanel = new JPanel();
		checkBoxesPanel.setBackground(Color.white);
		checkBoxesPanel.setLayout(new BoxLayout(checkBoxesPanel,
				BoxLayout.Y_AXIS));

		int vertSize = 0, numCheckBoxes = 0, horSize = 0, widthSize = 0;

		checkBoxes = new ExtendedCheckBox[multipleValues.length];

		for (int i = 0; i < multipleValues.length; i++) {

			checkBoxes[i] = new ExtendedCheckBox<Object>(multipleValues[i],
					false);
			checkBoxes[i].setAlignmentX(Component.LEFT_ALIGNMENT);
			checkBoxesPanel.add(checkBoxes[i]);

			if (numCheckBoxes < 7)
				vertSize += (int) checkBoxes[i].getPreferredSize().getHeight() + 2;

			widthSize = (int) checkBoxes[i].getPreferredSize().getWidth() + 20;
			if (horSize < widthSize)
				horSize = widthSize;

			numCheckBoxes++;
		}

		if (numCheckBoxes < 3)
			vertSize += 30;

		JScrollPane scrollPane = new JScrollPane(checkBoxesPanel,
				ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		
		add(scrollPane, BorderLayout.CENTER);

		setPreferredSize(new Dimension(horSize, vertSize));
	}

	public void addActionListener(ActionListener listener) {
		for (ExtendedCheckBox ecb : checkBoxes)
			ecb.addActionListener(listener);
	}

	public void removeActionListener(ActionListener listener) {
		for (ExtendedCheckBox ecb : checkBoxes)
			ecb.removeActionListener(listener);
	}

	public Object[] getSelectedValues() {
		ArrayList<Object> selectedObjects = new ArrayList<Object>();
		for (ExtendedCheckBox<Object> ecb : checkBoxes) {
			if (ecb.isSelected()) {
				selectedObjects.add(ecb.getObject());
			}
		}
		return selectedObjects.toArray();
	}

	public void setSelectedValues(Object[] values) {
		for (ExtendedCheckBox<Object> ecb : checkBoxes) {
			boolean isSelected = false;
			for (Object v : values) {
				if (v == ecb.getObject())
					isSelected = true;
			}
			ecb.setSelected(isSelected);
		}
	}

}
