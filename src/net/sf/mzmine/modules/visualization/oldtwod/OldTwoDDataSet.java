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

package net.sf.mzmine.modules.visualization.oldtwod;

import java.awt.Point;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.util.Date;
import java.util.logging.Logger;

import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.io.util.RawDataAcceptor;
import net.sf.mzmine.io.util.RawDataRetrievalTask;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.Task.TaskPriority;
import net.sf.mzmine.util.ScanUtils;
import net.sf.mzmine.util.ScanUtils.BinningType;

import org.jfree.chart.event.PlotChangeEvent;
import org.jfree.chart.event.PlotChangeListener;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.AbstractXYZDataset;

/**
 * 
 */
public class OldTwoDDataSet implements RawDataAcceptor {
	
	public static final int NO_DATA = 0;
	public static final int LOADING_DATA = 1;
	public static final int DATA_READY = 2;
	

    private Logger logger = Logger.getLogger(this.getClass().getName());
    
    private RawDataFile rawDataFile;
    
    private OldTwoDVisualizerWindow visualizer;

    private float intensityMatrix[][];
       
    // bounds for rendered data range
    private int scanMin, scanMax;
    private float rtMin, rtMax, rtStep;
    private float mzMin, mzMax, mzStep;

    // max intensity in current image
    private float maxIntensity;
    
    private Task currentTask;
    
    

    public OldTwoDDataSet(RawDataFile rawDataFile, OldTwoDVisualizerWindow visualizer) {

        this.rawDataFile = rawDataFile;
        this.visualizer = visualizer;

    }
    
    public void resampleIntensityMatrix(int msLevel, float rtMin, float rtMax, float mzMin,
            							float mzMax, int rtResolution, int mzResolution) {

    	System.out.println("resampleIntensityMatrix");
    	System.out.println("msLevel=" + msLevel);
    	System.out.println("rtMin=" + rtMin);
    	System.out.println("rtMax=" + rtMax);
    	System.out.println("mzMin=" + mzMin);
    	System.out.println("mzMax=" + mzMax);
    	
    	
    	this.rtMin = rtMin;
        this.rtMax = rtMax;
        this.mzMin = mzMin;
        this.mzMax = mzMax;
        this.rtStep = (rtMax - rtMin) / (rtResolution - 1);
        this.mzStep = (mzMax - mzMin) / (mzResolution - 1);
        intensityMatrix = new float[rtResolution][mzResolution];
      
        int scanNumbers[] = rawDataFile.getScanNumbers(msLevel, rtMin, rtMax);
        assert scanNumbers != null;
        scanMin = scanNumbers[0];
        scanMax = scanNumbers[scanNumbers.length-1];
        for (int scanNumber : scanNumbers) {
        	if (scanNumber<scanMin) scanMin = scanNumber;
        	if (scanNumber>scanMax) scanMax = scanNumber;
        }
        

        currentTask = new RawDataRetrievalTask(rawDataFile, scanNumbers,
                "Updating 2D visualizer of " + rawDataFile, this);

        MZmineCore.getTaskController().addTask(currentTask, TaskPriority.HIGH, visualizer);
    	
    }

    /**
     * @see net.sf.mzmine.io.RawDataAcceptor#addScan(net.sf.mzmine.data.Scan)
     */
    public synchronized void addScan(Scan scan, int index, int total) {

        int bitmapSizeX, bitmapSizeY;

        logger.finest("Adding scan " + scan);
        
        bitmapSizeX = intensityMatrix.length;
        bitmapSizeY = intensityMatrix[0].length;

        if ((scan.getRetentionTime() < rtMin)
                || (scan.getRetentionTime() > rtMax))
            return;

        int xIndex = (int) Math.floor((scan.getRetentionTime() - rtMin)
                / rtStep);

      
        float mzValues[] = scan.getMZValues();
        float intensityValues[] = scan.getIntensityValues();

        float binnedIntensities[] = ScanUtils.binValues(mzValues, intensityValues, mzMin, mzMax, bitmapSizeY, false, BinningType.SUM);
        
        for (int i = 0; i < bitmapSizeY; i++) {

            intensityMatrix[xIndex][bitmapSizeY-i-1] += binnedIntensities[i];

            if (intensityMatrix[xIndex][bitmapSizeY-i-1] > maxIntensity)
                maxIntensity = intensityMatrix[xIndex][bitmapSizeY-i-1];
            
        }

        if (index>=(total-1))
        	visualizer.datasetUpdateReady();
        else
        	visualizer.datasetUpdating();

        	

    }


    public float[][] getCurrentIntensityMatrix() {
        return intensityMatrix;
    }

    /*
    public float getRetentionTime(int xIndex) {
    	return retentionTimes[xIndex];
    }
    
    public float getXIndex(float retentionTime) {

    	for (int xIndex=0; xIndex<(retentionTimes.length-1); xIndex++) {
    		if (retentionTimes[xIndex+1]>retentionTime)
    			return xIndex;
    	}
    	
    	return (retentionTimes.length-1);
    		
    }
    */
    public float getRetentionTime(int scanNumber) {
    	return rawDataFile.getScan(scanNumber).getRetentionTime();
    }
    
    public int getCurrentMinScan() { 
    	return scanMin;
    }
    
    public int getCurrentMaxScan() {
    	return scanMax;
    }
    
    public float getCurrentMinRT() {
    	return rtMin;
    }
    
    public float getCurrentMaxRT() {
    	return rtMax;
    }
    
    public float getCurrentMinMZ() {
    	return mzMin;
    }
    
    public float getCurrentMaxMZ() {
    	return mzMax;
    }
    
    public float getMaxIntensity() {
    	return maxIntensity;
    }
    
    
    public int getStatus() {
    	if (currentTask == null) return NO_DATA;
    	if ((currentTask.getStatus() == Task.TaskStatus.FINISHED) ||
    		(currentTask.getStatus() == Task.TaskStatus.CANCELED) ||
    		(currentTask.getStatus() == Task.TaskStatus.ERROR)) return DATA_READY;
    	
    	if ((currentTask.getStatus() == Task.TaskStatus.PROCESSING) ||
    		(currentTask.getStatus() == Task.TaskStatus.WAITING)) return LOADING_DATA;
    	
    	return LOADING_DATA;
    }
    
}