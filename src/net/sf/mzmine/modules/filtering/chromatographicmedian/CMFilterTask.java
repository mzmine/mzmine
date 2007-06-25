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

package net.sf.mzmine.modules.filtering.chromatographicmedian;

import java.io.IOException;
import java.util.Vector;

import net.sf.mzmine.data.Scan;
import net.sf.mzmine.data.impl.SimpleParameterSet;
import net.sf.mzmine.data.impl.SimpleScan;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.io.RawDataFileWriter;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.util.MathUtils;

/**
 * 
 */
class CMFilterTask implements Task {

    private RawDataFile dataFile;
    private TaskStatus status = TaskStatus.WAITING;
    private String errorMessage;

    private int filteredScans;
    private int totalScans;

    private RawDataFile filteredRawDataFile;

    private float mzTolerance;
    private int oneSidedWindowLength;

    /**
     * @param dataFile
     * @param parameters
     */
    CMFilterTask(RawDataFile dataFile, SimpleParameterSet parameters) {
        this.dataFile = dataFile;

        mzTolerance = (Float) parameters.getParameterValue(CMFilter.parameterMZTolerance);
        oneSidedWindowLength = (Integer) parameters.getParameterValue(CMFilter.parameterOneSidedWindowLength);
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
     */
    public String getTaskDescription() {
        return "Chromatographic median filtering " + dataFile;
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
            rawDataFileWriter = MZmineCore.getIOController().createNewFile(
                    dataFile);
        } catch (IOException e) {
            status = TaskStatus.ERROR;
            errorMessage = e.toString();
            return;
        }

        Scan[] scanBuffer = new Scan[1 + 2 * oneSidedWindowLength];
        Scan sc = null;

        int[] scanNumbers = dataFile.getScanNumbers();
        totalScans = scanNumbers.length;

        for (int scani = 0; scani < (totalScans + oneSidedWindowLength); scani++) {

            // Pickup next scan from original raw data file
            if (scani < totalScans) {
                
                sc = dataFile.getScan(scanNumbers[scani]);

                // ignore scans of MS level other than 1
                if (sc.getMSLevel() != 1) {
                    try {
                        rawDataFileWriter.addScan(sc);
                        filteredScans++;
                    } catch (IOException e) {
                        status = TaskStatus.ERROR;
                        errorMessage = e.toString();
                        return;
                    }
                    continue;
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
            sc = scanBuffer[oneSidedWindowLength];
            if (sc != null) {

                Integer[] dataPointIndices = new Integer[scanBuffer.length];
                for (int bufferIndex = 0; bufferIndex < scanBuffer.length; bufferIndex++) {
                    dataPointIndices[bufferIndex] = new Integer(0);
                }

                float[] mzValues = sc.getMZValues();
                float[] intValues = sc.getIntensityValues();
                float[] newIntValues = new float[intValues.length];

                for (int datapointIndex = 0; datapointIndex < mzValues.length; datapointIndex++) {

                    float mzValue = mzValues[datapointIndex];
                    float intValue = intValues[datapointIndex];

                    Vector<Float> intValueBuffer = new Vector<Float>();
                    intValueBuffer.add(new Float(intValue));

                    // Loop through the buffer
                    for (int bufferIndex = 0; bufferIndex < scanBuffer.length; bufferIndex++) {
                        // Exclude middle buffer element
                        // if (bufferIndex==oneSidedWindowLength) { continue; }

                        if ((bufferIndex != oneSidedWindowLength)
                                && (scanBuffer[bufferIndex] != null)) {
                            Object[] res = findClosestDatapointIntensity(
                                    mzValue, scanBuffer[bufferIndex],
                                    dataPointIndices[bufferIndex].intValue());
                            Float closestInt = (Float) (res[0]);
                            dataPointIndices[bufferIndex] = (Integer) (res[1]);
                            if (closestInt != null) {
                                intValueBuffer.add(closestInt);
                            }
                        }
                    }

                    // Calculate median of all intensity values in the buffer
                    float[] tmpIntensities = new float[intValueBuffer.size()];
                    for (int bufferIndex = 0; bufferIndex < tmpIntensities.length; bufferIndex++) {
                        tmpIntensities[bufferIndex] = intValueBuffer.get(
                                bufferIndex).floatValue();
                    }
                    float medianIntensity = MathUtils.calcQuantile(
                            tmpIntensities, (float) 0.5);

                    newIntValues[datapointIndex] = medianIntensity;

                }

                // Write the modified scan to file
                try {

                    SimpleScan newScan = new SimpleScan(sc);
                    newScan.setData(mzValues, newIntValues);
                    rawDataFileWriter.addScan(newScan);
                    filteredScans++;

                } catch (IOException e) {
                    status = TaskStatus.ERROR;
                    errorMessage = e.toString();
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
     * @param mzValue Search for datapoint that is closest to this mzvalue
     * @param s Search among datapoints in this scan
     * @param startIndex Start searching from this datapoint
     * @return Array of two objects, [0] is intensity of closest datapoint as
     *         Float or null if not a single datapoint was close enough. [1] is
     *         index of datapoint that was closest to given mz value (this will
     *         be used as starting point for next search) if nothing was close
     *         enough to given mz value, then this is the start index Return
     *         intensity of the found data point as Float. If not a single data
     *         point is close enough (mz tolerance) then null value is returned.
     */
    private Object[] findClosestDatapointIntensity(float mzValue, Scan s,
            int startIndex) {
        float[] massValues = s.getMZValues();
        float[] intensityValues = s.getIntensityValues();

        Integer closestIndex = null;

        float closestIntensity = -1;
        float closestDistance = Float.MAX_VALUE;

        float prevDistance = Float.MAX_VALUE;

        // Loop through datapoints
        for (int i = startIndex; i < massValues.length; i++) {

            // Check if this mass values is within range to mz value
            float tmpDistance = Math.abs(massValues[i] - mzValue);
            if (tmpDistance < mzTolerance) {

                // If this is closest datapoint so far, then store its' mz and
                // intensity
                if (tmpDistance <= closestDistance) {
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
        result[0] = new Float(closestIntensity);
        result[1] = closestIndex;

        return result;

    }

}
