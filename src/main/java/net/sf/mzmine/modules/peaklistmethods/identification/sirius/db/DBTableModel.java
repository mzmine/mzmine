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

package net.sf.mzmine.modules.peaklistmethods.identification.sirius.db;

import de.unijena.bioinf.chemdb.DBLink;
import io.github.msdk.id.sirius.SiriusIonAnnotation;
import java.util.Vector;
import javax.swing.table.AbstractTableModel;
import net.sf.mzmine.modules.peaklistmethods.identification.sirius.SiriusCompound;

public class DBTableModel extends AbstractTableModel {
  private static final String[] columnNames = {"Database", "Index"};
  public static final int DB_NAME = 0;
  public static final int DB_INDEX = 1;

  private Vector<DBCompound> compounds = new Vector<DBCompound>();

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
   * Returns an object from a row by column
   * @param row index
   * @param col index
   * @return Object from a SiriusCompound
   */
  @Override
  public Object getValueAt(int row, int col) {
    Object value = null;
    DBCompound compound = compounds.get(row);
    switch (col) {
      case DB_NAME:
        value = compound.getDB();
        break;
      case DB_INDEX:
        value = compound.getID();
        break;
    }

    return value;
  }

  public void addElement(SiriusCompound compound) {
    SiriusIonAnnotation annotation = compound.getIonAnnotation();
    for (DBLink link: annotation.getDBLinks()) {
      compounds.add(new DBCompound(link.name, link.id));
      fireTableRowsInserted(compounds.size() - 1, compounds.size() - 1);
    }
  }

  public boolean isCellEditable(int row, int col) {
    return false;
  }

  public void setValueAt(Object value, int row, int col) {
  }

  public DBCompound getCompoundAt(int row) {
    return compounds.get(row);
  }
}
