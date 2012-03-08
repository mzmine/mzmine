/*
 * Copyright 2006-2012 The MZmine 2 Development Team
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

import java.util.Vector;

import javax.swing.table.AbstractTableModel;

public class ElementsTableModel extends AbstractTableModel {

	private static final String[] columnNames = { "Element", "Min", "Max" };

	private Vector<ElementRule> elementRules = new Vector<ElementRule>();

	public String getColumnName(int col) {
		return columnNames[col];
	}

	public int getRowCount() {
		return elementRules.size();
	}

	public int getColumnCount() {
		return columnNames.length;
	}

	public Object getValueAt(int row, int col) {
		ElementRule rule = elementRules.get(row);
		switch (col) {
		case (0):
			return rule.getElementSymbol();
		case (1):
			return rule.getMinCount();
		case (2):
			return rule.getMaxCount();
		}
		return null;
	}

	public boolean isCellEditable(int row, int col) {
		return (col == 1) || (col == 2);
	}

	public void setValueAt(Object value, int row, int col) {

		int intval;

		try {
			intval = Integer.parseInt((String) value);
		} catch (NumberFormatException e) {
			// ignore wrong numbers
			return;
		}

		if (intval < 0)
			return;
		
		ElementRule rule = elementRules.get(row);

		if (col == 1) {
			rule.setMinCount(intval);
			int currentMax = rule.getMaxCount();
			if (currentMax < intval)
				rule.setMaxCount(intval);
		} else if (col == 2) {
			rule.setMaxCount(intval);
			int currentMin = rule.getMinCount();
			if (currentMin > intval)
				rule.setMinCount(intval);
		}
	}

	public void addRow(ElementRule rule) {
		if (elementRules.contains(rule))
			return;
		int newRowIndex = elementRules.size();
		elementRules.add(newRowIndex, rule);
		fireTableRowsInserted(newRowIndex, newRowIndex);
	}

	public void removeRow(int index) {
		elementRules.remove(index);
		fireTableRowsDeleted(index, index);
	}

}
