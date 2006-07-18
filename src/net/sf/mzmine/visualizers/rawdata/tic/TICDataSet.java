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

/**
 * 
 */
package net.sf.mzmine.visualizers.rawdata.tic;

import java.util.Date;

import net.sf.mzmine.data.Scan;
import net.sf.mzmine.io.MZmineOpenedFile;
import net.sf.mzmine.io.RawDataAcceptor;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.io.util.RawDataRetrievalTask;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskController;
import net.sf.mzmine.taskcontrol.Task.TaskPriority;
import net.sf.mzmine.util.ScanUtils;
import net.sf.mzmine.visualizers.rawdata.tic.TICVisualizerWindow.PlotType;

import org.jfree.data.xy.DefaultTableXYDataset;
import org.jfree.data.xy.XYSeries;

/**
 * 
 */
class TICDataSet extends DefaultTableXYDataset implements RawDataAcceptor {

    // redraw the chart every 100 ms while updating
    private static final int REDRAW_INTERVAL = 100;

    private TICVisualizerWindow visualizer;
    private MZmineOpenedFile dataFile;
    private RawDataFile rawDataFile;
    private int[] scanNumbers;
    private double[] mzValues;
    private XYSeries series;
    private double mzMin, mzMax;

    private Date lastRedrawTime = new Date();

    TICDataSet(TaskController taskController, MZmineOpenedFile dataFile, int scanNumbers[], double mzMin, double mzMax, TICVisualizerWindow visualizer) {

        this.visualizer = visualizer;
        this.mzMin = mzMin;
        this.mzMax = mzMax;
        this.dataFile = dataFile;
        this.rawDataFile = dataFile.getCurrentFile();
        this.scanNumbers = scanNumbers;
        
        series = new XYSeries(rawDataFile.toString(), false,
                false);

        addSeries(series);
        
        if (visualizer.getPlotType() == PlotType.BASE_PEAK)
            mzValues = new double[scanNumbers.length];
        
        Task updateTask = new RawDataRetrievalTask(rawDataFile, scanNumbers,
                "Updating TIC visualizer of " + rawDataFile, this);

        taskController.addTask(updateTask, TaskPriority.HIGH, visualizer);
        
    }

    int getSeriesIndex(double retentionTime, double intensity) {
        int seriesIndex = series.indexOf(retentionTime);
        if (seriesIndex < 0)
            return -1;
        if (series.getY(seriesIndex).equals(intensity))
            return seriesIndex;
        return -1;
    }

    int getScanNumber(int index) {
        return scanNumbers[index];
    }
    
    double getMZValue(int index) {
        return mzValues[index];
    }

    MZmineOpenedFile getDataFile() {
        return dataFile;
    }

    /**
     * @see net.sf.mzmine.io.RawDataAcceptor#addScan(net.sf.mzmine.data.Scan)
     */
    public void addScan(Scan scan, int ind) {

        double intensityValues[] = scan.getIntensityValues();

        double totalIntensity = 0;

        switch (visualizer.getPlotType()) {
            
            case TIC:
                double mzValues[] = scan.getMZValues();
                for (int j = 0; j < intensityValues.length; j++) {
                    if ((mzValues[j] >= mzMin) && (mzValues[j] <= mzMax))
                        totalIntensity += intensityValues[j];
                }
                break;
                
            case BASE_PEAK:
                double basePeak[] = ScanUtils.findBasePeak(scan, mzMin, mzMax);
                this.mzValues[series.getItemCount()] = basePeak[0];
                totalIntensity = basePeak[1];
                break;
                
        }

        // redraw every REDRAW_INTERVAL ms
        boolean notify = false;
        Date currentTime = new Date();
        if (currentTime.getTime() - lastRedrawTime.getTime() > REDRAW_INTERVAL) {
            notify = true;
            lastRedrawTime = currentTime;
        }

        // always redraw when we add last value
        if (scan.getScanNumber() == scanNumbers[scanNumbers.length - 1])
            notify = true;

        series.add(scan.getRetentionTime(), totalIntensity, notify);


    }
}
