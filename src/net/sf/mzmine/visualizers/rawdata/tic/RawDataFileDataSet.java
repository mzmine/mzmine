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

import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.io.RawDataFile.PreloadLevel;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskController;
import net.sf.mzmine.taskcontrol.Task.TaskPriority;

import org.jfree.data.xy.DefaultTableXYDataset;
import org.jfree.data.xy.XYSeries;


/**
 *
 */
class RawDataFileDataSet extends DefaultTableXYDataset  {

    private RawDataFile rawDataFile;
    private int[] scanNumbers;
    private XYSeries series;
    private TICVisualizer visualizer; 
    
    RawDataFileDataSet(RawDataFile rawDataFile, int msLevel, TICVisualizer visualizer) {
        
        series = new XYSeries(rawDataFile.getOriginalFile().getName(), false, false);
        
        addSeries(series);
        
        this.visualizer = visualizer;
        this.rawDataFile = rawDataFile;
        
        scanNumbers = rawDataFile.getScanNumbers(msLevel);
        assert scanNumbers != null;

        setTICMode();
        
    }
    
    void setTICMode() {
        
        Task updateTask = new TICDataRetrievalTask(rawDataFile, scanNumbers,
                series);

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
        
        Task updateTask = new TICDataRetrievalTask(rawDataFile, scanNumbers,
                series, mzMin, mzMax);

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
}
