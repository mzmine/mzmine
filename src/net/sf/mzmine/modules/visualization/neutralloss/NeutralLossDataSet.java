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

package net.sf.mzmine.modules.visualization.neutralloss;

import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.Vector;

import net.sf.mzmine.data.Scan;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.io.util.RawDataAcceptor;
import net.sf.mzmine.io.util.RawDataRetrievalTask;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskController;
import net.sf.mzmine.taskcontrol.Task.TaskPriority;

import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.data.xy.AbstractXYDataset;
import org.jfree.data.xy.XYDataset;

/**
 * 
 */
class NeutralLossDataSet extends AbstractXYDataset implements RawDataAcceptor, XYToolTipGenerator {

    // redraw the chart every 100 ms while updating
    private static final int REDRAW_INTERVAL = 100;

    private RawDataFile rawDataFile;

    private float totalMZMin, totalMZMax;
    private int numOfFragments, xAxis;

    private Date lastRedrawTime = new Date();

    private Vector<NeutralLossDataPoint> dataPoints;

    NeutralLossDataSet(RawDataFile rawDataFile,
            int xAxis, float rtMin, float rtMax, float mzMin, float mzMax,
            int numOfFragments, NeutralLossVisualizerWindow visualizer) {

        this.rawDataFile = rawDataFile;

        totalMZMin = mzMin;
        totalMZMax = mzMax;
        this.numOfFragments = numOfFragments;
        this.xAxis = xAxis;

        // get MS/MS scans
        int scanNumbers[] = rawDataFile.getScanNumbers(2, rtMin, rtMax);
        assert scanNumbers != null;

        dataPoints = new Vector<NeutralLossDataPoint>(scanNumbers.length);

        Task updateTask = new RawDataRetrievalTask(rawDataFile, scanNumbers,
                "Updating neutral loss visualizer of " + rawDataFile, this);

        MZmineCore.getTaskController().addTask(updateTask, TaskPriority.HIGH, visualizer);

    }

    /**
     * @see net.sf.mzmine.io.RawDataAcceptor#addScan(net.sf.mzmine.data.Scan)
     */
    public synchronized void addScan(Scan scan, int index, int total) {

        // logger.finest("Adding scan " + scan);

        // check parent m/z
        if ((scan.getPrecursorMZ() < totalMZMin)
                || (scan.getPrecursorMZ() > totalMZMax))
            return;

        // get m/z and intensity values
        float mzValues[] = scan.getMZValues();
        float intensityValues[] = scan.getIntensityValues();
        
        // skip empty scans, or scans that only contain zero intensity peaks
        if (scan.getBasePeakIntensity() == 0) return;

        // topPeaks will contain indexes to mzValues peaks of top intensity
        int topPeaks[] = new int[numOfFragments];
        Arrays.fill(topPeaks, -1);

        for (int i = 0; i < intensityValues.length; i++) {

            fragmentsCycle: for (int j = 0; j < numOfFragments; j++) {

                if ((topPeaks[j] < 0) || (intensityValues[i] > intensityValues[topPeaks[j]])) {

                    // shift the top peaks array
                    for (int k = numOfFragments - 1; k > j; k--)
                        topPeaks[k] = topPeaks[k - 1];

                    // add the peak to the appropriate place
                    topPeaks[j] = i;

                    break fragmentsCycle;
                }
            }

        }

        // add the data points
        for (int i = 0; i < topPeaks.length; i++) {

            int peakIndex = topPeaks[i];

            // if we have a very few peaks, the array may not be full
            if (peakIndex < 0)
                break;

            NeutralLossDataPoint newPoint = new NeutralLossDataPoint(
                    mzValues[peakIndex], scan.getScanNumber(),
                    scan.getParentScanNumber(), scan.getPrecursorMZ(),
                    scan.getPrecursorCharge(), scan.getRetentionTime());

            dataPoints.add(newPoint);

        }

        // redraw every REDRAW_INTERVAL ms
        boolean notify = false;
        Date currentTime = new Date();
        if (currentTime.getTime() - lastRedrawTime.getTime() > REDRAW_INTERVAL) {
            notify = true;
            lastRedrawTime = currentTime;
        }

        // always redraw when we add last scan
        if (index == total - 1) {
            notify = true;
        }

        if (notify) {
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
        return dataPoints.size();
    }

    /**
     * @see org.jfree.data.xy.XYDataset#getX(int, int)
     */
    public Number getX(int series, int item) {
        if (xAxis == 0)
            return dataPoints.get(item).getPrecursorMass();
        else
            return dataPoints.get(item).getRetentionTime();

    }

    /**
     * @see org.jfree.data.xy.XYDataset#getY(int, int)
     */
    public Number getY(int series, int item) {
        return dataPoints.get(item).getNeutralLoss();
    }
    
    public NeutralLossDataPoint getDataPoint(int item) {
        return dataPoints.get(item);
    }
    
    public NeutralLossDataPoint getDataPoint(float xValue, float yValue) {
        Vector<NeutralLossDataPoint> dataCopy = new Vector<NeutralLossDataPoint>(dataPoints);
        Iterator<NeutralLossDataPoint> it = dataCopy.iterator();
        float currentX, currentY;
        while (it.hasNext()) {
            NeutralLossDataPoint point = it.next();
            if (xAxis == 0) currentX = point.getPrecursorMass();
            else currentX = point.getRetentionTime();
            currentY = point.getNeutralLoss();
            // check for equality
            if ((Math.abs(currentX - xValue) < 0.00000001) && (Math.abs(currentY - yValue) < 0.00000001))
                return point;
        }
        return null;
    }

    /**
     * @see org.jfree.chart.labels.XYToolTipGenerator#generateToolTip(org.jfree.data.xy.XYDataset, int, int)
     */
    public String generateToolTip(XYDataset dataset, int series, int item) {
        return dataPoints.get(item).toString();

    }
    


}