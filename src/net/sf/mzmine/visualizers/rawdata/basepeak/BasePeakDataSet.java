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
package net.sf.mzmine.visualizers.rawdata.basepeak;

import java.util.Date;

import net.sf.mzmine.interfaces.Scan;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskController;
import net.sf.mzmine.taskcontrol.Task.TaskPriority;
import net.sf.mzmine.util.RawDataAcceptor;
import net.sf.mzmine.util.RawDataRetrievalTask;
import net.sf.mzmine.util.ScanUtils;

import org.jfree.data.xy.DefaultTableXYDataset;
import org.jfree.data.xy.XYSeries;


/**
 *
 */
class BasePeakDataSet extends DefaultTableXYDataset implements RawDataAcceptor {

    // redraw the chart every 100 ms while updating
    private static final int REDRAW_INTERVAL = 100;
    
    private RawDataFile rawDataFile;
    private int[] scanNumbers;
    private double[] mzValues;
    private XYSeries series;
    private double mzMin, mzMax;
    
    private Date lastRedrawTime = new Date();
    
    BasePeakDataSet(RawDataFile rawDataFile, int scanNumbers[], double mzMin, double mzMax, BasePeakVisualizer visualizer) {
        
        this.mzMin = mzMin;
        this.mzMax = mzMax;
        this.rawDataFile = rawDataFile;
        this.scanNumbers = scanNumbers;
        
        series = new XYSeries(rawDataFile.getOriginalFile().getName(), false, false);
        
        addSeries(series);
        
        mzValues = new double[scanNumbers.length];

        Task updateTask = new RawDataRetrievalTask(rawDataFile, scanNumbers,
                "Updating base peak visualizer of " + rawDataFile, this);

        TaskController.getInstance().addTask(updateTask, TaskPriority.HIGH, visualizer);
        
    }
    
    int getSeriesIndex(double retentionTime, double intensity) {
        int seriesIndex = series.indexOf(retentionTime);
        if (seriesIndex < 0) return -1;
        if (series.getY(seriesIndex).equals(intensity)) return seriesIndex;
        return -1;
    }
    
    
    double getMZValue(int index) {
        return mzValues[index];
    }
    
    int getScanNumber(int index) {
        return scanNumbers[index];
    }
    
    RawDataFile getRawDataFile() {
        return rawDataFile;
    }

    /**
     * @see net.sf.mzmine.util.RawDataAcceptor#addScan(net.sf.mzmine.interfaces.Scan)
     */
    public void addScan(Scan scan, int index) {

        double basePeak[] = ScanUtils.findBasePeak(scan, mzMin, mzMax);
        
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

        series.add(scan.getRetentionTime() * 1000, basePeak[1],
                notify);

        mzValues[series.getItemCount() - 1] = basePeak[0];
        
    }
}
