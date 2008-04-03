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

import java.util.Arrays;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.Vector;

import net.sf.mzmine.data.DataPoint;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.data.impl.SimplePeakList;
import net.sf.mzmine.data.impl.SimplePeakListRow;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.project.MZmineProject;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.util.PeakSorterByDescendingHeight;

/**
 * Accurate mass peak picker task
 */
class AccurateMassPickerTask implements Task {

    private RawDataFile dataFile;

    private TaskStatus status = TaskStatus.WAITING;
    private String errorMessage;

    // scan counter
    private int processedScans, totalScans;

    // parameter values
    private String suffix;
    private float mzTolerance;
    private int minDataPoints;

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
        minDataPoints = (Integer) parameters.getParameterValue(AccurateMassPickerParameters.minDataPoints);

    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
     */
    public String getTaskDescription() {
        return "Accurate mass peak detection on " + dataFile;
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
        TreeSet<AccurateMassPeak> currentPeaks = new TreeSet<AccurateMassPeak>(
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
            TreeSet<AccurateMassPeak> connectedPeaks = new TreeSet<AccurateMassPeak>(
                    new PeakSorterByDescendingHeight());

            // Detect m/z peaks in this scan
            AccurateMassDataPoint scanMZPeaks[] = detectMZPeaksInOneScan(scan);

            // Sort by descending height
            Arrays.sort(scanMZPeaks);

            // Iterate detected m/z peaks
            for (AccurateMassDataPoint mzPeak : scanMZPeaks) {

                // Seach for peak to which we can connect this data point
                AccurateMassPeak connectedPeak = null;

                Iterator<AccurateMassPeak> peaksIterator = currentPeaks.iterator();
                while (peaksIterator.hasNext()) {
                    AccurateMassPeak peak = peaksIterator.next();

                    // Check if m/z peak data point can be connected to this
                    // peak
                    if (Math.abs(peak.getMZ() - mzPeak.getMZ()) <= mzTolerance) {

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
                    connectedPeak = new AccurateMassPeak(dataFile);

                // Add this data point to the peak
                connectedPeak.addDatapoint(scanNumbers[scanIndex], mzPeak);

                // Add the peak to connected peaks set
                connectedPeaks.add(connectedPeak);

            }

            // Check all peaks that could not be connected
            for (AccurateMassPeak candidatePeak : currentPeaks) {

                // Skip sequences that don't have enough data points
                if (candidatePeak.getScanNumbers().length < minDataPoints)
                    continue;

                // The candidatePeak represents a sequence of same data points.
                // This sequence may represent 0..n actual peaks. Detect those
                // peaks by calling processPeakCandidate()
                AccurateMassPeak goodPeaks[] = processPeakCandidate(candidatePeak);

                // For each good peak, add a new row to the peak list
                for (AccurateMassPeak goodPeak : goodPeaks) {
                    SimplePeakListRow newRow = new SimplePeakListRow(newPeakID);
                    newPeakID++;
                    newRow.addPeak(dataFile, goodPeak, goodPeak);
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
     * This method detects m/z peaks (represented by AccurateMassDataPoint
     * instances) in one scan. As a requirement, the scan must not be centroided
     * and scan data points must form well-shaped m/z peaks.
     * 
     * @param scan
     * @return
     */
    private AccurateMassDataPoint[] detectMZPeaksInOneScan(Scan scan) {

        DataPoint scanDataPoints[] = scan.getDataPoints();

        Vector<AccurateMassDataPoint> detectedMZPeaks = new Vector<AccurateMassDataPoint>(
                1024, 1024);
        Vector<DataPoint> currentMZPeak = new Vector<DataPoint>();

        boolean ascending = true;

        for (int i = 0; i < scanDataPoints.length; i++) {

            boolean nextPointIsHigher = ((i == scanDataPoints.length - 1)
                    || (scanDataPoints[i].getIntensity() == 0) || (scanDataPoints[i].getIntensity() < scanDataPoints[i + 1].getIntensity()));

            currentMZPeak.add(scanDataPoints[i]);

            // If we are at local maximum
            if (ascending && !nextPointIsHigher) {
                ascending = false;
                continue;
            }

            // If we are at the last data point of this m/z peak
            if (!ascending && nextPointIsHigher) {

                // Create a new AccurateMassDataPoint
                DataPoint currentPeakDataPoints[] = currentMZPeak.toArray(new DataPoint[0]);
                AccurateMassDataPoint newMZPeak = new AccurateMassDataPoint(
                        currentPeakDataPoints, scan.getRetentionTime());
                detectedMZPeaks.add(newMZPeak);

                // Reset the state
                currentMZPeak.clear();
                ascending = true;

                continue;
            }

        }

        AccurateMassDataPoint detectedMZPeaksArray[] = detectedMZPeaks.toArray(new AccurateMassDataPoint[0]);
        return detectedMZPeaksArray;

    }

    /**
     * This methods generates final chromatographic peaks from connected data
     * points.
     * 
     * @param candidate A chromatographic peak candidate consisting of same mass
     *            data points from successive scans
     * @return Well-shaped peaks separated from the peak candidate
     */
    private AccurateMassPeak[] processPeakCandidate(
            AccurateMassPeak candidatePeak) {

        if (1 == 1) {
            AccurateMassPeak smoothed = new AccurateMassPeak(dataFile);
            
            int peakScans[] = candidatePeak.getScanNumbers();
            for (int i = 5; i < peakScans.length - 5; i++) {
                AccurateMassDataPoint dp = candidatePeak.getDataPoint(peakScans[i]);
                
                float intSum = 0;
                for (int j = i - 5; j < i + 6; j++) {
                    intSum += candidatePeak.getDataPoint(peakScans[j]).getIntensity();
                }
                intSum /= 11f;
                dp.setIntensity(intSum);
                smoothed.addDatapoint(peakScans[i], dp);
                
            }
            
            // rule out empty peaks
            if (smoothed.getScanNumbers().length == 0) return new AccurateMassPeak[0];
            
            return new AccurateMassPeak[] { smoothed };
        }
        
        Vector<AccurateMassPeak> resultPeaks = new Vector<AccurateMassPeak>();

        // If the peak has minimum duration, add it to the peak list

        int peakScans[] = candidatePeak.getScanNumbers();

        Vector<AccurateMassDataPoint> goodLocalMaxima = new Vector<AccurateMassDataPoint>(); 
        
        // Find data points which represent maxima on the interval of at least 5
        // data points to the left and 5 data points to the right
        maximaSearch: for (int i = 5; i < peakScans.length - 5; i++) {
            AccurateMassDataPoint maximaCandidate = candidatePeak.getDataPoint(peakScans[i]);

            for (int j = 1; j <= 5; j++) {
                DataPoint leftNeighbour = candidatePeak.getDataPoint(peakScans[i
                        - j]);
                DataPoint rightNeighbour = candidatePeak.getDataPoint(peakScans[i
                        + j]);
                if ((leftNeighbour.getIntensity() > maximaCandidate.getIntensity())
                        || (rightNeighbour.getIntensity() > maximaCandidate.getIntensity()))
                    continue maximaSearch;
            }
            
            // found a good maximum
            goodLocalMaxima.add(maximaCandidate);
            
        }

        for (AccurateMassDataPoint localMaximum : goodLocalMaxima) {

            AccurateMassPeak newPeak = new AccurateMassPeak(dataFile);
            for (int i = 0; i < peakScans.length; i++) {
                AccurateMassDataPoint point = candidatePeak.getDataPoint(peakScans[i]);
                // 10s range
                if ((point.getRT() >= localMaximum.getRT() - 10) && ((point.getRT() <= localMaximum.getRT() + 10))) {
                    newPeak.addDatapoint(peakScans[i], point);
                }
            }
            
            resultPeaks.add(newPeak);
        }
        

        return resultPeaks.toArray(new AccurateMassPeak[0]);

    }
}
