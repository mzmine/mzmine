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
package net.sf.mzmine.parameters.parametertypes.tolerances;

import java.awt.BorderLayout;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.sf.mzmine.main.MZmineCore;

/**
 */
public class RTToleranceComponent extends JPanel {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private static final String toleranceTypes[] = { "absolute (min)",
            "relative (%)" };
    private JTextField toleranceField;
    private JComboBox<String> toleranceType;

    public RTToleranceComponent() {

        super(new BorderLayout());

        setBorder(BorderFactory.createEmptyBorder(0, 9, 0, 0));

        toleranceField = new JTextField();
        toleranceField.setColumns(6);
        add(toleranceField, BorderLayout.CENTER);

        toleranceType = new JComboBox<String>(toleranceTypes);
        add(toleranceType, BorderLayout.EAST);

    }

    public void setValue(RTTolerance value) {
        double tolerance = value.getTolerance();
        if (value.isAbsolute()) {
            toleranceType.setSelectedIndex(0);
            String valueString = String.valueOf(tolerance);
            toleranceField.setText(valueString);
        } else {
            toleranceType.setSelectedIndex(1);
            String valueString = String.valueOf(tolerance * 100);
            toleranceField.setText(valueString);
        }
    }

    public RTTolerance getValue() {

        int index = toleranceType.getSelectedIndex();

        String valueString = toleranceField.getText();

        double toleranceDouble;
        try {
            if (index == 0) {
                toleranceDouble = MZmineCore.getConfiguration().getRTFormat()
                        .parse(valueString).doubleValue();
            } else {
                Number toleranceValue = Double.parseDouble(valueString);
                toleranceDouble = toleranceValue.doubleValue() / 100;
            }
        } catch (Exception e) {
            return null;
        }

        RTTolerance value = new RTTolerance(index <= 0, toleranceDouble);

        return value;

    }

    @Override
    public void setToolTipText(String toolTip) {
        toleranceField.setToolTipText(toolTip);
    }
}
