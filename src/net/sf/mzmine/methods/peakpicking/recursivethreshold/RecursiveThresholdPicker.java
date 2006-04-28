/*
    Copyright 2005 VTT Biotechnology

    This file is part of MZmine.

    MZmine is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    MZmine is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with MZmine; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
*/
package net.sf.mzmine.methods.peakpicking.recursivethreshold;
import java.text.NumberFormat;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.Vector;

import net.sf.mzmine.interfaces.Scan;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.methods.peakpicking.PeakConstruction;
import net.sf.mzmine.methods.peakpicking.PeakList;
import net.sf.mzmine.methods.peakpicking.PeakPicker;
import net.sf.mzmine.methods.peakpicking.PeakPickerParameters;
import net.sf.mzmine.userinterface.dialogs.ParameterSetupDialog;
import net.sf.mzmine.userinterface.mainwindow.MainWindow;
import net.sf.mzmine.util.MyMath;

public class RecursiveThresholdPicker implements PeakPicker {





////////////////////////////////////////////////////////////////////



	// Labels for parameters
	private final String[] fieldNames = {	"M/Z bin size (Da)",
											"Chromatographic threshold level (%)",
											"Noise level (absolute value)",
											"Minimum peak height (absolute value)",
											"Minimum peak duration (seconds)",
											"Minimum m/z peak width (Da)",
											"Maximum m/z peak width (Da)",
											"Tolerance for m/z variation (Da)",
											"Tolerance for intensity variation (%)" };



	/**
	 * Method asks parameter values from user
	 */
	public RecursiveThresholdPickerParameters askParameters(MainWindow mainWin, RecursiveThresholdPickerParameters currentValues) {

		// Initialize parameters
		RecursiveThresholdPickerParameters myParameters;
		if (currentValues==null) {
			myParameters = new RecursiveThresholdPickerParameters();
		} else {
			myParameters = currentValues;
		}

		// Show parameter setup dialog
		double[] paramValues = new double[9];
		paramValues[0] = myParameters.binSize;
		paramValues[1] = myParameters.chromatographicThresholdLevel;
		paramValues[2] = myParameters.noiseLevel;
		paramValues[3] = myParameters.minimumPeakHeight;
		paramValues[4] = myParameters.minimumPeakDuration;
		paramValues[5] = myParameters.minimumMZPeakWidth;
		paramValues[6] = myParameters.maximumMZPeakWidth;
		paramValues[7] = myParameters.mzTolerance;
		paramValues[8] = myParameters.intTolerance;

		// Define number formats for displaying each parameter
		NumberFormat[] numberFormats = new NumberFormat[9];

		numberFormats[0] = NumberFormat.getNumberInstance(); numberFormats[0].setMinimumFractionDigits(3);
		numberFormats[1] = NumberFormat.getPercentInstance();
		numberFormats[2] = NumberFormat.getNumberInstance(); numberFormats[2].setMinimumFractionDigits(0);
		numberFormats[3] = NumberFormat.getNumberInstance(); numberFormats[3].setMinimumFractionDigits(0);
		numberFormats[4] = NumberFormat.getNumberInstance(); numberFormats[4].setMinimumFractionDigits(1);
		numberFormats[5] = NumberFormat.getNumberInstance(); numberFormats[5].setMinimumFractionDigits(3);
		numberFormats[6] = NumberFormat.getNumberInstance(); numberFormats[6].setMinimumFractionDigits(3);
		numberFormats[7] = NumberFormat.getNumberInstance(); numberFormats[7].setMinimumFractionDigits(3);
		numberFormats[8] = NumberFormat.getPercentInstance();


		ParameterSetupDialog psd = new ParameterSetupDialog(mainWin, "Please check the parameter values", fieldNames, paramValues, numberFormats);
		psd.show();


		// Check if user clicked Cancel-button
		if (psd.getExitCode()==-1) {
			return null;
		}


		// Read parameter values
		double d;

		d = psd.getFieldValue(0);
		if (d<=0) {
			mainWin.displayErrorMessage("Incorrect bin size!");
			return null;
		}
		myParameters.binSize= d;

		d = psd.getFieldValue(1);
		if (d<0) {
			mainWin.displayErrorMessage("Incorrect chromatographic threshold level!");
			return null;
		}
		myParameters.chromatographicThresholdLevel = d;


		d = psd.getFieldValue(2);
		if (d<0) {
			mainWin.displayErrorMessage("Incorrect noise level!");
			return null;
		}
		myParameters.noiseLevel = d;

		d = psd.getFieldValue(3);
		if (d<=0) {
			mainWin.displayErrorMessage("Incorrect minimum peak height!");
			return null;
		}
		myParameters.minimumPeakHeight = d;

		d = psd.getFieldValue(4);
		if (d<=0) {
			mainWin.displayErrorMessage("Incorrect minimum peak duration!");
			return null;
		}
		myParameters.minimumPeakDuration = d;

		d = psd.getFieldValue(5);
		if (d<=0) {
			mainWin.displayErrorMessage("Incorrect minimum M/Z peak width!");
			return null;
		}
		myParameters.minimumMZPeakWidth = d;

		d = psd.getFieldValue(6);
		if (d<=0) {
			mainWin.displayErrorMessage("Incorrect maximum M/Z peak width!");
			return null;
		}
		myParameters.maximumMZPeakWidth = d;

		d = psd.getFieldValue(7);
		if (d<0) {
			mainWin.displayErrorMessage("Incorrect M/Z tolerance value!");
			return null;
		}
		myParameters.mzTolerance = d;

		d = psd.getFieldValue(8);
		if (d<0) {
			mainWin.displayErrorMessage("Incorrect intensity tolerance value!");
			return null;
		}
		myParameters.intTolerance = d;

		return myParameters;
	}






////////////////////////////////////////////////////////////////////






	/**
	 * This method does the processing
	 */
	//public Vector<Peak> findPeaks(NodeServer nodeServer, RawDataFile rawData, PeakPickerParameters _parameters) {
	public PeakList findPeaks(RawDataFile rawData, PeakPickerParameters _parameters) {

		RecursiveThresholdPickerParameters parameters = (RecursiveThresholdPickerParameters)_parameters;

		/*
			INITIALIZE SOME VARIABLES
		*/

		// These variables are used during binning (calculating a set of RICs)

		/*double startMZ = rawData.getMinMZValue();							// minimum m/z value in the raw data file
		double endMZ = rawData.getMaxMZValue();								// maximum m/z value in the raw data file
		int maxScanNum = rawData.getNumberOfScans();							// number of spectra in the raw data file
		int numOfBins = (int)(java.lang.Math.ceil((endMZ-startMZ)/parameters.binSize));// Calculate how many bins of desired size fit to entire m/z range of this run

		double[] tmpInts;													// intensity values for all bins of one spectrum
		double[][] binInts = new double[numOfBins][maxScanNum];				// this matrix contains a set of RICs
		double[] chromatographicThresholds = new double[numOfBins];			// thresholds for each RIC


		// These are misc variables used along the procedure

		Scan s;					// a scan and mass and intensity data values for it
		double[] masses;
		double[] intensities;

		Vector<OneDimPeak> oneDimPeaks;	// temporarily contains data point indices of local maximums in one spectrum
		int ind;				// temp index number
		int bin;				// temp bin number

		PeakList peakList = new PeakList();
		Vector<PeakConstruction> underConstructionPeaks = new Vector<PeakConstruction>();					// This vector contains all peaks that are still available for adding new m/z peaks

		double mass;																	// mass value for current m/z peak
		double intensity;															// intensity value for current m/z peak


		int minPeakLengthInScans = rawData.getScanNumberByTime(parameters.minimumPeakDuration);	// Information about minimum peak length is used in initializing peak objects with some sensible quess of suitable length



		/*
			CALCULATE M/Z BINNING (SET OF XICS) AND THRESHOLD LEVEL FOR EACH XIC


		// Calculate set of XICs by binning intensites in each spectrum

		if (parameters.chromatographicThresholdLevel>0) {
			rawData.initializeScanBrowser(0,maxScanNum-1);
			for (int scani=0; scani<maxScanNum; scani++) {
				s = rawData.getNextScan();
				//statBar.setStatusProgBar(scani,maxScanNum*2-2);

				//nodeServer.updateJobCompletionRate((double)(scani)/(double)(maxScanNum*2-2));

				//tmpInts = s.getBinnedIntensities(startMZ, endMZ, numOfBins, true);
				for (int bini=0; bini<numOfBins; bini++) {
					//binInts[bini][scani] = tmpInts[bini];
				}
			}
			rawData.finalizeScanBrowser();

			// Calculate filtering threshold from each XIC
			for (int bini=0; bini<numOfBins; bini++) {
				chromatographicThresholds[bini] = MyMath.calcQuantile(binInts[bini], parameters.chromatographicThresholdLevel);
			}
		} else {
			for (int bini=0; bini<numOfBins; bini++) {
				chromatographicThresholds[bini] = 0;
			}
		}


		binInts = null;
		tmpInts = null;
		System.gc();



		/*
			LOOP THROUGH ALL SCANS IN THIS RAW DATA FILE


		rawData.initializeScanBrowser(0, rawData.getNumberOfScans()-1);
		for (int i=0; i<maxScanNum; i++) {


			// GET NEXT SPECTRUM

			//nodeServer.updateJobCompletionRate((double)(i+maxScanNum)/(double)(maxScanNum*2-2));

			s = rawData.getNextScan();
			masses = s.getMZValues();
			intensities = s.getIntensityValues();


			// FIND AND 1D-PEAKS IN THIS SPECTRUM

			Vector<Integer> inds = new Vector<Integer>();
			recursiveThreshold(masses, intensities, 0, masses.length-1, parameters.noiseLevel, parameters.minimumMZPeakWidth, parameters.maximumMZPeakWidth, inds, 0);

			oneDimPeaks = new Vector<OneDimPeak>();
			for (Integer j : inds) {

				// Is intensity above the noise level
				if ( intensities[j] >= parameters.noiseLevel ) {

					// Determine correct bin
					bin = (int)java.lang.Math.floor( (masses[j] - startMZ) / parameters.binSize );
					if (bin<0) { bin = 0; }
					if (bin>=numOfBins) { bin = numOfBins-1; }

					// Is intensity above the chromatographic threshold level for this bin?
					if (intensities[j]>=chromatographicThresholds[bin]) {
						oneDimPeaks.add(new OneDimPeak(i, j, masses[j], intensities[j]));
					}

				}

			}


			// CALCULATE SCORES BETWEEN ALL 1D-PEAKS AND UNDER-CONSTRUCTION -PEAKS


			TreeSet<MatchScore> scores = new TreeSet<MatchScore>();

			double maxScore = 1000;

			for (PeakConstruction ucPeak : underConstructionPeaks) {
				for (OneDimPeak oneDimPeak : oneDimPeaks) {
					MatchScore score = calcScore(ucPeak, oneDimPeak, maxScore, parameters);
					if (score.getScore()<Double.MAX_VALUE) { scores.add(score); }
				}
			}



			// CONNECT BEST (LOWEST) SCORING 1D- AND UC-PEAK PAIRS

			Iterator<MatchScore> scoreIterator = scores.iterator();
			while (scoreIterator.hasNext()) {
				MatchScore score = scoreIterator.next();

				// If score is too high for connecting, then stop the loop
				if ( score.getScore()>=Double.MAX_VALUE ) { break; }

				// If 1d- or uc-peak participating to this score is already connected, then skip this one
				if (score.getOneDimPeak().isConnected()) { continue; }
				if (score.getUnderConstructionPeak().isGrowing()) { continue; }

				// Connect 1d to uc
				PeakConstruction ucPeak = score.getUnderConstructionPeak();
				OneDimPeak oneDimPeak = score.getOneDimPeak();
				ucPeak.addScan(score.getOneDimPeak().scanNum, score.getOneDimPeak().datapointIndex, score.getOneDimPeak().mz, score.getOneDimPeak().intensity);
				ucPeak.setGrowingStatus(true);
				oneDimPeak.setConnected();

			}


			// CHECK IF THERE ARE SOME UNDER-CONSTRUCTION PEAKS THAT WERE NOT CONNECTED DURING THIS SCAN
			int ucInd=0;
			for (PeakConstruction ucPeak : underConstructionPeaks) {

				// If nothing was added,
				if (!ucPeak.isGrowing()) {


					// Check length
					double ucLength = ucPeak.getLengthInSecs();
					double ucHeight = ucPeak.getMaxIntensity();
					if (	(ucLength>=parameters.minimumPeakDuration) &&
							(ucHeight >= parameters.minimumPeakHeight)
					) {

						// Suitable length, finalize the peak
						ucPeak.finalizePeak();

						Peak finalPeak = new Peak(	ucPeak.getCentroidMZMedian(),
													ucPeak.getMaxIntensityTime(),
													ucPeak.getMaxIntensity(),
													ucPeak.getSumOfIntensities(),
													ucPeak.getCentroidMZs(),
													ucPeak.getScanNums(),
													ucPeak.getCentroidIntensities()	);

						peakList.addPeak(finalPeak);

					}

					// Remove the peak from under construction peaks
					underConstructionPeaks.set(ucInd, null);	// Can't remove this element right here, because then this loop would fail for successive peaks
				}
				ucInd++;
			}
			// Clear under construction peaks from removed peaks and clear growing status for rest of them
			for (ucInd=0; ucInd<underConstructionPeaks.size(); ucInd++) {
				PeakConstruction ucPeak = underConstructionPeaks.get(ucInd);
				if (ucPeak==null) {
					underConstructionPeaks.remove(ucInd);
					ucInd--;
				} else {
					ucPeak.setGrowingStatus(false);
				}
			}


			// CHECK IF THERE ARE SOME 1D-PEAKS THAT WERE NOT CONNECTED
			for (OneDimPeak oneDimPeak : oneDimPeaks) {


				if (!oneDimPeak.isConnected()) {

					PeakConstruction ucPeak = new PeakConstruction(rawData, minPeakLengthInScans, minPeakLengthInScans);
					ucPeak.addScan(i, oneDimPeak.datapointIndex, oneDimPeak.mz, oneDimPeak.intensity);
					underConstructionPeaks.add(ucPeak);
				}
			}

		} // end of next scan loop

		// Finally finalize all remaining under construction peaks

		for (PeakConstruction ucPeak : underConstructionPeaks) {

			// Check length
			double ucLength = ucPeak.getLengthInSecs();
			double ucHeight = ucPeak.getMaxIntensity();
			if (	(ucLength>=parameters.minimumPeakDuration) &&
					(ucHeight >= parameters.minimumPeakHeight)	) {

				// Suitable length, finalize the peak
				ucPeak.finalizePeak();
				Peak finalPeak = new Peak(	ucPeak.getCentroidMZMedian(),
											ucPeak.getMaxIntensityTime(),
											ucPeak.getMaxIntensity(),
											ucPeak.getSumOfIntensities(),
											ucPeak.getCentroidMZs(),
											ucPeak.getScanNums(),
											ucPeak.getCentroidIntensities()	);

				peakList.addPeak(finalPeak);

			}

		}


		// Done
		rawData.finalizeScanBrowser();

		return peakList;
        */
        return null;
	}



	/**
	 * This functions calculates the score for goodness of match between uc-peak and 1d-peak
	 */
	private MatchScore calcScore(PeakConstruction ucPeak, OneDimPeak oneDimPeak, double maxScore, RecursiveThresholdPickerParameters parameters) {

		double ucMZ = ucPeak.getCentroidMZMedian();
		double[] ucPrevInts = ucPeak.getCentroidIntensities();
		int ucUsedSize = ucPeak.getUsedSize();

		// If mz difference is too big? (do this first for optimal performance)
		if ( java.lang.Math.abs(ucMZ-oneDimPeak.mz) > parameters.mzTolerance ) {

			return new MatchScore(ucPeak, oneDimPeak, Double.MAX_VALUE);

		} else {

			// Calculate score components and total score
			double scoreMZComponent = java.lang.Math.abs(ucMZ-oneDimPeak.mz)*maxScore;
			double scoreRTComponent = calcScoreForRTShape(ucPrevInts, ucUsedSize, oneDimPeak.intensity, parameters.intTolerance, maxScore);
			double totalScore = java.lang.Math.sqrt(scoreMZComponent*scoreMZComponent + scoreRTComponent*scoreRTComponent);

			return new MatchScore(ucPeak, oneDimPeak, totalScore);

		}

	}


	/**
	 * This function check for the shape of the peak in RT direction, and
	 * determines if it is possible to add given m/z peak at the end of the peak.

	 *
	 *
	 */
	private double calcScoreForRTShape(double prevInts[], int usedSize, double nextInt, double intTolerance, double maxScore) {


		// If no previous m/z peaks
		if (usedSize==0) {
			return 0;
		}

		// If only one previous m/z peak,
		if (usedSize==1) {

			// If it goes up, then give minimum (best) score
			if ((nextInt-prevInts[0])>=0) {
				return 0;
			}

			// If it goes too much down, then give MAX_VALUE
			double bottomMargin = prevInts[0]*(1-intTolerance);
			if (nextInt<=bottomMargin) { return Double.MAX_VALUE; }

			// If it goes little bit down, but within marginal, then give score between 0...maxScore
			//return maxScore * ( (prevInts[0]-nextInt) / ( prevInts[0] - bottomMargin) );
			return 0;

		}


		// There are two or more previous m/z peaks in this peak

		// Determine shape of the peak

		int derSign = 1;
		for (int ind=2; ind<usedSize; ind++) {


			// If peak is currently going up
			if (derSign==1) {
				// Then next intensity must be above bottomMargin or derSign changes
				double bottomMargin = prevInts[ind-1]*(1-intTolerance);

				if ( prevInts[ind] <= bottomMargin ) {
					derSign = -1;
					continue;
				}
			}

			// If peak is currently going down
			if (derSign==-1) {
				// Then next intensity should be less than topMargin or peak ends
				double topMargin = prevInts[ind-1]*(1+intTolerance);

				if ( prevInts[ind] >= topMargin ) {
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

			// Then peak must not start going up again
			double topMargin = prevInts[usedSize-1]*(1+intTolerance);

			if ( nextInt>=topMargin) {
				return Double.MAX_VALUE;
			}

			if ( nextInt<prevInts[usedSize-1] ) {
				return 0;
			}

			//return maxScore * ( 1 - ( (topMargin-nextInt) / (topMargin-prevInts[usedSize-1]) ) );
			return 0;
		}

		// Should never go here
		return Double.MAX_VALUE;

	}


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

	private class MatchScore implements Comparable<MatchScore> {

		private double score;
		private PeakConstruction ucPeak;
		private OneDimPeak oneDimPeak;


		public MatchScore(PeakConstruction uc, OneDimPeak od, double s) {
			ucPeak = uc;
			oneDimPeak = od;
			score = s;
		}

		public int compareTo(MatchScore m) {
			int retsig = (int)java.lang.Math.signum(score-m.getScore());
			if (retsig==0) { retsig=-1; } // Must never return 0, because treeset can't hold equal elements
			return retsig;
		}

		public void setScore(double s) { score = s; }
		public double getScore() { return score; }

		public PeakConstruction getUnderConstructionPeak() { return ucPeak; }
		public OneDimPeak getOneDimPeak() { return oneDimPeak; }

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

}

