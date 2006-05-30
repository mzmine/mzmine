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
package net.sf.mzmine.visualizers.rawdata.twod;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;
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

import net.sf.mzmine.interfaces.Scan;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.io.RawDataFile.PreloadLevel;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskController;
import net.sf.mzmine.taskcontrol.Task.TaskPriority;
import net.sf.mzmine.util.RawDataAcceptor;
import net.sf.mzmine.util.RawDataRetrievalTask;

import org.jfree.data.xy.AbstractXYZDataset;

/**
 * 
 */
class TwoDDataSet extends AbstractXYZDataset implements RawDataAcceptor {

    // redraw the chart every 100 ms while updating
    private static final int REDRAW_INTERVAL = 100;

    private RawDataFile rawDataFile;
    private TwoDVisualizer visualizer;
    
    private int[] scanNumbers;
        
    private double intensityMatrix[][];
    
    private double rtMin, rtMax, mzMin, mzMax;
    private double rtStep, mzStep;
    
    private int bitmapSizeX, bitmapSizeY;
    
    private double maxIntensity;
    
    
    private Date lastRedrawTime = new Date();

    TwoDDataSet(RawDataFile rawDataFile, int msLevel, TwoDVisualizer visualizer) {

        this.visualizer = visualizer;
        this.rawDataFile = rawDataFile;

        scanNumbers = rawDataFile.getScanNumbers(msLevel);
        assert scanNumbers != null;
        
        rtMin = rawDataFile.getDataMinRT(msLevel);
        rtMax = rawDataFile.getDataMaxRT(msLevel);
        mzMin = rawDataFile.getDataMinMZ(msLevel);
        mzMax = rawDataFile.getDataMaxMZ(msLevel);
        
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize(); 
        bitmapSizeX = (int) 400; // screenSize.getWidth();
        bitmapSizeY = (int) 300; // screenSize.getHeight();
        
        rtStep = (rtMax - rtMin) / (bitmapSizeX - 1); 
        mzStep = (mzMax - mzMin) / (bitmapSizeY - 1);
        
        intensityMatrix = new double[bitmapSizeX][bitmapSizeY];
        
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
            TaskController.getInstance().addTask(updateTask, TaskPriority.HIGH,
                    this.visualizer);

    }

    /**
     * @see net.sf.mzmine.util.RawDataAcceptor#getTaskDescription()
     */
    public String getTaskDescription() {
        return "Updating 2D visualizer of " + rawDataFile;
    }

    /**
     * @see net.sf.mzmine.util.RawDataAcceptor#addScan(net.sf.mzmine.interfaces.Scan)
     */
    public synchronized void addScan(Scan scan) {

        if ((scan.getRetentionTime() < rtMin) || (scan.getRetentionTime() > rtMax)) return;
        int xIndex = (int) Math.floor((scan.getRetentionTime() - rtMin) / rtStep);
        int yIndex;
        
        
        double mzValues[] = scan.getMZValues();
        double intensityValues[] = scan.getIntensityValues();
        
        for (int i = 0; i < mzValues.length; i++) {
            
            if (mzValues[i] < mzMin) continue;
            if (mzValues[i] > mzMax) break;
            yIndex = (int) Math.floor((mzValues[i] - mzMin) / mzStep);
            intensityMatrix[xIndex][yIndex] += intensityValues[i];
             
            if (intensityMatrix[xIndex][yIndex] > maxIntensity) maxIntensity = intensityMatrix[xIndex][yIndex];
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

        if (notify) {
            renderImage();
            fireDatasetChanged();
        }

    }

    /**
     * @see org.jfree.data.general.AbstractSeriesDataset#getSeriesCount()
     */
    public int getSeriesCount() {
        return 1;
    }

    /**
     * @see org.jfree.data.general.AbstractSeriesDataset#getSeriesKey(int)
     */
    public Comparable getSeriesKey(int series) {
        return rawDataFile.toString();
    }

    /**
     * @see org.jfree.data.xy.XYZDataset#getZ(int, int)
     */
    public Number getZ(int series, int item) {
        return intensityMatrix[item / bitmapSizeY][item % bitmapSizeY];
    }

    /**
     * @see org.jfree.data.xy.XYDataset#getItemCount(int)
     */
    public int getItemCount(int series) {
        return bitmapSizeX * bitmapSizeY;
    }

    /**
     * @see org.jfree.data.xy.XYDataset#getX(int, int)
     */
    public Number getX(int series, int item) {
        return (rtMin + (rtStep * (item / bitmapSizeY))) * 1000;
    }

    /**
     * @see org.jfree.data.xy.XYDataset#getY(int, int)
     */
    public Number getY(int series, int item) {
        return mzMin + (mzStep * (item % bitmapSizeY));
    }
    
    double getMaxIntensity() {
        return maxIntensity;
    }
    
    double getRTStep() {
        return rtStep * 1000;
    }
    
    double getMZStep() {
        return mzStep;
    }
    
    
    private BufferedImage bi;
    
    
    BufferedImage getRenderedImage() {
        return bi;
    }
    
    synchronized void renderImage() {
        
        ColorSpace cs = null;
        double dataImgMax = Double.MAX_VALUE;

                cs = ColorSpace.getInstance(ColorSpace.CS_GRAY);

                dataImgMax = maxIntensity;

                
        // How many 8-bit components are used for representing shade of color in this color space?
        int nComp = cs.getNumComponents();
        int[] nBits = new int[nComp];
        for (int nb=0; nb<nComp; nb++) { nBits[nb]=8; }

        // Create sample model for storing the image
        ColorModel colorModel = new ComponentColorModel(cs, nBits, false,true,Transparency.OPAQUE,DataBuffer.TYPE_BYTE);
        SampleModel sampleModel = colorModel.createCompatibleSampleModel(bitmapSizeX, bitmapSizeY);
        DataBuffer dataBuffer = sampleModel.createDataBuffer();
        
        byte count=0;
        byte[] b = new byte[nComp];
        double bb;
        double fac;
        for (int xpos=0; xpos<bitmapSizeX; xpos++) {
            for (int ypos=0; ypos<bitmapSizeY; ypos++) {

                bb = 0;
                        bb = (double)((0.20*dataImgMax-intensityMatrix[xpos][ypos])/(0.20*dataImgMax));
                        if (bb<0) { bb = 0; }
                        b[0] = (byte)(255*bb);

                sampleModel.setDataElements(xpos,ypos,b,dataBuffer);
            }

        }
        
        WritableRaster wr = Raster.createWritableRaster(sampleModel,dataBuffer, new Point(0,0));
        bi = new BufferedImage(colorModel, wr,true,null);
        
    }
       
}
