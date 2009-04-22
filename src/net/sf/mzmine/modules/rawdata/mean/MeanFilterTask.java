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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.rawdata.mean;

import java.io.IOException;
import java.util.Vector;
import java.util.logging.Logger;

import net.sf.mzmine.data.DataPoint;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.RawDataFileWriter;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.data.impl.SimpleDataPoint;
import net.sf.mzmine.data.impl.SimpleScan;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskStatus;

/**
 * 
 */
class MeanFilterTask implements Task {

	private Logger logger = Logger.getLogger(this.getClass().getName());
	
    private RawDataFile dataFile, filteredRawDataFile;

    private TaskStatus status = TaskStatus.WAITING;
    private String errorMessage;

    // scan counter
    private int filteredScans, totalScans;

    // parameter values
    private String suffix;
    private double oneSidedWindowLength;
    private boolean removeOriginal;

    /**
     * @param rawDataFile
     * @param parameters
     */
    MeanFilterTask(RawDataFile dataFile, MeanFilterParameters parameters) {
        this.dataFile = dataFile;
        suffix = (String) parameters.getParameterValue(MeanFilterParameters.suffix);
        oneSidedWindowLength = ((Double) parameters.getParameterValue(MeanFilterParameters.oneSidedWindowLength)).doubleValue();
        removeOriginal = (Boolean) parameters.getParameterValue(MeanFilterParameters.autoRemove);
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
        logger.info("Running mean filter on " + dataFile);
        
        try {

            // Create new temporary file
            String newName = dataFile.getName() + " " + suffix;
            RawDataFileWriter rawDataFileWriter = MZmineCore.createNewFile(newName);

            // Get all scans
            int[] scanNumbers = dataFile.getScanNumbers();
            totalScans = scanNumbers.length;

            // Loop through all scans
            for (int i = 0; i < scanNumbers.length; i++) {

                if (status == TaskStatus.CANCELED)
                    return;

                Scan oldScan = dataFile.getScan(scanNumbers[i]);

                // ignore scans of MS level other than 1
                if (oldScan.getMSLevel() != 1) {
                    rawDataFileWriter.addScan(oldScan);
                    filteredScans++;
                    continue;
                }

                processOneScan(rawDataFileWriter, oldScan, oneSidedWindowLength);

                filteredScans++;

            }

            // Finalize writing
			filteredRawDataFile = rawDataFileWriter.finishWriting();
            MZmineCore.getCurrentProject().addFile(filteredRawDataFile);

            // Remove the original file if requested
            if (removeOriginal)
                MZmineCore.getCurrentProject().removeFile(dataFile);

            status = TaskStatus.FINISHED;
            logger.info("Finished mean filter on " + dataFile);

        } catch (IOException e) {
            status = TaskStatus.ERROR;
            errorMessage = e.toString();
            return;
        }

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

        DataPoint oldDataPoints[] = sc.getDataPoints();
        DataPoint newDataPoints[] = new DataPoint[oldDataPoints.length];

        int addi = 0;
        for (int i = 0; i < oldDataPoints.length; i++) {

            currentMass = oldDataPoints[i].getMZ();
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
            while ((addi < oldDataPoints.length)
                    && (oldDataPoints[addi].getMZ() <= hiLimit)) {
                massWindow.add(oldDataPoints[addi].getMZ());
                intensityWindow.add(oldDataPoints[addi].getIntensity());
                addi++;
            }

            elSum = 0;
            for (int j = 0; j < intensityWindow.size(); j++) {
                elSum += ((Double) (intensityWindow.get(j))).doubleValue();
            }

            newDataPoints[i] = new SimpleDataPoint(currentMass, elSum
                    / (double) intensityWindow.size());

        }

        // Create filtered scan
        Scan newScan = new SimpleScan(sc.getDataFile(), sc.getScanNumber(),
                sc.getMSLevel(), sc.getRetentionTime(),
                sc.getParentScanNumber(), sc.getPrecursorMZ(), sc.getPrecursorCharge(),
                sc.getFragmentScanNumbers(), newDataPoints, sc.isCentroided());

        // Write the scan to new file
        writer.addScan(newScan);

    }

	public Object[] getCreatedObjects() {
		return new Object[] { filteredRawDataFile };
	}

}
