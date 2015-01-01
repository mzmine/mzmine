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

package net.sf.mzmine.desktop.preferences;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

public class NumberFormatEditor extends JPanel {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private SpinnerNumberModel spinnerModel;
    private JSpinner decimalsSpinner;
    private JCheckBox exponentCheckbox;

    public NumberFormatEditor(boolean showExponentOption) {

	setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

	add(new JLabel("Decimals"));

	spinnerModel = new SpinnerNumberModel(1, 0, 20, 1);
	decimalsSpinner = new JSpinner(spinnerModel);
	add(decimalsSpinner);

	if (showExponentOption) {
	    exponentCheckbox = new JCheckBox("Show exponent");
	    add(exponentCheckbox);
	}

    }

    public int getDecimals() {
	return ((Number) spinnerModel.getValue()).intValue();
    }

    public boolean getShowExponent() {
	if (exponentCheckbox == null)
	    return false;
	else
	    return exponentCheckbox.isSelected();
    }

    public void setValue(int decimals, boolean showExponent) {
	spinnerModel.setValue(decimals);
	if (exponentCheckbox != null)
	    exponentCheckbox.setSelected(showExponent);
    }

}
