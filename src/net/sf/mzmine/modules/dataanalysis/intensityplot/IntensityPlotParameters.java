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

import java.util.logging.Logger;

import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.ParameterSet;
import net.sf.mzmine.io.OpenedRawDataFile;

import org.dom4j.Element;

/**
 */
class IntensityPlotParameters implements ParameterSet {

    public static final String DataFileOption = "Raw data file";
    public static final String PeakIntensityOption = "Peak intensity";
    public static final String PeakAreaOption = "Peak area";
    public static final String PeakRTOption = "Peak retention time";

    private PeakList sourcePeakList;

    private Object xAxisValueSource;
    private Object yAxisValueSource;

    private OpenedRawDataFile selectedDataFiles[];
    private PeakListRow selectedRows[];

    IntensityPlotParameters() {
    }

    IntensityPlotParameters(PeakList sourcePeakList) {
        this.sourcePeakList = sourcePeakList;
    }

    /**
     * @param axisValueSource
     * @param axisValueSource2
     * @param datafiles
     * @param selectedRows
     */
    IntensityPlotParameters(PeakList sourcePeakList,
            Object xAxisValueSource, Object yAxisValueSource,
            OpenedRawDataFile[] selectedDataFiles,
            PeakListRow[] selectedRows) {
        this.sourcePeakList = sourcePeakList;
        this.xAxisValueSource = xAxisValueSource;
        this.yAxisValueSource = yAxisValueSource;
        this.selectedDataFiles = selectedDataFiles;
        this.selectedRows = selectedRows;
    }

    /**
     * @return Returns the selectedDataFiles.
     */
    OpenedRawDataFile[] getSelectedDataFiles() {
        return selectedDataFiles;
    }

    /**
     * @param selectedDataFiles The selectedDataFiles to set.
     */
    void setSelectedDataFiles(OpenedRawDataFile[] selectedDataFiles) {
        this.selectedDataFiles = selectedDataFiles;
    }

    /**
     * @return Returns the selectedRows.
     */
    PeakListRow[] getSelectedRows() {
        return selectedRows;
    }

    /**
     * @param selectedRows The selectedRows to set.
     */
    void setSelectedRows(PeakListRow[] selectedRows) {
        this.selectedRows = selectedRows;
    }

    /**
     * @return Returns the sourcePeakList.
     */
    PeakList getSourcePeakList() {
        return sourcePeakList;
    }

    /**
     * @param sourcePeakList The sourcePeakList to set.
     */
    void setSourcePeakList(PeakList sourcePeakList) {
        this.sourcePeakList = sourcePeakList;
    }

    /**
     * @return Returns the xAxisValueSource.
     */
    Object getXAxisValueSource() {
        return xAxisValueSource;
    }

    /**
     * @param axisValueSource The xAxisValueSource to set.
     */
    void setXAxisValueSource(Object axisValueSource) {
        xAxisValueSource = axisValueSource;
    }

    /**
     * @return Returns the yAxisValueSource.
     */
    Object getYAxisValueSource() {
        return yAxisValueSource;
    }

    /**
     * @param axisValueSource The yAxisValueSource to set.
     */
    void setYAxisValueSource(Object axisValueSource) {
        yAxisValueSource = axisValueSource;
    }

    /**
     * Represent method's parameters and their values in human-readable format
     */
    public String toString() {
        return "X value source: " + xAxisValueSource
                + ", Y axis value source: " + yAxisValueSource
                + ", data files: " + selectedDataFiles + ", selected peaks: "
                + selectedRows;
    }

    /**
     * @see net.sf.mzmine.data.ParameterSet#getParameterValue(net.sf.mzmine.data.Parameter)
     */
    public Object getParameterValue(Parameter parameter) {
        // ignore
        return null;
    }

    /**
     * @see net.sf.mzmine.data.ParameterSet#clone()
     */
    public IntensityPlotParameters clone() {
        return new IntensityPlotParameters(sourcePeakList,
                xAxisValueSource, yAxisValueSource, selectedDataFiles,
                selectedRows);
    }

}
