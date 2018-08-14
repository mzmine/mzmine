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

package net.sf.mzmine.modules.peaklistmethods.identification.onlinedbsearch;

import java.text.NumberFormat;
import java.util.Vector;

import javax.swing.table.AbstractTableModel;

import net.sf.mzmine.datamodel.PeakIdentity;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.util.FormulaUtils;

public class ResultTableModel extends AbstractTableModel {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private static final String[] columnNames = { "ID", "Common Name",
	    "Formula", "Mass difference", "Isotope pattern score" };

    private double searchedMass;
    private Vector<DBCompound> compounds = new Vector<DBCompound>();

    private final NumberFormat percentFormat = NumberFormat
	    .getPercentInstance();
    private final NumberFormat massFormat = MZmineCore.getConfiguration()
	    .getMZFormat();

    ResultTableModel(double searchedMass) {
	this.searchedMass = searchedMass;
	percentFormat.setMaximumFractionDigits(1);
    }

    public String getColumnName(int col) {
	return columnNames[col];
    }

    public int getRowCount() {
	return compounds.size();
    }

    public int getColumnCount() {
	return columnNames.length;
    }

    public Object getValueAt(int row, int col) {
	Object value = null;
	DBCompound comp = compounds.get(row);
	switch (col) {
	case (0):

	    value = comp.getPropertyValue(PeakIdentity.PROPERTY_ID);
	    break;
	case (1):
	    value = comp.getName();
	    break;
	case (2):
	    value = comp.getPropertyValue(PeakIdentity.PROPERTY_FORMULA);
	    break;
	case (3):
	    String compFormula = comp
		    .getPropertyValue(PeakIdentity.PROPERTY_FORMULA);
	    if (compFormula != null) {
		double compMass = FormulaUtils.calculateExactMass(compFormula);
		double massDifference = Math.abs(searchedMass - compMass);
		value = massFormat.format(massDifference);
	    }
	    break;
	case (4):
	    Double score = comp.getIsotopePatternScore();
	    if (score != null)
		value = percentFormat.format(score);
	    break;
	}

	return value;
    }

    public DBCompound getCompoundAt(int row) {
	return compounds.get(row);
    }

    public boolean isCellEditable(int row, int col) {
	return false;
    }

    public void addElement(DBCompound compound) {
			compound.getAllProperties();

			compounds.add(compound);
	fireTableRowsInserted(compounds.size() - 1, compounds.size() - 1);
    }

    public void setValueAt(Object value, int row, int col) {
    }

}
