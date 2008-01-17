/*
 * Copyright 2006-2008 The MZmine Development Team
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

package net.sf.mzmine.modules.peakpicking.accuratemass;

import java.util.Iterator;
import java.util.TreeSet;
import java.util.Vector;

import net.sf.mzmine.data.Scan;
import net.sf.mzmine.data.Peak.PeakStatus;
import net.sf.mzmine.data.impl.ConstructionPeak;
import net.sf.mzmine.data.impl.SimplePeakList;
import net.sf.mzmine.data.impl.SimplePeakListRow;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.project.MZmineProject;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.util.PeakSorterByDescendingHeight;

/**
 * 
 */
class AccurateMassPickerTask implements Task {

    private RawDataFile dataFile;

    private TaskStatus status = TaskStatus.WAITING;
    private String errorMessage;

    // scan counter
    private int processedScans, totalScans;

    // parameter values
    private String suffix;
    private float mzTolerance, minimumPeakDuration;

    // peak id counter
    private int newPeakID = 1;

    /**
     * @param dataFile
     * @param parameters
     */
    AccurateMassPickerTask(RawDataFile dataFile,
            AccurateMassPickerParameters parameters) {

        this.dataFile = dataFile;
        suffix = (String) parameters.getParameterValue(AccurateMassPickerParameters.suffix);
        mzTolerance = (Float) parameters.getParameterValue(AccurateMassPickerParameters.mzTolerance);
        minimumPeakDuration = (Float) parameters.getParameterValue(AccurateMassPickerParameters.minimumPeakDuration);

    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
     */
    public String getTaskDescription() {
        return "Recursive threshold peak detection on " + dataFile;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
     */
    public float getFinishedPercentage() {
        if (totalScans == 0)
            return 0.0f;
        return (float) processedScans / (float) totalScans;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getStatus()
     */
    public TaskStatus getStatus() {
        return status;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getErrorMessage()
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    public RawDataFile getDataFile() {
        return dataFile;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#cancel()
     */
    public void cancel() {
        status = TaskStatus.CANCELED;
    }

    /**
     * @see Runnable#run()
     */
    public void run() {

        status = TaskStatus.PROCESSING;

        // Create new peak list
        SimplePeakList newPeakList = new SimplePeakList(
                dataFile + " " + suffix, dataFile);

        // Get all scans of MS level 1
        int[] scanNumbers = dataFile.getScanNumbers(1);
        totalScans = scanNumbers.length;

        // Keep a set of currently unconnected peaks, sorted by intensity
        TreeSet<ConstructionPeak> currentPeaks = new TreeSet<ConstructionPeak>(
                new PeakSorterByDescendingHeight());

        // Iterate scans
        for (int scanIndex = 0; scanIndex < totalScans; scanIndex++) {

            // Cancel?
            if (status == TaskStatus.CANCELED)
                return;

            // Get next scan
            Scan scan = dataFile.getScan(scanNumbers[scanIndex]);

            // We don't want centroided data
            if (scan.isCentroided()) {
                status = TaskStatus.ERROR;
                errorMessage = "Accurate mass peak detection can only run on non-centroided data";
                return;
            }

            // A set of those peaks, that are succesfully connected in this scan
            TreeSet<ConstructionPeak> connectedPeaks = new TreeSet<ConstructionPeak>(
                    new PeakSorterByDescendingHeight());

            // Detect m/z peaks in this scan
            DataPoint scanDataPoints[] = detectDataPointsInOneScan(scan);

            for (DataPoint dataPoint : scanDataPoints) {

                // Seach for peak to which we can connect this data point
                ConstructionPeak connectedPeak = null;

                Iterator<ConstructionPeak> peaksIterator = currentPeaks.iterator();
                while (peaksIterator.hasNext()) {
                    ConstructionPeak peak = peaksIterator.next();

                    // Check if data point can be connected to this peak
                    if (Math.abs(peak.getMZ() - dataPoint.getMZ()) <= mzTolerance) {

                        // Remove the peak from unconnected peaks set, but keep
                        // it if we're processing last scan
                        if (scanIndex < totalScans - 1)
                            peaksIterator.remove();

                        connectedPeak = peak;

                        break;

                    }
                }

                // If no peak could be connected to this data point, add a new
                // peak
                if (connectedPeak == null)
                    connectedPeak = new ConstructionPeak(dataFile);

                // Add this data point to the peak
                connectedPeak.addDatapoint(scanNumbers[scanIndex],
                        dataPoint.getMZ(), scan.getRetentionTime(),
                        dataPoint.getIntensity());

                // Add the peak to connected peaks set
                connectedPeaks.add(connectedPeak);

            }

            // Check all peaks that could not be connected
            for (ConstructionPeak peak : currentPeaks) {

                // If the peak has minimum duration, add it to the peak list
                if (peak.getDuration() >= minimumPeakDuration) {
                    peak.finalizedAddingDatapoints(PeakStatus.DETECTED);
                    SimplePeakListRow newRow = new SimplePeakListRow(newPeakID);
                    newPeakID++;
                    newRow.addPeak(dataFile, peak, peak);
                    newPeakList.addRow(newRow);
                }

            }

            // For next scan, use currently connected peaks
            currentPeaks = connectedPeaks;

            processedScans++;

        } // End of scan loop

        // Add new peaklist to the project
        MZmineProject currentProject = MZmineCore.getCurrentProject();
        currentProject.addPeakList(newPeakList);

        status = TaskStatus.FINISHED;

    }

    /**
     * 
     * @param scan
     * @return
     */
    private DataPoint[] detectDataPointsInOneScan(Scan scan) {

        float mzValues[] = scan.getMZValues();
        float intValues[] = scan.getIntensityValues();

        Vector<DataPoint> detectedDataPoints = new Vector<DataPoint>();

        double totalSum = 0, intensitySum = 0;
        float maxIntensity = 0;
        boolean ascending = true;
        
        for (int i = 0; i < mzValues.length; i++) {
            
            boolean nextPointIsHigher = ((i == mzValues.length - 1) || (intValues[i] == 0) || (intValues[i] < intValues[i+1]));
            
            // If we are ascending and next point is higher, or we are
            // descending and next point is lower
            if (ascending == nextPointIsHigher) {
                totalSum += mzValues[i] * intValues[i];
                intensitySum += intValues[i];
                continue;
            }
            
            // If we are at local maximum
            if (ascending && !nextPointIsHigher) {
                maxIntensity = intValues[i];
                ascending = false;
                continue;
            }
            
            // If we are at the last data point of this m/z peak
            if (!ascending && nextPointIsHigher) {
                
                totalSum += mzValues[i] * intValues[i];
                intensitySum += intValues[i];
                
                // Calculate the weighted average of m/z values
                float mzValue = (float) (totalSum / intensitySum);
                DataPoint p = new DataPoint(mzValue, maxIntensity);
                detectedDataPoints.add(p);

                // Reset the state
                totalSum = intValues[i] * mzValues[i];
                intensitySum = intValues[i];
                ascending = true;
                
                continue;
            }
            
        }
        
        DataPoint detectedDataPointsArray[] = detectedDataPoints.toArray(new DataPoint[0]);
        return detectedDataPointsArray;

    }
}
