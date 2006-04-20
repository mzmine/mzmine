/*
    Copyright 2005 VTT Biotechnology

    This file is part of MZmine.

    MZmine is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    MZmine is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with MZmine; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
*/

package net.sf.mzmine.visualizers.alignmentresult;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JInternalFrame;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.InternalFrameListener;
import javax.swing.table.AbstractTableModel;

import net.sf.mzmine.methods.alignment.AlignmentResult;
import net.sf.mzmine.methods.peakpicking.Peak;
import net.sf.mzmine.obsoletedatastructures.RawDataAtClient;
import net.sf.mzmine.userinterface.mainwindow.ItemSelector;
import net.sf.mzmine.userinterface.mainwindow.MainWindow;
import net.sf.mzmine.userinterface.mainwindow.Statusbar;
import net.sf.mzmine.util.GeneralParameters;
import sunutils.TableSorter;


/**
 * This class decribes a visualizer used for displaying alignment results in a table format
 */
public class AlignmentResultVisualizerList extends JInternalFrame implements AlignmentResultVisualizer, InternalFrameListener, MouseListener, ActionListener {

	// These constants are used for calculating table's column layout in two different modes
	private final int COLINFO_COMPACTMODE_IND = 0;
	private final int COLINFO_COMPACTMODE_STD = 1;
	private final int COLINFO_COMPACTMODE_MZ = 2;
	private final int COLINFO_COMPACTMODE_RT = 3;
	private final int COLINFO_COMPACTMODE_ISOTOPEPATTERNID = 4;
	private final int COLINFO_COMPACTMODE_ISOTOPEPEAKNUMBER = 5;
	private final int COLINFO_COMPACTMODE_CHARGESTATE = 6;
	private final int COLINFO_COMPACTMODE_COLSPERRUN = 1;
	private final int COLINFO_COMPACTMODE_LASTCOMMONCOL = COLINFO_COMPACTMODE_CHARGESTATE;

	private final int COLINFO_WIDEMODE_IND = 0;
	private final int COLINFO_WIDEMODE_STD = 1;
	private final int COLINFO_WIDEMODE_ISOTOPEPATTERNID = 2;
	private final int COLINFO_WIDEMODE_ISOTOPEPEAKNUMBER = 3;
	private final int COLINFO_WIDEMODE_CHARGESTATE = 4;
	private final int COLINFO_WIDEMODE_COLSPERRUN = 5;
	private final int COLINFO_WIDEMODE_LASTCOMMONCOL = COLINFO_WIDEMODE_CHARGESTATE;


	private MainWindow mainWin;
	private Statusbar statBar;
	private ItemSelector itemSelector;

	private AlignmentResult alignmentResult;

	private JTable table;
	private JScrollPane scrollPane;

	private JPopupMenu popupMenu;
	private JMenuItem changeFormattingMenuItem;
	private JMenuItem zoomToPeakMenuItem;

	private boolean compactMode = true;



	/**
	 * Constructor: initializes an empty visualizer
	 */
	public AlignmentResultVisualizerList(MainWindow _mainWin) {

		mainWin = _mainWin;
		statBar = mainWin.getStatusBar();
		itemSelector = mainWin.getItemSelector();
		setResizable( true );
		setIconifiable( true );

		table = new JTable();
		table.setAutoResizeMode( JTable.AUTO_RESIZE_OFF );
		scrollPane = new JScrollPane(table);

		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(scrollPane, BorderLayout.CENTER);

		popupMenu = new JPopupMenu();

		changeFormattingMenuItem = new JMenuItem("Change table column format");
		changeFormattingMenuItem.addActionListener(this);
		changeFormattingMenuItem.setEnabled(true);

		zoomToPeakMenuItem = new JMenuItem("Zoom visualizers to this peak");
		zoomToPeakMenuItem.addActionListener(this);
		zoomToPeakMenuItem.setEnabled(true);

		popupMenu.add(changeFormattingMenuItem);
		popupMenu.add(zoomToPeakMenuItem);

		table.addMouseListener(this);
		addInternalFrameListener(this);

	}


	/**
	 * Assign a dataset for the visualizer
	 */
    public void setAlignmentResult(AlignmentResult _alignmentResult) {

		alignmentResult = _alignmentResult;

		setTitle(alignmentResult.getNiceName() + ": " + alignmentResult.getDescription());

		refreshTableFormatting();

	}


	/**
	 * This method is called when selected row has changed in the alignment result
	 */
	public void updateSelectedRow() {
		int selectedRowNum = alignmentResult.getSelectedRow();

		// Because of possible sorting, we can't assume that it is alignID:th row
		int rowInd = -2;
		for (int i=0; i<alignmentResult.getNumOfRows(); i++) {
			int tmpID = ((Integer)table.getValueAt(i, 0)).intValue()-1;
			if (tmpID==selectedRowNum) { rowInd = i; break; }
		}

		// If matching row was not found
		if (rowInd == -2) { return; }


		// Change selection
		//table.changeSelection(rowInd, colNum, false, false);
		int selectedCol = table.getSelectedColumn();
		table.changeSelection(rowInd, selectedCol, false, false);

		double mz = alignmentResult.getAverageMZ(rowInd);
		double rt = alignmentResult.getAverageRT(rowInd);

		statBar.setCursorPosition(mz,rt);



	}

	/**
	 * Select a row in table with matching alignment ID.
	 * Also suitable column for given Run will be selected
	 *
	 */
/*
	public void selectAlignmentRow(int alignID) {
		// Find the row with this alignment ID
		// Because of possible sorting, we can't assume that it is alignID:th row
		int rowInd = -2;
		for (int i=0; i<alignmentResult.getNumOfRows(); i++) {
			int tmpID = ((Integer)table.getValueAt(i, 0)).intValue()-1;
			if (tmpID==alignID) { rowInd = i; break; }
		}

		// If matching row was not found
		if (rowInd == -2) { return; }


		// Change selection
		//table.changeSelection(rowInd, colNum, false, false);
		table.changeSelection(rowInd, 0, false, false);

		double mz = alignmentResult.getAverageMZ(rowInd);
		double rt = alignmentResult.getAverageRT(rowInd);

		statBar.setCursorPosition(mz,rt);

	}
*/

	public void refreshVisualizer(int changeType) {
		if (changeType == AlignmentResultVisualizer.CHANGETYPE_PEAK_MEASURING_SETTING) {
			refreshTableFormatting();
		}
	}

	/**
	 * Refresh table formatting, for example when column format mode is changed
	 *
	 */
	public void refreshTableFormatting() {

		AbstractTableModel mtm = new MyTableModel(alignmentResult);
		TableSorter sorter = new TableSorter(mtm); //ADDED THIS
		table.getTableHeader().setReorderingAllowed(false);
		//sorter.setTableHeader(table.getTableHeader()); //ADDED THIS	(REMOVED FOR TEST)
		sorter.addMouseListenerToHeaderInTable(table); // ADDED THIS TODAY
		table.setModel(sorter);

	}


	/**
	 * Methods for InternalFrameListener interface implementation
	 */
	public void internalFrameActivated(InternalFrameEvent e) {
		//alignmentResult.setActiveVisualizer(this);
		itemSelector.setActiveAlignmentResult(alignmentResult);
	}
	public void internalFrameClosed(InternalFrameEvent e) {	}
	public void internalFrameClosing(InternalFrameEvent e) { }
	public void internalFrameDeactivated(InternalFrameEvent e) { }
	public void internalFrameDeiconified(InternalFrameEvent e) { }
	public void internalFrameIconified(InternalFrameEvent e) { }
	public void internalFrameOpened(InternalFrameEvent e) { }


	/**
	 * Methods for MouseListener interface implementation
	 */
	public void mouseClicked(MouseEvent e) {}
	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}
	public void mousePressed(MouseEvent e) {}
	public void mouseReleased(MouseEvent e) {

		// Normal click: select row
		if (e.getButton()==MouseEvent.BUTTON1) {
			// Get alignment ID for selected row
			int rowInd = ((Integer)(table.getValueAt(table.getSelectedRow(), 0))).intValue();
			alignmentResult.setSelectedRow(rowInd-1);
		//	mainWin.updateAlignmentResultVisualizers(alignmentResult.getAlignmentResultID());

		}

		// If it was not first mouse button, then assume it was right-click (or equivalent)
	    if (e.getButton()!=MouseEvent.BUTTON1) {

			// If nothing is selected, then do not show pop-up menu
			if (table.getSelectedRow()==-1) { return; }

			// Get alignment ID for selected row
			int rowInd = ((Integer)(table.getValueAt(table.getSelectedRow(), 0))).intValue();

			popupMenu.show(e.getComponent(), e.getX(), e.getY());

		}
	}


	/**
	 * Method for calculating run number for given column number
	 */
	//private int calcRunNum(int colInd) {
	private int calcColumnGroupNum(int colInd) {

		int numOfRawDatas = alignmentResult.getNumOfRawDatas();
		int columnGroupNum = -1;

		// For both compact and wide mode, try to calculate column group number, if the selected column is some raw data specific column
		// Column group number is the
		if (!compactMode) {
			if ( (colInd>COLINFO_WIDEMODE_LASTCOMMONCOL) && (colInd<=(COLINFO_WIDEMODE_LASTCOMMONCOL+COLINFO_WIDEMODE_COLSPERRUN*numOfRawDatas)) ) {
				columnGroupNum = (int)java.lang.Math.floor((colInd-(COLINFO_WIDEMODE_LASTCOMMONCOL+1)) / (double)COLINFO_WIDEMODE_COLSPERRUN);
			}
		} else {

			if ( (colInd>COLINFO_COMPACTMODE_LASTCOMMONCOL) && (colInd<=(COLINFO_COMPACTMODE_LASTCOMMONCOL+COLINFO_COMPACTMODE_COLSPERRUN*numOfRawDatas)) ) {
				columnGroupNum = (int)java.lang.Math.floor((colInd-(COLINFO_COMPACTMODE_LASTCOMMONCOL+1)) / (double)COLINFO_COMPACTMODE_COLSPERRUN);
			}
		}

		return columnGroupNum;

	}




	/**
	 * Methods for ActionListener interface implementation
	 */
	public void actionPerformed(ActionEvent e) {
		Object src = e.getSource();

		// Change table column formatting
		if (src == changeFormattingMenuItem) {
			if (compactMode) { compactMode=false; } else { compactMode=true; }
			refreshTableFormatting();
		}

		// Zoom selected run around selected peak (or its estimated position if undetected)
		if (src == zoomToPeakMenuItem) {

			if (alignmentResult.isImported()) {
				try {
					JOptionPane.showMessageDialog(mainWin,
								"Zoom to peak is not available for IMPORTED alignment results. (" + alignmentResult.getNiceName() + ")",
								"Sorry",JOptionPane.ERROR_MESSAGE);
				} catch (Exception exce ) {}
				return;
			}

			// Get alignment ID for selected row
			int tmpRow = table.getSelectedRow();
			if (tmpRow == -1) { return; }
			int rowInd = ((Integer)(table.getValueAt(tmpRow , 0))).intValue()-1;

			// Calc zoom area
			double sumCentroidMZ=0;
			double sumCentroidRT=0;
			double sumStdevMZ=0;
			double sumDuration=0;
			int numSum = 0;

			int peakInd;
			int peakStatus;

			// Loop over all raw data files participating in this alignment
			// and calculate center and shape of the zoom area (average mz,rt and mz stdev & rt duration)
			int[] rawDataIDs = alignmentResult.getRawDataIDs();
			for (int rawDataID : rawDataIDs) {

				peakStatus = alignmentResult.getPeakStatus(rawDataID, rowInd);

				if (peakStatus==AlignmentResult.PEAKSTATUS_DETECTED) {
					peakInd = alignmentResult.getPeakID(rawDataID, rowInd);
					// RawDataAtClient rawData = mainWin.getItemSelector().getRawDataByID(rawDataID);
				//	Peak p = rawData.getPeakList().getPeak(peakInd);
			//		sumStdevMZ += p.getMZStdev();
				//	sumDuration += rawData.getScanTime(p.getStopScanNumber()) - rawData.getScanTime(p.getStartScanNumber());
					numSum++;
				}
			}


			double avgStdevMZ;
			double avgDuration;
			if (numSum>0) {
				avgStdevMZ = sumStdevMZ / (double)(numSum);
				avgDuration = sumDuration / (double)(numSum);
			} else {
				avgStdevMZ = 1;
				avgDuration = 10;
			}

			double avgMZ = alignmentResult.getAverageMZ(rowInd);
			double avgRT = alignmentResult.getAverageRT(rowInd);


			// Loop over all raw data files again, and set cursor and zoom on the spot
			for (int rawDataID : rawDataIDs) {

			RawDataAtClient rawData = null; // = mainWin.getItemSelector().getRawDataByID(rawDataID);
				int avgScan = rawData.getScanNumberByTime(avgRT);
				peakStatus = alignmentResult.getPeakStatus(rawDataID, rowInd);

				// If this is a normal detected peak, then center the cursor on the peak's max intensity data point
				if (peakStatus==AlignmentResult.PEAKSTATUS_DETECTED) {

					int peakID = alignmentResult.getPeakID(rawDataID, rowInd);

					rawData.getPeakList().setSelectedPeakID(peakID);
					rawData.setCursorPositionByPeakID(peakID);
				}

				// If this an estimated fill-in, then center the cursor on the location of the estimate
				if (peakStatus==AlignmentResult.PEAKSTATUS_ESTIMATED) {
					double tmpMZ = alignmentResult.getPeakMZ(rawDataID, rowInd);
					double tmpRT = alignmentResult.getPeakRT(rawDataID, rowInd);

					rawData.getPeakList().setSelectedPeakID(-1);
					int tmpScan = rawData.getScanNumberByTime(tmpRT);
					rawData.setCursorPosition(tmpScan, tmpMZ);
				}

				// If this is an empty gap, then center the cursor around the average mz,rt for this row
				if (peakStatus==AlignmentResult.PEAKSTATUS_NOTFOUND) {
					rawData.getPeakList().setSelectedPeakID(-1);
					rawData.setCursorPosition(avgScan, avgMZ);
				}

				// Set selection around the average mz,rt for this row (this means that selection will be the same for all runs on this row!)
				rawData.setSelectionAroundPeak(avgMZ,avgRT,avgStdevMZ,avgDuration);

			}

			// Refresh visualizers
		//	mainWin.startRefreshRawDataVisualizers(RawDataVisualizer.CHANGETYPE_SELECTION_BOTH, rawDataIDs);
			/*
			if 	(!(r.visualizersAreHidden())) {
				BackgroundThread bt = new BackgroundThread(mainWin, r, Visualizer.CHANGETYPE_SELECTION_BOTH, BackgroundThread.TASK_REFRESHVISUALIZERS);
				bt.start();
			}
			*/
		}
	}


	public void printMe() {}

	public void copyMe() {}

	public void closeMe() {
		dispose();
		alignmentResult = null;
	}
/*
	public void refreshVisualizer(int changeType) {

		if (changeType == AlignmentResultVisualizer.CHANGETYPE_PEAK_MEASURING_SETTING) {
			refreshTableFormatting();
		}
	}
*/

	/**
	 * This class is a slighty customized table model for JTable presenting the alignment results
	 */
	class MyTableModel extends AbstractTableModel {
		private String[] columnNames;
		private AlignmentResult alignmentResult;



		/**
		 * Constructor, assign given dataset to this table
		 */
		public MyTableModel(AlignmentResult _alignmentResult) {
			alignmentResult = _alignmentResult;
			columnNames = getColumnNames();
		}



		/**
		 * Counts number of columns in this table in current column mode
		 */
		public int getColumnCount() {
			if (!compactMode) {
				return COLINFO_WIDEMODE_LASTCOMMONCOL+COLINFO_WIDEMODE_COLSPERRUN*alignmentResult.getNumOfRawDatas()+1;
				//return 1+1+3*alignmentResult.getNumOfRuns();
			} else {
				return COLINFO_COMPACTMODE_LASTCOMMONCOL+COLINFO_COMPACTMODE_COLSPERRUN*alignmentResult.getNumOfRawDatas()+1;
				//return 1+1+2+alignmentResult.getNumOfRuns();
			}
		}



		public int getRowCount() {
			return alignmentResult.getNumOfRows();
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


			// First column is index numbering
			if (col==0) { return new Integer(row+1); }

			// Second column is the standard flag
			if (col==1) { return new Boolean(alignmentResult.getStandardCompoundFlag(row)); }

			int numOfRawDatas = alignmentResult.getNumOfRawDatas();

			if (!compactMode) {

				if (col==COLINFO_WIDEMODE_IND) { return new Integer(row+1); }
				if (col==COLINFO_WIDEMODE_STD) { return new Boolean(alignmentResult.getStandardCompoundFlag(row)); }
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
						if (mainWin.getParameterStorage().getGeneralParameters().getPeakMeasuringType() == GeneralParameters.PARAMETERVALUE_PEAKMEASURING_HEIGHT)
							{ preValue = alignmentResult.getPeakHeight(rawDataID, row); }
						if (mainWin.getParameterStorage().getGeneralParameters().getPeakMeasuringType() == GeneralParameters.PARAMETERVALUE_PEAKMEASURING_AREA)
							{ preValue = alignmentResult.getPeakArea(rawDataID, row); }
					}
					if (preValue<0) { return null; } else {	return new Double(preValue); }
				}

			}

			return null;

		}


		/**
		 * This method returns the class of the objects in this column of the table
		 */
		public Class getColumnClass(int col) {

			int numOfRuns = alignmentResult.getNumOfRawDatas();

			if (!compactMode) {

				if (col==COLINFO_WIDEMODE_IND) { return Integer.class; }
				if (col==COLINFO_WIDEMODE_STD) { return Boolean.class; }
				if (col==COLINFO_WIDEMODE_ISOTOPEPATTERNID) { return Integer.class; }
				if (col==COLINFO_WIDEMODE_ISOTOPEPEAKNUMBER) { return Integer.class; }
				if (col==COLINFO_WIDEMODE_CHARGESTATE) { return Integer.class; }

				if ((col >COLINFO_WIDEMODE_LASTCOMMONCOL) &&
					(col<=(COLINFO_WIDEMODE_LASTCOMMONCOL+COLINFO_WIDEMODE_COLSPERRUN*numOfRuns)) ) {
						// Calc runNumber and offset from the first column of this run
						int columnGroupNumber = calcColumnGroupNum(col);
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
				if (col==COLINFO_COMPACTMODE_STD) { return Boolean.class; }
				if (col==COLINFO_COMPACTMODE_MZ) { return Double.class; }
				if (col==COLINFO_COMPACTMODE_RT) { return Double.class; }
				if (col==COLINFO_COMPACTMODE_ISOTOPEPATTERNID) { return Integer.class; }
				if (col==COLINFO_COMPACTMODE_ISOTOPEPEAKNUMBER) { return Integer.class; }
				if (col==COLINFO_COMPACTMODE_CHARGESTATE) { return Integer.class; }


				if ((col >COLINFO_COMPACTMODE_LASTCOMMONCOL) &&
					(col<=(COLINFO_COMPACTMODE_LASTCOMMONCOL+COLINFO_COMPACTMODE_COLSPERRUN*numOfRuns)) ) {

					int columnGroupNumber = calcColumnGroupNum(col);
					int columnGroupOffset = col - (COLINFO_COMPACTMODE_LASTCOMMONCOL+1) - columnGroupNumber*COLINFO_COMPACTMODE_COLSPERRUN;

					// Only one column per each run (height or area)
					if (columnGroupOffset==0) { return Double.class; }
				}

			}
			return null;
		}

		public boolean isCellEditable(int row, int col) {
			if (!compactMode) {
				if (col==COLINFO_WIDEMODE_STD) { return true; }
			} else {
				if (col==COLINFO_COMPACTMODE_STD) { return true; }
			}

			return false;
		}

		public void setValueAt(Object value, int row, int col) {
			if (!compactMode) {
				if (col!=COLINFO_WIDEMODE_STD) { return; }
			} else {
				if (col!=COLINFO_COMPACTMODE_STD) { return; }
			}

			alignmentResult.setStandardCompoundFlag(row, ((Boolean)value).booleanValue());

			mainWin.repaint();
		}


		private String[] getColumnNames() {

			String[] s = new String[getColumnCount()];

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

			}

			return s;

		}


	}

}

