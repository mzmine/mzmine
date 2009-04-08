/*
 * Copyright 2006-2009 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.identification.pubchem;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JTextField;

import net.sf.mzmine.data.IonizationType;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.util.dialogs.ParameterSetupDialog;

public class PubChemSearchDialog extends ParameterSetupDialog implements
		ActionListener, PropertyChangeListener {

	private static final Color BACKGROUND_COLOR = new Color(173, 216, 230);
	private double rawMassValue;
	private JTextField chargeField;
	private JFormattedTextField neutralMassField;
	private JComboBox ionizationMethodCombo;

	/**
     * 
     */
	public PubChemSearchDialog(PubChemSearchParameters parameters,
			double massValue) {

		// Make dialog modal
		super("PubChem search setup dialog ", parameters);

		this.rawMassValue = massValue;

		chargeField = (JTextField) getComponentForParameter(PubChemSearchParameters.charge);
		chargeField.addPropertyChangeListener("value", this);

		neutralMassField = (JFormattedTextField) getComponentForParameter(PubChemSearchParameters.neutralMass);
		neutralMassField.setEditable(false);
		neutralMassField.setBackground(BACKGROUND_COLOR);

		JFormattedTextField peakMass = (JFormattedTextField) getComponentForParameter(PubChemSearchParameters.rawMass);
		peakMass.setEditable(false);
		peakMass.setBackground(BACKGROUND_COLOR);
		peakMass.setValue(massValue);

		ionizationMethodCombo = (JComboBox) getComponentForParameter(PubChemSearchParameters.ionizationMethod);
		ionizationMethodCombo.addActionListener(this);

		setNeutralMassValue();

		setResizable(false);
		setLocationRelativeTo(MZmineCore.getDesktop().getMainFrame());

	}

	/**
	 * Implementation for ActionListener interface
	 */
	public void actionPerformed(ActionEvent ae) {

		super.actionPerformed(ae);

		Object src = ae.getSource();
		if (src instanceof JComboBox) {
			setNeutralMassValue();
		}

	}

	/**
	 * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
	 */
	public void propertyChange(PropertyChangeEvent pro) {
		if (pro.getPropertyName() == "value") {
			setNeutralMassValue();
		}
	}

	/**
	 * Update the field of neutral mass according with the rest of parameters.
	 */
	private void setNeutralMassValue() {

		int charge = Integer.parseInt(chargeField.getText());

		IonizationType ionType = (IonizationType) ionizationMethodCombo
				.getSelectedItem();

		double neutral = (rawMassValue * charge) - ionType.getAddedMass();

		neutralMassField.setValue(neutral);

	}

}
