/*
 * Copyright 2006-2015 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.peaklistmethods.basiclearner;

import java.util.Arrays;
import java.util.Vector;
import java.util.logging.Logger;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.IsotopePattern.IsotopePatternStatus;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakList.PeakListAppliedMethod;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.impl.SimpleDataPoint;
import net.sf.mzmine.datamodel.impl.SimpleFeature;
import net.sf.mzmine.datamodel.impl.SimpleIsotopePattern;
import net.sf.mzmine.datamodel.impl.SimplePeakList;
import net.sf.mzmine.datamodel.impl.SimplePeakListAppliedMethod;
import net.sf.mzmine.datamodel.impl.SimplePeakListRow;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import net.sf.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.PeakSorter;
import net.sf.mzmine.util.PeakUtils;
import net.sf.mzmine.util.SortingDirection;
import net.sf.mzmine.util.SortingProperty;

class LearnerTask extends AbstractTask {

  private Logger logger = Logger.getLogger(this.getClass().getName());

  private final MZmineProject project;
  private PeakList peakList;
  private PeakList resultPeakList;

  // peaks counter
  private int processedPeaks;
  private int totalPeaks;

  // parameter values
  private String suffix;
  private MZTolerance mzTolerance;
  private RTTolerance rtTolerance;
  private boolean removeOriginal;
  private int maximumCharge;
  private ParameterSet parameters;

  /**
   * Constructor to set all parameters and the project
   * 
   * @param rawDataFile
   * @param parameters
   */
  public LearnerTask(MZmineProject project, PeakList peakList, ParameterSet parameters) {
    this.project = project;
    this.peakList = peakList;
    this.parameters = parameters;
    // Get parameter values for easier use
    suffix = parameters.getParameter(LearnerParameters.suffix).getValue();
    mzTolerance = parameters.getParameter(LearnerParameters.mzTolerance).getValue();
    rtTolerance = parameters.getParameter(LearnerParameters.rtTolerance).getValue();
    maximumCharge = parameters.getParameter(LearnerParameters.maximumCharge).getValue();
    removeOriginal = parameters.getParameter(LearnerParameters.autoRemove).getValue();
  }

  /**
   * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
   */
  @Override
  public String getTaskDescription() {
    return "Learner task on " + peakList;
  }

  /**
   * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
   */
  @Override
  public double getFinishedPercentage() {
    if (totalPeaks == 0)
      return 0;
    return (double) processedPeaks / (double) totalPeaks;
  }

  /**
   * @see Runnable#run()
   */
  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);
    logger.info("Running learner task on " + peakList);

    // Create a new results peakList which is added at the end
    resultPeakList = new SimplePeakList(peakList + " " + suffix, peakList.getRawDataFiles());

    /**
     * - A PeakList contains one or multiple RawDataFiles <br>
     * ---- access mean retention time, mean m/z, maximum intensity, ...<br>
     * - A RawDataFile contains multiple scans with full spectral data <br>
     * ---- Each Scan and the underlying raw data can be accessed <br>
     * ---- Scans can be filtered by MS level, polarity, ...<br>
     */

    // number of rawFiles is 1 prior to peaklist alignment
    int rawFiles = peakList.getNumberOfRawDataFiles();
    boolean isAlignedPeakList = rawFiles > 1;

    // is the data provided by peaklist enough for this task or
    // do you want to work on one raw data file or on all files?
    RawDataFile dataFile = peakList.getRawDataFile(0);

    // Sort peaks by ascending mz
    Feature[] sortedPeaks = peakList.getPeaks(dataFile);
    Arrays.sort(sortedPeaks, new PeakSorter(SortingProperty.MZ, SortingDirection.Ascending));

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
        if ((score > bestFitScore) || ((score == bestFitScore) && (bestFitCharge > charge))) {
          bestFitScore = score;
          bestFitCharge = charge;
          bestFitPeaks = fittedPeaks;
        }

      }

      PeakListRow oldRow = peakList.getPeakRow(aPeak);

      assert bestFitPeaks != null;

      // Verify the number of detected isotopes. If there is only one
      // isotope, we skip this left the original peak in the peak list.
      if (bestFitPeaks.size() == 1) {
        resultPeakList.addRow(oldRow);
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
      SimpleIsotopePattern newPattern =
          new SimpleIsotopePattern(isotopes, IsotopePatternStatus.DETECTED, aPeak.toString());

      // Depending on user's choice, we leave either the most intenst, or
      // the lowest m/z peak
      if (chooseMostIntense) {
        Arrays.sort(originalPeaks,
            new PeakSorter(SortingProperty.Height, SortingDirection.Descending));
      } else {
        Arrays.sort(originalPeaks, new PeakSorter(SortingProperty.MZ, SortingDirection.Ascending));
      }

      Feature newPeak = new SimpleFeature(originalPeaks[0]);
      newPeak.setIsotopePattern(newPattern);
      newPeak.setCharge(bestFitCharge);

      // Keep old ID
      int oldID = oldRow.getID();
      SimplePeakListRow newRow = new SimplePeakListRow(oldID);
      PeakUtils.copyPeakListRowProperties(oldRow, newRow);
      newRow.addPeak(dataFile, newPeak);
      resultPeakList.addRow(newRow);

      // Remove all peaks already assigned to isotope pattern
      for (int i = 0; i < sortedPeaks.length; i++) {
        if (bestFitPeaks.contains(sortedPeaks[i]))
          sortedPeaks[i] = null;
      }

      // Update completion rate
      processedPeaks++;

    }

    // Add new peakList to the project
    project.addPeakList(resultPeakList);

    // Load previous applied methods
    for (PeakListAppliedMethod proc : peakList.getAppliedMethods()) {
      resultPeakList.addDescriptionOfAppliedTask(proc);
    }

    // Add task description to peakList
    resultPeakList.addDescriptionOfAppliedTask(
        new SimplePeakListAppliedMethod("Isotopic peaks grouper", parameters));

    // Remove the original peakList if requested
    if (removeOriginal)
      project.removePeakList(peakList);

    logger.info("Finished on " + peakList);
    setStatus(TaskStatus.FINISHED);
  }

}
