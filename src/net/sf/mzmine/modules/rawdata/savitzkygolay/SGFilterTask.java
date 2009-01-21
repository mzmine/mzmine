/*
 * Copyright 2006-2009 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.rawdata.savitzkygolay;

import java.io.IOException;
import java.util.Hashtable;

import net.sf.mzmine.data.MzDataPoint;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.RawDataFileWriter;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.data.impl.SimpleDataPoint;
import net.sf.mzmine.data.impl.SimpleScan;
import net.sf.mzmine.main.mzmineclient.MZmineCore;
import net.sf.mzmine.taskcontrol.Task;

/**
 * 
 */
class SGFilterTask implements Task {

    private RawDataFile dataFile;

    private TaskStatus status = TaskStatus.WAITING;
    private String errorMessage;

    // scan counter
    private int filteredScans, totalScans;

    // parameter values
    private String suffix;
    private int numberOfDataPoints;;
    private boolean removeOriginal;

    private Hashtable<Integer, Integer> Hvalues;
    private Hashtable<Integer, int[]> Avalues;

    /**
     * @param rawDataFile
     * @param parameters
     */
    SGFilterTask(RawDataFile dataFile, SGFilterParameters parameters) {
        this.dataFile = dataFile;

        suffix = (String) parameters.getParameterValue(SGFilterParameters.suffix);
        numberOfDataPoints = (Integer) parameters.getParameterValue(SGFilterParameters.datapoints);
        removeOriginal = (Boolean) parameters.getParameterValue(SGFilterParameters.autoRemove);
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
     */
    public String getTaskDescription() {
        return "Savitzky-Golay filtering " + dataFile;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
     */
    public double getFinishedPercentage() {
        if (totalScans == 0)
            return 0.0f;
        return (double) filteredScans / totalScans;
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
     * @see java.lang.Runnable#run()
     */
    public void run() {

        status = TaskStatus.PROCESSING;

        try {

            // Create new temporary file
            String newName = dataFile.getName() + " " + suffix;
            RawDataFileWriter rawDataFileWriter = MZmineCore.createNewFile(newName);

            // Get all scans
            int[] scanNumbers = dataFile.getScanNumbers();
            totalScans = scanNumbers.length;

            initializeAHValues();
            int[] aVals = Avalues.get(new Integer(numberOfDataPoints));
            int h = Hvalues.get(new Integer(numberOfDataPoints)).intValue();

            for (int i = 0; i < scanNumbers.length; i++) {

                if (status == TaskStatus.CANCELED)
                    return;

                Scan oldScan = dataFile.getScan(scanNumbers[i]);
                processOneScan(rawDataFileWriter, oldScan, numberOfDataPoints,
                        h, aVals);

                filteredScans++;

            }

            // Finalize writing
            RawDataFile filteredRawDataFile = rawDataFileWriter.finishWriting();
            MZmineCore.getCurrentProject().addFile(filteredRawDataFile);

            // Remove the original file if requested
            if (removeOriginal)
                MZmineCore.getCurrentProject().removeFile(dataFile);

            status = TaskStatus.FINISHED;

        } catch (IOException e) {
            status = TaskStatus.ERROR;
            errorMessage = e.toString();
            return;
        }

    }

    private void processOneScan(RawDataFileWriter writer, Scan sc,
            int numOfDataPoints, int h, int[] aVals) throws IOException {

        // only process MS level 1 scans
        if (sc.getMSLevel() != 1) {
            writer.addScan(sc);
            return;
        }

        int marginSize = (numOfDataPoints + 1) / 2 - 1;
        double sumOfInts;

        MzDataPoint oldDataPoints[] = sc.getDataPoints();
        int newDataPointsLength = oldDataPoints.length - (marginSize * 2);

        // only process scans with datapoints
        if (newDataPointsLength < 1) {
            writer.addScan(sc);
            return;
        }

        MzDataPoint newDataPoints[] = new MzDataPoint[newDataPointsLength];

        for (int spectrumInd = marginSize; spectrumInd < (oldDataPoints.length - marginSize); spectrumInd++) {

            sumOfInts = aVals[0] * oldDataPoints[spectrumInd].getIntensity();

            for (int windowInd = 1; windowInd <= marginSize; windowInd++) {
                sumOfInts += aVals[windowInd]
                        * (oldDataPoints[spectrumInd + windowInd].getIntensity() + oldDataPoints[spectrumInd
                                - windowInd].getIntensity());
            }

            sumOfInts = sumOfInts / h;

            if (sumOfInts < 0) {
                sumOfInts = 0;
            }
            newDataPoints[spectrumInd - marginSize] = new SimpleDataPoint(
                    oldDataPoints[spectrumInd].getMZ(), sumOfInts);

        }

        SimpleScan newScan = new SimpleScan(sc);
        newScan.setDataPoints(newDataPoints);
        writer.addScan(newScan);

    }

    /**
     * Initialize Avalues and Hvalues
     */
    private void initializeAHValues() {

        Avalues = new Hashtable<Integer, int[]>();
        Hvalues = new Hashtable<Integer, Integer>();

        int[] a5Ints = { 17, 12, -3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
        Avalues.put(5, a5Ints);
        int[] a7Ints = { 7, 6, 3, -2, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
        Avalues.put(7, a7Ints);
        int[] a9Ints = { 59, 54, 39, 14, -21, 0, 0, 0, 0, 0, 0, 0, 0 };
        Avalues.put(9, a9Ints);
        int[] a11Ints = { 89, 84, 69, 44, 9, -36, 0, 0, 0, 0, 0, 0, 0 };
        Avalues.put(11, a11Ints);
        int[] a13Ints = { 25, 24, 21, 16, 9, 0, -11, 0, 0, 0, 0, 0, 0 };
        Avalues.put(13, a13Ints);
        int[] a15Ints = { 167, 162, 147, 122, 87, 42, -13, -78, 0, 0, 0, 0, 0 };
        Avalues.put(15, a15Ints);
        int[] a17Ints = { 43, 42, 39, 34, 27, 18, 7, -6, -21, 0, 0, 0, 0 };
        Avalues.put(17, a17Ints);
        int[] a19Ints = { 269, 264, 249, 224, 189, 144, 89, 24, -51, -136, 0,
                0, 0 };
        Avalues.put(19, a19Ints);
        int[] a21Ints = { 329, 324, 309, 284, 249, 204, 149, 84, 9, -76, -171,
                0, 0 };
        Avalues.put(21, a21Ints);
        int[] a23Ints = { 79, 78, 75, 70, 63, 54, 43, 30, 15, -2, -21, -42, 0 };
        Avalues.put(23, a23Ints);
        int[] a25Ints = { 467, 462, 447, 422, 387, 343, 287, 222, 147, 62, -33,
                -138, -253 };
        Avalues.put(25, a25Ints);

        Hvalues.put(5, 35);
        Hvalues.put(7, 21);
        Hvalues.put(9, 231);
        Hvalues.put(11, 429);
        Hvalues.put(13, 143);
        Hvalues.put(15, 1105);
        Hvalues.put(17, 323);
        Hvalues.put(19, 2261);
        Hvalues.put(21, 3059);
        Hvalues.put(23, 805);
        Hvalues.put(25, 5175);
    }

}
