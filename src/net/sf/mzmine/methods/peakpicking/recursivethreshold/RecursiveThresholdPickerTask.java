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

			// Get next scan
			Scan sc = null;
			try {
				sc = rawDataFile.getScan(scanNumbers[i]);
			} catch(IOException e) {
				status = TaskStatus.ERROR;
				errorMessage = e.toString();
				return;
			}

			double[] masses = sc.getMZValues();
			double[] intensities = sc.getIntensityValues();


			// Find 1D-peaks

			Vector<Integer> inds = new Vector<Integer>();
			recursiveThreshold(masses, intensities, 0, masses.length-1, parameters.noiseLevel, parameters.minimumMZPeakWidth, parameters.maximumMZPeakWidth, inds, 0);

			Vector<OneDimPeak> oneDimPeaks = new Vector<OneDimPeak>();
			for (Integer j : inds) {
				// Is intensity above the noise level
				if ( intensities[j] >= parameters.noiseLevel ) {

					// Determine correct bin
					int bin = (int)java.lang.Math.floor( (masses[j] - startMZ) / parameters.binSize );
					if (bin<0) { bin = 0; }
					if (bin>=numOfBins) { bin = numOfBins-1; }

					// Is intensity above the chromatographic threshold level for this bin?
					if (intensities[j]>=chromatographicThresholds[bin]) {
						oneDimPeaks.add(new OneDimPeak(i, j, masses[j], intensities[j]));
					}

				}

			}




			processedScans++;
		}

		status = TaskStatus.FINISHED;

    }



	/**
	 * This function searches for maximums from given part of a spectrum
	 */
	private int recursiveThreshold(double[] masses, double intensities[], int startInd, int stopInd, double thresholdLevel, double minPeakWidthMZ, double maxPeakWidthMZ, Vector<Integer> CentroidInds, int recuLevel) {

		int peakStartInd;
		int peakStopInd;
		int lastKnownGoodPeakStopInd;
		double peakWidthMZ;
		int peakMinInd;
		int peakMaxInd;

		lastKnownGoodPeakStopInd = stopInd;

		for (int ind = startInd; ind <= stopInd; ind++) {
			// While below threshold
			while ((ind<=stopInd) && (intensities[ind]<=thresholdLevel)) { ind++; }

			if (ind>=stopInd) { break; }

			peakStartInd = ind;
			peakMinInd = peakStartInd;
			peakMaxInd = peakStartInd;

			// While peak is on
			while ((ind<=stopInd) && (intensities[ind]>thresholdLevel)) {
				// Check if this is the minimum point of the peak
				if (intensities[ind]<intensities[peakMinInd]) {
					peakMinInd = ind;
				}

				// Check if this is the maximum poin of the peak
				if (intensities[ind]>intensities[peakMaxInd]) {
					peakMaxInd = ind;
				}

				ind++;
			}

			if (ind==stopInd) { ind--; }
			//peakStopInd = ind - 1;
			peakStopInd = ind-1;

			// Is this suitable peak?

			if (peakStopInd<0) {
				peakWidthMZ = 0;
			} else {
				int tmpInd1 = peakStartInd - 1;
				if (tmpInd1<startInd) { tmpInd1 = startInd; }
				int tmpInd2 = peakStopInd + 1;
				if (tmpInd2>stopInd) { tmpInd2 = stopInd; }
				peakWidthMZ = masses[peakStopInd]-masses[peakStartInd];
			}

			if ( (peakWidthMZ>=minPeakWidthMZ) && (peakWidthMZ<=maxPeakWidthMZ) ) {

				// Two options: define peak centroid index as maxintensity index or mean index of all indices
				CentroidInds.add(new Integer(peakMaxInd));

				if (recuLevel>0) { return peakStopInd+1; }
				// lastKnownGoodPeakStopInd = peakStopInd;
			}

			// Is there need for further investigation?
			if (peakWidthMZ>maxPeakWidthMZ) {
				ind = recursiveThreshold(masses, intensities, peakStartInd, peakStopInd, intensities[peakMinInd], minPeakWidthMZ, maxPeakWidthMZ, CentroidInds, recuLevel+1);
			}

			if (ind==(stopInd-1)) { break; }
		}

		// return lastKnownGoodPeakStopInd;
		return stopInd;

	}

	/**
	 * This class represent a 1D peak
	 */
	private class OneDimPeak {

		public int scanNum;

		public double mz;
		public double intensity;
		public int datapointIndex;

		private boolean connected;

		public OneDimPeak(int _scanNum, int _datapointIndex, double _mz, double _intensity) {
			scanNum = _scanNum;
			datapointIndex = _datapointIndex;
			mz = _mz;
			intensity = _intensity;

			connected = false;
		}

		public void setConnected() { connected = true; }
		public boolean isConnected() { return connected; }

	}



}
