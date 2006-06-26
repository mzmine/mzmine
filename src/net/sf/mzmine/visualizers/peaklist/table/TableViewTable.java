/*
 * Copyright 2006 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * MZmine; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.visualizers.peaklist.table;


import javax.swing.JTable;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import javax.swing.table.AbstractTableModel;

import net.sf.mzmine.interfaces.PeakList;
import net.sf.mzmine.util.IsotopePatternGrouper;
import net.sf.mzmine.io.RawDataFile;


public class TableViewTable extends JTable {

	public TableViewTable(RawDataFile rawData) {
	}


	/*
    private class SelectionListener implements ListSelectionListener {

        TableViewTable table;

        // It is necessary to keep the table since it is not possible
        // to determine the table from the event's source
        SelectionListener(TableViewTable table) {

            this.table = table;

        }

        public void valueChanged(ListSelectionEvent e) {

			if (!e.getValueIsAdjusting()) {

				int row = table.getSelectedRow();
				if (row!=-1) {
				}

            }

        }

    }
	*/



	/*
	private class MyTableModel extends AbstractTableModel {

		private static final String[] columnNames = {
														"M/Z",
														"RT",
														"Height",
														"Area",
														"Duration",
														"M/Z difference",
														"Norm. M/Z",
														"Norm. RT",
														"Norm. Height",
														"Norm. Area",
														"Isotope Pattern #",
														"Charge State",
														"Identification result(s)"
													};

		private static final String unassignedValue = new String("N/A");

		private PeakList peakList;
		private IsotopePatternGrouper isotopePatternGrouper;

		public MyTableModel(PeakList peakList) {
			this.peakList = peakList;
			isotopePatternGrouper = new IsotopePatternGrouper(peakList);
		}

		public int getColumnCount() {
			return columnNames.length;
		}

		public int getRowCount() {
			return peakList.getNumberOfPeaks();
		}

		public String getColumnName(int col) {
			return columnNames[col];
		}

		public Object getValueAt(int row, int col) {

			if (row<0) return null;
			if (row>peakList.getNumberOfPeaks()) return null;
			if (col<0) return null;
			if (col>columnNames.length) return null;

			Peak p = peakList.get(row);

			// Isotope & charge columns
			if (col == 0) return p.getMZ();
			if (col == 1) return p.getRT();
			if (col == 2) return p.getHeight();
			if (col == 3) return p.getArea();

			if (col == 4) return p.getMaxRT() - p.getMinRT();
			if (col == 5) return p.getMaxMZ() - p.getMinMZ();

			if (col == 6) return p.getNormalizedMZ();
			if (col == 7) return p.getNormalizedRT();
			if (col == 8) return p.getNormalizedHeight();
			if (col == 9) return p.getNormalizedArea();

			if (col == 10) {
				IsotopePattern isotopePattern = p.getIsotopePattern();
				if (isotopePattern = null) return unassignedValue;
				return isotopePatternGrouper.getIsotopePatternNumber(isotopePattern);
			}
			if (col == 11) {
				IsotopePattern isotopePattern = p.getIsotopePattern();
				if (isotopePattern = null) return unassignedValue;
				return isotopePattern.getChargeState();

			}
			// Identification column

			if (col == COL_CHARGE) { if (((Integer)(data[row][col]))<0) { return unassignedValue; } }
			if (col == COL_ISOTOPEPATTERNID) { if (((Integer)(data[row][col]))<0) { return unassignedValue; } }

			return data[row][col];
		}

		public Class getColumnClass(int c) {
			return getValueAt(0, c).getClass();
		}

		public boolean isCellEditable(int row, int col) {
			return false;
		}
		public void setValueAt(Object value, int row, int col) {
			return;
		}

	}
	*/


}