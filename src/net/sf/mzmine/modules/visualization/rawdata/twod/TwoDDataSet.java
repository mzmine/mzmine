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

package net.sf.mzmine.modules.visualization.rawdata.twod;

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

import net.sf.mzmine.data.Scan;
import net.sf.mzmine.io.RawDataAcceptor;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.io.util.RawDataRetrievalTask;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskController;
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
class TwoDDataSet extends AbstractXYZDataset implements RawDataAcceptor,
        PlotChangeListener {

    private Logger logger = Logger.getLogger(this.getClass().getName());
    
    // redraw the chart every 100 ms while updating
    private static final int REDRAW_INTERVAL = 100;

    private RawDataFile rawDataFile;

    private double totalIntensityMatrix[][];
    private double currentIntensityMatrix[][];

    // bounds for the total (zoom-out) image
    private double totalRTMin, totalRTMax, totalMZMin, totalMZMax;
    private double totalRTStep, totalMZStep;

    // bounds for current zoom image
    private double currentRTMin, currentRTMax, currentMZMin, currentMZMax;
    private double currentRTStep, currentMZStep;

    // max intensity in current image
    private double currentMaxIntensity;

    private BufferedImage currentImage;

    private Date lastRedrawTime = new Date();

    private boolean totalDataLoaded = false;

    TwoDDataSet(TaskController taskController, RawDataFile rawDataFile,
            int msLevel, double rtMin, double rtMax, double mzMin,
            double mzMax, int rtResolution, int mzResolution,
            TwoDVisualizerWindow visualizer) {

        this.rawDataFile = rawDataFile;

        totalRTMin = rtMin;
        totalRTMax = rtMax;
        totalMZMin = mzMin;
        totalMZMax = mzMax;
        totalRTStep = (rtMax - rtMin) / (rtResolution - 1);
        totalMZStep = (mzMax - mzMin) / (mzResolution - 1);
        totalIntensityMatrix = new double[rtResolution][mzResolution];

        currentRTMin = rtMin;
        currentRTMax = rtMax;
        currentMZMin = mzMin;
        currentMZMax = mzMax;
        currentRTStep = (rtMax - rtMin) / (rtResolution - 1);
        currentMZStep = (mzMax - mzMin) / (mzResolution - 1);
        currentIntensityMatrix = totalIntensityMatrix;

        int scanNumbers[] = rawDataFile.getScanNumbers(msLevel, rtMin, rtMax);
        assert scanNumbers != null;

        Task updateTask = new RawDataRetrievalTask(rawDataFile, scanNumbers,
                "Updating 2D visualizer of " + rawDataFile, this);

        taskController.addTask(updateTask, TaskPriority.HIGH, visualizer);

    }

    /**
     * @see net.sf.mzmine.io.RawDataAcceptor#addScan(net.sf.mzmine.data.Scan)
     */
    public synchronized void addScan(Scan scan, int index, int total) {

        double rtMin, rtMax, rtStep, mzMin, mzMax, mzStep;
        double intensityMatrix[][];
        int bitmapSizeX, bitmapSizeY;

        logger.finest("Adding scan " + scan);
        
        if (totalDataLoaded) {
            rtMin = currentRTMin;
            rtMax = currentRTMax;
            rtStep = currentRTStep;
            mzMin = currentMZMin;
            mzMax = currentMZMax;
            mzStep = currentMZStep;
            intensityMatrix = currentIntensityMatrix;
            bitmapSizeX = totalIntensityMatrix.length;
            bitmapSizeY = totalIntensityMatrix[0].length;
        } else {
            rtMin = totalRTMin;
            rtMax = totalRTMax;
            rtStep = totalRTStep;
            mzMin = totalMZMin;
            mzMax = totalMZMax;
            mzStep = totalMZStep;
            intensityMatrix = totalIntensityMatrix;
            bitmapSizeX = currentIntensityMatrix.length;
            bitmapSizeY = currentIntensityMatrix[0].length;

        }

        if ((scan.getRetentionTime() < rtMin)
                || (scan.getRetentionTime() > rtMax))
            return;

        int xIndex = (int) Math.floor((scan.getRetentionTime() - rtMin)
                / rtStep);

        double mzValues[] = scan.getMZValues();
        double intensityValues[] = scan.getIntensityValues();

        double binnedIntensities[] = ScanUtils.binValues(mzValues, intensityValues, mzMin, mzMax, bitmapSizeY, false, BinningType.SUM);
        
        for (int i = 0; i < bitmapSizeY; i++) {

            intensityMatrix[xIndex][i] += binnedIntensities[i];

            if (intensityMatrix[xIndex][i] > currentMaxIntensity)
                currentMaxIntensity = intensityMatrix[xIndex][i];
            
        }

        // redraw every REDRAW_INTERVAL ms
        boolean notify = false;
        Date currentTime = new Date();
        if (currentTime.getTime() - lastRedrawTime.getTime() > REDRAW_INTERVAL) {
            notify = true;
            lastRedrawTime = currentTime;
        }

        // always redraw when we add last value
        if (index == total - 1) {
            notify = true;
            totalDataLoaded = true;
        }

        if (notify) {
            renderCurrentImage();
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
     * @see org.jfree.data.xy.XYDataset#getItemCount(int)
     */
    public int getItemCount(int series) {
        // return 2 points - the lower left and upper right bounds
        return 2;
    }

    /**
     * @see org.jfree.data.xy.XYDataset#getX(int, int)
     */
    public Number getX(int series, int item) {
        if (item == 0) return currentRTMin; else return currentRTMax;
    }

    /**
     * @see org.jfree.data.xy.XYDataset#getY(int, int)
     */
    public Number getY(int series, int item) {
        if (item == 0) return currentMZMin; else return currentMZMax;
    }

    /**
     * @see org.jfree.data.xy.XYZDataset#getZ(int, int)
     */
    public Number getZ(int series, int item) {
        if (item == 0) return 0; else return currentMaxIntensity;
    }

    BufferedImage getCurrentImage() {
        return currentImage;
    }

    synchronized void renderCurrentImage() {

        ColorSpace cs = null;
        double dataImgMax = Double.MAX_VALUE;

        cs = ColorSpace.getInstance(ColorSpace.CS_GRAY);

        dataImgMax = currentMaxIntensity;

        int bitmapSizeX = currentIntensityMatrix.length;
        int bitmapSizeY = currentIntensityMatrix[0].length;

        // How many 8-bit components are used for representing shade of color in
        // this color space?
        int nComp = cs.getNumComponents();
        int[] nBits = new int[nComp];
        for (int nb = 0; nb < nComp; nb++) {
            nBits[nb] = 8;
        }

        // Create sample model for storing the image
        ColorModel colorModel = new ComponentColorModel(cs, nBits, false, true,
                Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
        SampleModel sampleModel = colorModel.createCompatibleSampleModel(
                bitmapSizeX, bitmapSizeY);
        DataBuffer dataBuffer = sampleModel.createDataBuffer();

        byte count = 0;
        byte[] b = new byte[nComp];
        double bb;
        double fac;
        for (int xpos = 0; xpos < bitmapSizeX; xpos++) {
            for (int ypos = 0; ypos < bitmapSizeY; ypos++) {

                bb = 0;
                bb = (double) ((0.20 * dataImgMax - currentIntensityMatrix[xpos][ypos]) / (0.20 * dataImgMax));
                if (bb < 0) {
                    bb = 0;
                }
                b[0] = (byte) (255 * bb);

                sampleModel.setDataElements(xpos, ypos, b, dataBuffer);
            }

        }

        WritableRaster wr = Raster.createWritableRaster(sampleModel,
                dataBuffer, new Point(0, 0));
        currentImage = new BufferedImage(colorModel, wr, true, null);

    }

    /**
     * @see org.jfree.chart.event.PlotChangeListener#plotChanged(org.jfree.chart.event.PlotChangeEvent)
     */
    public void plotChanged(PlotChangeEvent event) {
        System.out.println(event);
        XYPlot plot = (XYPlot) event.getPlot();
        double newRTMin = plot.getDomainAxis().getLowerBound();
        double newRTMax = plot.getDomainAxis().getUpperBound();
        double newMZMin = plot.getRangeAxis().getLowerBound();
        double newMZMax = plot.getRangeAxis().getUpperBound();
        
        // int rtResolution = currentIntensityMatrix.length;
        // int mzResolution = currentIntensityMatrix[0].length;

        //currentRTStep = (currentRTMax - currentRTMin) / (rtResolution - 1);
        //currentMZStep = (currentMZMax - currentMZMin) / (mzResolution - 1);
        //currentIntensityMatrix = new double[rtResolution][mzResolution];
        // TODO copy data

    }

    
    void setDataLoaded() {
        totalDataLoaded = true;
    }
}