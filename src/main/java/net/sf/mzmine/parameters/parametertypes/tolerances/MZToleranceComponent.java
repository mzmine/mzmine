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

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class MZToleranceComponent extends JPanel {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private final JTextField mzToleranceField, ppmToleranceField;

    public MZToleranceComponent() {

        setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 0));

        mzToleranceField = new JTextField();
        mzToleranceField.setColumns(6);
        add(mzToleranceField);

        add(new JLabel("m/z  or"));

        ppmToleranceField = new JTextField();
        ppmToleranceField.setColumns(6);
        add(ppmToleranceField);

        add(new JLabel("ppm"));
    }

    public void setValue(MZTolerance value) {
        mzToleranceField.setText(String.valueOf(value.getMzTolerance()));
        ppmToleranceField.setText(String.valueOf(value.getPpmTolerance()));
    }

    public MZTolerance getValue() {
        try {
            double mzTolerance = Double.parseDouble(mzToleranceField.getText().trim());
            double ppmTolerance = Double
                    .parseDouble(ppmToleranceField.getText().trim());
            MZTolerance value = new MZTolerance(mzTolerance, ppmTolerance);
            return value;
        } catch (NumberFormatException e) {
            return null;
        }

    }

    @Override
    public void setToolTipText(String toolTip) {
        mzToleranceField.setToolTipText(toolTip);
        ppmToleranceField.setToolTipText(toolTip);
    }

}
