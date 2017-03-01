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

package net.sf.mzmine.modules.peaklistmethods.identification.formulaprediction;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Vector;

import javax.swing.table.AbstractTableModel;

import net.sf.mzmine.main.MZmineCore;

public class ResultTableModel extends AbstractTableModel {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    public static final String questionMark = "?";
    public static final String checkMark = new String(new char[] { '\u2713' });
    public static final String crossMark = new String(new char[] { '\u2717' });

    private static final String[] columnNames = { "Formula", "Mass difference (Da)","Mass difference (ppm)",
	    "RDBE", "Isotope pattern score", "MS/MS score" };

    private double searchedMass;

    private Vector<ResultFormula> formulas = new Vector<ResultFormula>();

    private final NumberFormat massFormat = MZmineCore.getConfiguration()
	    .getMZFormat();
    
    private final NumberFormat ppmFormat = new DecimalFormat("0.0");

    ResultTableModel(double searchedMass) {
	this.searchedMass = searchedMass;
    }

    public String getColumnName(int col) {
	return columnNames[col].toString();
    }

    public Class<?> getColumnClass(int col) {
	switch (col) {
	case 0:
	case 1:
	case 2:
	    return String.class;
	case 3:
	case 4:
	case 5:
	    return Double.class;
	}
	return null;
    }

    public int getRowCount() {
	return formulas.size();
    }

    public int getColumnCount() {
	return columnNames.length;
    }

    public Object getValueAt(int row, int col) {
	ResultFormula formula = formulas.get(row);
        double formulaMass = formula.getExactMass();
        double massDifference = searchedMass - formulaMass;
	switch (col) {
	case 0:
	    return "<HTML>" + formula.getFormulaAsHTML() + "</HTML>";
	case 1:
	    return massFormat.format(massDifference);
	case 2:
            double massDifferencePPM = massDifference/formulaMass*1E6;
            return ppmFormat.format(massDifferencePPM);
	case 3:
	    return formula.getRDBE();
	case 4:
	    return formula.getIsotopeScore();
	case 5:
	    return formula.getMSMSScore();
	}
	return null;
    }

    public ResultFormula getFormula(int row) {
	return formulas.get(row);
    }

    public boolean isCellEditable(int row, int col) {
	return false;
    }

    public void addElement(ResultFormula formula) {
	formulas.add(formula);
	fireTableRowsInserted(formulas.size() - 1, formulas.size() - 1);
    }

}
