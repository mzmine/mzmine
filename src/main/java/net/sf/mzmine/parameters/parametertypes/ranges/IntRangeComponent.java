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

package net.sf.mzmine.parameters.parametertypes.ranges;

import java.awt.GridBagConstraints;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JTextField;

import net.sf.mzmine.util.components.GridBagPanel;

import com.google.common.collect.Range;

public class IntRangeComponent extends GridBagPanel {
    private static final long serialVersionUID = 1L;

    private JTextField minTxtField, maxTxtField;

    public IntRangeComponent() {

        setBorder(BorderFactory.createEmptyBorder(0, 9, 0, 0));

        minTxtField = new JTextField();
        minTxtField.setColumns(8);

        maxTxtField = new JTextField();
        maxTxtField.setColumns(8);

        add(minTxtField, 0, 0, 1, 1, 1, 0, GridBagConstraints.HORIZONTAL);
        add(new JLabel(" - "), 1, 0, 1, 1, 0, 0);
        add(maxTxtField, 2, 0, 1, 1, 1, 0, GridBagConstraints.HORIZONTAL);
    }

    public Range<Integer> getValue() {
        String minString = minTxtField.getText();
        String maxString = maxTxtField.getText();

        try {
            if (!minString.isEmpty() && !maxString.isEmpty()) {
                return Range.closed(Integer.parseInt(minString), Integer.parseInt(maxString));
            } else if (!minString.isEmpty()) {
                return Range.closed(Integer.parseInt(minString), Integer.MAX_VALUE);
            } else if (!maxString.isEmpty()) {
                return Range.closed(0, Integer.parseInt(maxString));
            } else {
                return null;
            }
        } catch (Exception e) {
            return null;
        }
    }

    public void setValue(Range<Integer> value) {
        minTxtField.setText(String.valueOf(value.lowerEndpoint()));
        maxTxtField.setText(String.valueOf(value.upperEndpoint()));
    }

    @Override
    public void setToolTipText(String toolTip) {
        minTxtField.setToolTipText(toolTip);
        maxTxtField.setToolTipText(toolTip);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        minTxtField.setEnabled(enabled);
        maxTxtField.setEnabled(enabled);
    }
}
