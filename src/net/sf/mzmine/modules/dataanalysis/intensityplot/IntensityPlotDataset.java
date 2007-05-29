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

package net.sf.mzmine.modules.dataanalysis.intensityplot;

import java.util.ArrayList;
import java.util.List;

import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.Peak;
import net.sf.mzmine.io.OpenedRawDataFile;

import org.jfree.data.category.CategoryDataset;
import org.jfree.data.general.AbstractDataset;

/**
 * 
 */
class IntensityPlotDataset extends AbstractDataset implements CategoryDataset {

    private Object yAxisValueSource;

    private OpenedRawDataFile selectedFiles[];
    private PeakListRow selectedRows[];

    IntensityPlotDataset(IntensityPlotParameters parameters) {
        this.yAxisValueSource = parameters.getYAxisValueSource();
        this.selectedFiles = parameters.getSelectedDataFiles();
        this.selectedRows = parameters.getSelectedRows();
    }

    /**
     * @see org.jfree.data.KeyedValues2D#getRowIndex(java.lang.Comparable)
     */
    public int getRowIndex(Comparable rowKey) {
        for (int i = 0; i < selectedRows.length; i++) {
            if (selectedRows[i].toString().equals(rowKey))
                return i;
        }
        return -1;
    }

    /**
     * @see org.jfree.data.KeyedValues2D#getRowKeys()
     */
    public List getRowKeys() {
        ArrayList<Object> rowKeys = new ArrayList<Object>();
        for (PeakListRow row : selectedRows) {
            rowKeys.add(row.toString());
        }
        return rowKeys;
    }

    /**
     * @see org.jfree.data.KeyedValues2D#getColumnKey(int)
     */
    public Comparable getColumnKey(int column) {
        return selectedFiles[column].toString();
    }

    /**
     * @see org.jfree.data.KeyedValues2D#getColumnIndex(java.lang.Comparable)
     */
    public int getColumnIndex(Comparable columnKey) {
        for (int i = 0; i < selectedFiles.length; i++) {
            if (selectedFiles[i].toString().equals(columnKey))
                return i;
        }
        return -1;
    }

    /**
     * @see org.jfree.data.KeyedValues2D#getColumnKeys()
     */
    public List getColumnKeys() {
        ArrayList<Object> columnKeys = new ArrayList<Object>();
        for (OpenedRawDataFile file : selectedFiles) {
            columnKeys.add(file.toString());
        }
        return columnKeys;
    }

    /**
     * @see org.jfree.data.KeyedValues2D#getValue(java.lang.Comparable,
     *      java.lang.Comparable)
     */
    public Number getValue(Comparable rowKey, Comparable columnKey) {
        int row = getRowIndex(rowKey);
        int column = getColumnIndex(columnKey);
        return getValue(row, column);
    }

    /**
     * @see org.jfree.data.Values2D#getRowCount()
     */
    public int getRowCount() {
        return selectedRows.length;
    }

    /**
     * @see org.jfree.data.Values2D#getColumnCount()
     */
    public int getColumnCount() {
        return selectedFiles.length;
    }

    /**
     * @see org.jfree.data.Values2D#getValue(int, int)
     */
    public Number getValue(int row, int column) {

        Double value = null;
        
        Peak peak = getPeak(row, column);
        if (peak == null) return null;

        if (yAxisValueSource == IntensityPlotParameters.PeakIntensityOption)
            value = peak.getHeight();

        if (yAxisValueSource == IntensityPlotParameters.PeakAreaOption)
            value = peak.getArea();

        if (yAxisValueSource == IntensityPlotParameters.PeakRTOption)
            value = peak.getRT();

        return value;

    }

    /**
     * @see org.jfree.data.KeyedValues2D#getRowKey(int)
     */
    public Comparable getRowKey(int row) {
        return selectedRows[row].toString();
    }

    public Peak getPeak(int row, int column) {
        OpenedRawDataFile file = selectedFiles[column];
        return selectedRows[row].getPeak(file);

    }
    
    /**
     * @see org.jfree.data.KeyedValues2D#getRowKey(int)
     */
    public OpenedRawDataFile getFile(int column) {
        return selectedFiles[column];
    }

    /**
     * @see org.jfree.data.KeyedValues2D#getRowKey(int)
     */
    public PeakListRow getRow(int row) {
        return selectedRows[row];
    }
    
}
