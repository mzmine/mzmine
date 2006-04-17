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
package net.sf.mzmine.methods.peakpicking;
import java.text.NumberFormat;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.Vector;

import net.sf.mzmine.obsoletedatastructures.RawDataAtNode;
import net.sf.mzmine.obsoletedatastructures.Scan;
import net.sf.mzmine.userinterface.MainWindow;
import net.sf.mzmine.userinterface.ParameterSetupDialog;
import net.sf.mzmine.util.MyMath;


/**
 * This class implements a peak picker based on searching for local maximums in each spectra
 */
public class CentroidPicker implements PeakPicker {

	// Labels for parameters
	private final String[] fieldNames = {	"M/Z bin size (Da)",
											"Chromatographic threshold level (%)",
											"Noise level (absolute value)",
											"Minimum peak height (absolute value)",
											"Minimum peak duration (seconds)",
											"Tolerance for m/z variation (Da)",
											"Tolerance for intensity variation (%)" };



	/**
	 * Method asks parameter values from user
	 */
	public CentroidPickerParameters askParameters(MainWindow mainWin, CentroidPickerParameters currentValues) {

		// Initialize parameters
		CentroidPickerParameters myParameters;
		if (currentValues==null) {
			myParameters = new CentroidPickerParameters();
		} else {
			myParameters = currentValues;
		}

		// Show parameter setup dialog
		double[] paramValues = new double[7];
		paramValues[0] = myParameters.binSize;
		paramValues[1] = myParameters.chromatographicThresholdLevel;
		paramValues[2] = myParameters.noiseLevel;
		paramValues[3] = myParameters.minimumPeakHeight;
		paramValues[4] = myParameters.minimumPeakDuration;
		paramValues[5] = myParameters.mzTolerance;
		paramValues[6] = myParameters.intTolerance;

		// Define number formats for displaying each parameter
		NumberFormat[] numberFormats = new NumberFormat[7];

		numberFormats[0] = NumberFormat.getNumberInstance(); numberFormats[0].setMinimumFractionDigits(2);
		numberFormats[1] = NumberFormat.getPercentInstance();
		numberFormats[2] = NumberFormat.getNumberInstance(); numberFormats[2].setMinimumFractionDigits(0);
		numberFormats[3] = NumberFormat.getNumberInstance(); numberFormats[3].setMinimumFractionDigits(0);
		numberFormats[4] = NumberFormat.getNumberInstance(); numberFormats[4].setMinimumFractionDigits(1);
		numberFormats[5] = NumberFormat.getNumberInstance(); numberFormats[5].setMinimumFractionDigits(3);
		numberFormats[6] = NumberFormat.getPercentInstance();


		ParameterSetupDialog psd = new ParameterSetupDialog(mainWin, "Please check the parameter values", fieldNames, paramValues, numberFormats);
		psd.showModal(mainWin.getDesktop());

		/*
		psd.setLocationRelativeTo(mainWin);
		psd.setVisible(true);
		*/


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
		myParameters.binSize = d;

		d = psd.getFieldValue(1);
		if ((d<0) || (d>1)) {
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
		if (d<0) {
			mainWin.displayErrorMessage("Incorrect m/z tolerance value!");
			return null;
		}
		myParameters.mzTolerance = d;

		d = psd.getFieldValue(6);
		if (d<0) {
			mainWin.displayErrorMessage("Incorrect intensity tolerance value!");
			return null;
		}
		myParameters.intTolerance = d;

		return myParameters;
	}



	/**
	 * This method does the processing
	 */
	//public Vector<Peak> findPeaks(NodeServer nodeServer, RawDataAtNode rawData, PeakPickerParameters _parameters) {
	public PeakList findPeaks(RawDataAtNode rawData, PeakPickerParameters _parameters) {

		CentroidPickerParameters parameters = (CentroidPickerParameters)_parameters;

		/*
			INITIALIZE SOME VARIABLES
		*/

		// These variables are used during binning (calculating a set of RICs)

		double startMZ = rawData.getMinMZValue();							// minimum m/z value in the raw data file
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
		*/

		// Calculate set of XICs by binning intensites in each spectrum

		if (parameters.chromatographicThresholdLevel>0) {
			rawData.initializeScanBrowser(0,maxScanNum-1);
			for (int scani=0; scani<maxScanNum; scani++) {
				s = rawData.getNextScan();
				//statBar.setStatusProgBar(scani,maxScanNum*2-2);

				//nodeServer.updateJobCompletionRate((double)(scani)/(double)(maxScanNum*2-2));

				tmpInts = s.getBinnedIntensities(startMZ, endMZ, numOfBins, true);
				for (int bini=0; bini<numOfBins; bini++) {
					binInts[bini][scani] = tmpInts[bini];
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
		*/

		rawData.initializeScanBrowser(0, rawData.getNumberOfScans()-1);
		for (int i=0; i<maxScanNum; i++) {


			// GET NEXT SPECTRUM

			// nodeServer.updateJobCompletionRate((double)(i+maxScanNum)/(double)(maxScanNum*2-2));

			s = rawData.getNextScan();
			masses = s.getMZValues();
			intensities = s.getIntensityValues();


			// FIND AND 1D-PEAKS IN THIS SPECTRUM

			oneDimPeaks = new Vector<OneDimPeak>();
			for (int j=0; j<intensities.length; j++) {

				// Is intensity above the noise level?
				if ( intensities[j] >= parameters.noiseLevel ) {

					// Determine correct bin
					bin = (int)java.lang.Math.floor( (masses[j] - startMZ) / parameters.binSize );
					if (bin<0) { bin = 0; }
					if (bin>=numOfBins) { bin = numOfBins-1; }

					// Is intensity above the chromatographic threshold level for this bin?
					if (intensities[j]>=chromatographicThresholds[bin]) {

						// Yes, then mark this index as 1D-peak
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


					// Crop peak shape
					ucPeak.finalizePeak();
					int[] startStopInd = cropRTShape(ucPeak.getCentroidIntensities(), parameters);
					if (startStopInd[0]<=startStopInd[1]) {

						ucPeak.finalizePeak(startStopInd[0], startStopInd[1]);

						// Check length
						double ucLength = ucPeak.getLengthInSecs();
						double ucHeight = ucPeak.getMaxIntensity();
						if (	(ucLength>=parameters.minimumPeakDuration) &&
								(ucHeight >= parameters.minimumPeakHeight)
						) {

							// Suitable length => Create a peak
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

			// Crop peak shape
			ucPeak.finalizePeak();
			int[] startStopInd = cropRTShape(ucPeak.getCentroidIntensities(), parameters);
			if (startStopInd[0]<=startStopInd[1]) {
				ucPeak.finalizePeak(startStopInd[0], startStopInd[1]);

				// Check length
				double ucLength = ucPeak.getLengthInSecs();
				double ucHeight = ucPeak.getMaxIntensity();
				if (	(ucLength>=parameters.minimumPeakDuration) &&
						(ucHeight >= parameters.minimumPeakHeight)
				) {

					// Suitable length => Create a peak
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
		}


		// Done
		rawData.finalizeScanBrowser();

		return peakList;

	}



	/**
	 * This functions calculates the score for goodness of match between uc-peak and 1d-peak
	 */
	private MatchScore calcScore(PeakConstruction ucPeak, OneDimPeak oneDimPeak, double maxScore, CentroidPickerParameters parameters) {

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
	 * this function crops the shape of the final peak in RT direction
	 * cropping removes continuous regions that stay within intensity
	 * tolerance from beginning and end of the peak
	 *
	 * @return	array of two indices, left and right one.
	 */
	private int[] cropRTShape(double[] intensities, CentroidPickerParameters parameters) {
/*
		int leftInd=0;
		double bottomMargin = intensities[leftInd] * (1-parameters.intTolerance);
		double topMargin = intensities[leftInd] * (1+parameters.intTolerance);

		while (	((leftInd+1)<intensities.length) &&
				(intensities[leftInd+1]>=bottomMargin) &&
				(intensities[leftInd+1]<=topMargin) ) {
			leftInd++;
			bottomMargin = intensities[leftInd] * (1-parameters.intTolerance);
			topMargin = intensities[leftInd] * (1+parameters.intTolerance);
		}

		int rightInd = intensities.length - 1;
		bottomMargin = intensities[rightInd] * (1-parameters.intTolerance);
		topMargin = intensities[rightInd] * (1+parameters.intTolerance);

		while (	((rightInd-1)>=0) &&
				(intensities[rightInd-1]>=bottomMargin) &&
				(intensities[rightInd-1]<=topMargin) ) {
			rightInd--;
			bottomMargin = intensities[rightInd] * (1-parameters.intTolerance);
			topMargin = intensities[rightInd] * (1+parameters.intTolerance);
		}

		int res[] = new int[2];
		res[0] = leftInd;
		res[1] = rightInd;
*/
		// DEBUG: Don't do cropping
		int res[] = new int[2];
		res[0] = 0;
		res[1] = intensities.length-1;

		return res;

	}

	/**
	 * This function check for the shape of the peak in RT direction, and
	 * determines if it is possible to add given m/z peak at the end of the peak.
	 *
	 */
	private double calcScoreForRTShape(double prevInts[], int usedSize, double nextInt, double intTolerance, double maxScore) {


		// If no previous m/z peaks
		if (usedSize==0) {
			return 0;
		}

		// If only one previous m/z peak, then the second one must go up OR stay within marginal
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

}
