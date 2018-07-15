/*
 * Copyright 2006-2018 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.peaklistmethods.identification.sirius;

import java.text.NumberFormat;
import java.util.Vector;
import javax.swing.table.AbstractTableModel;
import net.sf.mzmine.datamodel.PeakIdentity;

public class ResultTableModel extends AbstractTableModel {

  private static final long serialVersionUID = 1L;

  private static final String[] columnNames = {"ID", "Name",
      "Formula", "SMILES", "Inchi", "DBs", "Sirius score", "FingerId Score"};

  private final NumberFormat percentFormat = NumberFormat.getPercentInstance();
  private Vector<SiriusCompound> compounds = new Vector<SiriusCompound>();

  //TODO: todo
  ResultTableModel() {
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

  //TODO: todo
  public Object getValueAt(int row, int col) {
    Object value = null;
    SiriusCompound compound = compounds.get(row);
    switch (col) {
      case 0:
        value = compound.getPropertyValue(PeakIdentity.PROPERTY_ID);
        break;
      case 1:
        String name = compound.getAnnotationDescription();
        value = name;
        break;
      case 2:
        value = compound.getStringFormula();
        break;
      case 3:
        value = compound.getSMILES();
        break;
      case 4:
        value = compound.getInchi();
        break;
      case 5:
        value = compound.getDBS();
        break;
      case 6:
        value = compound.getSiriusScore();
        break;
      case 7:
        value = compound.getFingerIdScore();
        break;
    }

    return value;
  }

  public void addElement(SiriusCompound compound) {
    compounds.add(compound);
    fireTableRowsInserted(compounds.size() - 1, compounds.size() - 1);
  }


  public boolean isCellEditable(int row, int col) {
    return false;
  }

  //TODO: todo
  public void setValueAt(Object value, int row, int col) {
  }

  public SiriusCompound getCompoundAt(int row) {
    return compounds.get(row);
  }
}
