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

package net.sf.mzmine.modules.visualization.twod;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Arrays;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.datamodel.impl.SimpleDataPoint;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskPriority;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.DataPointSorter;
import net.sf.mzmine.util.SortingDirection;
import net.sf.mzmine.util.SortingProperty;

import org.jfree.data.xy.AbstractXYDataset;

import com.google.common.collect.Range;

class TwoDDataSet extends AbstractXYDataset implements Task {

    private static final long serialVersionUID = 1L;

    private RawDataFile rawDataFile;

    private double retentionTimes[];
    private double basePeaks[];
    private SoftReference<DataPoint[]> dataPointMatrix[];

    private final Range<Double> totalRTRange, totalMZRange;
    private int totalScans, processedScans;
    private final Scan scans[];

    private TaskStatus status = TaskStatus.WAITING;

    public double curMaxIntensity;
    private ArrayList<Double> rtValuesInUserRange;


    @SuppressWarnings("unchecked")
    TwoDDataSet(RawDataFile rawDataFile, Scan scans[], Range<Double> rtRange,
            Range<Double> mzRange, TwoDVisualizerWindow visualizer) {

        this.rawDataFile = rawDataFile;

        totalRTRange = rtRange;
        totalMZRange = mzRange;

        this.scans = scans;

        totalScans = scans.length;

        dataPointMatrix = new SoftReference[totalScans];
        retentionTimes = new double[totalScans];
        basePeaks = new double[totalScans];

        MZmineCore.getTaskController().addTask(this, TaskPriority.HIGH);


    }

    @Override
    public void run() {

        status = TaskStatus.PROCESSING;

        for (int index = 0; index < totalScans; index++) {

            // Cancel?
            if (status == TaskStatus.CANCELED)
                return;

            Scan scan = scans[index];
            DataPoint scanBasePeak = scan.getHighestDataPoint();
            retentionTimes[index] = scan.getRetentionTime();
            basePeaks[index] = (scanBasePeak == null ? 0 : scanBasePeak
                    .getIntensity());
            DataPoint scanDataPoints[] = scan.getDataPoints();
            dataPointMatrix[index] = new SoftReference<DataPoint[]>(
                    scanDataPoints);
            processedScans++;
        }

        fireDatasetChanged();

        status = TaskStatus.FINISHED;

    }

    /**
     * @see org.jfree.data.general.AbstractSeriesDataset#getSeriesCount()
     */
    public int getSeriesCount() {
        return 2;
    }

    /**
     * @see org.jfree.data.general.AbstractSeriesDataset#getSeriesKey(int)
     */
    public Comparable<?> getSeriesKey(int series) {
        return rawDataFile.getName();
    }

    /**
     * @see org.jfree.data.xy.XYDataset#getItemCount(int)
     */
    public int getItemCount(int series) {
        return 2;
    }

    /**
     * @see org.jfree.data.xy.XYDataset#getX(int, int)
     */
    public Number getX(int series, int item) {
        if (series == 0)
            return totalRTRange.lowerEndpoint();
        else
            return totalRTRange.upperEndpoint();
    }

    /**
     * @see org.jfree.data.xy.XYDataset#getY(int, int)
     */
    public Number getY(int series, int item) {
        if (item == 0)
            return totalMZRange.lowerEndpoint();
        else
            return totalMZRange.upperEndpoint();
    }

    double upperEndpointIntensity(Range<Double> rtRange, Range<Double> mzRange,
            PlotMode plotMode) {

        double maxIntensity = 0;

        double searchRetentionTimes[] = retentionTimes;
        if (processedScans < totalScans) {
            searchRetentionTimes = new double[processedScans];
            System.arraycopy(retentionTimes, 0, searchRetentionTimes, 0,
                    searchRetentionTimes.length);
        }

        int startScanIndex = Arrays.binarySearch(searchRetentionTimes,
                rtRange.lowerEndpoint());

        if (startScanIndex < 0)
            startScanIndex = (startScanIndex * -1) - 1;

        if (startScanIndex >= searchRetentionTimes.length) {
            return 0;
        }

        if (searchRetentionTimes[startScanIndex] > rtRange.upperEndpoint()) {
            if (startScanIndex == 0)
                return 0;

            if (startScanIndex == searchRetentionTimes.length - 1)
                return upperEndpointIntensity(startScanIndex - 1, mzRange,
                        plotMode);

            // find which scan point is closer
            double diffNext = searchRetentionTimes[startScanIndex]
                    - rtRange.upperEndpoint();
            double diffPrev = rtRange.lowerEndpoint()
                    - searchRetentionTimes[startScanIndex - 1];

            if (diffPrev < diffNext)
                return upperEndpointIntensity(startScanIndex - 1, mzRange,
                        plotMode);
            else
                return upperEndpointIntensity(startScanIndex, mzRange, plotMode);
        }

        for (int scanIndex = startScanIndex; ((scanIndex < searchRetentionTimes.length) && (searchRetentionTimes[scanIndex] <= rtRange
                .upperEndpoint())); scanIndex++) {

            // ignore scans where all peaks are smaller than current max
            if (basePeaks[scanIndex] < maxIntensity)
                continue;

            double scanMax = upperEndpointIntensity(scanIndex, mzRange,
                    plotMode);

            if (scanMax > maxIntensity)
                maxIntensity = scanMax;

        }

        return maxIntensity;

    }
    public ArrayList getrtValuesInUserRange(){
        return rtValuesInUserRange;
    }

    // Sets the private list to contain the rt values for each data point scan of scans that fall in the user
    // range. returns an array of the data points but not the rt.
    ArrayList getCentroidedDataPointsInRTMZRange(Range<Double> rtRange, Range<Double> mzRange){
        ArrayList<DataPoint> dataPointsInRanges = new ArrayList<DataPoint>();
        ArrayList rtInRange = new ArrayList();

        curMaxIntensity = 0.0;

        double searchRetentionTimes[] = retentionTimes;

        if (processedScans < totalScans) {
            searchRetentionTimes = new double[processedScans];
            System.arraycopy(retentionTimes, 0, searchRetentionTimes, 0,
                    searchRetentionTimes.length);
        }

        // Find the rt of the scan at the bottom of our rtRange
        int startScanIndex = Arrays.binarySearch(searchRetentionTimes, rtRange.lowerEndpoint());


        // a couple of checks
        if (startScanIndex < 0){
            startScanIndex = (startScanIndex * -1) - 1;
        }

        if (startScanIndex >= searchRetentionTimes.length) {
            startScanIndex = 0;
        }



        // With this we can grab the data points from the scans we want using dataPointMatrix

        for (int scanIndex = startScanIndex; ((scanIndex < searchRetentionTimes.length) && (searchRetentionTimes[scanIndex] <= rtRange
                .upperEndpoint())); scanIndex++) {
            // get the list of data points
            DataPoint dataPoints[] = dataPointMatrix[scanIndex].get();
            // Binary search for the mz values in the range you want

            DataPoint searchMZ = new SimpleDataPoint(mzRange.lowerEndpoint(), 0);
            int startMZIndex = Arrays.binarySearch(dataPoints, searchMZ,
                    new DataPointSorter(SortingProperty.MZ,
                            SortingDirection.Ascending));
            if (startMZIndex < 0)
                startMZIndex = (startMZIndex * -1) - 1;

            if (startMZIndex >= dataPoints.length)
                startMZIndex = 0;

            for (int mzIndex = startMZIndex; ((mzIndex < dataPoints.length) && (dataPoints[mzIndex]
                    .getMZ() <= mzRange.upperEndpoint())); mzIndex++) {

                DataPoint curFoundDataPoint;
                curFoundDataPoint = dataPoints[mzIndex];

                //System.out.println("curFoundDataPoint.getMZ()");
                //System.out.println(curFoundDataPoint.getMZ());

                dataPointsInRanges.add(curFoundDataPoint);
                Double toAddRt = new Double(searchRetentionTimes[scanIndex]);
                rtInRange.add( toAddRt );

                double curIntensity = curFoundDataPoint.getIntensity();


                if (curIntensity > curMaxIntensity)
                    curMaxIntensity = curIntensity ;


            }

        }
        rtValuesInUserRange = rtInRange;

        return dataPointsInRanges;
    }

    private double upperEndpointIntensity(int dataPointMatrixIndex,
            Range<Double> mzRange, PlotMode plotMode) {
        DataPoint dataPoints[] = dataPointMatrix[dataPointMatrixIndex].get();
        if (dataPoints == null) {
            Scan scan = scans[dataPointMatrixIndex];
            dataPoints = scan.getDataPoints();
            dataPointMatrix[dataPointMatrixIndex] = new SoftReference<DataPoint[]>(
                    dataPoints);
        }
        return upperEndpointIntensity(dataPoints, mzRange, plotMode);
    }

    private double upperEndpointIntensity(DataPoint dataPoints[],
            Range<Double> mzRange, PlotMode plotMode) {

        double maxIntensity = 0;

        DataPoint searchMZ = new SimpleDataPoint(mzRange.lowerEndpoint(), 0);
        int startMZIndex = Arrays.binarySearch(dataPoints, searchMZ,
                new DataPointSorter(SortingProperty.MZ,
                        SortingDirection.Ascending));
        if (startMZIndex < 0)
            startMZIndex = (startMZIndex * -1) - 1;

        if (startMZIndex >= dataPoints.length)
            return 0;

        if (dataPoints[startMZIndex].getMZ() > mzRange.upperEndpoint()) {
            if (plotMode != PlotMode.CENTROID) {
                if (startMZIndex == 0)
                    return 0;
                if (startMZIndex == dataPoints.length - 1)
                    return dataPoints[startMZIndex - 1].getIntensity();

                // find which data point is closer
                double diffNext = dataPoints[startMZIndex].getMZ()
                        - mzRange.upperEndpoint();
                double diffPrev = mzRange.lowerEndpoint()
                        - dataPoints[startMZIndex - 1].getMZ();

                if (diffPrev < diffNext)
                    return dataPoints[startMZIndex - 1].getIntensity();
                else
                    return dataPoints[startMZIndex].getIntensity();
            } else {
                return 0;
            }

        }

        for (int mzIndex = startMZIndex; ((mzIndex < dataPoints.length) && (dataPoints[mzIndex]
                .getMZ() <= mzRange.upperEndpoint())); mzIndex++) {
            if (dataPoints[mzIndex].getIntensity() > maxIntensity)
                maxIntensity = dataPoints[mzIndex].getIntensity();
        }

        return maxIntensity;

    }

    @Override
    public void cancel() {
        status = TaskStatus.CANCELED;
    }

    @Override
    public String getErrorMessage() {
        return null;
    }

    @Override
    public double getFinishedPercentage() {
        if (totalScans == 0)
            return 0;
        return (double) processedScans / totalScans;
    }

    @Override
    public TaskStatus getStatus() {
        return status;
    }

    @Override
    public String getTaskDescription() {
        return "Updating 2D visualizer of " + rawDataFile;
    }

}
