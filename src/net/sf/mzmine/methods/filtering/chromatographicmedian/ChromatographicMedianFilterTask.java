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

package net.sf.mzmine.methods.filtering.chromatographicmedian;

import java.io.IOException;
import java.util.Vector;

import net.sf.mzmine.interfaces.Scan;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.io.RawDataFileWriter;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.util.MyMath;
import net.sf.mzmine.util.SimpleScan;

/**
 * 
 */
public class ChromatographicMedianFilterTask implements Task {

    private RawDataFile rawDataFile;
    private ChromatographicMedianFilterParameters parameters;
    private TaskStatus status;
    private String errorMessage;

    private int filteredScans;
    private int totalScans;

    private RawDataFile filteredRawDataFile;

    /**
     * @param rawDataFile
     * @param parameters
     */
    ChromatographicMedianFilterTask(RawDataFile rawDataFile,
            ChromatographicMedianFilterParameters parameters) {
        status = TaskStatus.WAITING;
        this.rawDataFile = rawDataFile;
        this.parameters = parameters;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
     */
    public String getTaskDescription() {
        return "Chromatographic median filtering " + rawDataFile;
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
        results[0] = rawDataFile;
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
     * @see net.sf.mzmine.taskcontrol.Task#getPriority()
     */
    public TaskPriority getPriority() {
        return TaskPriority.NORMAL;
    }

    /**
     * @see java.lang.Runnable#run()
     */
    public void run() {

        status = TaskStatus.PROCESSING;

        // Create new temporary copy
        RawDataFileWriter rawDataFileWriter;
        try {
            rawDataFileWriter = rawDataFile.createNewTemporaryFile();
        } catch (IOException e) {
            status = TaskStatus.ERROR;
            errorMessage = e.toString();
            return;
        }

        Scan[] scanBuffer = new Scan[1 + 2 * parameters.oneSidedWindowLength];
        Scan sc = null;

        int[] scanNumbers = rawDataFile.getScanNumbers(1);
        totalScans = scanNumbers.length;

        for (int scani = 0; scani < (totalScans + parameters.oneSidedWindowLength); scani++) {

            // Pickup next scan from original raw data file
            if (scani < totalScans) {
                try {
                    sc = rawDataFile.getScan(scanNumbers[scani]);
                } catch (IOException e) {
                    status = TaskStatus.ERROR;
                    errorMessage = e.toString();
                    try {
                        filteredRawDataFile = rawDataFileWriter.finishWriting();
                    } catch (IOException e2) {
                    }
                    return;
                }
            } else {
                sc = null;
            }

            // Advance scan buffer
            for (int bufferIndex = 0; bufferIndex < (scanBuffer.length - 1); bufferIndex++) {
                scanBuffer[bufferIndex] = scanBuffer[bufferIndex + 1];
            }
            scanBuffer[scanBuffer.length - 1] = sc;

            // Pickup mid element in the buffer
            sc = scanBuffer[parameters.oneSidedWindowLength];
            if (sc != null) {

                Integer[] dataPointIndices = new Integer[scanBuffer.length];
                for (int bufferIndex = 0; bufferIndex < scanBuffer.length; bufferIndex++) {
                    dataPointIndices[bufferIndex] = new Integer(0);
                }

                double[] mzValues = sc.getMZValues();
                double[] intValues = sc.getIntensityValues();
                double[] newIntValues = new double[intValues.length];

                for (int datapointIndex = 0; datapointIndex < mzValues.length; datapointIndex++) {

                    double mzValue = mzValues[datapointIndex];
                    double intValue = intValues[datapointIndex];

                    Vector<Double> intValueBuffer = new Vector<Double>();
                    intValueBuffer.add(new Double(intValue));

                    // Loop through the buffer
                    for (int bufferIndex = 0; bufferIndex < scanBuffer.length; bufferIndex++) {
                        // Exclude middle buffer element
                        // if (bufferIndex==oneSidedWindowLength) { continue; }

                        if ((bufferIndex != parameters.oneSidedWindowLength)
                                && (scanBuffer[bufferIndex] != null)) {
                            Object[] res = findClosestDatapointIntensity(
                                    mzValue, scanBuffer[bufferIndex],
                                    dataPointIndices[bufferIndex].intValue(),
                                    parameters);
                            Double closestInt = (Double) (res[0]);
                            dataPointIndices[bufferIndex] = (Integer) (res[1]);
                            if (closestInt != null) {
                                intValueBuffer.add(closestInt);
                            }
                        }
                    }

                    // Calculate median of all intensity values in the buffer
                    double[] tmpIntensities = new double[intValueBuffer.size()];
                    for (int bufferIndex = 0; bufferIndex < tmpIntensities.length; bufferIndex++) {
                        tmpIntensities[bufferIndex] = intValueBuffer.get(
                                bufferIndex).doubleValue();
                    }
                    double medianIntensity = MyMath.calcQuantile(
                            tmpIntensities, (double) 0.5);

                    newIntValues[datapointIndex] = medianIntensity;

                }

                // Write the modified scan to file
                try {

                    SimpleScan newScan = new SimpleScan(sc);
                    newScan.setData(mzValues, newIntValues);
                    rawDataFileWriter.addScan(newScan);

                } catch (IOException e) {
                    status = TaskStatus.ERROR;
                    errorMessage = e.toString();
                    try {
                        filteredRawDataFile = rawDataFileWriter.finishWriting();
                    } catch (IOException e2) {
                    }
                    return;
                }

            }

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

    /**
     * Searches for data point in a scan closest to given mz value.
     * 
     * @param mzValue
     *            Search for datapoint that is closest to this mzvalue
     * @param s
     *            Search among datapoints in this scan
     * @param startIndex
     *            Start searching from this datapoint
     * @return Array of two objects, [0] is intensity of closest datapoint as
     *         Double or null if not a single datapoint was close enough. [1] is
     *         index of datapoint that was closest to given mz value (this will
     *         be used as starting point for next search) if nothing was close
     *         enough to given mz value, then this is the start index Return
     *         intensity of the found data point as Double. If not a single data
     *         point is close enough (mz tolerance) then null value is returned.
     */
    private Object[] findClosestDatapointIntensity(double mzValue, Scan s,
            int startIndex, ChromatographicMedianFilterParameters param) {
        double[] massValues = s.getMZValues();
        double[] intensityValues = s.getIntensityValues();

        Integer closestIndex = null;

        double closestMZ = -1;
        double closestIntensity = -1;
        double closestDistance = Double.MAX_VALUE;

        double prevDistance = Double.MAX_VALUE;

        // Loop through datapoints
        for (int i = startIndex; i < massValues.length; i++) {

            // Check if this mass values is within range to mz value
            double tmpDistance = java.lang.Math.abs(massValues[i] - mzValue);
            if (tmpDistance < param.mzTolerance) {

                // If this is first datapoint within range, then save the index
                // if (firstIndex==null) { firstIndex = new Integer(i); }

                // If this is closest datapoint so far, then store its' mz and
                // intensity
                if (tmpDistance <= closestDistance) {
                    closestMZ = massValues[i];
                    closestIntensity = intensityValues[i];
                    closestDistance = tmpDistance;
                    closestIndex = new Integer(i);
                }

            }

            if (tmpDistance > prevDistance) {
                break;
            }

            prevDistance = tmpDistance;

        }

        if (closestIndex == null) {
            closestIndex = new Integer(startIndex);
        }

        Object[] result = new Object[2];
        result[0] = new Double(closestIntensity);
        result[1] = closestIndex;

        return result;

    }

}
