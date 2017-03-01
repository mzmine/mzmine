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
import java.util.TreeMap;

import javax.swing.table.AbstractTableModel;

public class InterpolatingLookupPaintScaleSetupDialogTableModel extends
	AbstractTableModel {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private static String[] columnNames = { "Value", "Color" };

    private TreeMap<Double, Color> lookupTable;

    public InterpolatingLookupPaintScaleSetupDialogTableModel(
	    TreeMap<Double, Color> lookupTable) {
	this.lookupTable = lookupTable;
    }

    public int getColumnCount() {
	return 2;
    }

    public int getRowCount() {
	return lookupTable.size();
    }

    public String getColumnName(int column) {
	return columnNames[column];
    }

    public Object getValueAt(int row, int column) {
	if (column == 0)
	    return lookupTable.keySet().toArray(new Double[0])[row];
	return null;
    }

}
