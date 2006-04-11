/*
    Copyright 2005-2006 VTT Biotechnology

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


import java.util.Comparator;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

import net.sf.mzmine.datastructures.Peak;
import net.sf.mzmine.datastructures.PeakList;
import net.sf.mzmine.datastructures.RawDataAtNode;
import net.sf.mzmine.obsoletedistributionframework.NodeServer;
import net.sf.mzmine.userinterface.MainWindow;


/**
 * This class implements a simple deisotoping method based on searhing for neighbouring peaks from expected locations.
 *
 * @version 31 March 2006
 */
public class SimpleDeisotoper implements PeakListProcessor {

	// Labels for parameters
	private final String[] fieldNames = {	"Tolerance for m/z variation",
											"Tolerance in retention time variation" };

	private static final double neutronMW = 1.008665;

	/**
	 * Method asks parameter values from user
	 */
	public SimpleDeisotoperParameters askParameters(MainWindow mainWin, SimpleDeisotoperParameters currentValues) {

		// Initialize parameters
		SimpleDeisotoperParameters myParameters;
		if (currentValues==null) {
			myParameters = new SimpleDeisotoperParameters();
		} else {
			myParameters = currentValues;
		}

		SimpleDeisotoperParameterSetupDialog sdpsd = new SimpleDeisotoperParameterSetupDialog(myParameters);

		sdpsd.showModal(mainWin.getDesktop());


		if (sdpsd.getExitCode()==-1) { return null; }

		myParameters = sdpsd.getParameters();

		return myParameters;

	}



	/**
	 * This method does the processing
	 */
	public PeakList processPeakList(NodeServer nodeServer, RawDataAtNode theData, PeakList peakList, PeakListProcessorParameters _parameters) {

		// Take the parameters
		SimpleDeisotoperParameters parameters = (SimpleDeisotoperParameters)_parameters;


		// Collect all selected charge states
		int charges[] = new int[parameters.chargeStates.size()];
		Iterator<Integer> chargeIter = parameters.chargeStates.iterator();
		int index = 0;
		while (chargeIter.hasNext()) {
			charges[index] = chargeIter.next().intValue();
			index++;
		}

		// Assign all peaks to a TreeSet for sorting them in order of descending intensity
		// At the same time, clear all previous isotope pattern IDs
		TreeSet<Peak> peakTree = new TreeSet<Peak>(new PeakOrdererByDescendingHeight());

		Vector<Peak> allPeaks = peakList.getPeaks();
		for (Peak p : allPeaks) {
			peakTree.add(p);
			p.setIsotopePatternID(-1);
		}

		// Loop through all peaks in the order of descending intensity
		Iterator<Peak> peakIterator = peakTree.iterator();
		int isotopePatternID = 0;
		int numberOfPeaks = peakTree.size();
		int currentPeak = 0;
		while (peakIterator.hasNext()) {

			// Update wait dialog
			nodeServer.updateJobCompletionRate((double)currentPeak / (double)numberOfPeaks);
			currentPeak++;

			// Get next peak
			Peak aPeak = peakIterator.next();

			// If this peak is already assigned to some isotope pattern, then skip it
			if (aPeak.getIsotopePatternID()>=0) { continue; }

			// Check which charge state fits best around this peak
			int bestFitCharge=0;
			int bestFitScore=-1;
			for (int charge : charges) {

				int score = fitPattern(aPeak, charge, parameters, allPeaks, false, 0);
				if ( (score>bestFitScore) || ( (score==bestFitScore) && (bestFitCharge>charge) ) ) {
					bestFitScore = score;
					bestFitCharge = charge;
				}

			}

			// Do best fitting again, and assign peaks to the same isotope pattern
			fitPattern(aPeak, bestFitCharge, parameters, allPeaks, true, isotopePatternID);

			// Move to new isotope pattern ID
			isotopePatternID++;

		}

		// Finally, start a new peak list and add all peaks to it
		PeakList modifiedPeakList = new PeakList();
		for (Peak aPeak : allPeaks) {
			modifiedPeakList.addPeakKeepOldID(aPeak);
		}


		return modifiedPeakList;

	}


	/**
	 * Fits isotope pattern around one peak and calculates how many peaks match.
	 *
	 * @param	p			Pattern is fitted around this peak
	 * @param	charge		Charge state of the fitted pattern
	 * @param	parameters	User-defined parameters
	 * @param	allPeaks	Vector containing all peaks
	 * @param	assignPeaks	If true, all fitted peaks are assigned to same isotope pattern and numbered according to their position within the pattern.
	 * @return	Number of peaks that match to the fitted pattern
	 */
	private int fitPattern(Peak p, int charge, SimpleDeisotoperParameters parameters, Vector<Peak> allPeaks, boolean assignPeaks, int isotopePatternID) {

		if (charge==0) { return 0; }

		// All peaks of the fitted pattern will be collected to this set
		TreeSet<Peak> fittedPeaks = new TreeSet<Peak>(new PeakOrdererByAscendingMZ());

		// Naturally 'p' will be always fitted to the pattern
		fittedPeaks.add(p);

		// Search for peaks before the start peak
		if (!parameters.monotonicShape) { fitHalfPattern(p, charge, -1, parameters, allPeaks, fittedPeaks, isotopePatternID); }

		// Search for peaks after the start peak
		fitHalfPattern(p, charge, 1, parameters, allPeaks, fittedPeaks, isotopePatternID);


		// Assign fitted peaks to same isotope pattern, if requested.
		if (assignPeaks) {

			Iterator<Peak> fittedPeaksIter = fittedPeaks.iterator();
			int isotopePeakNumber = 0;


			while (fittedPeaksIter.hasNext()) {
				Peak tmpPeak = fittedPeaksIter.next();

				tmpPeak.setIsotopePatternID(isotopePatternID);
				tmpPeak.setIsotopePeakNumber(isotopePeakNumber);
				tmpPeak.setChargeState(charge);
				isotopePeakNumber++;
			}

		}


		return fittedPeaks.size();

	}


	/**
	 * Helper method for fitPattern.
	 * Fits only one half of the pattern.
	 *
	 * @param	p				Pattern is fitted around this peak
	 * @param	charge			Charge state of the fitted pattern
	 * @param	direction		Defines which half to fit: -1=fit to peaks before start M/Z, +1=fit to peaks after start M/Z
	 * @param	allPeaks		Vector of all peaks
	 * @param	fittedPeaks		All matching peaks will be added to this set
	 */
	private void fitHalfPattern(Peak p, int charge, int direction, SimpleDeisotoperParameters parameters, Vector<Peak> allPeaks, Set<Peak> fittedPeaks, Integer isotopePatternID) {

		// Calculate average M/Z of peaks fitted so far
		/*
		Iterator<Peak> fittedPeaksIter = fittedPeaks.iterator();
		double tmpSum = 0.0;
		while ( fittedPeaksIter.hasNext() ) {
			Peak tmpPeak = fittedPeaksIter.next();
			tmpSum += tmpPeak.getMZ();
		}
		*/

		// Use M/Z and RT of the strongest peak of the pattern (peak 'p')
		double currentMZ = p.getMZ();
		double currentRT = p.getRT();

		// Also, use height of the strongest peak as initial height limit
		double currentHeight = p.getHeight();



		// Variable n is the number of peak we are currently searching. 1=first peak before/after start peak, 2=peak before/after previous, 3=...
		boolean followingPeakFound = true;
		int n=1;
		while (followingPeakFound) {

			// Assume we don't find match for n:th peak in the pattern (which will end the loop)
			followingPeakFound = false;

			// Loop through all peaks, and collect candidates for the n:th peak in the pattern
			Vector<Peak> goodCandidates = new Vector<Peak>();
			for (Peak candidatePeak : allPeaks) {

				// If this peak is already assigned to some isotope pattern, the skip it
				if (candidatePeak.getIsotopePatternID()>=0) { continue; }

				// Get properties of the candidate peak
				double candidatePeakMZ = candidatePeak.getMZ();
				double candidatePeakRT = candidatePeak.getRT();
				double candidatePeakIntensity = candidatePeak.getHeight();

				// Does this peak fill all requirements of a candidate?
				// - intensity less than intensity of previous peak in the pattern
				// - within tolerances from the expected location (M/Z and RT)
				// - not already a fitted peak (only necessary to avoid conflicts when parameters are set too wide)
				if ( 	(candidatePeakIntensity<currentHeight) &&
						(java.lang.Math.abs( candidatePeakRT - currentRT)<parameters.rtTolerance ) &&
						(java.lang.Math.abs((candidatePeakMZ-direction*n*neutronMW/(double)charge) - currentMZ) < parameters.mzTolerance) &&
						(!fittedPeaks.contains(candidatePeak)) ) {
							goodCandidates.add(candidatePeak);
				}

			}



			// If there are some candidates for n:th peak, then select the one with biggest intensity
			// We collect all candidates, because we might want to do something more sophisticated at
			// this step. For example, we might want to remove all other candidates. However, currently
			// nothing is done with other candidates.
			Peak bestCandidate = null;
			for (Peak candidatePeak : goodCandidates) {
				if (bestCandidate!=null) {
					if (bestCandidate.getHeight()<candidatePeak.getHeight()) {
						bestCandidate = candidatePeak;
					}
				} else {
					bestCandidate = candidatePeak;
				}

			}



			// If best candidate was found, then assign it to this isotope pattern
			if (bestCandidate != null) {

				// Add best candidate to fitted peaks of the pattern
				fittedPeaks.add(bestCandidate);

				// Update height limit
				currentHeight = bestCandidate.getHeight();

				// n:th peak was found, so let's move on to n+1
				n++;
				followingPeakFound = true;

			}

		}

	}


	/**
	 * This is a helper class required for TreeSet to sorting peaks in order of decreasing intensity.
	 */
	private class PeakOrdererByDescendingHeight implements Comparator<Peak> {
		public int compare(Peak p1, Peak p2) {
			if (p1.getHeight()<=p2.getHeight()) { return 1; } else { return -1; }
		}

		public boolean equals(Object obj) { return false; }
	}

	/**
	 * This is a helper class required for TreeSet to sorting peaks in order of ascending M/Z.
	 */
	private class PeakOrdererByAscendingMZ implements Comparator<Peak> {
		public int compare(Peak p1, Peak p2) {
			if (p1.getMZ()<p2.getMZ()) { return -1; }

			if (p1.getMZ()==p2.getMZ()) { return 0; }

			return 1;
		}

		public boolean equals(Object obj) { return false; }
	}



}

