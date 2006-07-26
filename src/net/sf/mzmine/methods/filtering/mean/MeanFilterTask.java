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

package net.sf.mzmine.methods.filtering.mean;

import java.io.IOException;
import java.util.Vector;

import net.sf.mzmine.data.Scan;
import net.sf.mzmine.data.impl.SimpleScan;
import net.sf.mzmine.io.OpenedRawDataFile;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.io.RawDataFileWriter;
import net.sf.mzmine.taskcontrol.Task;

/**
 * 
 */
class MeanFilterTask implements Task {

    private OpenedRawDataFile dataFile;
    private RawDataFile rawDataFile;
    private MeanFilterParameters parameters;
    private TaskStatus status;
    private String errorMessage;

    private int filteredScans;
    private int totalScans;

    private RawDataFile filteredRawDataFile;

    /**
     * @param rawDataFile
     * @param parameters
     */
    MeanFilterTask(OpenedRawDataFile dataFile, MeanFilterParameters parameters) {
        status = TaskStatus.WAITING;
        this.dataFile = dataFile;
        this.rawDataFile = dataFile.getCurrentFile();
        this.parameters = parameters;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
     */
    public String getTaskDescription() {
        return "Mean filtering " + dataFile;
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

        // Create new temporary copy
        RawDataFileWriter rawDataFileWriter;
        try {
            rawDataFileWriter = dataFile.createNewTemporaryFile();
        } catch (IOException e) {
            status = TaskStatus.ERROR;
            errorMessage = e.toString();
            return;
        }

        int[] scanNumbers = rawDataFile.getScanNumbers(1);
        totalScans = scanNumbers.length;

        Scan oldScan;

        for (int i = 0; i < scanNumbers.length; i++) {

            if (status == TaskStatus.CANCELED)
                return;

            try {
                oldScan = rawDataFile.getScan(scanNumbers[i]);
                processOneScan(rawDataFileWriter, oldScan,
                        parameters.oneSidedWindowLength);

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
            double windowLength) throws IOException {

        Vector<Double> massWindow = new Vector<Double>();
        Vector<Double> intensityWindow = new Vector<Double>();

        double currentMass;
        double lowLimit;
        double hiLimit;
        double mzVal;

        double elSum;

        double[] masses = sc.getMZValues();
        double[] intensities = sc.getIntensityValues();
        double[] newIntensities = new double[masses.length];

        int addi = 0;
        for (int i = 0; i < masses.length; i++) {

            currentMass = masses[i];
            lowLimit = currentMass - windowLength;
            hiLimit = currentMass + windowLength;

            // Remove all elements from window whose m/z value is less than the
            // low limit
            if (massWindow.size() > 0) {
                mzVal = massWindow.get(0).doubleValue();
                while ((massWindow.size() > 0) && (mzVal < lowLimit)) {
                    massWindow.remove(0);
                    intensityWindow.remove(0);
                    if (massWindow.size() > 0) {
                        mzVal = massWindow.get(0).doubleValue();
                    }
                }
            }

            // Add new elements as long as their m/z values are less than the hi
            // limit
            while ((addi < masses.length) && (masses[addi] <= hiLimit)) {
                massWindow.add(new Double(masses[addi]));
                intensityWindow.add(new Double(intensities[addi]));
                addi++;
            }

            elSum = 0;
            for (int j = 0; j < intensityWindow.size(); j++) {
                elSum += ((Double) (intensityWindow.get(j))).doubleValue();
            }

            newIntensities[i] = elSum / (double) intensityWindow.size();

        }

        Scan newScan = new SimpleScan(sc.getScanNumber(), sc.getMSLevel(),
                sc.getRetentionTime(), sc.getParentScanNumber(),
                sc.getPrecursorMZ(), sc.getFragmentScanNumbers(),
                sc.getMZValues(), newIntensities, sc.isCentroided());

        writer.addScan(newScan);

    }

}
