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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.util.components;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

import net.sf.mzmine.util.GUIUtils;

/**
 * Component with multiple checkboxes in a list
 */
public class MultipleSelectionComponent extends JPanel implements
	ActionListener {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private ExtendedCheckBox<Object> checkBoxes[];

    private JButton selectAllButton, selectNoneButton;

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

	    widthSize = (int) checkBoxes[i].getPreferredSize().getWidth();
	    if (horSize < widthSize)
		horSize = widthSize;

	    numCheckBoxes++;
	}

	if (numCheckBoxes < 3)
	    vertSize += 30;

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

    public void addActionListener(ActionListener listener) {
	for (ExtendedCheckBox<?> ecb : checkBoxes)
	    ecb.addActionListener(listener);
    }

    public void removeActionListener(ActionListener listener) {
	for (ExtendedCheckBox<?> ecb : checkBoxes)
	    ecb.removeActionListener(listener);
    }

    public Object[] getSelectedValues() {
	ArrayList<Object> selectedObjects = new ArrayList<Object>();
	for (ExtendedCheckBox<Object> ecb : checkBoxes) {
	    if (ecb.isSelected()) {
		selectedObjects.add(ecb.getObject());
	    }
	}
	return selectedObjects.toArray(new Object[0]);
    }

    public void setSelectedValues(Object[] values) {

	for (ExtendedCheckBox<Object> ecb : checkBoxes) {
	    boolean isSelected = false;
	    for (Object v : values) {

		// We compare the identity of the objects, as well as their
		// string representations, because when a project is saved, only
		// string representation is saved to the configuration file
		if ((v == ecb.getObject())
			|| (v.toString().equals(ecb.getObject().toString())))
		    isSelected = true;
	    }
	    ecb.setSelected(isSelected);
	}

    }

    public void actionPerformed(ActionEvent event) {

	Object src = event.getSource();

	if (src == selectAllButton) {
	    for (ExtendedCheckBox<?> ecb : checkBoxes)
		ecb.setSelected(true);
	}

	if (src == selectNoneButton) {
	    for (ExtendedCheckBox<?> ecb : checkBoxes)
		ecb.setSelected(false);
	}

    }

}
