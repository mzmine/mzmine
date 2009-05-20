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

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Vector;

import javax.swing.table.AbstractTableModel;

public class PubChemResultTableModel extends AbstractTableModel {

	private Vector<PubChemCompound> compounds = new Vector<PubChemCompound>();
	private static final String[] columnNames = { "CID", "Common Name",
			"Formula", "Mass difference", "Isotope pattern score" };

	public static final NumberFormat percentFormat = NumberFormat
			.getPercentInstance();
	public static final DecimalFormat massFormat = new DecimalFormat("##.#####");

	private double searchedMass;

	PubChemResultTableModel(double searchedMass) {
		this.searchedMass = searchedMass;
	}

	public String getColumnName(int col) {
		return columnNames[col].toString();
	}

	public int getRowCount() {
		return compounds.size();
	}

	public int getColumnCount() {
		return columnNames.length;
	}

	public Object getValueAt(int row, int col) {
		Object value = null;
		PubChemCompound comp = compounds.get(row);
		switch (col) {
		case (0):
			value = comp.getID();
			break;
		case (1):
			value = comp.getName();
			break;
		case (2):
			value = comp.getCompoundFormula();
			break;
		case (3):
			double massDifference = Math
					.abs(searchedMass - comp.getExactMass());
			value = massFormat.format(massDifference);
			break;
		case (4):
			String text = comp.getIsotopePatternScore();
			if (text.length() == 0)
				break;
			double score = Double.parseDouble(text);
			value = percentFormat.format(score);
			break;
		}

		return value;
	}

	public PubChemCompound getCompoundAt(int row) {
		return compounds.get(row);
	}

	public boolean isCellEditable(int row, int col) {
		return false;
	}

	public void addElement(PubChemCompound compound) {
		compounds.add(compound);
		fireTableRowsInserted(0, compounds.size() - 1);
	}

	public void setValueAt(Object value, int row, int col) {
	}

}
