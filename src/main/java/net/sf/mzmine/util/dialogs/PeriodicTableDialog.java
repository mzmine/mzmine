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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.util.dialogs;

import java.awt.BorderLayout;
import java.util.EventObject;

import javax.swing.JDialog;
import javax.swing.JFrame;

import org.openscience.cdk.config.IsotopeFactory;
import org.openscience.cdk.config.Isotopes;
import org.openscience.cdk.event.ICDKChangeListener;
import org.openscience.cdk.interfaces.IIsotope;

public class PeriodicTableDialog extends JDialog implements ICDKChangeListener {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private PeriodicTablePanel periodicTable;
    private IIsotope selectedIsotope;

    public PeriodicTableDialog(JFrame parent) {

	super(parent, "Choose an element...", true);

	setLayout(new BorderLayout());

	periodicTable = new PeriodicTablePanel();
	periodicTable.addCDKChangeListener(this);
	add(BorderLayout.CENTER, periodicTable);

	pack();

	setLocationRelativeTo(parent);
    }

    public void stateChanged(EventObject event) {

	if (event.getSource() == periodicTable) {
	    try {
		String elementSymbol = periodicTable.getSelectedElement();
		IsotopeFactory isoFac = Isotopes.getInstance();
		selectedIsotope = isoFac.getMajorIsotope(elementSymbol);
	    } catch (Exception e) {
		e.printStackTrace();
	    }
	    dispose();
	}
    }

    public IIsotope getSelectedIsotope() {
	return selectedIsotope;
    }

}
