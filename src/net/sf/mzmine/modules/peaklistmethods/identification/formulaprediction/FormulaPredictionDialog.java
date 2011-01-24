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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.peaklistmethods.identification.formulaprediction;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;

import net.sf.mzmine.data.ChromatographicPeak;
import net.sf.mzmine.data.IonizationType;
import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.util.dialogs.ParameterSetupDialog;

public class FormulaPredictionDialog extends ParameterSetupDialog implements
		ActionListener, PropertyChangeListener {

	private static final Color BACKGROUND_COLOR = new Color(173, 216, 230);
	
	private double rawMassValue;
	private JFormattedTextField chargeField;
	private JFormattedTextField neutralMassField;
	private JComboBox ionizationMethodCombo;
	private ElementsTableComponent elementsTable;

	/**
     * 
     */
	public FormulaPredictionDialog(FormulaPredictionParameters parameters,
			PeakListRow row, String helpID) {

		// Make dialog modal
		super("Formula prediction setup dialog ", parameters, helpID);

		ionizationMethodCombo = (JComboBox) getComponentForParameter(FormulaPredictionParameters.ionizationMethod);
		ionizationMethodCombo.addActionListener(this);

		JFormattedTextField peakMass = (JFormattedTextField) getComponentForParameter(FormulaPredictionParameters.rawMass);
		peakMass.setEditable(false);
		peakMass.setBackground(BACKGROUND_COLOR);

		neutralMassField = (JFormattedTextField) getComponentForParameter(FormulaPredictionParameters.neutralMass);
		neutralMassField.setEditable(false);
		neutralMassField.setBackground(BACKGROUND_COLOR);

		chargeField = (JFormattedTextField) getComponentForParameter(FormulaPredictionParameters.charge);
		chargeField.addPropertyChangeListener("value", this);

		if (row != null) {
			this.rawMassValue = row.getAverageMZ();
			peakMass.setValue(rawMassValue);
			ChromatographicPeak peak = row.getBestPeak();
			int rowCharge = peak.getCharge();

			// If the charge is unknown, assume charge 1
			if (rowCharge == 0)
				rowCharge = 1;

			chargeField.setValue(rowCharge);

		}

		setNeutralMassValue();

		if (row == null) {
			chargeField.setEnabled(false);
			neutralMassField.setEnabled(false);
			peakMass.setEnabled(false);
		}
		
		// Find which row is the "Elements" parameter
		List parametersList = Arrays.asList(parameters.getParameters());
		int elementsParameterIndex = parametersList.indexOf(FormulaPredictionParameters.elements);
		JComponent originalComponent = parametersAndComponents.get(FormulaPredictionParameters.elements);

		// Replace the default component with a table of elements
		mainPanel.remove(originalComponent);
		elementsTable = new ElementsTableComponent();
		mainPanel.add(elementsTable, 1, elementsParameterIndex, 1, 1, 1, 1);
		
		// Load the current values into the table
		String currentValue = (String) parameters.getParameterValue(FormulaPredictionParameters.elements);
		if (currentValue != null) elementsTable.setElementsFromString(currentValue);
		
		pack();

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

		// When the dialog is being constructed, chargeField may still be null when we get here 
		if (chargeField == null) return;
		
		int charge = Integer.parseInt(chargeField.getText());
		if (charge < 1) {
			charge = 1;
		}

		IonizationType ionType = (IonizationType) ionizationMethodCombo
				.getSelectedItem();

		double neutral = (rawMassValue - ionType.getAddedMass()) * charge;

		neutralMassField.setValue(neutral);

	}
	
	protected Object getComponentValue(Parameter p) {
		
		if (p == FormulaPredictionParameters.elements)
			return elementsTable.getElementsAsString();
		
		return super.getComponentValue(p);
	}

}
