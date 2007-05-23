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

package net.sf.mzmine.modules.visualization.alignmentresult;

import java.awt.Color;
import java.awt.Font;
import java.util.Hashtable;

import javax.swing.JLabel;
import javax.swing.table.AbstractTableModel;

import net.sf.mzmine.data.AlignmentResult;
import net.sf.mzmine.data.AlignmentResultRow;
import net.sf.mzmine.data.Peak;
import net.sf.mzmine.io.OpenedRawDataFile;
import net.sf.mzmine.userinterface.components.PeakXICComponent;

public class AlignmentResultTableModel extends AbstractTableModel {

    private AlignmentResult alignmentResult;
    private AlignmentResultTableColumns columnSelection;
    private Hashtable<Peak,PeakXICComponent> XICcomponents;
    
    private static final Font statusFont = new Font("SansSerif", Font.PLAIN, 10);

    /**
     * Constructor, assign given dataset to this table
     */
    public AlignmentResultTableModel(AlignmentResult alignmentResult,
            AlignmentResultTableColumns columnSelection) {
        this.alignmentResult = alignmentResult;
        this.columnSelection = columnSelection;
        XICcomponents = new Hashtable<Peak,PeakXICComponent>();

    }


    public int getColumnCount() {
        return  columnSelection.getNumberOfSelectedCommonColumns()
                + alignmentResult.getNumberOfRawDataFiles()
                * columnSelection.getNumberOfSelectedRawDataColumns();
    }

    public int getRowCount() {
        return alignmentResult.getNumberOfRows();
    }

    public String getColumnName(int col) {

        int[] groupOffset = getColumnGroupAndOffset(col);

        // Common column
        if (groupOffset[0] < 0) {
            return columnSelection.getSelectedCommonColumns()[groupOffset[1]].getColumnName();
        }

        if (groupOffset[0] >= 0) {
            OpenedRawDataFile rawData = alignmentResult.getRawDataFile(groupOffset[0]);
            String rawDataName = rawData.toString();
            return rawDataName
                    + ": "
                    + columnSelection.getSelectedRawDataColumns()[groupOffset[1]].getColumnName();
        }

        return new String("No Name");

    }

    /**
     * This method returns the value at given coordinates of the dataset or null
     * if it is a missing value
     */

    public Object getValueAt(int row, int col) {

        int[] groupOffset = getColumnGroupAndOffset(col);

        // Common column
        if (groupOffset[0] < 0) {

            AlignmentResultRow alignmentRow = alignmentResult.getRow(row);

            switch (columnSelection.getSelectedCommonColumns()[groupOffset[1]]) {
            /*
             * case STDCOMPOUND: return
             * alignmentRow.hasData(StandardCompoundFlag.class);
             */
            case ROWNUM:
                return new Integer(row + 1);
            case AVGMZ:
                return new Double(alignmentRow.getAverageMZ());
            case AVGRT:
                return new Double(alignmentRow.getAverageRT());
            case COMMENT:
                return alignmentRow.getComment();
            default:
                return null;
            }

        }

        else { // if (groupOffset[0]>=0)

            OpenedRawDataFile rawData = alignmentResult.getRawDataFile(groupOffset[0]);
            Peak p = alignmentResult.getPeak(row, rawData);
            if (p == null)
                return null;

            switch (columnSelection.getSelectedRawDataColumns()[groupOffset[1]]) {
            case MZ:
                return new Double(p.getMZ());
            case RT:
                return new Double(p.getRT());
            case HEIGHT:
                return new Double(p.getHeight());
            case AREA:
                return new Double(p.getArea());
            case SHAPE:
                PeakXICComponent pc = XICcomponents.get(p);
                if (pc == null) {
                    pc = new PeakXICComponent(p, 100, 30);
                    XICcomponents.put(p, pc);
                }
                return pc;
            case STATUS:
                JLabel statusLabel = new JLabel(p.getPeakStatus().toString());
                statusLabel.setOpaque(true);
                statusLabel.setFont(statusFont);
                switch(p.getPeakStatus()) {
                case DETECTED:
                    statusLabel.setBackground(Color.green);
                    break;
                case ESTIMATED:
                    statusLabel.setBackground(Color.yellow);
                    break;
                default:
                    statusLabel.setBackground(Color.red);
                    break;
                }
                return statusLabel;

            default:
                return null;
                
            }

        }

    }

    /**
     * This method returns the class of the objects in this column of the table
     */
    public Class<?> getColumnClass(int col) {

        int[] groupOffset = getColumnGroupAndOffset(col);

        // Common column
        if (groupOffset[0] < 0) {
            return columnSelection.getSelectedCommonColumns()[groupOffset[1]].getColumnClass();
        } else { // if (groupOffset[0]>=0)
            return columnSelection.getSelectedRawDataColumns()[groupOffset[1]].getColumnClass();
        }

    }

    private int[] getColumnGroupAndOffset(int col) {

        // Is this a common column?
        if (col < columnSelection.getNumberOfSelectedCommonColumns()) {
            int[] res = new int[2];
            res[0] = -1;
            res[1] = col;
            return res;
        }
        
        // This is a raw data specific column.

        // Calc number of raw data
        int[] res = new int[2];
        res[0] = (int) Math.floor((double) (col - columnSelection.getNumberOfSelectedCommonColumns())
                / (double) columnSelection.getNumberOfSelectedRawDataColumns());
        res[1] = col - columnSelection.getNumberOfSelectedCommonColumns() - res[0]
                * columnSelection.getNumberOfSelectedRawDataColumns();
        
        return res;

    }

    public boolean isCellEditable(int row, int col) {
        /*
         * int[] groupOffset = getColumnGroupAndOffset(col);
         * 
         * if (groupOffset[0]<0) {
         * switch(columnSelection.getSelectedCommonColumn(groupOffset[1])) {
         * case STDCOMPOUND: return true; default: return false; } }
         */
        return false;
    }

    public void setValueAt(Object value, int row, int col) {
        /*
         * int[] groupOffset = getColumnGroupAndOffset(col);
         * 
         * if (groupOffset[0]<0) {
         * switch(columnSelection.getSelectedCommonColumn(groupOffset[1])) {
         * case STDCOMPOUND: AlignmentResultRow alignmentRow =
         * alignmentResult.getRow(row); if
         * (alignmentRow.hasData(StandardCompoundFlag.class)) {
         * alignmentRow.removeAllData(StandardCompoundFlag.class); } else {
         * alignmentRow.addData(StandardCompoundFlag.class, new
         * StandardCompoundFlag()); } break; default: break; } }
         */
    }

}
