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
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

import net.sf.mzmine.util.GUIUtils;

/**
 */
public class MultiChoiceComponent extends JPanel implements ActionListener {

	private static final Font smallFont = new Font("SansSerif", Font.PLAIN, 11);

	private JCheckBox checkBoxes[];
	private Object choices[];

	private JButton selectAllButton, selectNoneButton;

	public MultiChoiceComponent(Object choices[]) {

		super(new BorderLayout());

		this.choices = choices;

		JPanel checkBoxesPanel = new JPanel();
		checkBoxesPanel.setBackground(Color.white);
		checkBoxesPanel.setLayout(new BoxLayout(checkBoxesPanel,
				BoxLayout.Y_AXIS));

		int vertSize = 0, numCheckBoxes = 0, horSize = 0, widthSize = 0;

		checkBoxes = new JCheckBox[choices.length];

		for (int i = 0; i < choices.length; i++) {

			checkBoxes[i] = new JCheckBox(choices[i].toString());
			checkBoxes[i].setFont(smallFont);

			checkBoxesPanel.add(checkBoxes[i]);

			if (numCheckBoxes < 7)
				vertSize += (int) checkBoxes[i].getPreferredSize().getHeight() + 2;

			widthSize = (int) checkBoxes[i].getPreferredSize().getWidth();
			if (horSize < widthSize)
				horSize = widthSize;

			numCheckBoxes++;

		}

		if (numCheckBoxes < 3)
			vertSize += 30;

		horSize += 50;

		// If the selection panel is too small, it does not look good, so let's
		// set a minimum width of 150 pix
		if (horSize < 150)
			horSize = 150;

		JScrollPane scrollPane = new JScrollPane(checkBoxesPanel,
				ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

		scrollPane.setPreferredSize(new Dimension(horSize, vertSize));

		add(scrollPane, BorderLayout.CENTER);

		JPanel buttonsPanel = new JPanel();
		BoxLayout buttonsLayout = new BoxLayout(buttonsPanel, BoxLayout.Y_AXIS);
		buttonsPanel.setLayout(buttonsLayout);
		selectAllButton = GUIUtils.addButton(buttonsPanel, "All", null, this);
		buttonsPanel.add(Box.createVerticalStrut(3));
		selectNoneButton = GUIUtils
				.addButton(buttonsPanel, "Clear", null, this);
		add(buttonsPanel, BorderLayout.EAST);

	}

	public Object[] getValue() {

		ArrayList<Object> selectedObjects = new ArrayList<Object>();
		for (int i = 0; i < checkBoxes.length; i++) {
			if (checkBoxes[i].isSelected()) {
				selectedObjects.add(choices[i]);
			}
		}

		return selectedObjects.toArray();
	}

	public void setValue(Object values[]) {

		for (int i = 0; i < checkBoxes.length; i++) {

			boolean isSelected = false;
			for (Object v : values) {
				/*
				 * We compare the string representations, not the actual
				 * objects, because when a project is saved, only the string
				 * representation is saved to the configuration file
				 */
				if (v.toString().equals(choices[i].toString())) {
					isSelected = true;
					break;
				}
			}
			checkBoxes[i].setSelected(isSelected);

		}
	}

	public void actionPerformed(ActionEvent event) {

		Object src = event.getSource();

		if (src == selectAllButton) {
			for (JCheckBox ecb : checkBoxes)
				ecb.setSelected(true);
		}

		if (src == selectNoneButton) {
			for (JCheckBox ecb : checkBoxes)
				ecb.setSelected(false);
		}

	}

	@Override
	public void setToolTipText(String toolTip) {
		for (JCheckBox box : checkBoxes)
			box.setToolTipText(toolTip);
	}

}
