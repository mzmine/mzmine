/*
 * Copyright 2006-2015 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.visualization.peaklisttable.table;

import javax.swing.table.AbstractTableModel;

import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.Feature.FeatureStatus;
import net.sf.mzmine.datamodel.PeakIdentity;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;

public class PeakListTableModel extends AbstractTableModel {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private PeakList peakList;

    /**
     * Constructor, assign given dataset to this table
     */
    public PeakListTableModel(PeakList peakList) {
	this.peakList = peakList;

    }

    public int getColumnCount() {
	return CommonColumnType.values().length
		+ peakList.getNumberOfRawDataFiles()
		* DataFileColumnType.values().length;
    }

    public int getRowCount() {
	return peakList.getNumberOfRows();
    }

    public String getColumnName(int col) {
	return "column" + col;
    }

    public Class<?> getColumnClass(int col) {

	if (isCommonColumn(col)) {
	    CommonColumnType commonColumn = getCommonColumn(col);
	    return commonColumn.getColumnClass();
	} else {
	    DataFileColumnType dataFileColumn = getDataFileColumn(col);
	    return dataFileColumn.getColumnClass();
	}

    }

    /**
     * This method returns the value at given coordinates of the dataset or null
     * if it is a missing value
     */

    public Object getValueAt(int row, int col) {

	PeakListRow peakListRow = peakList.getRow(row);

	if (isCommonColumn(col)) {
	    CommonColumnType commonColumn = getCommonColumn(col);

	    switch (commonColumn) {
	    case ROWID:
		return new Integer(peakListRow.getID());
	    case AVERAGEMZ:
		return new Double(peakListRow.getAverageMZ());
	    case AVERAGERT:
		if (peakListRow.getAverageRT() <= 0)
		    return null;
		return new Double(peakListRow.getAverageRT());
	    case COMMENT:
		return peakListRow.getComment();
	    case IDENTITY:
		return peakListRow.getPreferredPeakIdentity();
	    case PEAKSHAPE:
		return peakListRow;
	    }

	} else {

	    DataFileColumnType dataFileColumn = getDataFileColumn(col);
	    RawDataFile file = getColumnDataFile(col);
	    Feature peak = peakListRow.getPeak(file);

	    if (peak == null) {
		if (dataFileColumn == DataFileColumnType.STATUS)
		    return FeatureStatus.UNKNOWN;
		else
		    return null;
	    }

	    switch (dataFileColumn) {
	    case STATUS:
		return peak.getFeatureStatus();
	    case PEAKSHAPE:
		return peak;
	    case MZ:
		return peak.getMZ();
	    case RT:
		if (peak.getRT() <= 0)
		    return null;
		return peak.getRT();
	    case HEIGHT:
		if (peak.getHeight() <= 0)
		    return null;
		return peak.getHeight();
	    case AREA:
		return peak.getArea();
	    case DURATION:
		double rtLen = peak.getRawDataPointsRTRange().upperEndpoint()
			- peak.getRawDataPointsRTRange().lowerEndpoint();
		return rtLen;
	    case CHARGE:
		if (peak.getCharge() <= 0)
		    return null;
		return new Integer(peak.getCharge());
	    case RT_START:
		return peak.getRawDataPointsRTRange().lowerEndpoint();
	    case RT_END:
		return peak.getRawDataPointsRTRange().upperEndpoint();
	    case DATAPOINTS:
		return peak.getScanNumbers().length;
	    case FWHM:
                return peak.getFWHM();
            case TF:
                return peak.getTailingFactor();
            case AF:
                return peak.getAsymmetryFactor();
	    }

	}

	return null;

    }

    public boolean isCellEditable(int row, int col) {

	CommonColumnType columnType = getCommonColumn(col);

	return ((columnType == CommonColumnType.COMMENT) || (columnType == CommonColumnType.IDENTITY));

    }

    public void setValueAt(Object value, int row, int col) {

	CommonColumnType columnType = getCommonColumn(col);

	PeakListRow peakListRow = peakList.getRow(row);

	if (columnType == CommonColumnType.COMMENT) {
	    peakListRow.setComment((String) value);
	}

	if (columnType == CommonColumnType.IDENTITY) {
	    if (value instanceof PeakIdentity)
		peakListRow.setPreferredPeakIdentity((PeakIdentity) value);
	}

    }

    boolean isCommonColumn(int col) {
	return col < CommonColumnType.values().length;
    }

    CommonColumnType getCommonColumn(int col) {

	CommonColumnType commonColumns[] = CommonColumnType.values();

	if (col < commonColumns.length)
	    return commonColumns[col];

	return null;

    }

    DataFileColumnType getDataFileColumn(int col) {

	CommonColumnType commonColumns[] = CommonColumnType.values();
	DataFileColumnType dataFileColumns[] = DataFileColumnType.values();

	if (col < commonColumns.length)
	    return null;

	// substract common columns from the index
	col -= commonColumns.length;

	// divide by number of data file columns
	col %= dataFileColumns.length;

	return dataFileColumns[col];

    }

    RawDataFile getColumnDataFile(int col) {

	CommonColumnType commonColumns[] = CommonColumnType.values();
	DataFileColumnType dataFileColumns[] = DataFileColumnType.values();

	if (col < commonColumns.length)
	    return null;

	// substract common columns from the index
	col -= commonColumns.length;

	// divide by number of data file columns
	int fileIndex = (col / dataFileColumns.length);

	return peakList.getRawDataFile(fileIndex);

    }

}
