/*
 * Copyright 2006-2020 The MZmine Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.dataprocessing.filter_deisotoper;

import java.util.Arrays;
import java.util.Vector;
import java.util.logging.Logger;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.Feature;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.PeakList;
import io.github.mzmine.datamodel.PeakListRow;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.IsotopePattern.IsotopePatternStatus;
import io.github.mzmine.datamodel.PeakList.PeakListAppliedMethod;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.datamodel.impl.SimpleFeature;
import io.github.mzmine.datamodel.impl.SimpleIsotopePattern;
import io.github.mzmine.datamodel.impl.SimplePeakList;
import io.github.mzmine.datamodel.impl.SimplePeakListAppliedMethod;
import io.github.mzmine.datamodel.impl.SimplePeakListRow;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.PeakSorter;
import io.github.mzmine.util.PeakUtils;
import io.github.mzmine.util.SortingDirection;
import io.github.mzmine.util.SortingProperty;

/**
 * 
 */
class IsotopeGrouperTask extends AbstractTask {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    /**
     * The isotopeDistance constant defines expected distance between isotopes.
     * Actual weight of 1 neutron is 1.008665 Da, but part of this mass is
     * consumed as binding energy to other protons/neutrons. Actual mass
     * increase of isotopes depends on chemical formula of the molecule. Since
     * we don't know the formula, we can assume the distance to be ~1.0033 Da,
     * with user-defined tolerance.
     */
    private static final double isotopeDistance = 1.0033;

    private final MZmineProject project;
    private PeakList peakList, deisotopedPeakList;

    // peaks counter
    private int processedPeaks, totalPeaks;

    // parameter values
    private String suffix;
    private MZTolerance mzTolerance;
    private RTTolerance rtTolerance;
    private boolean monotonicShape, removeOriginal, chooseMostIntense;
    private int maximumCharge;
    private ParameterSet parameters;

    /**
     * @param rawDataFile
     * @param parameters
     */
    IsotopeGrouperTask(MZmineProject project, PeakList peakList,
            ParameterSet parameters) {

        this.project = project;
        this.peakList = peakList;
        this.parameters = parameters;

        // Get parameter values for easier use
        suffix = parameters.getParameter(IsotopeGrouperParameters.suffix)
                .getValue();
        mzTolerance = parameters
                .getParameter(IsotopeGrouperParameters.mzTolerance).getValue();
        rtTolerance = parameters
                .getParameter(IsotopeGrouperParameters.rtTolerance).getValue();
        monotonicShape = parameters
                .getParameter(IsotopeGrouperParameters.monotonicShape)
                .getValue();
        maximumCharge = parameters
                .getParameter(IsotopeGrouperParameters.maximumCharge)
                .getValue();
        chooseMostIntense = (parameters
                .getParameter(IsotopeGrouperParameters.representativeIsotope)
                .getValue() == IsotopeGrouperParameters.ChooseTopIntensity);
        removeOriginal = parameters
                .getParameter(IsotopeGrouperParameters.autoRemove).getValue();

    }

    /**
     * @see io.github.mzmine.taskcontrol.Task#getTaskDescription()
     */
    public String getTaskDescription() {
        return "Isotopic peaks grouper on " + peakList;
    }

    /**
     * @see io.github.mzmine.taskcontrol.Task#getFinishedPercentage()
     */
    public double getFinishedPercentage() {
        if (totalPeaks == 0)
            return 0.0f;
        return (double) processedPeaks / (double) totalPeaks;
    }

    /**
     * @see Runnable#run()
     */
    public void run() {

        setStatus(TaskStatus.PROCESSING);
        logger.info("Running isotopic peak grouper on " + peakList);

        // We assume source peakList contains one datafile
        RawDataFile dataFile = peakList.getRawDataFile(0);

        // Create a new deisotoped peakList
        deisotopedPeakList = new SimplePeakList(peakList + " " + suffix,
                peakList.getRawDataFiles());

        // Collect all selected charge states
        int charges[] = new int[maximumCharge];
        for (int i = 0; i < maximumCharge; i++)
            charges[i] = i + 1;

        // Sort peaks by descending height
        Feature[] sortedPeaks = peakList.getPeaks(dataFile);
        Arrays.sort(sortedPeaks, new PeakSorter(SortingProperty.Height,
                SortingDirection.Descending));

        // Loop through all peaks
        totalPeaks = sortedPeaks.length;

        for (int ind = 0; ind < totalPeaks; ind++) {

            if (isCanceled())
                return;

            Feature aPeak = sortedPeaks[ind];

            // Check if peak was already deleted
            if (aPeak == null) {
                processedPeaks++;
                continue;
            }

            // Check which charge state fits best around this peak
            int bestFitCharge = 0;
            int bestFitScore = -1;
            Vector<Feature> bestFitPeaks = null;
            for (int charge : charges) {

                Vector<Feature> fittedPeaks = new Vector<Feature>();
                fittedPeaks.add(aPeak);
                fitPattern(fittedPeaks, aPeak, charge, sortedPeaks);

                int score = fittedPeaks.size();
                if ((score > bestFitScore) || ((score == bestFitScore)
                        && (bestFitCharge > charge))) {
                    bestFitScore = score;
                    bestFitCharge = charge;
                    bestFitPeaks = fittedPeaks;
                }

            }

            PeakListRow oldRow = peakList.getPeakRow(aPeak);

            assert bestFitPeaks != null;

            // Verify the number of detected isotopes. If there is only one
            // isotope, we skip this left the original peak in the feature list.
            if (bestFitPeaks.size() == 1) {
                deisotopedPeakList.addRow(oldRow);
                processedPeaks++;
                continue;
            }

            // Convert the peak pattern to array
            Feature originalPeaks[] = bestFitPeaks.toArray(new Feature[0]);

            // Create a new SimpleIsotopePattern
            DataPoint isotopes[] = new DataPoint[bestFitPeaks.size()];
            for (int i = 0; i < isotopes.length; i++) {
                Feature p = originalPeaks[i];
                isotopes[i] = new SimpleDataPoint(p.getMZ(), p.getHeight());

            }
            SimpleIsotopePattern newPattern = new SimpleIsotopePattern(isotopes,
                    IsotopePatternStatus.DETECTED, aPeak.toString());

            // Depending on user's choice, we leave either the most intenst, or
            // the lowest m/z peak
            if (chooseMostIntense) {
                Arrays.sort(originalPeaks, new PeakSorter(
                        SortingProperty.Height, SortingDirection.Descending));
            } else {
                Arrays.sort(originalPeaks, new PeakSorter(SortingProperty.MZ,
                        SortingDirection.Ascending));
            }

            Feature newPeak = new SimpleFeature(originalPeaks[0]);
            newPeak.setIsotopePattern(newPattern);
            newPeak.setCharge(bestFitCharge);

            // Keep old ID
            int oldID = oldRow.getID();
            SimplePeakListRow newRow = new SimplePeakListRow(oldID);
            PeakUtils.copyPeakListRowProperties(oldRow, newRow);
            newRow.addPeak(dataFile, newPeak);
            deisotopedPeakList.addRow(newRow);

            // Remove all peaks already assigned to isotope pattern
            for (int i = 0; i < sortedPeaks.length; i++) {
                if (bestFitPeaks.contains(sortedPeaks[i]))
                    sortedPeaks[i] = null;
            }

            // Update completion rate
            processedPeaks++;

        }

        // Add new peakList to the project
        project.addPeakList(deisotopedPeakList);

        // Load previous applied methods
        for (PeakListAppliedMethod proc : peakList.getAppliedMethods()) {
            deisotopedPeakList.addDescriptionOfAppliedTask(proc);
        }

        // Add task description to peakList
        deisotopedPeakList.addDescriptionOfAppliedTask(
                new SimplePeakListAppliedMethod("Isotopic peaks grouper",
                        parameters));

        // Remove the original peakList if requested
        if (removeOriginal)
            project.removePeakList(peakList);

        logger.info("Finished isotopic peak grouper on " + peakList);
        setStatus(TaskStatus.FINISHED);

    }

    /**
     * Fits isotope pattern around one peak.
     * 
     * @param p
     *            Pattern is fitted around this peak
     * @param charge
     *            Charge state of the fitted pattern
     */
    private void fitPattern(Vector<Feature> fittedPeaks, Feature p, int charge,
            Feature[] sortedPeaks) {

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
     * @param p
     *            Pattern is fitted around this peak
     * @param charge
     *            Charge state of the fitted pattern
     * @param direction
     *            Defines which half to fit: -1=fit to peaks before start M/Z,
     *            +1=fit to peaks after start M/Z
     * @param fittedPeaks
     *            All matching peaks will be added to this set
     */
    private void fitHalfPattern(Feature p, int charge, int direction,
            Vector<Feature> fittedPeaks, Feature[] sortedPeaks) {

        // Use M/Z and RT of the strongest peak of the pattern (peak 'p')
        double mainMZ = p.getMZ();
        double mainRT = p.getRT();

        // Variable n is the number of peak we are currently searching. 1=first
        // peak before/after start peak, 2=peak before/after previous, 3=...
        boolean followingPeakFound;
        int n = 1;
        do {

            // Assume we don't find match for n:th peak in the pattern (which
            // will end the loop)
            followingPeakFound = false;

            // Loop through all peaks, and collect candidates for the n:th peak
            // in the pattern
            Vector<Feature> goodCandidates = new Vector<Feature>();
            for (int ind = 0; ind < sortedPeaks.length; ind++) {

                Feature candidatePeak = sortedPeaks[ind];

                if (candidatePeak == null)
                    continue;

                // Get properties of the candidate peak
                double candidatePeakMZ = candidatePeak.getMZ();
                double candidatePeakRT = candidatePeak.getRT();

                // Does this peak fill all requirements of a candidate?
                // - within tolerances from the expected location (M/Z and RT)
                // - not already a fitted peak (only necessary to avoid
                // conflicts when parameters are set too wide)
                double isotopeMZ = candidatePeakMZ
                        - isotopeDistance * direction * n / (double) charge;

                if (mzTolerance.checkWithinTolerance(isotopeMZ, mainMZ)
                        && rtTolerance.checkWithinTolerance(candidatePeakRT,
                                mainRT)
                        && (!fittedPeaks.contains(candidatePeak))) {
                    goodCandidates.add(candidatePeak);

                }

            }

            // Add all good candidates to the isotope pattern (note: in MZmine
            // 2.3 and older, only the highest candidate was added)
            if (!goodCandidates.isEmpty()) {

                fittedPeaks.addAll(goodCandidates);

                // n:th peak was found, so let's move on to n+1
                n++;
                followingPeakFound = true;
            }

        } while (followingPeakFound);

    }

}
