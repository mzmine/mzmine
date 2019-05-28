/*
 * Copyright 2006-2018 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.isotopes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import java.util.logging.Logger;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import net.sf.mzmine.util.DataPointSorter;
import net.sf.mzmine.util.SortingDirection;
import net.sf.mzmine.util.SortingProperty;

/**
 * Deisotope mass list This is basically copy-pasted from
 * net.sf.mzmine.modules.peaklistmethods.isotopes.deisotoper.IsotopeGrouperTask
 * 
 */
public class MassListDeisotoper {

  private static Logger logger = Logger.getLogger(MassListDeisotoper.class.getName());

  // parameter values
  private static final double DELTA = 1.003354838;

  public static DataPoint[] filterIsotopes(DataPoint[] dataPoints, ParameterSet parameterSet) {
    if (dataPoints == null || dataPoints.length == 0) {
      return dataPoints;
    }
    MZTolerance mzTolerance =
        parameterSet.getParameter(MassListDeisotoperParameters.mzTolerance).getValue();
    boolean monotonicShape =
        parameterSet.getParameter(MassListDeisotoperParameters.monotonicShape).getValue();
    int maximumCharge =
        parameterSet.getParameter(MassListDeisotoperParameters.maximumCharge).getValue();

    int charges[] = new int[maximumCharge];
    for (int i = 0; i < maximumCharge; i++)
      charges[i] = i + 1;

    // sort by intensity
    dataPoints = dataPoints.clone();
    Arrays.sort(dataPoints,
        new DataPointSorter(SortingProperty.Intensity, SortingDirection.Descending));

    List<DataPoint> deisotopedDataPoints = new ArrayList<>();

    for (int i = 0; i < dataPoints.length; i++) {
      DataPoint aPeak = dataPoints[i];
      if (aPeak == null) {
        continue;
      }

      // Check which charge state fits best around this peak
      int bestFitCharge = 0;
      int bestFitScore = -1;
      List<DataPoint> bestFitPeaks = null;
      for (int charge : charges) {
        List<DataPoint> fittedPeaks = new ArrayList<>();
        fittedPeaks.add(aPeak);
        fitPattern(fittedPeaks, aPeak, charge, dataPoints, DELTA, monotonicShape, mzTolerance);

        int score = fittedPeaks.size();
        if ((score > bestFitScore) || ((score == bestFitScore) && (bestFitCharge > charge))) {
          bestFitScore = score;
          bestFitCharge = charge;
          bestFitPeaks = fittedPeaks;
        }
      }

      assert bestFitPeaks != null;

      // add to deisotoped
      deisotopedDataPoints.add(dataPoints[i]);
      // remove all
      for (int j = 0; j < dataPoints.length; j++) {
        if (bestFitPeaks.contains(dataPoints[j]))
          dataPoints[j] = null;
      }
    }
    return deisotopedDataPoints.toArray(new DataPoint[deisotopedDataPoints.size()]);
  }

  /**
   * Fits isotope pattern around one peak.
   * 
   * @param p Pattern is fitted around this peak
   * @param charge Charge state of the fitted pattern
   * @param mzTolerance
   */
  private static void fitPattern(List<DataPoint> fittedPeaks, DataPoint p, int charge,
      DataPoint[] sortedPeaks, double isotopeDistance, boolean monotonicShape,
      MZTolerance mzTolerance) {

    if (charge == 0) {
      return;
    }

    // Search for peaks before the start peak
    if (!monotonicShape) {
      fitHalfPattern(p, charge, -1, fittedPeaks, sortedPeaks, isotopeDistance, mzTolerance);
    }

    // Search for peaks after the start peak
    fitHalfPattern(p, charge, 1, fittedPeaks, sortedPeaks, isotopeDistance, mzTolerance);

  }

  /**
   * Helper method for fitPattern. Fits only one half of the pattern.
   * 
   * @param p Pattern is fitted around this peak
   * @param charge Charge state of the fitted pattern
   * @param direction Defines which half to fit: -1=fit to peaks before start M/Z, +1=fit to peaks
   *        after start M/Z
   * @param fittedPeaks All matching peaks will be added to this set
   * @param mzTolerance
   */
  private static void fitHalfPattern(DataPoint p, int charge, int direction,
      List<DataPoint> fittedPeaks, DataPoint[] sortedPeaks, double isotopeDistance,
      MZTolerance mzTolerance) {

    double mainMZ = p.getMZ();

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
      Vector<DataPoint> goodCandidates = new Vector<DataPoint>();
      for (int ind = 0; ind < sortedPeaks.length; ind++) {

        DataPoint candidatePeak = sortedPeaks[ind];

        if (candidatePeak == null)
          continue;

        // Get properties of the candidate peak
        double candidatePeakMZ = candidatePeak.getMZ();

        // Does this peak fill all requirements of a candidate?
        // - within tolerances from the expected location (M/Z and RT)
        // - not already a fitted peak (only necessary to avoid
        // conflicts when parameters are set too wide)
        double isotopeMZ = candidatePeakMZ - isotopeDistance * direction * n / charge;

        if (mzTolerance.checkWithinTolerance(isotopeMZ, mainMZ)
            // && rtTolerance.checkWithinTolerance(candidatePeakRT, mainRT)
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
