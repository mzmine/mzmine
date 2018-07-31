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

import java.util.Comparator;
import javax.swing.table.TableRowSorter;

public class ResultTableSorter extends TableRowSorter<ResultTableModel> {

  public ResultTableSorter(ResultTableModel model) {
    super(model);

    /**
     * Sets a comparator for sirius results
     * Uses a trick with parsing Strings
     */
    this.setComparator(ResultTableModel.SIRIUS_SCORE_INDEX, new Comparator<String>() {
      @Override
      public int compare(String s1, String s2) {
        if (s1 == null && s2 == null)
          return 0;
        if (s1 == null)
          return 1;
        if (s2 == null)
          return -1;

        Double d1 = Double.parseDouble(s1);
        Double d2 = Double.parseDouble(s2);
        return d1.compareTo(d2);
      }
    });

    /**
     * Sets a comparator for finger id results
     * Uses a trick with parsing Strings
     */
    this.setComparator(ResultTableModel.FINGERID_SCORE_INDEX, new Comparator<String>() {
      @Override
      public int compare(String s1, String s2) {
        if (s1.equals("") && s2.equals(""))
          return 0;
        if (s1.equals(""))
          return 1;
        if (s2.equals(""))
          return -1;

        Double d1 = Double.parseDouble(s1);
        Double d2 = Double.parseDouble(s2);
        return d2.compareTo(d1);
      }
    });
    toggleSortOrder(ResultTableModel.FINGERID_SCORE_INDEX);
  }
}