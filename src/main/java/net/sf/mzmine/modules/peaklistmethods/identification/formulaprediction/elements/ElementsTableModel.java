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

package net.sf.mzmine.modules.peaklistmethods.identification.formulaprediction.elements;

import java.util.ArrayList;

import javax.swing.table.AbstractTableModel;

import org.openscience.cdk.interfaces.IIsotope;

public class ElementsTableModel extends AbstractTableModel {

    private static final long serialVersionUID = 1L;

    private static final String[] columnNames = { "Element", "Min", "Max" };

    private ArrayList<IIsotope> isotopes = new ArrayList<IIsotope>();
    private ArrayList<Integer> minCounts = new ArrayList<Integer>();
    private ArrayList<Integer> maxCounts = new ArrayList<Integer>();

    @Override
    public String getColumnName(int col) {
	return columnNames[col];
    }

    @Override
    public Class<?> getColumnClass(int col) {
	if (col == 0)
	    return IIsotope.class;
	else
	    return Integer.class;

    }

    @Override
    public int getRowCount() {
	return isotopes.size();
    }

    @Override
    public int getColumnCount() {
	return columnNames.length;
    }

    @Override
    public Object getValueAt(int row, int col) {
	switch (col) {
	case 0:
	    return isotopes.get(row);
	case 1:
	    return minCounts.get(row);
	case 2:
	    return maxCounts.get(row);
	}
	return null;
    }

    @Override
    public boolean isCellEditable(int row, int col) {
	return (col == 1) || (col == 2);
    }

    @Override
    public synchronized void setValueAt(Object value, int row, int col) {

	int intval = (Integer) value;

	if (intval < 0)
	    return;

	if (col == 1) {
	    minCounts.set(row, intval);
	    int currentMax = maxCounts.get(row);
	    if (currentMax < intval)
		maxCounts.set(row, intval);
	} else if (col == 2) {

	    maxCounts.set(row, intval);
	    int currentMin = minCounts.get(row);
	    if (currentMin > intval)
		minCounts.set(row, intval);
	}
    }

    public synchronized void addRow(IIsotope isotope, int minCount, int maxCount) {
	if (isotopes.contains(isotope))
	    return;
	int newRowIndex = isotopes.size();
	isotopes.add(newRowIndex, isotope);
	minCounts.add(newRowIndex, minCount);
	maxCounts.add(newRowIndex, maxCount);
	fireTableRowsInserted(newRowIndex, newRowIndex);
    }

    public synchronized void removeRow(int index) {
	isotopes.remove(index);
	minCounts.remove(index);
	maxCounts.remove(index);
	fireTableRowsDeleted(index, index);
    }

}
