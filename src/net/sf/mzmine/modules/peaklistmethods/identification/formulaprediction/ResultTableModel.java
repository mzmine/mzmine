/*
 * Copyright 2006-2011 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.peaklistmethods.identification.formulaprediction;

import java.text.NumberFormat;
import java.util.Vector;

import javax.swing.table.AbstractTableModel;

import net.sf.mzmine.data.IsotopePattern;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.util.FormulaUtils;

public class ResultTableModel extends AbstractTableModel {

	
	// TODO: show formula in HTML form, add copy to clipboard button 
	
	private static final String[] columnNames = { "Formula", "Mass difference",
			"Isotope pattern score" };

	private double searchedMass;

	
	private Vector<String> formulas = new Vector<String>();
	private Vector<IsotopePattern> isotopePatterns = new Vector<IsotopePattern>();
	private Vector<Double> scores = new Vector<Double>();

	final NumberFormat percentFormat = NumberFormat.getPercentInstance();
	final NumberFormat massFormat = MZmineCore.getMZFormat();

	ResultTableModel(double searchedMass) {
		this.searchedMass = searchedMass;
	}

	public String getColumnName(int col) {
		return columnNames[col].toString();
	}

	public int getRowCount() {
		return formulas.size();
	}

	public int getColumnCount() {
		return columnNames.length;
	}

	public Object getValueAt(int row, int col) {
		switch (col) {
		case (0):
			return formulas.get(row);
		case (1):
			String formula = formulas.get(row);
			double mass = FormulaUtils.calculateExactMass(formula);
			double massDifference = Math.abs(searchedMass - mass);
			return massFormat.format(massDifference);
		case (2):
			double score = scores.get(row);
			return percentFormat.format(score);
		}
		return null;
	}

	public IsotopePattern getIsotopePattern(int row) {
		return isotopePatterns.get(row);
	}

	public boolean isCellEditable(int row, int col) {
		return false;
	}

	public void addElement(String formula,
			IsotopePattern predictedIsotopePattern, double score) {

		formulas.add(formula);
		scores.add(score);

		isotopePatterns.add(predictedIsotopePattern);

		fireTableRowsInserted(formulas.size() - 1, formulas.size() - 1);
	}

	public void setValueAt(Object value, int row, int col) {
	}

}
