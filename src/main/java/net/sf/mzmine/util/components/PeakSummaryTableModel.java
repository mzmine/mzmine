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

package net.sf.mzmine.util.components;

import java.awt.Color;
import java.text.NumberFormat;
import java.util.Vector;

import javax.swing.table.AbstractTableModel;

import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.main.MZmineCore;

public class PeakSummaryTableModel extends AbstractTableModel {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private static NumberFormat mzFormat = MZmineCore.getConfiguration()
	    .getMZFormat();
    private static NumberFormat rtFormat = MZmineCore.getConfiguration()
	    .getRTFormat();
    private static NumberFormat intensityFormat = MZmineCore.getConfiguration()
	    .getIntensityFormat();

    private Vector<Feature> peaks = new Vector<Feature>();
    private Vector<Color> peakColors = new Vector<Color>();

    private static String[] columnNames = { "File Name", "Mass", "RT",
	    "Height", "Area" };

    public String getColumnName(int col) {
	return columnNames[col].toString();
    }

    public int getRowCount() {
	return peaks.size();
    }

    public int getColumnCount() {
	return columnNames.length;
    }

    public Object getValueAt(int row, int col) {
	Object value = null;
	Feature peak = peaks.get(row);
	switch (col) {
	case (0):
	    value = peak.getDataFile().getName();
	    break;
	case (1):
	    value = mzFormat.format(peak.getMZ());
	    break;
	case (2):
	    value = rtFormat.format(peak.getRT());
	    break;
	case (3):
	    value = intensityFormat.format(peak.getHeight());
	    break;
	case (4):
	    value = intensityFormat.format(peak.getArea());
	    break;
	}

	return value;
    }

    public Feature getElementAt(int row) {
	return peaks.get(row);
    }

    public boolean isCellEditable(int row, int col) {
	return false;
    }

    public Color getPeakColor(int row) {
	return peakColors.get(row);
    }

    public void addElement(Feature peak, Color peakColor) {
	peaks.add(peak);
	peakColors.add(peakColor);
	fireTableRowsInserted(0, peaks.size() - 1);
    }

    public void setValueAt(Object value, int row, int col) {
    }

    public int getIndexRow(String fileName) {

	String localFileName;
	int index = -1;
	for (int i = 0; i < peaks.size(); i++) {
	    localFileName = peaks.get(i).getDataFile().getName();
	    if (localFileName.equals(fileName)) {
		index = i;
	    }
	}

	return index;
    }

}
