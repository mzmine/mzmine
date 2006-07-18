/*
 * Copyright 2006 The MZmine Development Team This file is part of MZmine.
 * MZmine is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version. MZmine is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details. You should have received a copy of the GNU General Public License
 * along with MZmine; if not, write to the Free Software Foundation, Inc., 51
 * Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.methods.filtering.savitzkygolay;

import java.io.IOException;
import java.util.Hashtable;

import net.sf.mzmine.data.Scan;
import net.sf.mzmine.data.impl.SimpleScan;
import net.sf.mzmine.io.OpenedRawDataFile;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.io.RawDataFileWriter;
import net.sf.mzmine.taskcontrol.Task;

/**
 * 
 */
class SavitzkyGolayFilterTask implements Task {

    private OpenedRawDataFile dataFile;
    private RawDataFile rawDataFile;
    private SavitzkyGolayFilterParameters parameters;
    private TaskStatus status;
    private String errorMessage;

    private int filteredScans;
    private int totalScans;

    private RawDataFile filteredRawDataFile;

    private Hashtable<Integer, Integer> Hvalues;
    private Hashtable<Integer, int[]> Avalues;

    /**
     * @param rawDataFile
     * @param parameters
     */
    SavitzkyGolayFilterTask(OpenedRawDataFile dataFile,
            SavitzkyGolayFilterParameters parameters) {
        status = TaskStatus.WAITING;
        this.dataFile = dataFile;
        this.rawDataFile = dataFile.getCurrentFile();
        this.parameters = parameters;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
     */
    public String getTaskDescription() {
        return "Savitzky-Golay filtering " + rawDataFile;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
     */
    public float getFinishedPercentage() {
        if (totalScans == 0)
            return 0.0f;
        return (float) filteredScans / totalScans;
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

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getResult()
     */
    public Object getResult() {
        Object[] results = new Object[3];
        results[0] = dataFile;
        results[1] = filteredRawDataFile;
        results[2] = parameters;

        return results;
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

        initializeAHValues();

        // Create new temporary copy
        RawDataFileWriter rawDataFileWriter;
        try {
            rawDataFileWriter = dataFile.createNewTemporaryFile();
        } catch (IOException e) {
            status = TaskStatus.ERROR;
            errorMessage = e.toString();
            return;
        }

        int[] aVals = Avalues.get(new Integer(parameters.numberOfDataPoints));
        int h = Hvalues.get(new Integer(parameters.numberOfDataPoints)).intValue();

        int[] scanNumbers = rawDataFile.getScanNumbers(1);
        totalScans = scanNumbers.length;

        Scan oldScan;

        for (int i = 0; i < scanNumbers.length; i++) {

            if (status == TaskStatus.CANCELED)
                return;

            try {
                oldScan = rawDataFile.getScan(scanNumbers[i]);
                processOneScan(rawDataFileWriter, oldScan,
                        parameters.numberOfDataPoints, h, aVals);

            } catch (IOException e) {
                status = TaskStatus.ERROR;
                errorMessage = e.toString();
                try {
                    filteredRawDataFile = rawDataFileWriter.finishWriting();
                } catch (IOException e2) {
                }
                return;
            }

            filteredScans++;

        }

        try {
            filteredRawDataFile = rawDataFileWriter.finishWriting();
        } catch (IOException e) {
            status = TaskStatus.ERROR;
            errorMessage = e.toString();
            return;
        }

        status = TaskStatus.FINISHED;

    }

    private void processOneScan(RawDataFileWriter writer, Scan sc,
            int numOfDataPoints, int h, int[] aVals) throws IOException {

        int marginSize = (numOfDataPoints + 1) / 2 - 1;
        double sumOfInts;

        double[] masses = sc.getMZValues();
        double[] intensities = sc.getIntensityValues();
        double[] newIntensities = new double[masses.length];

        for (int spectrumInd = marginSize; spectrumInd < (masses.length - marginSize); spectrumInd++) {

            sumOfInts = aVals[0] * intensities[spectrumInd];

            for (int windowInd = 1; windowInd <= marginSize; windowInd++) {
                sumOfInts += aVals[windowInd]
                        * (intensities[spectrumInd + windowInd] + intensities[spectrumInd
                                - windowInd]);
            }

            sumOfInts = sumOfInts / h;

            if (sumOfInts < 0) {
                sumOfInts = 0;
            }
            newIntensities[spectrumInd] = sumOfInts;

        }

        SimpleScan newScan = new SimpleScan(sc);
        newScan.setData(sc.getMZValues(), newIntensities);
        writer.addScan(newScan);

    }

    /**
     * Initialize Avalues and Hvalues These are actually constants, but it is
     * difficult to define them as static final
     */
    private void initializeAHValues() {
        Avalues = new Hashtable<Integer, int[]>();
        Hvalues = new Hashtable<Integer, Integer>();

        int[] a5Ints = { 17, 12, -3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
        Avalues.put(new Integer(5), a5Ints);
        int[] a7Ints = { 7, 6, 3, -2, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
        Avalues.put(new Integer(7), a7Ints);
        int[] a9Ints = { 59, 54, 39, 14, -21, 0, 0, 0, 0, 0, 0, 0, 0 };
        Avalues.put(new Integer(9), a9Ints);
        int[] a11Ints = { 89, 84, 69, 44, 9, -36, 0, 0, 0, 0, 0, 0, 0 };
        Avalues.put(new Integer(11), a11Ints);
        int[] a13Ints = { 25, 24, 21, 16, 9, 0, -11, 0, 0, 0, 0, 0, 0 };
        Avalues.put(new Integer(13), a13Ints);
        int[] a15Ints = { 167, 162, 147, 122, 87, 42, -13, -78, 0, 0, 0, 0, 0 };
        Avalues.put(new Integer(15), a15Ints);
        int[] a17Ints = { 43, 42, 39, 34, 27, 18, 7, -6, -21, 0, 0, 0, 0 };
        Avalues.put(new Integer(17), a17Ints);
        int[] a19Ints = { 269, 264, 249, 224, 189, 144, 89, 24, -51, -136, 0,
                0, 0 };
        Avalues.put(new Integer(19), a19Ints);
        int[] a21Ints = { 329, 324, 309, 284, 249, 204, 149, 84, 9, -76, -171,
                0, 0 };
        Avalues.put(new Integer(21), a21Ints);
        int[] a23Ints = { 79, 78, 75, 70, 63, 54, 43, 30, 15, -2, -21, -42, 0 };
        Avalues.put(new Integer(23), a23Ints);
        int[] a25Ints = { 467, 462, 447, 422, 387, 343, 287, 222, 147, 62, -33,
                -138, -253 };
        Avalues.put(new Integer(25), a25Ints);

        Integer h5Int = new Integer(35);
        Hvalues.put(new Integer(5), h5Int);
        Integer h7Int = new Integer(21);
        Hvalues.put(new Integer(7), h7Int);
        Integer h9Int = new Integer(231);
        Hvalues.put(new Integer(9), h9Int);
        Integer h11Int = new Integer(429);
        Hvalues.put(new Integer(11), h11Int);
        Integer h13Int = new Integer(143);
        Hvalues.put(new Integer(13), h13Int);
        Integer h15Int = new Integer(1105);
        Hvalues.put(new Integer(15), h15Int);
        Integer h17Int = new Integer(323);
        Hvalues.put(new Integer(17), h17Int);
        Integer h19Int = new Integer(2261);
        Hvalues.put(new Integer(19), h19Int);
        Integer h21Int = new Integer(3059);
        Hvalues.put(new Integer(21), h21Int);
        Integer h23Int = new Integer(805);
        Hvalues.put(new Integer(23), h23Int);
        Integer h25Int = new Integer(5175);
        Hvalues.put(new Integer(25), h25Int);
    }

}
