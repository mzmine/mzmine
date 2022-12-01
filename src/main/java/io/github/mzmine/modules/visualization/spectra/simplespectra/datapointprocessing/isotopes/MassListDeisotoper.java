/*
 * Copyright (c) 2004-2022 The MZmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.isotopes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import java.util.logging.Logger;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.DataPointSorter;
import io.github.mzmine.util.SortingDirection;
import io.github.mzmine.util.SortingProperty;

/**
 * Deisotope mass list This is basically copy-pasted from
 * io.github.mzmine.modules.peaklistmethods.isotopes.deisotoper.IsotopeGrouperTask
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

        if (candidatePeak == null || Double.compare(candidatePeak.getIntensity(), 0) == 0)
          continue;

        // Get properties of the candidate peak
        double candidatePeakMZ = candidatePeak.getMZ();

        // Does this peak fill all requirements of a candidate?
        // - within tolerances from the expected location (M/Z and RT)
        // - not already a fitted peak (only necessary to avoid
        // conflicts when parameters are set too wide)
        double isotopeMZ = candidatePeakMZ - isotopeDistance * direction * n / charge;

        if (mzTolerance.checkWithinTolerance(isotopeMZ, mainMZ)
            // && rtTolerance.checkWithinTolerance(candidatePeakRT,
            // mainRT)
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
