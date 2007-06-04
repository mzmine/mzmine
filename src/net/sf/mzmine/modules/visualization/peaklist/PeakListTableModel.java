/*
 * Copyright 2006-2007 The MZmine Development Team
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

package net.sf.mzmine.modules.visualization.peaklist;

import java.awt.Color;

import javax.swing.table.AbstractTableModel;

import net.sf.mzmine.data.CompoundIdentity;
import net.sf.mzmine.data.Peak;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.io.OpenedRawDataFile;
import net.sf.mzmine.modules.visualization.peaklist.PeakListTableColumnModel.CommonColumns;
import net.sf.mzmine.modules.visualization.peaklist.PeakListTableColumnModel.DataFileColumns;
import net.sf.mzmine.userinterface.components.ColorCircle;
import net.sf.mzmine.userinterface.components.PeakXICComponent;

public class PeakListTableModel extends AbstractTableModel {

    private PeakList peakList;

    static final ColorCircle greenCircle = new ColorCircle(Color.green);
    static final ColorCircle redCircle = new ColorCircle(Color.red);
    static final ColorCircle yellowCircle = new ColorCircle(Color.yellow);

    /**
     * Constructor, assign given dataset to this table
     */
    public PeakListTableModel(PeakList peakList) {
        this.peakList = peakList;
    }

    public int getColumnCount() {
        return CommonColumns.values().length
                + peakList.getNumberOfRawDataFiles()
                * DataFileColumns.values().length;
    }

    public int getRowCount() {
        return peakList.getNumberOfRows();
    }

    public String getColumnName(int col) {

        if (isCommonColumn(col)) {
            CommonColumns commonColumn = getCommonColumn(col);
            return commonColumn.getColumnName();
        } else {
            DataFileColumns dataFileColumn = getDataFileColumn(col);
            return dataFileColumn.getColumnName();
        }

    }

    /**
     * This method returns the value at given coordinates of the dataset or null
     * if it is a missing value
     */

    public Object getValueAt(int row, int col) {

        PeakListRow peakListRow = peakList.getRow(row);

        if (isCommonColumn(col)) {
            CommonColumns commonColumn = getCommonColumn(col);

            switch (commonColumn) {
            case ROWID:
                return new Integer(peakListRow.getID());
            case AVERAGEMZ:
                return new Double(peakListRow.getAverageMZ());
            case AVERAGERT:
                return new Double(peakListRow.getAverageRT());
            case COMMENT:
                return peakListRow.getComment();
            case IDENTITY:
                CompoundIdentity preferredIdentity = peakListRow.getPreferredCompoundIdentity();
                if (preferredIdentity != CompoundIdentity.UNKNOWN_IDENTITY)
                    return preferredIdentity.toString();
                else {
                    CompoundIdentity identities[] = peakListRow.getCompoundIdentities();
                    if ((identities != null) && (identities.length > 0)) return "..."; 
                    return null;
                }
            }

        }

        if (!isCommonColumn(col)) {
            DataFileColumns dataFileColumn = getDataFileColumn(col);
            OpenedRawDataFile file = getColumnDataFile(col);
            Peak peak = peakListRow.getPeak(file);

            if (peak == null) {
                if (dataFileColumn == DataFileColumns.STATUS)
                    return redCircle;
                else
                    return null;
            }

            switch (dataFileColumn) {
            case STATUS:
                switch (peak.getPeakStatus()) {
                case DETECTED:
                    return greenCircle;
                case ESTIMATED:
                    return yellowCircle;
                }
            case PEAKSHAPE:
                return new PeakXICComponent(peak);
            case MZ:
                return new Double(peak.getMZ());
            case RT:
                return new Double(peak.getRT());
            case HEIGHT:
                return new Double(peak.getHeight());
            case AREA:
                return new Double(peak.getArea());
            case DURATION:
                return new Double(peak.getDuration());

            }

        }

        return null;

    }

    /**
     * This method returns the class of the objects in this column of the table
     */
    public Class<?> getColumnClass(int col) {

        if (isCommonColumn(col)) {
            CommonColumns commonColumn = getCommonColumn(col);
            return commonColumn.getColumnClass();
        } else {
            DataFileColumns dataFileColumn = getDataFileColumn(col);
            return dataFileColumn.getColumnClass();
        }

    }

    public boolean isCellEditable(int row, int col) {

        CommonColumns columnType = getCommonColumn(col);

        if (columnType == CommonColumns.COMMENT)
            return true;

        if (columnType == CommonColumns.IDENTITY) {
            PeakListRow peakListRow = peakList.getRow(row);
            CompoundIdentity identities[] = peakListRow.getCompoundIdentities();
            if ((identities == null) || (identities.length == 0))
                return false;
            return true;
        }

        return false;

    }

    public void setValueAt(Object value, int row, int col) {

        CommonColumns columnType = getCommonColumn(col);

        PeakListRow peakListRow = peakList.getRow(row);

        if (columnType == CommonColumns.COMMENT) {
            peakListRow.setComment((String) value);
        }

        if (columnType == CommonColumns.IDENTITY) {
            if (value instanceof CompoundIdentity)
                peakListRow.setPreferredCompoundIdentity((CompoundIdentity) value);
            else
                peakListRow.setPreferredCompoundIdentity(CompoundIdentity.UNKNOWN_IDENTITY);
        }

    }

    boolean isCommonColumn(int col) {
        return col < CommonColumns.values().length;
    }

    CommonColumns getCommonColumn(int col) {

        CommonColumns commonColumns[] = CommonColumns.values();

        if (col < commonColumns.length)
            return commonColumns[col];

        return null;

    }

    DataFileColumns getDataFileColumn(int col) {

        CommonColumns commonColumns[] = CommonColumns.values();
        DataFileColumns dataFileColumns[] = DataFileColumns.values();

        if (col < commonColumns.length)
            return null;

        // substract common columns from the index
        col -= commonColumns.length;

        // divide by number of data file columns
        col %= dataFileColumns.length;

        return dataFileColumns[col];

    }

    OpenedRawDataFile getColumnDataFile(int col) {

        CommonColumns commonColumns[] = CommonColumns.values();
        DataFileColumns dataFileColumns[] = DataFileColumns.values();

        if (col < commonColumns.length)
            return null;

        // substract common columns from the index
        col -= commonColumns.length;

        // divide by number of data file columns
        int fileIndex = (col / dataFileColumns.length);

        return peakList.getRawDataFile(fileIndex);

    }

}
