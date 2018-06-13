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
import javax.swing.table.AbstractTableModel;
import net.sf.mzmine.main.MZmineCore;

public class ResultTableModel extends AbstractTableModel {
  private static final long serialVersionUID = 1L;

  private static final String[] columnNames = {"ID", "Common Name",
      "Formula", "Mass difference", "Isotope pattern score"};

  private double searchedMass;

  private final NumberFormat percentFormat = NumberFormat
      .getPercentInstance();
  private final NumberFormat massFormat = MZmineCore.getConfiguration()
      .getMZFormat();


  //TODO: todo
  ResultTableModel(double searchedMass) {
    this.searchedMass = searchedMass;
    percentFormat.setMaximumFractionDigits(1);
  }

  public String getColumnName(int col) {
    return columnNames[col];
  }

  //TODO: todo
  public int getRowCount() {
    return 0;
  }

  //TODO: todo
  public int getColumnCount() {
    return columnNames.length;
  }

  //TODO: todo
  public Object getValueAt(int row, int col) {
    Object value = null;

    return value;
  }


  //TODO: todo
  public boolean isCellEditable(int row, int col) {
    return false;
  }

  //TODO: todo
  public void setValueAt(Object value, int row, int col) {
  }
}
