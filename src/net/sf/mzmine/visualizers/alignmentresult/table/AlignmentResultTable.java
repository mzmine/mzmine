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
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.io.OpenedRawDataFile;
import net.sf.mzmine.methods.deisotoping.util.IsotopePatternUtility;
import sunutils.TableSorter;

public class AlignmentResultTable extends JTable {

	// These constants are used for calculating table's column layout in two different modes
	private final int COLUMNMODE_COMPACT = 1;
	private final int COLUMNMODE_WIDE = 2;


	private AlignmentResult alignmentResult;
	private TableSorter sorter;

	private int columnMode;


	public AlignmentResultTable(AlignmentResultTableVisualizerWindow masterFrame, AlignmentResult alignmentResult) {
		this.alignmentResult = alignmentResult;

		AbstractTableModel mtm = new MyTableModel(alignmentResult);

		// Initialize sorter
		sorter = new TableSorter(mtm);
		getTableHeader().setReorderingAllowed(false);
		sorter.addMouseListenerToHeaderInTable(this);
		setModel(sorter);

	}

	private void switchColumnMode() {
		if (columnMode == COLUMNMODE_COMPACT) {
			setColumnMode(COLUMNMODE_WIDE);
		} else {
			setColumnMode(COLUMNMODE_COMPACT);
		}
	}

	private void setColumnMode(int columnMode) {
		this.columnMode = columnMode;

		/*
		AbstractTableModel mtm = new MyTableModel(alignmentResult);
		TableSorter sorter = new TableSorter(mtm); //ADDED THIS
		table.getTableHeader().setReorderingAllowed(false);
		//sorter.setTableHeader(table.getTableHeader()); //ADDED THIS	(REMOVED FOR TEST)
		sorter.addMouseListenerToHeaderInTable(table); // ADDED THIS TODAY
		table.setModel(sorter);
		*/
	}






	/**
	 * This class is a slighty customized table model for JTable presenting the alignment results
	 */
	class MyTableModel extends AbstractTableModel {

		private final int COLINFO_COMPACTMODE_IND = 0;
		private final int COLINFO_COMPACTMODE_MZ = 1;
		private final int COLINFO_COMPACTMODE_RT = 2;
		private final int COLINFO_COMPACTMODE_ISOTOPEPATTERNID = 3;
		private final int COLINFO_COMPACTMODE_ISOTOPEPEAKNUMBER = 4;
		private final int COLINFO_COMPACTMODE_CHARGESTATE = 5;
		private final int COLINFO_COMPACTMODE_COLSPERRUN = 1;
		private final int COLINFO_COMPACTMODE_LASTCOMMONCOL = COLINFO_COMPACTMODE_CHARGESTATE;

		private final int COLINFO_WIDEMODE_IND = 0;
		private final int COLINFO_WIDEMODE_ISOTOPEPATTERNID = 1;
		private final int COLINFO_WIDEMODE_ISOTOPEPEAKNUMBER = 2;
		private final int COLINFO_WIDEMODE_CHARGESTATE = 3;
		private final int COLINFO_WIDEMODE_COLSPERRUN = 5;
		private final int COLINFO_WIDEMODE_LASTCOMMONCOL = COLINFO_WIDEMODE_CHARGESTATE;

		private String[] columnNames;
		private AlignmentResult alignmentResult;
		private IsotopePatternUtility isoUtil;
		private Vector<String> rawDataNames;
		private Vector<IsotopePatternUtility> isotopePatternUtils;

		/**
		 * Constructor, assign given dataset to this table
		 */
		public MyTableModel(AlignmentResult alignmentResult) {

			this.alignmentResult = alignmentResult;
			columnNames = getColumnNames();

			// Collect names of raw data files (for column headers) and construct isotope pattern utilities for each peak list (for getting isotope pattern information to table)
			OpenedRawDataFile[] openedRawDataFiles = alignmentResult.getRawDataFiles();
			rawDataNames = new Vector<String>();
			isotopePatternUtils = new Vector<IsotopePatternUtility>();

			for (OpenedRawDataFile openedRawDataFile : openedRawDataFiles) {
				PeakList peakList = (PeakList)openedRawDataFile.getCurrentFile().getLastData(PeakList.class);
				rawDataNames.add(openedRawDataFile.toString());
				isotopePatternUtils.add(new IsotopePatternUtility(peakList));
			}

		}

		public int getColumnCount() {

			if (columnMode == COLUMNMODE_WIDE) {
				return COLINFO_WIDEMODE_LASTCOMMONCOL+COLINFO_WIDEMODE_COLSPERRUN*alignmentResult.getNumberOfRawDataFiles()+1;
			}

			if (columnMode == COLUMNMODE_COMPACT) {
				return COLINFO_COMPACTMODE_LASTCOMMONCOL+COLINFO_COMPACTMODE_COLSPERRUN*alignmentResult.getNumberOfRawDataFiles()+1;
			}

			return 0;

		}

		public int getRowCount() {
			return alignmentResult.getNumberOfRows();
		}



		public String getColumnName(int col) {
			return columnNames[col];
		}


		/**
		 * This method returns the value at given coordinates of the dataset or null if it is a missing value
		 */
		private String peakStatusDetected = new String("Found");
		private String peakStatusEstimated = new String("Estimated");
		private String peakStatusNotFound = new String("Not Found");

		public Object getValueAt(int row, int col) {
/*
			// First column is index numbering
			if (col==0) { return new Integer(row+1); }

			// Second column is the standard flag
			if (col==1) { return new Boolean(alignmentResult.getStandardCompoundFlag(row)); }

			int numOfRawDatas = alignmentResult.getNumberOfRawDataFiles();

			if (!compactMode) {

				if (col==COLINFO_WIDEMODE_IND) { return new Integer(row+1); }
				if (col==COLINFO_WIDEMODE_ISOTOPEPATTERNID) { return new Integer(alignmentResult.getIsotopePatternID(row)); }
				if (col==COLINFO_WIDEMODE_ISOTOPEPEAKNUMBER) { return new Integer(alignmentResult.getIsotopePeakNumber(row)); }
				if (col==COLINFO_WIDEMODE_CHARGESTATE) { return new Integer(alignmentResult.getChargeState(row)); }

				if ((col >COLINFO_WIDEMODE_LASTCOMMONCOL) &&
					(col<=(COLINFO_WIDEMODE_LASTCOMMONCOL+COLINFO_WIDEMODE_COLSPERRUN*numOfRawDatas)) ) {

					// Calc runNumber and offset from the first column of this run
					int columnGroupNumber = calcColumnGroupNum(col);
					int columnGroupOffset = col - (COLINFO_WIDEMODE_LASTCOMMONCOL+1) - columnGroupNumber*COLINFO_WIDEMODE_COLSPERRUN;
					int rawDataID = alignmentResult.getRawDataID(columnGroupNumber);

					// mz, rt or intensity?
					if ( (columnGroupOffset>=0) && (columnGroupOffset<=3) ) {
						double preValue = 0;
						if (columnGroupOffset==0) { preValue = alignmentResult.getPeakMZ(rawDataID, row); }
						if (columnGroupOffset==1) { preValue = alignmentResult.getPeakRT(rawDataID, row); }
						if (columnGroupOffset==2) { preValue = alignmentResult.getPeakHeight(rawDataID, row); }
						if (columnGroupOffset==3) { preValue = alignmentResult.getPeakArea(rawDataID, row); }
						if (preValue<0) { return null; } else { return new Double(preValue); }
					}

					int statValue=0;
					if (columnGroupOffset==4) {
						statValue = alignmentResult.getPeakStatus(rawDataID, row);

						if (statValue==AlignmentResult.PEAKSTATUS_DETECTED) { return peakStatusDetected; }
						if (statValue==AlignmentResult.PEAKSTATUS_ESTIMATED) { return peakStatusEstimated; }
						if (statValue==AlignmentResult.PEAKSTATUS_NOTFOUND) { return peakStatusNotFound; }
					}

				}

			} else {

				if (col==COLINFO_COMPACTMODE_IND) { return new Integer(row+1); }
				if (col==COLINFO_COMPACTMODE_STD) { return new Boolean(alignmentResult.getStandardCompoundFlag(row)); }
				if (col==COLINFO_COMPACTMODE_ISOTOPEPATTERNID) { return new Integer(alignmentResult.getIsotopePatternID(row)); }
				if (col==COLINFO_COMPACTMODE_ISOTOPEPEAKNUMBER) { return new Integer(alignmentResult.getIsotopePeakNumber(row)); }
				if (col==COLINFO_COMPACTMODE_CHARGESTATE) {
					return new Integer(alignmentResult.getChargeState(row));
				}



				if (col==COLINFO_COMPACTMODE_MZ) {
					double preValue = alignmentResult.getAverageMZ(row);
					if (preValue>=0) { return new Double(preValue); } else { return null; }
				}

				if (col==COLINFO_COMPACTMODE_RT) {
					double preValue = alignmentResult.getAverageRT(row);
					if (preValue>=0) { return new Double(preValue); } else { return null; }
				}

				if ((col >COLINFO_COMPACTMODE_LASTCOMMONCOL) &&
					(col<=(COLINFO_COMPACTMODE_LASTCOMMONCOL+COLINFO_COMPACTMODE_COLSPERRUN*numOfRawDatas)) ) {

					int columnGroupNumber = calcColumnGroupNum(col);
					int columnGroupOffset = col - (COLINFO_COMPACTMODE_LASTCOMMONCOL+1) - columnGroupNumber*COLINFO_COMPACTMODE_COLSPERRUN;
					int rawDataID = alignmentResult.getRawDataID(columnGroupNumber);

					double preValue = -1;
					if (columnGroupOffset==0) {
						//if (mainWin.getParameterStorage().getGeneralParameters().getPeakMeasuringType() == GeneralParameters.PARAMETERVALUE_PEAKMEASURING_HEIGHT) { preValue = alignmentResult.getPeakHeight(rawDataID, row); }
						//if (mainWin.getParameterStorage().getGeneralParameters().getPeakMeasuringType() == GeneralParameters.PARAMETERVALUE_PEAKMEASURING_AREA) { preValue = alignmentResult.getPeakArea(rawDataID, row); }
					}
					if (preValue<0) { return null; } else {	return new Double(preValue); }
				}

			}
*/
			return null;

		}


		/**
		 * This method returns the class of the objects in this column of the table
		 */
		public Class getColumnClass(int col) {

			int numOfRuns = 0; //alignmentResult.getNumOfRawDatas();

			if (columnMode == COLUMNMODE_WIDE) {

				if (col==COLINFO_WIDEMODE_IND) { return Integer.class; }
				if (col==COLINFO_WIDEMODE_ISOTOPEPATTERNID) { return Integer.class; }
				if (col==COLINFO_WIDEMODE_ISOTOPEPEAKNUMBER) { return Integer.class; }
				if (col==COLINFO_WIDEMODE_CHARGESTATE) { return Integer.class; }

				if ((col >COLINFO_WIDEMODE_LASTCOMMONCOL) &&
					(col<=(COLINFO_WIDEMODE_LASTCOMMONCOL+COLINFO_WIDEMODE_COLSPERRUN*numOfRuns)) ) {
						// Calc runNumber and offset from the first column of this run
						int columnGroupNumber = 0; //calcColumnGroupNum(col);
						int columnGroupOffset = col - (COLINFO_WIDEMODE_LASTCOMMONCOL+1) - columnGroupNumber*COLINFO_WIDEMODE_COLSPERRUN;

						// mz, rt, height, area or status?
						if (columnGroupOffset==0) { return Double.class; }
						if (columnGroupOffset==1) { return Double.class; }
						if (columnGroupOffset==2) { return Double.class; }
						if (columnGroupOffset==3) { return Double.class; }
						if (columnGroupOffset==4) { return String.class; }
				}

			} else {

				if (col==COLINFO_COMPACTMODE_IND) { return Integer.class; }
				if (col==COLINFO_COMPACTMODE_MZ) { return Double.class; }
				if (col==COLINFO_COMPACTMODE_RT) { return Double.class; }
				if (col==COLINFO_COMPACTMODE_ISOTOPEPATTERNID) { return Integer.class; }
				if (col==COLINFO_COMPACTMODE_ISOTOPEPEAKNUMBER) { return Integer.class; }
				if (col==COLINFO_COMPACTMODE_CHARGESTATE) { return Integer.class; }


				if ((col >COLINFO_COMPACTMODE_LASTCOMMONCOL) &&
					(col<=(COLINFO_COMPACTMODE_LASTCOMMONCOL+COLINFO_COMPACTMODE_COLSPERRUN*numOfRuns)) ) {

					int columnGroupNumber = 0; //calcColumnGroupNum(col);
					int columnGroupOffset = col - (COLINFO_COMPACTMODE_LASTCOMMONCOL+1) - columnGroupNumber*COLINFO_COMPACTMODE_COLSPERRUN;

					// Only one column per each run (height or area)
					if (columnGroupOffset==0) { return Double.class; }
				}

			}
			return null;
		}

		public boolean isCellEditable(int row, int col) {
			return false;
		}

		public void setValueAt(Object value, int row, int col) {}


		private String[] getColumnNames() {

			String[] s = new String[getColumnCount()];
/*
			if (!compactMode) {

				s[0] = new String("ID");
				s[1] = new String("Std");
				s[2] = new String("Isotope Pattern ID");
				s[3] = new String("Isotope Peak Number");
				s[4] = new String("Charge State");


				int i=0;
				int[] rawDataIDs = alignmentResult.getRawDataIDs();
				for (int rawDataID : rawDataIDs) {
					String rawDataName = null;
					if (alignmentResult.isImported()) {
						rawDataName = alignmentResult.getImportedRawDataName(rawDataID);
					} else {
					//	RawDataAtClient rawData = mainWin.getItemSelector().getRawDataByID(rawDataID);
					//	rawDataName = rawData.getNiceName();
					}
					s[COLINFO_WIDEMODE_LASTCOMMONCOL + 1 + i*COLINFO_WIDEMODE_COLSPERRUN] = new String("m/z: " + rawDataName);
					s[COLINFO_WIDEMODE_LASTCOMMONCOL + 2 + i*COLINFO_WIDEMODE_COLSPERRUN] = new String("rt: " + rawDataName);
					s[COLINFO_WIDEMODE_LASTCOMMONCOL + 3 + i*COLINFO_WIDEMODE_COLSPERRUN] = new String("height: " + rawDataName);
					s[COLINFO_WIDEMODE_LASTCOMMONCOL + 4 + i*COLINFO_WIDEMODE_COLSPERRUN] = new String("area: " + rawDataName);
					s[COLINFO_WIDEMODE_LASTCOMMONCOL + 5 + i*COLINFO_WIDEMODE_COLSPERRUN] = new String("status: " + rawDataName);
					i++;
				}


			} else {

				s[0] = new String("ID");
				s[1] = new String("Std");
				s[2] = new String("M/Z");
				s[3] = new String("RT");
				s[4] = new String("Isotope Pattern ID");
				s[5] = new String("Isotope Peak Number");
				s[6] = new String("Charge State");


				int[] rawDataIDs = alignmentResult.getRawDataIDs();
				int i = 0;
				for (int rawDataID : rawDataIDs) {

					String rawDataName ="";
					if (alignmentResult.isImported()) {
						rawDataName = alignmentResult.getImportedRawDataName(rawDataID);
					} else {
					//	RawDataAtClient rawData = mainWin.getItemSelector().getRawDataByID(rawDataID);
					//	rawDataName = rawData.getNiceName();
					}

					if (mainWin.getParameterStorage().getGeneralParameters().getPeakMeasuringType() == GeneralParameters.PARAMETERVALUE_PEAKMEASURING_HEIGHT) {
						s[COLINFO_COMPACTMODE_LASTCOMMONCOL + 1 + i*COLINFO_COMPACTMODE_COLSPERRUN] = new String("height: " + rawDataName);
					}

					if (mainWin.getParameterStorage().getGeneralParameters().getPeakMeasuringType() == GeneralParameters.PARAMETERVALUE_PEAKMEASURING_AREA) {
						s[COLINFO_COMPACTMODE_LASTCOMMONCOL + 1 + i*COLINFO_COMPACTMODE_COLSPERRUN] = new String("area: " + rawDataName);
					}


					i++;
				}

			}*/

			return s;

        }

	}

}