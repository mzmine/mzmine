package net.sf.mzmine.modules.visualization.alignmentresult.table;

import javax.swing.table.AbstractTableModel;

import net.sf.mzmine.data.AlignmentResult;
import net.sf.mzmine.data.AlignmentResultRow;
import net.sf.mzmine.data.IsotopePattern;
import net.sf.mzmine.data.Peak;
import net.sf.mzmine.data.Peak.PeakStatus;
import net.sf.mzmine.data.impl.StandardCompoundFlag;
import net.sf.mzmine.io.OpenedRawDataFile;
import net.sf.mzmine.userinterface.dialogs.alignmentresultcolumnselection.AlignmentResultColumnSelection;
import net.sf.mzmine.util.IsotopePatternUtils;

public class AlignmentResultTableModel extends AbstractTableModel {

	private AlignmentResult alignmentResult;
	private AlignmentResultColumnSelection columnSelection;

	private IsotopePatternUtils isoUtil;


	/**
	 * Constructor, assign given dataset to this table
	 */
	public AlignmentResultTableModel(AlignmentResult alignmentResult, AlignmentResultColumnSelection columnSelection) {
		this.alignmentResult = alignmentResult;
		this.columnSelection = columnSelection;

		isoUtil = new IsotopePatternUtils(alignmentResult);

	}

	public AlignmentResultColumnSelection getColumnSelection() {
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

	public Object getValueAt(int row, int col) {

		int[] groupOffset = getColumnGroupAndOffset(col);

		// Common column
		if (groupOffset[0]<0) {

			AlignmentResultRow alignmentRow = alignmentResult.getRow(row);

			switch(columnSelection.getSelectedCommonColumn(groupOffset[1])) {
			/*
				case STDCOMPOUND:
					return alignmentRow.hasData(StandardCompoundFlag.class);
					*/
				case ROWNUM:
					return new Integer(row+1);
				case AVGMZ:
					return new Double(alignmentRow.getAverageMZ());
				case AVGRT:
					return new Double(alignmentRow.getAverageRT());
				case ISOTOPEID:
					IsotopePattern isoPatt = (IsotopePattern)alignmentRow.getLastData(IsotopePattern.class);
					return new Integer(isoUtil.getIsotopePatternNumber(isoPatt));
				case ISOTOPEPEAK:
					isoPatt = (IsotopePattern)alignmentRow.getLastData(IsotopePattern.class);
					return new Integer(isoUtil.getRowNumberWithinPattern(alignmentRow));
				case CHARGE:
					isoPatt = (IsotopePattern)alignmentRow.getLastData(IsotopePattern.class);
					return new Integer(isoPatt.getChargeState());
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
				case STATUS:
					PeakStatus ps = p.getPeakStatus();
					return ps;
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



	public boolean isCellEditable(int row, int col) {
		/*
		int[] groupOffset = getColumnGroupAndOffset(col);
		
		if (groupOffset[0]<0) {
			switch(columnSelection.getSelectedCommonColumn(groupOffset[1])) {
				case STDCOMPOUND:				
					return true;
				default:
					return false;
			}
		}
		*/
		return false;	
	}

	public void setValueAt(Object value, int row, int col) {
		/*
		int[] groupOffset = getColumnGroupAndOffset(col);
		
		if (groupOffset[0]<0) {
			switch(columnSelection.getSelectedCommonColumn(groupOffset[1])) {
				case STDCOMPOUND:
					AlignmentResultRow alignmentRow = alignmentResult.getRow(row);
					if (alignmentRow.hasData(StandardCompoundFlag.class)) {
						alignmentRow.removeAllData(StandardCompoundFlag.class);
					} else {
						alignmentRow.addData(StandardCompoundFlag.class, new StandardCompoundFlag());
					}
					break;
				default:
					break;
			}
		}	
		*/
	}


}
