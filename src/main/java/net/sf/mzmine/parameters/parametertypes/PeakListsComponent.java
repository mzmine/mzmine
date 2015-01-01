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

package net.sf.mzmine.parameters.parametertypes;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;

import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;

import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.util.ExitCode;
import net.sf.mzmine.util.GUIUtils;

/**
 */
public class PeakListsComponent extends JPanel implements ActionListener {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private final DefaultListModel<String> listModel = new DefaultListModel<String>();
    private final JList<String> nameList;
    private final JButton addPatternButton, selectPeakListsButton,
	    removeButton;

    public PeakListsComponent(int rows, int inputsize) {

	super(new BorderLayout());

	nameList = new JList<String>(listModel);
	nameList.setVisibleRowCount(rows);

	JScrollPane scroll = new JScrollPane(nameList,
		ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
		ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
	scroll.setPreferredSize(new Dimension(inputsize, 10));
	add(scroll, BorderLayout.CENTER);

	JPanel buttonsPanel = new JPanel();
	buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.Y_AXIS));
	addPatternButton = GUIUtils.addButton(buttonsPanel, "Add name pattern",
		null, this);
	selectPeakListsButton = GUIUtils.addButton(buttonsPanel,
		"Select peak lists", null, this);
	removeButton = GUIUtils.addButton(buttonsPanel, "Remove", null, this);
	add(buttonsPanel, BorderLayout.EAST);

    }

    void setValue(String newValue[]) {
	listModel.clear();
	for (String value : newValue)
	    listModel.addElement(value);
    }

    String[] getValue() {
	Object values[] = listModel.toArray();
	String returnValue[] = Arrays.copyOf(values, values.length,
		String[].class);
	return returnValue;
    }

    public void actionPerformed(ActionEvent event) {

	Object src = event.getSource();

	if (src == addPatternButton) {
	    final StringParameter nameParameter = new StringParameter(
		    "Name pattern",
		    "Set name pattern that may include wildcards (*), e.g. *mouse* matches any name that contains mouse");
	    final SimpleParameterSet paramSet = new SimpleParameterSet(
		    new Parameter[] { nameParameter });
	    Window parent = (Window) SwingUtilities.getAncestorOfClass(
		    Window.class, this);
	    final ExitCode exitCode = paramSet.showSetupDialog(parent, true);
	    if (exitCode == ExitCode.OK) {
		String newName = paramSet.getParameter(nameParameter)
			.getValue();
		if (!listModel.contains(newName))
		    listModel.addElement(newName);
	    }
	    return;
	}

	if (src == selectPeakListsButton) {
	    final MultiChoiceParameter<PeakList> plParameter = new MultiChoiceParameter<PeakList>(
		    "Select peak lists", "Select peak lists", MZmineCore
			    .getCurrentProject().getPeakLists());
	    final SimpleParameterSet paramSet = new SimpleParameterSet(
		    new Parameter[] { plParameter });
	    Window parent = (Window) SwingUtilities.getAncestorOfClass(
		    Window.class, this);
	    final ExitCode exitCode = paramSet.showSetupDialog(parent, true);
	    if (exitCode == ExitCode.OK) {
		PeakList selectedPeakLists[] = paramSet.getParameter(
			plParameter).getValue();
		for (PeakList selectedPeakList : selectedPeakLists) {
		    final String name = selectedPeakList.getName();
		    if (!listModel.contains(name))
			listModel.addElement(name);
		}
	    }
	    return;
	}

	if (src == removeButton) {
	    int selectedIndices[] = nameList.getSelectedIndices();
	    for (int i = selectedIndices.length - 1; i >= 0; i--) {
		listModel.remove(selectedIndices[i]);
	    }
	    return;
	}

    }

    @Override
    public void setToolTipText(String toolTip) {
	nameList.setToolTipText(toolTip);
    }
}
