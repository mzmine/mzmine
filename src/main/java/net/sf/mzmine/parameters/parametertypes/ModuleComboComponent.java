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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import net.sf.mzmine.modules.MZmineProcessingStep;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.dialogs.ParameterSetupDialog;

public class ModuleComboComponent extends JPanel implements ActionListener {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private JComboBox<MZmineProcessingStep<?>> comboBox;
    private JButton setButton;

    public ModuleComboComponent(MZmineProcessingStep<?> modules[]) {

	super(new BorderLayout());

        setBorder(BorderFactory.createEmptyBorder(0, 9, 0, 0));

	assert modules != null;
	assert modules.length > 0;

	comboBox = new JComboBox<MZmineProcessingStep<?>>(modules);
	comboBox.addActionListener(this);
	add(comboBox, BorderLayout.CENTER);

	setButton = new JButton("...");
	setButton.addActionListener(this);
	boolean buttonEnabled = (modules[0].getParameterSet() != null);
	setButton.setEnabled(buttonEnabled);
	add(setButton, BorderLayout.EAST);

    }

    public int getSelectedIndex() {
	return comboBox.getSelectedIndex();
    }

    public void setSelectedItem(Object selected) {
	comboBox.setSelectedItem(selected);
    }

    public void actionPerformed(ActionEvent event) {

	Object src = event.getSource();

	MZmineProcessingStep<?> selected = (MZmineProcessingStep<?>) comboBox
		.getSelectedItem();

	if (src == comboBox) {
	    if (selected == null) {
		setButton.setEnabled(false);
		return;
	    }
	    ParameterSet parameterSet = selected.getParameterSet();
	    int numOfParameters = parameterSet.getParameters().length;
	    setButton.setEnabled(numOfParameters > 0);
	}

	if (src == setButton) {
	    if (selected == null)
		return;
	    ParameterSetupDialog dialog = (ParameterSetupDialog) SwingUtilities
		    .getAncestorOfClass(ParameterSetupDialog.class, this);
	    if (dialog == null)
		return;
	    ParameterSet parameterSet = selected.getParameterSet();
	    parameterSet.showSetupDialog(dialog, dialog.isValueCheckRequired());
	}

    }

    @Override
    public void setToolTipText(String toolTip) {
	comboBox.setToolTipText(toolTip);
    }
}
