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
package net.sf.mzmine.visualizers.alignmentresult.table;

import java.util.Vector;

import javax.swing.JTable;
import javax.swing.JPopupMenu;
import javax.swing.table.AbstractTableModel;

import net.sf.mzmine.data.AlignmentResult;
import net.sf.mzmine.data.AlignmentResultRow;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.Peak;
import net.sf.mzmine.data.Peak.PeakStatus;
import net.sf.mzmine.io.OpenedRawDataFile;
import net.sf.mzmine.methods.deisotoping.util.IsotopePatternUtility;
import sunutils.TableSorter;

public class AlignmentResultTable extends JTable {


	private AlignmentResult alignmentResult;
	private MyTableModel tableModel;
	private TableSorter sorter;


	public AlignmentResultTable(AlignmentResultTableVisualizerWindow masterFrame, AlignmentResult alignmentResult) {
		this.alignmentResult = alignmentResult;
		initializeTableModel(new AlignmentResultTableColumnSelection());

	}


	private void initializeTableModel(AlignmentResultTableColumnSelection columnSelection) {

		tableModel = new MyTableModel(alignmentResult, columnSelection);

		// Initialize sorter
		sorter = new TableSorter(tableModel);
		getTableHeader().setReorderingAllowed(false);
		sorter.addMouseListenerToHeaderInTable(this);
		setModel(sorter);

	}






	/**
	 * This class is a slighty customized table model for JTable presenting the alignment results
	 */
	private class MyTableModel extends AbstractTableModel {

		private AlignmentResult alignmentResult;
		private AlignmentResultTableColumnSelection columnSelection;

		private IsotopePatternUtility isoUtil;


		/**
		 * Constructor, assign given dataset to this table
		 */
		public MyTableModel(AlignmentResult alignmentResult, AlignmentResultTableColumnSelection columnSelection) {
			this.alignmentResult = alignmentResult;
			this.columnSelection = columnSelection;

			isoUtil = new IsotopePatternUtility(alignmentResult);

		}

		public AlignmentResultTableColumnSelection getColumnSelection() {
			return columnSelection;
		}

		public int getColumnCount() {
			return columnSelection.getNumberOfCommonColumns() + alignmentResult.getNumberOfRawDataFiles()*columnSelection.getNumberOfRawDataColumns();
		}

		public int getRowCount() {
			return alignmentResult.getNumberOfRows();
		}


		public String getColumnName(int col) {

			int[] groupOffset = getColumnGroupAndOffset(col);

			// Common column
			if (groupOffset[0]<0) {
				return columnSelection.getSelectedCommonColumn(groupOffset[1]).getColumnName();
			}

			if (groupOffset[0]>=0) {
				OpenedRawDataFile rawData = alignmentResult.getRawDataFile(groupOffset[0]);
				String rawDataName = rawData.toString();
				return rawDataName + ": " + columnSelection.getSelectedRawDataColumn(groupOffset[1]).getColumnName();
			}

			return new String("No Name");

		}


		/**
		 * This method returns the value at given coordinates of the dataset or null if it is a missing value
		 */
		private String peakStatusDetected = new String("Found");
		private String peakStatusEstimated = new String("Estimated");
		private String peakStatusNotFound = new String("Not Found");

		public Object getValueAt(int row, int col) {



			int[] groupOffset = getColumnGroupAndOffset(col);



			// Common column
			if (groupOffset[0]<0) {

				AlignmentResultRow alignmentRow = alignmentResult.getRow(row);

				switch(columnSelection.getSelectedCommonColumn(groupOffset[1])) {
					case ROWNUM:
						return new Integer(row+1);
					case AVGMZ:
						return new Double(alignmentRow.getAverageMZ());
					case AVGRT:
						return new Double(alignmentRow.getAverageRT());
					case ISOTOPEID:
						return new Integer(isoUtil.getIsotopePatternNumber(alignmentRow.getIsotopePattern()));
					case ISOTOPEPEAK:
						return new Integer(isoUtil.getIsotopePatternNumber(alignmentRow.getIsotopePattern()));
					case CHARGE:
						return new Integer(alignmentRow.getIsotopePattern().getChargeState());
					default:
						//System.out.println("Illegal common column");
						return null;
				}

			}

			else { //if (groupOffset[0]>=0)

				OpenedRawDataFile rawData = alignmentResult.getRawDataFile(groupOffset[0]);
				Peak p = alignmentResult.getPeak(row, rawData);
				if (p==null) return null;

				switch(columnSelection.getSelectedRawDataColumn(groupOffset[1])) {
					case MZ:
						return new Double(p.getNormalizedMZ());
					case RT:
						return new Double(p.getNormalizedRT());
					case HEIGHT:
						return new Double(p.getNormalizedHeight());
					case AREA:
						return new Double(p.getNormalizedArea());
					default:
						//System.out.println("Illegal raw data column");
						return null;
				}

			}

		}


		/**
		 * This method returns the class of the objects in this column of the table
		 */
		public Class getColumnClass(int col) {


			int[] groupOffset = getColumnGroupAndOffset(col);

			// Common column
			if (groupOffset[0]<0) {
				return columnSelection.getSelectedCommonColumn(groupOffset[1]).getColumnClass();
			} else { //if (groupOffset[0]>=0)
				return columnSelection.getSelectedRawDataColumn(groupOffset[1]).getColumnClass();
			}

		}

		private int[] getColumnGroupAndOffset(int col) {

			// Is this a common column?
			if (col<columnSelection.getNumberOfCommonColumns()) {
				int[] res = new int[2];
				res[0] = -1;
				res[1] = col;
				return res;
			}

			// This is a raw data specific column.

			// Calc number of raw data
			int[] res = new int[2];
			res[0] = (int)java.lang.Math.floor( (double)(col-columnSelection.getNumberOfCommonColumns()) / (double)columnSelection.getNumberOfRawDataColumns() );
			res[1] = col - columnSelection.getNumberOfCommonColumns() - res[0] * columnSelection.getNumberOfRawDataColumns();

			return res;

		}



		public boolean isCellEditable(int row, int col) { return false;	}

		public void setValueAt(Object value, int row, int col) {}


	}

}