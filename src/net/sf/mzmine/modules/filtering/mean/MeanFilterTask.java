/*
 * Copyright 2006-2007 The MZmine Development Team
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

package net.sf.mzmine.modules.filtering.mean;

import java.io.IOException;
import java.util.Vector;

import net.sf.mzmine.data.Scan;
import net.sf.mzmine.data.impl.SimpleParameterSet;
import net.sf.mzmine.data.impl.SimpleScan;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.io.RawDataFileWriter;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.taskcontrol.Task;

/**
 * 
 */
class MeanFilterTask implements Task {

    private RawDataFile dataFile;

    private TaskStatus status = TaskStatus.WAITING;
    private String errorMessage;

    private int filteredScans;
    private int totalScans;

    private RawDataFile filteredRawDataFile;

    private float oneSidedWindowLength;

    /**
     * @param rawDataFile
     * @param parameters
     */
    MeanFilterTask(RawDataFile dataFile, SimpleParameterSet parameters) {
        this.dataFile = dataFile;
        oneSidedWindowLength = ((Float) parameters.getParameterValue(MeanFilter.parameterOneSidedWindowLength)).floatValue();
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
        return filteredRawDataFile;
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

        // Create new temporary copy
        RawDataFileWriter rawDataFileWriter;
        try {
            rawDataFileWriter = MZmineCore.getIOController().createNewFile(dataFile);
        } catch (IOException e) {
            status = TaskStatus.ERROR;
            errorMessage = e.toString();
            return;
        }

        int[] scanNumbers = dataFile.getScanNumbers();
        totalScans = scanNumbers.length;

        Scan oldScan;

        for (int i = 0; i < scanNumbers.length; i++) {

            if (status == TaskStatus.CANCELED)
                return;

            try {
                oldScan = dataFile.getScan(scanNumbers[i]);
                processOneScan(rawDataFileWriter, oldScan, oneSidedWindowLength);

            } catch (IOException e) {
                status = TaskStatus.ERROR;
                errorMessage = e.toString();
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
            float windowLength) throws IOException {

        // only process MS level 1 scans
        if (sc.getMSLevel() != 1) {
            writer.addScan(sc);
            return;
        }

        Vector<Float> massWindow = new Vector<Float>();
        Vector<Float> intensityWindow = new Vector<Float>();

        float currentMass;
        float lowLimit;
        float hiLimit;
        float mzVal;

        float elSum;

        float[] masses = sc.getMZValues();
        float[] intensities = sc.getIntensityValues();
        float[] newIntensities = new float[masses.length];

        int addi = 0;
        for (int i = 0; i < masses.length; i++) {

            currentMass = masses[i];
            lowLimit = currentMass - windowLength;
            hiLimit = currentMass + windowLength;

            // Remove all elements from window whose m/z value is less than the
            // low limit
            if (massWindow.size() > 0) {
                mzVal = massWindow.get(0).floatValue();
                while ((massWindow.size() > 0) && (mzVal < lowLimit)) {
                    massWindow.remove(0);
                    intensityWindow.remove(0);
                    if (massWindow.size() > 0) {
                        mzVal = massWindow.get(0).floatValue();
                    }
                }
            }

            // Add new elements as long as their m/z values are less than the hi
            // limit
            while ((addi < masses.length) && (masses[addi] <= hiLimit)) {
                massWindow.add(new Float(masses[addi]));
                intensityWindow.add(new Float(intensities[addi]));
                addi++;
            }

            elSum = 0;
            for (int j = 0; j < intensityWindow.size(); j++) {
                elSum += ((Float) (intensityWindow.get(j))).floatValue();
            }

            newIntensities[i] = elSum / (float) intensityWindow.size();

        }

        Scan newScan = new SimpleScan(sc.getScanNumber(), sc.getMSLevel(),
                sc.getRetentionTime(), sc.getParentScanNumber(),
                sc.getPrecursorMZ(), sc.getFragmentScanNumbers(),
                sc.getMZValues(), newIntensities, sc.isCentroided());

        writer.addScan(newScan);

    }

}
