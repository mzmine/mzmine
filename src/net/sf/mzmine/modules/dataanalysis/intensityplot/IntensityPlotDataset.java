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

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.Peak;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.project.MZmineProject;
import net.sf.mzmine.util.CollectionUtils;
import net.sf.mzmine.util.MathUtils;

import org.jfree.data.DomainOrder;
import org.jfree.data.general.AbstractDataset;
import org.jfree.data.statistics.StatisticalCategoryDataset;
import org.jfree.data.xy.IntervalXYDataset;

/**
 * This class implements 2 kinds of data sets - CategoryDataset and XYDataset
 * CategoryDataset is used if X axis is a raw data file or string parameter
 * XYDataset is used if X axis is a number parameter
 */
class IntensityPlotDataset extends AbstractDataset implements
        StatisticalCategoryDataset, IntervalXYDataset {

    private Object yAxisValueSource, xAxisValueSource;
    private Comparable xValues[];
    private RawDataFile selectedFiles[];
    private PeakListRow selectedRows[];

    IntensityPlotDataset(IntensityPlotParameters parameters) {

        this.xAxisValueSource = parameters.getXAxisValueSource();
        this.yAxisValueSource = parameters.getYAxisValueSource();
        this.selectedFiles = parameters.getSelectedDataFiles();
        this.selectedRows = parameters.getSelectedRows();

        if (xAxisValueSource instanceof Parameter) {
            MZmineProject project = MZmineCore.getCurrentProject();
            Parameter xAxisParameter = (Parameter) xAxisValueSource;
            HashSet<Comparable> parameterValues = new HashSet<Comparable>();
            for (RawDataFile file : selectedFiles) {
                Object value = project.getParameterValue(xAxisParameter, file);
                parameterValues.add((Comparable) value);
            }
            xValues = parameterValues.toArray(new Comparable[0]);
        }

        if (xAxisValueSource == IntensityPlotParameters.DataFileOption) {
            xValues = new String[selectedFiles.length];
            for (int i = 0; i < selectedFiles.length; i++)
                xValues[i] = selectedFiles[i].toString();
        }
    }

    Peak[] getPeaks(int row, int column) {
        return getPeaks(xValues[column], selectedRows[row]);
    }
    
    Peak[] getPeaks(Comparable xValue, PeakListRow row) {
        RawDataFile files[] = getFiles(xValue);
        Peak[] peaks = new Peak[files.length];
        for (int i = 0; i < files.length; i++) {
            peaks[i] = row.getPeak(files[i]);
        }
        return peaks;
    }
    
    RawDataFile[] getFiles(int column) {
        return getFiles(xValues[column]);
    }
    
    
    RawDataFile[] getFiles(Comparable xValue) {
        if (xAxisValueSource == IntensityPlotParameters.DataFileOption) {
            RawDataFile columnFile = selectedFiles[getColumnIndex(xValue)];
            return new RawDataFile[] { columnFile };
        }
        if (xAxisValueSource instanceof Parameter) {
            HashSet<RawDataFile> files = new HashSet<RawDataFile>();
            Parameter xAxisParameter = (Parameter) xAxisValueSource;
            MZmineProject project = MZmineCore.getCurrentProject();
            for (RawDataFile file : selectedFiles) {
                Object fileValue = project.getParameterValue(xAxisParameter,
                        file);
                if (fileValue.equals(xValue))
                    files.add(file);
            }
            return files.toArray(new RawDataFile[0]);
        }
        return null;
    }

    public Number getMeanValue(int row, int column) {
        Peak[] peaks = getPeaks(xValues[column], selectedRows[row]);
        HashSet<Float> values = new HashSet<Float>();
        for (int i = 0; i < peaks.length; i++) {
            if (peaks[i] == null) continue;
            if (yAxisValueSource == IntensityPlotParameters.PeakHeightOption)
                values.add(peaks[i].getHeight());
            if (yAxisValueSource == IntensityPlotParameters.PeakAreaOption)
                values.add(peaks[i].getArea());
            if (yAxisValueSource == IntensityPlotParameters.PeakRTOption)
                values.add(peaks[i].getRT());
        }
        float floatValues[] = CollectionUtils.toFloatArray(values);
        if (floatValues.length == 0) return 0;
        float mean = MathUtils.calcAvg(floatValues);
        return mean;
    }

    public Number getMeanValue(Comparable rowKey, Comparable columnKey) {
        throw (new UnsupportedOperationException("Unsupported"));
    }

    public Number getStdDevValue(int row, int column) {
        Peak[] peaks = getPeaks(xValues[column], selectedRows[row]);
        
        // if we have only 1 peak, there is no standard deviation
        if (peaks.length == 1) return 0;
        
        HashSet<Float> values = new HashSet<Float>();
        for (int i = 0; i < peaks.length; i++) {
            if (peaks[i] == null) continue;
            if (yAxisValueSource == IntensityPlotParameters.PeakHeightOption)
                values.add(peaks[i].getHeight());
            if (yAxisValueSource == IntensityPlotParameters.PeakAreaOption)
                values.add(peaks[i].getArea());
            if (yAxisValueSource == IntensityPlotParameters.PeakRTOption)
                values.add(peaks[i].getRT());
        }
        float floatValues[] = CollectionUtils.toFloatArray(values);
        float std = MathUtils.calcStd(floatValues);
        return std;
    }

    public Number getStdDevValue(Comparable rowKey, Comparable columnKey) {
        throw (new UnsupportedOperationException("Unsupported"));
    }

    public int getColumnIndex(Comparable column) {
        for (int i = 0; i < selectedFiles.length; i++) {
            if (selectedFiles[i].toString().equals(column))
                return i;
        }
        return -1;
    }

    public Comparable getColumnKey(int column) {
        return xValues[column];
    }

    public List getColumnKeys() {
        return Arrays.asList(xValues);
    }

    public int getRowIndex(Comparable row) {
        for (int i = 0; i < selectedRows.length; i++) {
            if (selectedRows[i].toString().equals(row))
                return i;
        }
        return -1;
    }

    public Comparable getRowKey(int row) {
        return selectedRows[row].toString();
    }

    public List getRowKeys() {
        return Arrays.asList(selectedRows);
    }

    public Number getValue(Comparable rowKey, Comparable columnKey) {
        return getMeanValue(rowKey, columnKey);

    }

    public int getColumnCount() {
        return xValues.length;
    }

    public int getRowCount() {
        return selectedRows.length;
    }

    public Number getValue(int row, int column) {
        return getMeanValue(row, column);
    }

    public Number getEndX(int row, int column) {
        return getEndXValue(row, column);
    }

    public double getEndXValue(int row, int column) {
        return ((Number) xValues[column]).doubleValue();

    }

    public Number getEndY(int row, int column) {
        return getEndYValue(row, column);
    }

    public double getEndYValue(int row, int column) {
        return getMeanValue(row, column).doubleValue()
                + getStdDevValue(row, column).doubleValue();
    }

    public Number getStartX(int row, int column) {
        return getEndXValue(row, column);
    }

    public double getStartXValue(int row, int column) {
        return getEndXValue(row, column);
    }

    public Number getStartY(int row, int column) {
        return getStartYValue(row, column);
    }

    public double getStartYValue(int row, int column) {
        return getMeanValue(row, column).doubleValue()
                - getStdDevValue(row, column).doubleValue();
    }

    public DomainOrder getDomainOrder() {
        return DomainOrder.ASCENDING;
    }

    public int getItemCount(int series) {
        return xValues.length;
    }

    public Number getX(int series, int item) {
        return getStartX(series, item);
    }

    public double getXValue(int series, int item) {
        return getStartX(series, item).doubleValue();
    }

    public Number getY(int series, int item) {
        return getMeanValue(series, item);
    }

    public double getYValue(int series, int item) {
        return getMeanValue(series, item).doubleValue();
    }

    public int getSeriesCount() {
        return selectedRows.length;
    }

    public Comparable getSeriesKey(int series) {
        return getRowKey(series);
    }

    public int indexOf(Comparable value) {
        throw (new UnsupportedOperationException("Unsupported"));
    }

}
