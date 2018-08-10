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

package net.sf.mzmine.modules.peaklistmethods.identification.sirius.table;

import java.text.NumberFormat;
import java.util.Vector;
import javax.swing.ImageIcon;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import net.sf.mzmine.main.MZmineCore;

public class ResultTableModel extends AbstractTableModel {

  private static final long serialVersionUID = 1L;
  private static final String[] columnNames = {"Name",
      "Formula", "DBs", "Sirius score", "FingerId Score", "Chemical structure"};
  public static final int NAME_INDEX = 0;
  public static final int FORMULA_INDEX = 1;
  public static final int DBS_INDEX = 2;
  public static final int SIRIUS_SCORE_INDEX = 3;
  public static final int FINGERID_SCORE_INDEX = 4;
  public static final int PREVIEW_INDEX = 5;


  private final NumberFormat mzFormat = MZmineCore.getConfiguration().getMZFormat();
  private Vector<SiriusCompound> compounds = new Vector<SiriusCompound>();

  // JTable object is used to resize the row height to show image
  private JTable table;

  public ResultTableModel() {
    mzFormat.setMaximumFractionDigits(1);
  }

  public void setTable(JTable table) {
    this.table = table;
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

  /**
   * There was used Double.class for score fields, but it did not work properly
   * Now it processes Double values from String
   * @param column
   * @return
   */
  @Override
  public Class getColumnClass(int column) {
    switch (column) {
      case DBS_INDEX: // Multiline content cell
        return String[].class;
      case PREVIEW_INDEX: // Cell with image
        return ImageIcon.class;
      default:
        return String.class;
    }
  }

  /**
   * Returns an object from a row by column
   * @param row index
   * @param col index
   * @return Object from a SiriusCompound
   */
  @Override
  public Object getValueAt(int row, int col) {
    Object value = null;
    SiriusCompound compound = compounds.get(row);
    switch (col) {
      case NAME_INDEX:
        String name = compound.getAnnotationDescription();
        value = name;
        break;
      case FORMULA_INDEX:
        value = compound.getStringFormula();
        break;
      case DBS_INDEX:
        value = compound.getDBS();
        break;
      case SIRIUS_SCORE_INDEX:
        value = compound.getSiriusScore();
        break;
      case FINGERID_SCORE_INDEX:
        value = compound.getFingerIdScore();
        break;
      case PREVIEW_INDEX:
        value = compound.getPreview();
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

  public void setValueAt(Object value, int row, int col) {

  }

  public SiriusCompound getCompoundAt(int row) {
    return compounds.get(row);
  }
}
