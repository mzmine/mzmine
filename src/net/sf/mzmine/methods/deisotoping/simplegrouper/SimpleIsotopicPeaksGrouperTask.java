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

package net.sf.mzmine.methods.deisotoping.simplegrouper;

import java.io.IOException;
import java.util.Vector;
import java.util.TreeSet;
import java.util.Iterator;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;


import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.interfaces.Scan;
import net.sf.mzmine.interfaces.Peak;
import net.sf.mzmine.interfaces.PeakList;
import net.sf.mzmine.interfaces.IsotopePattern;
import net.sf.mzmine.methods.deisotoping.GrouperIsotopePattern;
import net.sf.mzmine.methods.deisotoping.GrouperPeak;
import net.sf.mzmine.methods.peakpicking.SimplePeakList;
import net.sf.mzmine.methods.peakpicking.SimplePeak;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.util.MyMath;


/**
 *
 */
public class SimpleIsotopicPeaksGrouperTask implements Task {

	private static final double neutronMW = 1.008665;

    private RawDataFile rawDataFile;
    private SimpleIsotopicPeaksGrouperParameters parameters;
    private TaskStatus status;
    private String errorMessage;

    private int processedPeaks;
    private int totalPeaks;

	private PeakList currentPeakList;
	private SimplePeakList processedPeakList;

    /**
     * @param rawDataFile
     * @param parameters
     */
    SimpleIsotopicPeaksGrouperTask(RawDataFile rawDataFile, PeakList currentPeakList, SimpleIsotopicPeaksGrouperParameters parameters) {
        status = TaskStatus.WAITING;
        this.rawDataFile = rawDataFile;
        this.parameters = parameters;
		this.currentPeakList = currentPeakList;

        processedPeakList = new SimplePeakList();

    }


    /**
     * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
     */
    public String getTaskDescription() {
        return "Simple isotopic peaks grouper on " + rawDataFile;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
     */
    public float getFinishedPercentage() {
		if (totalPeaks == 0) return 0.0f;
        return (float) processedPeaks / (float)totalPeaks;
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
		results[1] = processedPeakList;
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

		Peak[] allPeaks = currentPeakList.getPeaks();
		for (Peak p : allPeaks) {
			peakTree.add(p);
		}

		HashSet<Peak> alreadyAssignedPeaks = new HashSet<Peak>();

		// Loop through all peaks in the order of descending intensity
		Iterator<Peak> peakIterator = peakTree.iterator();
		while (peakIterator.hasNext()) {

			if (status == TaskStatus.CANCELED) return;

			// Get next peak
			Peak aPeak = peakIterator.next();

			// If this peak is already assigned to some isotope pattern, then skip it
			if (alreadyAssignedPeaks.contains(aPeak)) { continue; }

			// Check which charge state fits best around this peak
			int bestFitCharge=0;
			int bestFitScore=-1;
			Peak[] bestFitPeaks = null;
			for (int charge : charges) {

				Peak[] fittedPeaks = fitPattern(aPeak, charge, parameters, allPeaks, alreadyAssignedPeaks);
				int score = fittedPeaks.length;
				if ( (score>bestFitScore) || ( (score==bestFitScore) && (bestFitCharge>charge) ) ) {
					bestFitScore = score;
					bestFitCharge = charge;
					bestFitPeaks = fittedPeaks;
				}

			}

			// Assign peaks in best fitted pattern to same isotope pattern
			GrouperIsotopePattern isotopePattern = new GrouperIsotopePattern(bestFitCharge);
			for (Peak p : bestFitPeaks) {
				alreadyAssignedPeaks.add(p);
				GrouperPeak processedPeak = new GrouperPeak(p);
				processedPeak.setIsotopePattern(isotopePattern);
				processedPeakList.addPeak(processedPeak);
			}


			// Update completion rate
			processedPeaks++;

		}

		status = TaskStatus.FINISHED;

    }


	/**
	 * Fits isotope pattern around one peak.
	 *
	 * @param	p			Pattern is fitted around this peak
	 * @param	charge		Charge state of the fitted pattern
	 * @param	parameters	User-defined parameters
	 * @param	allPeaks	Array containing all peaks
	 * @param	assignPeaks	If true, all fitted peaks are assigned to same isotope pattern and numbered according to their position within the pattern.
	 * @return	Array of peaks in same pattern
	 */
	private Peak[] fitPattern(Peak p, int charge, SimpleIsotopicPeaksGrouperParameters parameters, Peak[] allPeaks, Set<Peak> alreadyAssignedPeaks) {

		if (charge==0) { return null; }

		// All peaks of the fitted pattern will be collected to this set
		Vector<Peak> fittedPeaks = new Vector<Peak>();

		// Naturally 'p' will be always fitted to the pattern
		fittedPeaks.add(p);

		// Search for peaks before the start peak
		if (!parameters.monotonicShape) { fitHalfPattern(p, charge, -1, parameters, allPeaks, fittedPeaks, alreadyAssignedPeaks); }

		// Search for peaks after the start peak
		//fitHalfPattern(p, charge, 1, parameters, allPeaks, fittedPeaks, isotopePatternID);

		return fittedPeaks.toArray(new Peak[0]);

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
	private void fitHalfPattern(Peak p, int charge, int direction, SimpleIsotopicPeaksGrouperParameters parameters, Peak[] allPeaks, Vector<Peak> fittedPeaks, Set<Peak> alreadyAssignedPeaks) {


		// Use M/Z and RT of the strongest peak of the pattern (peak 'p')
		double currentMZ = p.getNormalizedMZ();
		double currentRT = p.getNormalizedRT();

		// Also, use height of the strongest peak as initial height limit
		double currentHeight = p.getNormalizedHeight();



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
				if (alreadyAssignedPeaks.contains(candidatePeak)) { continue; }

				// Get properties of the candidate peak
				double candidatePeakMZ = candidatePeak.getNormalizedMZ();
				double candidatePeakRT = candidatePeak.getNormalizedRT();
				double candidatePeakIntensity = candidatePeak.getNormalizedHeight();

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
					if (bestCandidate.getNormalizedHeight()<candidatePeak.getNormalizedHeight()) {
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
				currentHeight = bestCandidate.getNormalizedHeight();

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
			if (p1.getNormalizedHeight()<=p2.getNormalizedHeight()) { return 1; } else { return -1; }
		}

		public boolean equals(Object obj) { return false; }
	}


}
