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

package net.sf.mzmine.methods.deisotoping.simplegrouper;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Hashtable;

import net.sf.mzmine.data.IsotopePattern;
import net.sf.mzmine.data.ParameterSet;
import net.sf.mzmine.data.Peak;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.impl.SimpleIsotopePattern;
import net.sf.mzmine.data.impl.SimplePeak;
import net.sf.mzmine.data.impl.SimplePeakList;
import net.sf.mzmine.io.OpenedRawDataFile;
import net.sf.mzmine.taskcontrol.Task;

/**
 *
 */
class SimpleGrouperTask implements Task {

    private static final double neutronMW = 1.008665;

    private OpenedRawDataFile dataFile;
    
    private TaskStatus status;
    private String errorMessage;

    private int processedPeaks;
    private int totalPeaks;

    private PeakList currentPeakList;
    private SimplePeakList processedPeakList;
    
    private ParameterSet parameters;
    private double mzTolerance;
    private double rtTolerance;
    private boolean monotonicShape;
    private int maximumCharge;

    /**
     * @param rawDataFile
     * @param parameters
     */
    SimpleGrouperTask(OpenedRawDataFile dataFile,
            PeakList currentPeakList,
            ParameterSet parameters) {
        status = TaskStatus.WAITING;
        this.dataFile = dataFile;
        
        this.currentPeakList = currentPeakList;

        processedPeakList = new SimplePeakList();
        
        this.parameters = parameters;
        mzTolerance = (Double) parameters.getParameterValue(SimpleGrouper.mzTolerance);
        rtTolerance = (Double) parameters.getParameterValue(SimpleGrouper.rtTolerance);
        monotonicShape = (Boolean) parameters.getParameterValue(SimpleGrouper.monotonicShape);
        maximumCharge = (Integer) parameters.getParameterValue(SimpleGrouper.maximumCharge);

    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
     */
    public String getTaskDescription() {
        return "Simple isotopic peaks grouper on " + dataFile;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
     */
    public float getFinishedPercentage() {
        if (totalPeaks == 0)
            return 0.0f;
        return (float) processedPeaks / (float) totalPeaks;
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
        results[0] = dataFile;
        results[1] = processedPeakList;
        results[2] = parameters;
        return results;
    }
    
    public OpenedRawDataFile getDataFile() {
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

        // Collect all selected charge states
        int charges[] = new int[maximumCharge];
        for (int i=0; i<maximumCharge; i++) charges[i] = (i+1);

        Peak[] sortedPeaks = currentPeakList.getPeaks();
        Arrays.sort(sortedPeaks, new PeakSorterByDescendingHeight());

        // Loop through all peaks in the order of descending intensity
        totalPeaks = sortedPeaks.length;

        for (int ind=0; ind<sortedPeaks.length; ind++) {

			Peak aPeak = sortedPeaks[ind];

            if (status == TaskStatus.CANCELED) return;

            if (aPeak == null) continue;

            // Check which charge state fits best around this peak
            int bestFitCharge = 0;
            int bestFitScore = -1;
            Hashtable<Peak, Integer> bestFitPeaks = null;
            for (int charge : charges) {

                Hashtable<Peak, Integer> fittedPeaks = new Hashtable<Peak, Integer>();
                fittedPeaks.put(aPeak, new Integer(ind));
                fitPattern(fittedPeaks, aPeak, charge, sortedPeaks);

                int score = fittedPeaks.size();
                if ((score > bestFitScore)
                        || ((score == bestFitScore) && (bestFitCharge > charge))) {
                    bestFitScore = score;
                    bestFitCharge = charge;
                    bestFitPeaks = fittedPeaks;
                }

            }

            // Assign peaks in best fitted pattern to same isotope pattern
            SimpleIsotopePattern isotopePattern = new SimpleIsotopePattern(bestFitCharge);

			for (Peak p : bestFitPeaks.keySet()) {
				SimplePeak processedPeak = new SimplePeak(p);
				processedPeak.addData(IsotopePattern.class, isotopePattern);
				processedPeakList.addPeak(processedPeak);
			}
			for (Integer ind2 : bestFitPeaks.values()) { sortedPeaks[ind2] = null;}

            // Update completion rate
            processedPeaks++;

        }

        status = TaskStatus.FINISHED;

    }

    /**
     * Fits isotope pattern around one peak.
     *
     * @param p Pattern is fitted around this peak
     * @param charge Charge state of the fitted pattern
     * @param parameters User-defined parameters
     * @param allPeaks Array containing all peaks
     * @param assignPeaks If true, all fitted peaks are assigned to same isotope
     *            pattern and numbered according to their position within the
     *            pattern.
     * @return Array of peaks in same pattern
     */
    private void fitPattern(Hashtable<Peak, Integer> fittedPeaks, Peak p, int charge, Peak[] sortedPeaks) {

        if (charge == 0) {
            return;
        }

        // Search for peaks before the start peak
        if (!monotonicShape) {
            fitHalfPattern(p, charge, -1, fittedPeaks, sortedPeaks);
        }

        // Search for peaks after the start peak
        fitHalfPattern(p, charge, 1, fittedPeaks, sortedPeaks);

    }

    /**
     * Helper method for fitPattern. Fits only one half of the pattern.
     *
     * @param p Pattern is fitted around this peak
     * @param charge Charge state of the fitted pattern
     * @param direction Defines which half to fit: -1=fit to peaks before start
     *            M/Z, +1=fit to peaks after start M/Z
     * @param allPeaks Vector of all peaks
     * @param fittedPeaks All matching peaks will be added to this set
     */
    private void fitHalfPattern(Peak p, int charge, int direction,
            Hashtable<Peak, Integer> fittedPeaks,
            Peak[] sortedPeaks) {

        // Use M/Z and RT of the strongest peak of the pattern (peak 'p')
        double currentMZ = p.getNormalizedMZ();
        double currentRT = p.getNormalizedRT();

        // Also, use height of the strongest peak as initial height limit
        double currentHeight = p.getNormalizedHeight();

        // Variable n is the number of peak we are currently searching. 1=first
        // peak before/after start peak, 2=peak before/after previous, 3=...
        boolean followingPeakFound = true;
        int n = 1;
        while (followingPeakFound) {

            // Assume we don't find match for n:th peak in the pattern (which
            // will end the loop)
            followingPeakFound = false;

            // Loop through all peaks, and collect candidates for the n:th peak
            // in the pattern
            Hashtable<Peak, Integer> goodCandidates = new Hashtable<Peak, Integer>();
			for (int ind=0; ind<sortedPeaks.length; ind++) {

				Peak candidatePeak = sortedPeaks[ind];

                if (candidatePeak==null) continue;

                // Get properties of the candidate peak
                double candidatePeakMZ = candidatePeak.getNormalizedMZ();
                double candidatePeakRT = candidatePeak.getNormalizedRT();
                double candidatePeakIntensity = candidatePeak.getNormalizedHeight();

                // Does this peak fill all requirements of a candidate?
                // - intensity less than intensity of previous peak in the
                // pattern
                // - within tolerances from the expected location (M/Z and RT)
                // - not already a fitted peak (only necessary to avoid
                // conflicts when parameters are set too wide)
                
                if (java.lang.Math.abs((candidatePeakMZ - direction * n * neutronMW / (double) charge) - currentMZ) < mzTolerance) {
                	if (java.lang.Math.abs(candidatePeakRT - currentRT) < rtTolerance) {
                		if (candidatePeakIntensity < currentHeight) {
                			if (!fittedPeaks.contains(candidatePeak)) {
                				goodCandidates.put(candidatePeak, new Integer(ind));
                			}
                		}
                	}
                }
              
            }

            // If there are some candidates for n:th peak, then select the one
            // with biggest intensity
            // We collect all candidates, because we might want to do something
            // more sophisticated at
            // this step. For example, we might want to remove all other
            // candidates. However, currently
            // nothing is done with other candidates.
            Peak bestCandidate = null;
            for (Peak candidatePeak : goodCandidates.keySet()) {
                if (bestCandidate != null) {
                    if (bestCandidate.getNormalizedHeight() < candidatePeak.getNormalizedHeight()) {
                        bestCandidate = candidatePeak;
                    }
                } else {
                    bestCandidate = candidatePeak;
                }

            }

            // If best candidate was found, then assign it to this isotope
            // pattern
            if (bestCandidate != null) {

                // Add best candidate to fitted peaks of the pattern
                fittedPeaks.put(bestCandidate, goodCandidates.get(bestCandidate));

                // Update height limit
                currentHeight = bestCandidate.getNormalizedHeight();

                // n:th peak was found, so let's move on to n+1
                n++;
                followingPeakFound = true;

            }

        }

    }



}
