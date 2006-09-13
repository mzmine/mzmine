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


import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

import net.sf.mzmine.data.IsotopePattern;
import net.sf.mzmine.data.Peak;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.DataUnit;
import net.sf.mzmine.io.OpenedRawDataFile;
import net.sf.mzmine.methods.deisotoping.util.IsotopePatternUtility;
import net.sf.mzmine.project.MZmineProject;
import net.sf.mzmine.util.GUIUtils;
import sunutils.TableSorter;


public class PeakListTable extends JTable {

	private PeakList peakList;
	private TableSorter sorter;

	protected PeakListTable(PeakListTableViewWindow masterFrame, OpenedRawDataFile rawData) {

		DataUnit[] peakLists = (DataUnit[])rawData.getCurrentFile().getData(PeakList.class);
		peakList = (PeakList)peakLists[peakLists.length-1];

		if (peakList!=null) {
			AbstractTableModel mtm = new MyTableModel(peakList);

			// Initialize sorter
			sorter = new TableSorter(mtm);
			getTableHeader().setReorderingAllowed(false);
			sorter.addMouseListenerToHeaderInTable(this);
			setModel(sorter);

			// Setup popup menu
			JPopupMenu popupMenu = new JPopupMenu();
			popupMenu.addSeparator();
			GUIUtils.addMenuItem(popupMenu, "Zoom visualizers to peak", masterFrame, "ZOOM_TO_PEAK");
			GUIUtils.addMenuItem(popupMenu, "Find peak in alignments", masterFrame, "FIND_IN_ALIGNMENTS");
			popupMenu.addSeparator();
			setComponentPopupMenu(popupMenu);

		} else {
			//TODO (No peak list available: display nothing or throw error?
		}

	}

	protected Peak getSelectedPeak() {


		int row = getSelectedRow();
		if (row<0) return null;
		int unsortedrow = sorter.getUnsortedRowIndex(row);
		Peak p = peakList.getPeak(unsortedrow);
		return p;
	}

	protected void setSelectedPeak(Peak p) {

		int unsortedrow = peakList.indexOf(p);
		if (unsortedrow<0) return;
		int row = sorter.getSortedRowIndex(unsortedrow);
		setRowSelectionInterval(row,row);
		scrollRectToVisible(getCellRect(row, 0, true));

	}




	private class MyTableModel extends AbstractTableModel {

		private final String[] columnNames = {
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
														"Isotope Peak #",
														"Charge State",
														"Identification result(s)"
													};

		private final String unassignedValue = new String("N/A");

		private PeakList peakList;
		private IsotopePatternUtility isotopePatternUtility;

		public MyTableModel(PeakList peakList) {
			this.peakList = peakList;
			isotopePatternUtility = new IsotopePatternUtility(peakList);
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

			Peak p = peakList.getPeak(row);

			// Isotope & charge columns
			if (col == 0) return p.getRawMZ();
			if (col == 1) return p.getRawRT();
			if (col == 2) return p.getRawHeight();
			if (col == 3) return p.getRawArea();

			if (col == 4) return p.getMaxRT() - p.getMinRT();
			if (col == 5) return p.getMaxMZ() - p.getMinMZ();

			if (col == 6) return p.getNormalizedMZ();
			if (col == 7) return p.getNormalizedRT();
			if (col == 8) return p.getNormalizedHeight();
			if (col == 9) return p.getNormalizedArea();

			if (col == 10) {
				if (!p.hasData(IsotopePattern.class)) return unassignedValue;
				IsotopePattern isotopePattern = (IsotopePattern)p.getData(IsotopePattern.class)[0];
				return isotopePatternUtility.getIsotopePatternNumber(isotopePattern);
			}
			if (col == 11) {
				if (!p.hasData(IsotopePattern.class)) return unassignedValue;
				return isotopePatternUtility.getPeakNumberWithinPattern(p);
			}
			if (col == 12) {
				if (!p.hasData(IsotopePattern.class)) return unassignedValue;
				IsotopePattern isotopePattern = (IsotopePattern)p.getData(IsotopePattern.class)[0];
				return isotopePattern.getChargeState();
			}

			return unassignedValue;
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



}