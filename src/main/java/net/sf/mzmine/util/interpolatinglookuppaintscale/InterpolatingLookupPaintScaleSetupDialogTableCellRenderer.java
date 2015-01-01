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

package net.sf.mzmine.util.interpolatinglookuppaintscale;

import java.awt.Color;
import java.awt.Component;
import java.util.TreeMap;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

public class InterpolatingLookupPaintScaleSetupDialogTableCellRenderer extends
	DefaultTableCellRenderer {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private TreeMap<Double, Color> lookupTable;

    public InterpolatingLookupPaintScaleSetupDialogTableCellRenderer(
	    TreeMap<Double, Color> lookupTable) {
	this.lookupTable = lookupTable;
    }

    public Component getTableCellRendererComponent(JTable table, Object value,
	    boolean isSelected, boolean hasFocus, int row, int column) {
	if (value == null)
	    this.setText("");
	else
	    this.setText(String.valueOf(value));

	if (lookupTable == null) {
	    return this;
	}
	if (lookupTable.size() < row) {
	    return this;
	}

	Double key = lookupTable.keySet().toArray(new Double[0])[row];
	Color color = lookupTable.get(key);

	if (column == 0)
	    this.setBackground(table.getBackground());
	if (column == 1)
	    this.setBackground(color);

	return this;
    }
}