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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.identification.pubchem;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DecimalFormat;

import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JTextField;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.util.dialogs.ParameterSetupDialog;

public class PubChemSearchDialog extends ParameterSetupDialog implements
        ActionListener, PropertyChangeListener {

    private PubChemSearchParameters parameters;
    private static final Color BACKGROUND_COLOR = new Color(173, 216, 230);
    private double rawMassValue;
    public static final DecimalFormat massFormat = new DecimalFormat("#####.#####");

    /**
     * 
     */
    public PubChemSearchDialog(PubChemSearchParameters parameters,
            double massValue) {

        // Make dialog modal
        super("PubChem search setup dialog ", parameters);

        this.parameters = parameters;
        this.rawMassValue = massValue;

        Component[] fields = pnlFields.getComponents();
        Parameter[] params = parameters.getParameters();
        for (int i = 0; i < params.length; i++) {
            if (params[i].getName() == "Neutral mass") {
                ((JTextField) fields[i]).setEditable(false);
                ((JTextField) fields[i]).setBackground(BACKGROUND_COLOR);
                continue;
            }
            if (params[i].getName() == "Peak mass") {
                ((JTextField) fields[i]).setText(massFormat.format(massValue));
                ((JTextField) fields[i]).setEditable(false);
                ((JTextField) fields[i]).setBackground(BACKGROUND_COLOR);
                continue;
            }
            if (fields[i] instanceof JComboBox) {
                ((JComboBox) fields[i]).addActionListener(this);
                continue;
            }

            fields[i].addPropertyChangeListener("value", this);

        }

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
        int chargeLevel = 1;
        double ion = 0.0f;
        double neutral = rawMassValue;
        int sign = 1;

        Component[] fields = pnlFields.getComponents();
        Parameter[] params = parameters.getParameters();
        for (int i = 0; i < params.length; i++) {
            if (params[i].getName() == "Charge") {
                chargeLevel = Integer.parseInt(((JTextField) fields[i]).getText());
                continue;
            }
            if (params[i].getName() == "Ionization method") {
                Object a = ((JComboBox) fields[i]).getSelectedItem();
                ion = ((TypeOfIonization) a).getMass();
                sign = ((TypeOfIonization) a).getSign();
                ion *= sign;
                continue;
            }
        }

        neutral /= chargeLevel;
        neutral += ion;

        for (int i = 0; i < params.length; i++) {
            if (params[i].getName() == "Neutral mass") {
                ((JFormattedTextField) fields[i]).setValue(neutral);
                break;
            }
        }
    }

}
