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

package net.sf.mzmine.modules.visualization.intensityplot;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;

import org.jfree.data.DomainOrder;
import org.jfree.data.general.AbstractDataset;
import org.jfree.data.statistics.StatisticalCategoryDataset;
import org.jfree.data.xy.IntervalXYDataset;

import com.google.common.primitives.Doubles;

import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.UserParameter;
import net.sf.mzmine.parameters.parametertypes.DoubleParameter;
import net.sf.mzmine.util.MathUtils;

/**
 * This class implements 2 kinds of data sets - CategoryDataset and XYDataset
 * CategoryDataset is used if X axis is a raw data file or string parameter
 * XYDataset is used if X axis is a number parameter
 */
class IntensityPlotDataset extends AbstractDataset
        implements StatisticalCategoryDataset, IntervalXYDataset {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private Object xAxisValueSource;
    private YAxisValueSource yAxisValueSource;
    private Comparable<?> xValues[];
    private RawDataFile selectedFiles[];
    private PeakListRow selectedRows[];

    @SuppressWarnings("rawtypes")
    IntensityPlotDataset(ParameterSet parameters) {

        PeakList peakList = parameters
                .getParameter(IntensityPlotParameters.peakList).getValue()
                .getMatchingPeakLists()[0];
        this.xAxisValueSource = parameters
                .getParameter(IntensityPlotParameters.xAxisValueSource)
                .getValue();
        this.yAxisValueSource = parameters
                .getParameter(IntensityPlotParameters.yAxisValueSource)
                .getValue();
        this.selectedFiles = parameters
                .getParameter(IntensityPlotParameters.dataFiles).getValue();

        this.selectedRows = parameters
                .getParameter(IntensityPlotParameters.selectedRows)
                .getMatchingRows(peakList);

        if (xAxisValueSource instanceof ParameterWrapper) {
            MZmineProject project = MZmineCore.getProjectManager()
                    .getCurrentProject();
            UserParameter xAxisParameter = ((ParameterWrapper) xAxisValueSource)
                    .getParameter();
            LinkedHashSet<Comparable> parameterValues = new LinkedHashSet<Comparable>();
            for (RawDataFile file : selectedFiles) {
                Object value = project.getParameterValue(xAxisParameter, file);
                parameterValues.add((Comparable<?>) value);
            }
            xValues = parameterValues.toArray(new Comparable[0]);

            // if we have a numerical axis, we don't want the values to be
            // sorted by the data file order, but rather numerically
            if (xAxisParameter instanceof DoubleParameter) {
                Arrays.sort(xValues);
            }
        }

        if (xAxisValueSource == IntensityPlotParameters.rawDataFilesOption) {
            xValues = new String[selectedFiles.length];
            for (int i = 0; i < selectedFiles.length; i++)
                xValues[i] = selectedFiles[i].getName();
        }
    }

    Feature[] getPeaks(int row, int column) {
        return getPeaks(xValues[column], selectedRows[row]);
    }

    Feature[] getPeaks(Comparable<?> xValue, PeakListRow row) {
        RawDataFile files[] = getFiles(xValue);
        Feature[] peaks = new Feature[files.length];
        for (int i = 0; i < files.length; i++) {
            peaks[i] = row.getPeak(files[i]);
        }
        return peaks;
    }

    RawDataFile[] getFiles(int column) {
        return getFiles(xValues[column]);
    }

    RawDataFile[] getFiles(Comparable<?> xValue) {
        if (xAxisValueSource instanceof String) {
            RawDataFile columnFile = selectedFiles[getColumnIndex(xValue)];
            return new RawDataFile[] { columnFile };
        }
        if (xAxisValueSource instanceof ParameterWrapper) {
            HashSet<RawDataFile> files = new HashSet<RawDataFile>();
            UserParameter<?, ?> xAxisParameter = ((ParameterWrapper) xAxisValueSource)
                    .getParameter();
            MZmineProject project = MZmineCore.getProjectManager()
                    .getCurrentProject();
            for (RawDataFile file : selectedFiles) {
                Object fileValue = project.getParameterValue(xAxisParameter,
                        file);
                if (fileValue == null)
                    continue;
                if (fileValue.equals(xValue))
                    files.add(file);
            }
            return files.toArray(new RawDataFile[0]);
        }
        return null;
    }

    public Number getMeanValue(int row, int column) {
        Feature[] peaks = getPeaks(xValues[column], selectedRows[row]);
        HashSet<Double> values = new HashSet<Double>();
        for (int i = 0; i < peaks.length; i++) {
            if (peaks[i] == null)
                continue;
            if (yAxisValueSource == YAxisValueSource.HEIGHT)
                values.add(peaks[i].getHeight());
            if (yAxisValueSource == YAxisValueSource.AREA)
                values.add(peaks[i].getArea());
            if (yAxisValueSource == YAxisValueSource.RT)
                values.add(peaks[i].getRT());
        }
        double doubleValues[] = Doubles.toArray(values);
        if (doubleValues.length == 0)
            return 0;
        double mean = MathUtils.calcAvg(doubleValues);
        return mean;
    }

    @SuppressWarnings("rawtypes")
    public Number getMeanValue(Comparable rowKey, Comparable columnKey) {
        throw (new UnsupportedOperationException("Unsupported"));
    }

    public Number getStdDevValue(int row, int column) {
        Feature[] peaks = getPeaks(xValues[column], selectedRows[row]);

        // if we have only 1 peak, there is no standard deviation
        if (peaks.length == 1)
            return 0;

        HashSet<Double> values = new HashSet<Double>();
        for (int i = 0; i < peaks.length; i++) {
            if (peaks[i] == null)
                continue;
            if (yAxisValueSource == YAxisValueSource.HEIGHT)
                values.add(peaks[i].getHeight());
            if (yAxisValueSource == YAxisValueSource.AREA)
                values.add(peaks[i].getArea());
            if (yAxisValueSource == YAxisValueSource.RT)
                values.add(peaks[i].getRT());
        }
        double doubleValues[] = Doubles.toArray(values);
        double std = MathUtils.calcStd(doubleValues);
        return std;
    }

    @SuppressWarnings("rawtypes")
    public Number getStdDevValue(Comparable rowKey, Comparable columnKey) {
        throw (new UnsupportedOperationException("Unsupported"));
    }

    @SuppressWarnings("rawtypes")
    public int getColumnIndex(Comparable column) {
        for (int i = 0; i < selectedFiles.length; i++) {
            if (selectedFiles[i].getName().equals(column))
                return i;
        }
        return -1;
    }

    public Comparable<?> getColumnKey(int column) {
        return xValues[column];
    }

    @SuppressWarnings("rawtypes")
    public List getColumnKeys() {
        return Arrays.asList(xValues);
    }

    @SuppressWarnings("rawtypes")
    public int getRowIndex(Comparable row) {
        for (int i = 0; i < selectedRows.length; i++) {
            if (selectedRows[i].toString().equals(row))
                return i;
        }
        return -1;
    }

    public Comparable<?> getRowKey(int row) {
        return selectedRows[row].toString();
    }

    @SuppressWarnings("rawtypes")
    public List getRowKeys() {
        return Arrays.asList(selectedRows);
    }

    @SuppressWarnings("rawtypes")
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

    public Comparable<?> getSeriesKey(int series) {
        return getRowKey(series);
    }

    @SuppressWarnings("rawtypes")
    public int indexOf(Comparable value) {
        return getRowIndex(value);
    }

}
