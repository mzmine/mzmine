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

package net.sf.mzmine.methods.peakpicking.recursivethreshold;

import java.io.IOException;
import java.util.Vector;

import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.interfaces.Scan;
import net.sf.mzmine.interfaces.PeakList;
import net.sf.mzmine.methods.peakpicking.PeakListImpl;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.util.MyMath;


/**
 *
 */
public class RecursiveThresholdPickerTask implements Task {

    private RawDataFile rawDataFile;
    private RecursiveThresholdPickerParameters parameters;
    private TaskStatus status;
    private String errorMessage;

    private int processedScans;
    private int totalScans;

    private PeakListImpl readyPeakList;



    /**
     * @param rawDataFile
     * @param parameters
     */
    RecursiveThresholdPickerTask(RawDataFile rawDataFile, RecursiveThresholdPickerParameters parameters) {
        status = TaskStatus.WAITING;
        this.rawDataFile = rawDataFile;
        this.parameters = parameters;

        readyPeakList = new PeakListImpl();
    }


    /**
     * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
     */
    public String getTaskDescription() {
        return "Recursive threshold peak detection on " + rawDataFile;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
     */
    public float getFinishedPercentage() {
		if (totalScans == 0) return 0.0f;
        return (float) processedScans / (2.0f*totalScans);
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
		results[1] = readyPeakList;
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

		int[] scanNumbers = rawDataFile.getScanNumbers(1);

		totalScans = scanNumbers.length;

		/*
		 * Calculate M/Z binning
		 */

		double startMZ = rawDataFile.getDataMinMZ(1);						// minimum m/z value in the raw data file
		double endMZ = rawDataFile.getDataMaxMZ(1);							// maximum m/z value in the raw data file
		int numOfBins = (int)(java.lang.Math.ceil((endMZ-startMZ)/parameters.binSize));
		double[][] binInts = new double[numOfBins][totalScans];

		// Loop through scans and calculate binned maximum intensities
		for (int i=0; i<totalScans; i++) {

			if (status == TaskStatus.CANCELED) return;

			try {
				Scan sc = rawDataFile.getScan(scanNumbers[i]);

				double[] mzValues = sc.getMZValues();
				double[] intensityValues = sc.getIntensityValues();
				double[] tmpInts = MyMath.binValues(mzValues, intensityValues, startMZ, endMZ, numOfBins, true, MyMath.BinningType.MAX);
				for (int bini=0; bini<numOfBins; bini++) {
					binInts[bini][i] = tmpInts[bini];
				}


			} catch(IOException e) {
				status = TaskStatus.ERROR;
				errorMessage = e.toString();
			}

			processedScans++;

		}

		// Calculate filtering threshold from each RIC
		double initialThreshold = Double.MAX_VALUE;
		double[] chromatographicThresholds = new double[totalScans];
		for (int bini=0; bini<numOfBins; bini++) {
			chromatographicThresholds[bini] = MyMath.calcQuantile(binInts[bini], parameters.chromatographicThresholdLevel);
			if (chromatographicThresholds[bini]<initialThreshold) {
				initialThreshold = chromatographicThresholds[bini];
			}
		}

		binInts = null;
		System.gc();




		/**
		 * Loop through scans
		 */
		for (int i=0; i<totalScans; i++) {

			if (status == TaskStatus.CANCELED) return;

			try {
				Scan sc = rawDataFile.getScan(scanNumbers[i]);
			} catch(IOException e) {
				status = TaskStatus.ERROR;
				errorMessage = e.toString();
			}

			processedScans++;
		}

		status = TaskStatus.FINISHED;

    }


}
