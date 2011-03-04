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

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.NumberFormat;

import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;

import net.sf.mzmine.data.IonizationType;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.util.components.GridBagPanel;

/**
 */
public class NeutralMassComponent extends GridBagPanel implements
		PropertyChangeListener, ActionListener {

	private static final Color BACKGROUND_COLOR = new Color(192, 224, 240);

	private JComboBox ionTypeCombo;
	private JFormattedTextField ionMassField, chargeField, neutralMassField;

	public NeutralMassComponent() {

		add(new JLabel("m/z:"), 0, 0);

		ionMassField = new JFormattedTextField(MZmineCore.getMZFormat());
		ionMassField.addPropertyChangeListener("value", this);
		ionMassField.setColumns(8);
		add(ionMassField, 1, 0);

		add(new JLabel("Charge:"), 2, 0);

		chargeField = new JFormattedTextField(NumberFormat.getIntegerInstance());
		chargeField.addPropertyChangeListener("value", this);
		chargeField.setColumns(2);
		add(chargeField, 3, 0);

		add(new JLabel("Ionization type:"), 0, 1, 2, 1);
		ionTypeCombo = new JComboBox(IonizationType.values());
		ionTypeCombo.addActionListener(this);
		add(ionTypeCombo, 2, 1, 2, 1);

		add(new JLabel("Calculated mass:"), 0, 2, 2, 1);

		neutralMassField = new JFormattedTextField(MZmineCore.getMZFormat());
		neutralMassField.setColumns(8);
		neutralMassField.setBackground(BACKGROUND_COLOR);
		neutralMassField.setEditable(false);
		add(neutralMassField, 2, 2, 2, 1);

	}

	public void setIonMass(double ionMass) {
		ionMassField.setValue(ionMass);
		updateNeutralMass();
	}

	public void setCharge(int charge) {
		chargeField.setValue(charge);
		updateNeutralMass();
	}

	public void setIonType(IonizationType ionType) {
		ionTypeCombo.setSelectedItem(ionType);
		updateNeutralMass();
	}

	public Double getValue() {
		Number val = (Number) neutralMassField.getValue();
		if (val == null)
			return null;
		return val.doubleValue();
	}

	public Double getIonMass() {
		Number val = (Number) ionMassField.getValue();
		if (val == null)
			return null;
		return val.doubleValue();
	}

	public Integer getCharge() {
		Number val = (Number) chargeField.getValue();
		if (val == null)
			return null;
		return val.intValue();
	}

	public IonizationType getIonType() {
		return (IonizationType) ionTypeCombo.getSelectedItem();
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		updateNeutralMass();
	}

	@Override
	public void propertyChange(PropertyChangeEvent e) {
		updateNeutralMass();
	}

	private void updateNeutralMass() {

		Number charge = (Number) chargeField.getValue();
		if (charge == null)
			return;

		Number ionMass = (Number) ionMassField.getValue();
		if (ionMass == null)
			return;

		IonizationType ionType = (IonizationType) ionTypeCombo
				.getSelectedItem();

		double neutral = (ionMass.doubleValue() - ionType.getAddedMass())
				* charge.intValue();

		neutralMassField.setValue(neutral);
	}

}
