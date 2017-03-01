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

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import net.sf.mzmine.datamodel.IonizationType;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.util.components.GridBagPanel;

public class NeutralMassComponent extends GridBagPanel implements
	DocumentListener, ActionListener {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private static final Color BACKGROUND_COLOR = new Color(192, 224, 240);

    private JComboBox<IonizationType> ionTypeCombo;
    private JTextField ionMassField, chargeField, neutralMassField;

    public NeutralMassComponent() {

	add(new JLabel("m/z:"), 0, 0);

	ionMassField = new JTextField();
	ionMassField.getDocument().addDocumentListener(this);
	ionMassField.setColumns(8);
	add(ionMassField, 1, 0);

	add(new JLabel("Charge:"), 2, 0);

	chargeField = new JTextField();
	chargeField.getDocument().addDocumentListener(this);
	chargeField.setColumns(2);
	add(chargeField, 3, 0);

	add(new JLabel("Ionization type:"), 0, 1, 2, 1);
	ionTypeCombo = new JComboBox<IonizationType>(IonizationType.values());
	ionTypeCombo.addActionListener(this);
	add(ionTypeCombo, 2, 1, 2, 1);

	add(new JLabel("Calculated mass:"), 0, 2, 2, 1);

	neutralMassField = new JTextField();
	neutralMassField.setColumns(8);
	neutralMassField.setBackground(BACKGROUND_COLOR);
	neutralMassField.setEditable(false);
	add(neutralMassField, 2, 2, 2, 1);

    }

    public void setIonMass(double ionMass) {
	ionMassField.setText(MZmineCore.getConfiguration().getMZFormat()
		.format(ionMass));
	updateNeutralMass();
    }

    public void setCharge(int charge) {
	chargeField.setText(String.valueOf(charge));
	updateNeutralMass();
    }

    public void setIonType(IonizationType ionType) {
	ionTypeCombo.setSelectedItem(ionType);
	updateNeutralMass();
    }

    public Double getValue() {
	String stringValue = neutralMassField.getText();
	try {
	    double doubleValue = Double.parseDouble(stringValue);
	    return doubleValue;
	} catch (NumberFormatException e) {
	    return null;
	}
    }

    public Double getIonMass() {
	String stringValue = ionMassField.getText();
	try {
	    double doubleValue = Double.parseDouble(stringValue);
	    return doubleValue;
	} catch (NumberFormatException e) {
	    return null;
	}
    }

    public Integer getCharge() {
	String stringValue = chargeField.getText();
	try {
	    int intValue = Integer.parseInt(stringValue);
	    return intValue;
	} catch (NumberFormatException e) {
	    return null;
	}
    }

    public IonizationType getIonType() {
	return (IonizationType) ionTypeCombo.getSelectedItem();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
	updateNeutralMass();
    }

    private void updateNeutralMass() {

	Integer charge = getCharge();
	if (charge == null)
	    return;

	Double ionMass = getIonMass();
	if (ionMass == null)
	    return;

	IonizationType ionType = getIonType();

	double neutral = (ionMass.doubleValue() - ionType.getAddedMass())
		* charge.intValue();

	neutralMassField.setText(MZmineCore.getConfiguration().getMZFormat()
		.format(neutral));
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
	updateNeutralMass();
    }

    @Override
    public void insertUpdate(DocumentEvent e) {
	updateNeutralMass();
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
	updateNeutralMass();
    }

    @Override
    public void setToolTipText(String toolTip) {
	ionMassField.setToolTipText(toolTip);
    }
}
