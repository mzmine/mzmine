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

import net.sf.mzmine.interfaces.Scan;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.io.RawDataFile.PreloadLevel;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskController;
import net.sf.mzmine.taskcontrol.Task.TaskPriority;
import net.sf.mzmine.util.RawDataAcceptor;
import net.sf.mzmine.util.RawDataRetrievalTask;

import org.jfree.data.xy.DefaultTableXYDataset;
import org.jfree.data.xy.XYSeries;


/**
 *
 */
class TICDataSet extends DefaultTableXYDataset implements RawDataAcceptor  {

    // redraw the chart every 100 ms while updating
    private static final int REDRAW_INTERVAL = 100;
    
    private RawDataFile rawDataFile;
    private int[] scanNumbers;
    private XYSeries series;
    private TICVisualizer visualizer;
    
    private boolean xicMode = false;
    private double mzRangeMin, mzRangeMax;
    
    private Date lastRedrawTime = new Date();
    
    TICDataSet(RawDataFile rawDataFile, int msLevel, TICVisualizer visualizer) {
        
        series = new XYSeries(rawDataFile.getOriginalFile().getName(), false, false);
        
        addSeries(series);
        
        this.visualizer = visualizer;
        this.rawDataFile = rawDataFile;
        
        scanNumbers = rawDataFile.getScanNumbers(msLevel);
        assert scanNumbers != null;

        setTICMode();
        
    }
    
    void setTICMode() {
        
        xicMode = false;
        
        Task updateTask = new RawDataRetrievalTask(rawDataFile, scanNumbers,
                this);

        /*
         * if the file data is preloaded in memory, we can update the visualizer
         * in this thread, otherwise start a task
         */
        if (rawDataFile.getPreloadLevel() == PreloadLevel.PRELOAD_ALL_SCANS) {
            visualizer.taskStarted(updateTask);
            updateTask.run();
            visualizer.taskFinished(updateTask);
        } else
            TaskController.getInstance().addTask(updateTask, TaskPriority.HIGH, visualizer);
        
    }
    
    void setXICMode(double mzMin, double mzMax) {
        
        xicMode = true;
        this.mzRangeMin = mzMin;
        this.mzRangeMax = mzMax;
        
        Task updateTask = new RawDataRetrievalTask(rawDataFile, scanNumbers,
                this);

        /*
         * if the file data is preloaded in memory, we can update the visualizer
         * in this thread, otherwise start a task
         */
        if (rawDataFile.getPreloadLevel() == PreloadLevel.PRELOAD_ALL_SCANS) {
            visualizer.taskStarted(updateTask);
            updateTask.run();
            visualizer.taskFinished(updateTask);
        } else
            TaskController.getInstance().addTask(updateTask, visualizer);
    }
    
    int getSeriesIndex(double retentionTime, double intensity) {
        int seriesIndex = series.indexOf(retentionTime);
        if (seriesIndex < 0) return -1;
        if (series.getY(seriesIndex).equals(intensity)) return seriesIndex;
        return -1;
    }
    
    int getScanNumber(int index) {
        return scanNumbers[index];
    }
    
    RawDataFile getRawDataFile() {
        return rawDataFile;
    }

    /**
     * @see net.sf.mzmine.util.RawDataAcceptor#getTaskDescription()
     */
    public String getTaskDescription() {
        return "Updating TIC visualizer of " + rawDataFile;
    }

    /**
     * @see net.sf.mzmine.util.RawDataAcceptor#addScan(net.sf.mzmine.interfaces.Scan)
     */
    public void addScan(Scan scan) {

        double intensityValues[] = scan.getIntensityValues();
        double mzValues[] = null;
        double totalIntensity = 0;
        
        if (xicMode)
            mzValues = scan.getMZValues();
        for (int j = 0; j < intensityValues.length; j++) {
            if ((!xicMode)
                    || ((mzValues[j] >= mzRangeMin) && (mzValues[j] <= mzRangeMax)))
                totalIntensity += intensityValues[j];
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

        series.add(scan.getRetentionTime() * 1000, totalIntensity,
                notify);
        
        
    }
}
