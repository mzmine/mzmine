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
import javax.swing.ImageIcon;
import javax.swing.JTable;
import javax.swing.RowSorter;
import javax.swing.table.AbstractTableModel;

public class ResultTableModel extends AbstractTableModel {

  private static final long serialVersionUID = 1L;
  private static final int PREVIEW_HEIGHT = 100;
  private static final int PREVIEW_WIDTH = 150;

  private static final String[] columnNames = {"Name",
      "Formula", "DBs", "Sirius score", "FingerId Score", "Chemical structure"};

  private final JTable table;
  private final NumberFormat percentFormat = NumberFormat.getPercentInstance();
  private Vector<SiriusCompound> compounds = new Vector<SiriusCompound>();

  ResultTableModel(JTable table) {
    this.table = table;
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

  @Override
  public Class getColumnClass(int column) {
    switch (column) {
      case 5:
        return ImageIcon.class;
//      case 4:
//      case 3:
//        return Double.class;
      default:
        return String.class;
    }
  }

  public Object getValueAt(int row, int col) {
    Object value = null;
    SiriusCompound compound = compounds.get(row);
    switch (col) {
      case 0:
        String name = compound.getAnnotationDescription();
        value = name;
        break;
      case 1:
        value = compound.getStringFormula();
        break;
      case 2:
        value = compound.getDBS();
        break;
      case 3:
        value = compound.getSiriusScore();
        break;
      case 4:
        value = compound.getFingerIdScore();
        break;
      case 5:
        value = compound.getStructureImage(PREVIEW_WIDTH, PREVIEW_HEIGHT);
        if (value != null) {
          table.getColumnModel().getColumn(5).setWidth(PREVIEW_WIDTH);
          table.setRowHeight(row, PREVIEW_HEIGHT);
        }
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
