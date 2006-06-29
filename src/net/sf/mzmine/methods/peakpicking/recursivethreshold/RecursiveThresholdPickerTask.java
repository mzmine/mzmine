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
import java.util.Hashtable;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.Vector;

import net.sf.mzmine.interfaces.Scan;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.methods.peakpicking.SimplePeak;
import net.sf.mzmine.methods.peakpicking.SimplePeakList;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.util.MyMath;
import net.sf.mzmine.util.ScanUtils;


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

    private SimplePeakList readyPeakList;



    /**
     * @param rawDataFile
     * @param parameters
     */
    RecursiveThresholdPickerTask(RawDataFile rawDataFile, RecursiveThresholdPickerParameters parameters) {
        status = TaskStatus.WAITING;
        this.rawDataFile = rawDataFile;
        this.parameters = parameters;

        readyPeakList = new SimplePeakList();
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
				double[] tmpInts = ScanUtils.binValues(mzValues, intensityValues, startMZ, endMZ, numOfBins, true, ScanUtils.BinningType.MAX);
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
		double[] chromatographicThresholds = new double[numOfBins];
		for (int bini=0; bini<numOfBins; bini++) {

			chromatographicThresholds[bini] = MyMath.calcQuantile(binInts[bini], parameters.chromatographicThresholdLevel);
			if (chromatographicThresholds[bini]<initialThreshold) {
				initialThreshold = chromatographicThresholds[bini];
			}
		}

		binInts = null;
		System.gc();



		Vector<SimplePeak> underConstructionPeaks = new Vector<SimplePeak>();
		Vector<SimplePeak> detectedPeaks = new Vector<SimplePeak>();

		/**
		 * Loop through scans
		 */

		Vector<OneDimPeak> oneDimPeaks = new Vector<OneDimPeak>();
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

			//System.out.print("Find 1D-peaks: ");

			Vector<Integer> inds = new Vector<Integer>();
			recursiveThreshold(masses, intensities, 0, masses.length-1, parameters.noiseLevel, parameters.minimumMZPeakWidth, parameters.maximumMZPeakWidth, inds, 0);

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

			//System.out.println("Found " + oneDimPeaks.size() + " 1D-peaks.");


			// Calculate scores between under-construction scores and 1d-peaks

			TreeSet<MatchScore> scores = new TreeSet<MatchScore>();

			for (SimplePeak ucPeak : underConstructionPeaks) {

				for (OneDimPeak oneDimPeak : oneDimPeaks) {
					MatchScore score = new MatchScore(ucPeak, oneDimPeak);
					if (score.getScore()<Double.MAX_VALUE) { scores.add(score); }
				}

			}


			// Connect the best scoring pairs of under-construction and 1d peaks

			Iterator<MatchScore> scoreIterator = scores.iterator();
			while (scoreIterator.hasNext()) {
				MatchScore score = scoreIterator.next();

				// If score is too high for connecting, then stop the loop
				if ( score.getScore()>=Double.MAX_VALUE ) { break; }

				// If 1d peak is already connected, then move to next score
				OneDimPeak oneDimPeak = score.getOneDimPeak();
				if (oneDimPeak.isConnected()) { continue; }

				// If uc peak is already connected, then move on to next score
				SimplePeak ucPeak = score.getPeak();
				if (ucPeak.isGrowing()) { continue; }

				// Connect 1d to uc
				ucPeak.addDatapoint(sc.getScanNumber(), oneDimPeak.mz, sc.getRetentionTime(), oneDimPeak.intensity);
				oneDimPeak.setConnected();

			}

			// Check if there are any under-construction peaks that were not connected
			for (SimplePeak ucPeak : underConstructionPeaks) {

				// If nothing was added,
				if (!ucPeak.isGrowing()) {

					// Check length
					double ucLength = ucPeak.getMaxRT()-ucPeak.getMinRT();
					double ucHeight = ucPeak.getRawHeight();
					if (	(ucLength>=parameters.minimumPeakDuration) &&
							(ucHeight >= parameters.minimumPeakHeight)
					) {

						// Good peak, add it to the peak list
						readyPeakList.addPeak(ucPeak);
					}

					// Remove the peak from under construction peaks
					int ucInd = underConstructionPeaks.indexOf(ucPeak);
					underConstructionPeaks.set(ucInd, null);
				}

			}

			//System.out.println("" + readyPeakList.getNumberOfPeaks() +  " ready peaks.");

			// Clean-up empty slots under-construction peaks collection and reset growing statuses for remaining under construction peaks
			for (int ucInd=0; ucInd<underConstructionPeaks.size(); ucInd++) {
				SimplePeak ucPeak = underConstructionPeaks.get(ucInd);
				if (ucPeak==null) {
					underConstructionPeaks.remove(ucInd);
					ucInd--;
				} else {
					ucPeak.resetGrowingState();
				}
			}



			// If there are some unconnected 1d-peaks, then start a new under-construction peak for each of them
			for (OneDimPeak oneDimPeak : oneDimPeaks) {

				if (!oneDimPeak.isConnected()) {

					SimplePeak ucPeak = new SimplePeak();
					ucPeak.addDatapoint(sc.getScanNumber(), oneDimPeak.mz, sc.getRetentionTime(), oneDimPeak.intensity);
					underConstructionPeaks.add(ucPeak);

				}

			}

			oneDimPeaks.clear();


			processedScans++;

		} // End of scan loop



		// Finally process all remaining under-construction peaks

		for (SimplePeak ucPeak : underConstructionPeaks) {

			// Check length & height
			double ucLength = ucPeak.getMaxRT()-ucPeak.getMinRT();
			double ucHeight = ucPeak.getRawHeight();
			if (	(ucLength>=parameters.minimumPeakDuration) &&
					(ucHeight >= parameters.minimumPeakHeight)
			) {

				// Good peak, add it to the peak list
				readyPeakList.addPeak(ucPeak);

			}

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



	/**
	 * This class represents a score (goodness of fit) between Peak and 1D-peak
	 */
	private class MatchScore implements Comparable<MatchScore> {

		private double score;
		private SimplePeak ucPeak;
		private OneDimPeak oneDimPeak;


		public MatchScore(SimplePeak uc, OneDimPeak od) {
			ucPeak = uc;
			oneDimPeak = od;
			score = calcScore(uc, od);
		}

		public double getScore() { return score; }
		public SimplePeak getPeak() { return ucPeak; }
		public OneDimPeak getOneDimPeak() { return oneDimPeak; }

		public int compareTo(MatchScore m) {
			int retsig = (int)java.lang.Math.signum(score-m.getScore());
			if (retsig==0) { retsig=-1; } // Must never return 0, because treeset can't hold equal elements
			return retsig;
		}


		private double calcScore(SimplePeak uc, OneDimPeak od) {

			double ucMZ = uc.getRawMZ();

			// If mz difference is too big? (do this first for optimal performance)
			if ( java.lang.Math.abs(ucMZ-od.mz) > parameters.mzTolerance ) {
				return Double.MAX_VALUE;

			} else {

				// Calculate score components and total score
				double scoreMZComponent = java.lang.Math.abs(ucMZ-od.mz);
				double scoreRTComponent = calcScoreForRTShape(uc, od);
				double totalScore = java.lang.Math.sqrt(scoreMZComponent*scoreMZComponent + scoreRTComponent*scoreRTComponent);

				return totalScore;
			}

		}

		/**
		 * This function check for the shape of the peak in RT direction, and
		 * determines if it is possible to add given m/z peak at the end of the peak.
		 *
		 */
		private double calcScoreForRTShape(SimplePeak uc, OneDimPeak od) {

			double nextIntensity = od.intensity;
			Hashtable<Integer, Double[]> datapoints = uc.getRawDatapoints();

			// If no previous m/z peaks
			if (datapoints.size() == 0) {
				return 0;
			}


			// If only one previous m/z peak
			if (datapoints.size() == 1) {

				Object[] triplets = datapoints.values().toArray();
				double prevIntensity = ((Double[])(triplets[0]))[2];
				triplets = null;

				// If it goes up, then give minimum (best) score
				if ((nextIntensity-prevIntensity) >=0 ) {
					return 0;
				}

				// If it goes too much down, then give MAX_VALUE
				double bottomMargin = prevIntensity*(1-parameters.intTolerance);
				if (nextIntensity<=bottomMargin) { return Double.MAX_VALUE; }

				// If it goes little bit down, but within marginal, then give score between 0...maxScore
				//return ( (prevIntensity-nextIntensity) / ( prevIntensity-bottomMargin) );
				return 0;

			}


			// There are two or more previous m/z peaks in this peak

			// Determine shape of the peak

			Object[] triplets = datapoints.values().toArray();
			int derSign = 1;
			for (int ind=1; ind<triplets.length; ind++) {

				double prevIntensity = ((Double[])(triplets[ind-1]))[2];
				double currIntensity = ((Double[])(triplets[ind]))[2];

				// If peak is currently going up
				if (derSign==1) {
					// Then next intensity must be above bottomMargin or derSign changes
					double bottomMargin = prevIntensity*(1-parameters.intTolerance);

					if ( currIntensity <= bottomMargin ) {
						derSign = -1;
						continue;
					}
				}

				// If peak is currently going down
				if (derSign==-1) {
					// Then next intensity should be less than topMargin or peak ends
					double topMargin = prevIntensity*(1+parameters.intTolerance);

					if ( currIntensity >= topMargin ) {
						return Double.MAX_VALUE;
					}
				}

			}
			// derSign now contains information about RT peak shape at the end of the peak so far

			// If peak is currently going up
			if (derSign==1) {

				// Then give minimum (best) score in any case (peak can continue going up or start going down)
				return 0;
			}

			// If peak is currently going down
			if (derSign==-1) {


				double prevIntensity = ((Double[])(triplets[triplets.length-1]))[2];


				// Then peak must not start going up again
				double topMargin = prevIntensity*(1+parameters.intTolerance);

				if ( nextIntensity>=topMargin) {
					return Double.MAX_VALUE;
				}

				if ( nextIntensity<prevIntensity ) {
					return 0;
				}

				//return maxScore * ( 1 - ( (topMargin-nextInt) / (topMargin-prevInts[usedSize-1]) ) );
				return 0;
			}

			// Should never go here
			return Double.MAX_VALUE;

		}

	}

}
