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

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.dialogs.ParameterSetupDialog;

/**
 */
public class OptionalModuleComponent extends JPanel implements ActionListener {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private JCheckBox checkBox;
    private JButton setButton;
    private ParameterSet embeddedParameters;

    public OptionalModuleComponent(ParameterSet embeddedParameters) {

	super(new FlowLayout(FlowLayout.LEFT));

	this.embeddedParameters = embeddedParameters;

	checkBox = new JCheckBox();
	checkBox.addActionListener(this);
	add(checkBox);

	setButton = new JButton("Setup..");
	setButton.addActionListener(this);
	setButton.setEnabled(false);
	add(setButton);

    }

    public boolean isSelected() {
	return checkBox.isSelected();
    }

    public void setSelected(boolean selected) {
	checkBox.setSelected(selected);
	setButton.setEnabled(selected);
    }

    public void actionPerformed(ActionEvent event) {

	Object src = event.getSource();

	if (src == checkBox) {
	    boolean checkBoxSelected = checkBox.isSelected();
	    setButton.setEnabled(checkBoxSelected);
	}

	if (src == setButton) {
	    ParameterSetupDialog dialog = (ParameterSetupDialog) SwingUtilities
		    .getAncestorOfClass(ParameterSetupDialog.class, this);
	    if (dialog == null)
		return;
	    embeddedParameters.showSetupDialog(dialog,
		    dialog.isValueCheckRequired());
	}

    }

    @Override
    public void setToolTipText(String toolTip) {
	checkBox.setToolTipText(toolTip);
    }
}
