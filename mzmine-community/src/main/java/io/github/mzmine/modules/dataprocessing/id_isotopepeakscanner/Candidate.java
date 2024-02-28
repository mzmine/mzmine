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

package io.github.mzmine.modules.dataprocessing.id_isotopepeakscanner;

import java.util.ArrayList;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.util.scans.ScanUtils;

/**
 * This class is used to calculate ratings and store the peak with the best rating. Intensities and
 * m/z are either taken directly from the peakListRow or given to this class by the Candidates
 * class.
 *
 * @author Steffen Heuckeroth steffen.heuckeroth@gmx.de / s_heuc03@uni-muenster.de
 *
 */
public class Candidate {

  private int candID; // row represents index in groupedPeaks list, candID is
                      // ID in original
                      // PeakList
  private double rating;
  private double mz;
  private FeatureListRow row;

  Candidate() {
    candID = 0;
    rating = 0.0;
  }

  public double getRating() {
    return rating;
  }

  public double getMZ() {
    return mz;
  }

  public int getCandID() {
    return candID;
  }

  private void setCandID(int candID) {
    this.candID = candID;
  }

  /**
   *
   * @param parent row of parent peak
   * @param candidate row pf candidate peak
   * @param pParent data point of predicted parent mass and intensity
   * @param pChild data point of predicted child mass and intensity
   * @return
   */
  public double calcIntensityAccuracy_Pattern(FeatureListRow parent, FeatureListRow candidate,
      DataPoint pParent, DataPoint pChild) {
    double idealIntensity = pChild.getIntensity() / pParent.getIntensity();
    return ((idealIntensity * parent.getAverageHeight()) / candidate.getAverageHeight());

    // return ( (pChild.getIntensity() / pParent.getIntensity()) *
    // (parent.getAverageArea()) /
    // candidate.getAverageArea() );
  }

  /**
   * for RatingType.HIGHEST
   *
   * @param parent
   * @param candidate
   * @param pattern
   * @param peakNum
   * @param minRating
   * @param checkIntensity
   * @return
   */
  public boolean checkForBetterRating(FeatureListRow parent, FeatureListRow candidate,
      IsotopePattern pattern, int peakNum, double minRating, boolean checkIntensity) {
    double parentMZ = parent.getAverageMZ();
    double candMZ = candidate.getAverageMZ();
    DataPoint[] points = ScanUtils.extractDataPoints(pattern);
    double mzDiff = points[peakNum].getMZ() - points[0].getMZ();

    double tempRating = candMZ / (parentMZ + mzDiff);
    double intensAcc = 0;

    if (tempRating > 1.0) // 0.99 and 1.01 should be comparable
      tempRating = 1 / tempRating;

    if (checkIntensity) {
      intensAcc = calcIntensityAccuracy_Pattern(parent, candidate, points[0], points[peakNum]);

      if (intensAcc > 1.0) // 0.99 and 1.01 should be comparable
        intensAcc = 1 / intensAcc;
    }

    if (intensAcc > 1.0 || intensAcc < 0.0 || tempRating > 1.0 || tempRating < 0.0) {
      return false;
    }

    if (checkIntensity)
      tempRating = intensAcc * tempRating;

    if (tempRating > rating && tempRating >= minRating) {
      rating = tempRating;

      row = candidate;
      mz = row.getAverageMZ();
      row.getAverageHeight();

      this.setCandID(candidate.getID());
      // this.setIsotope(isotopes[isotopenum]);
      return true;
    }
    return false;
  }

  /**
   * for RatingType.TEMPAVG
   *
   * @param parent dataPoint containing MZ and average intensity of parent
   * @param candidateIntensity average candidate intensity
   * @param pattern isotope pattern
   * @param peakNum peak number in isotope pattern
   * @param minRating
   * @param checkIntensity
   * @return
   */
  public boolean checkForBetterRating(DataPoint parent, double candidateIntensity,
      FeatureListRow candidate, IsotopePattern pattern, int peakNum, double minRating,
      boolean checkIntensity) {
    double parentMZ = parent.getMZ();
    double candMZ = candidate.getAverageMZ();
    DataPoint[] points = ScanUtils.extractDataPoints(pattern);
    double mzDiff = points[peakNum].getMZ() - points[0].getMZ();

    double tempRating = candMZ / (parentMZ + mzDiff);
    double intensAcc = 0;

    if (tempRating > 1.0) // 0.99 and 1.01 should be comparable
      tempRating = 1 / tempRating;

    if (checkIntensity) {
      intensAcc = calcIntensityAccuracy_Avg(parent.getIntensity(), candidateIntensity, points[0],
          points[peakNum]);

      if (intensAcc > 1.0) // 0.99 and 1.01 should be comparable
        intensAcc = 1 / intensAcc;
    }

    if (intensAcc > 1.0 || intensAcc < 0.0 || tempRating > 1.0 || tempRating < 0.0) {
      return false;
    }

    if (checkIntensity)
      tempRating = intensAcc * tempRating;

    if (tempRating > rating && tempRating >= minRating) {
      rating = tempRating;

      row = candidate;
      mz = row.getAverageMZ();
      this.setCandID(candidate.getID());
      // this.setIsotope(isotopes[isotopenum]);
      return true;
    }
    return false;
  }

  /**
   * method for neutralLoss
   *
   * @param pL
   * @param parentindex
   * @param candindex
   * @param mzDiff
   * @param minRating
   * @return
   */
  public boolean checkForBetterRating(ArrayList<FeatureListRow> pL, int parentindex, int candindex,
      double mzDiff, double minRating) {
    double parentMZ = pL.get(parentindex).getAverageMZ();
    double candMZ = pL.get(candindex).getAverageMZ();

    double tempRating = candMZ / (parentMZ + mzDiff);
    double intensAcc = 0;

    if (tempRating > 1.0) // 0.99 and 1.01 should be comparable
    {
      tempRating -= 1.0;
      tempRating = 1 - tempRating;
    }

    if (intensAcc > 1.0 || intensAcc < 0.0 || tempRating > 1.0 || tempRating < 0.0) {
      return false;
    }

    if (tempRating > rating && tempRating >= minRating) {
      rating = tempRating;

      row = pL.get(candindex);
      mz = row.getAverageMZ();
      row.getAverageHeight();

      this.setCandID(pL.get(candindex).getID());
      return true;
    }
    return false;
  }

  /**
   * done in the end, after all peaks have been identified, called by Candidates class
   *
   * @param parentMZ
   * @param pattern
   * @param peakNum
   * @param avgIntensity
   * @return
   */
  public double recalcRatingWithAvgIntensities(double parentMZ, IsotopePattern pattern, int peakNum,
      double[] avgIntensity) {
    double candMZ = this.mz;
    DataPoint[] points = ScanUtils.extractDataPoints(pattern);
    double mzDiff = points[peakNum].getMZ() - points[0].getMZ();

    double tempRating = candMZ / (parentMZ + mzDiff);
    double intensAcc = 0;

    if (tempRating > 1.0) // 0.99 and 1.01 should be comparable
      tempRating = 1 / tempRating;

    intensAcc = calcIntensityAccuracy_Avg(avgIntensity[0], avgIntensity[peakNum], points[0],
        points[peakNum]);

    if (intensAcc > 1.0) // 0.99 and 1.01 should be comparable
      intensAcc = 1 / intensAcc;

    if (intensAcc > 1.0 || intensAcc < 0.0 || tempRating > 1.0 || tempRating < 0.0) {
      return 0;
    }

    tempRating = intensAcc * tempRating;

    // rating = tempRating;
    return tempRating;
  }

  /**
   *
   * @param iParent measured intensity of parent
   * @param iChild measured intensity of candidate
   * @param pParent parent dataPoint in pattern
   * @param pChild child datePoint in pattern
   * @return IntensityAccuracy
   */
  private double calcIntensityAccuracy_Avg(double iParent, double iChild, DataPoint pParent,
      DataPoint pChild) {
    double idealIntensity = pChild.getIntensity() / pParent.getIntensity();
    return idealIntensity * iParent / iChild;
  }
}
