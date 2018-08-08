/*
 * Copyright 2006-2015 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.peaklistmethods.IsotopePeakScanner;

import java.util.ArrayList;
import org.jmol.util.Logger;
import org.openscience.cdk.interfaces.IIsotope;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.IsotopePattern;
import net.sf.mzmine.datamodel.PeakListRow;

public class Candidate {

  private int candID; // row represents index in groupedPeaks list, candID is ID in original
                      // PeakList
  private double rating;
  private double mz;
  private double height;
  private PeakListRow row;

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
  public double calcIntensityAccuracy_Pattern(PeakListRow parent, PeakListRow candidate,
      DataPoint pParent, DataPoint pChild) {
    double idealIntensity = pChild.getIntensity() / pParent.getIntensity();
    return ((idealIntensity * parent.getAverageHeight()) / candidate.getAverageHeight());

    // return ( (pChild.getIntensity() / pParent.getIntensity()) * (parent.getAverageArea()) /
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
  public boolean checkForBetterRating(PeakListRow parent, PeakListRow candidate,
      IsotopePattern pattern, int peakNum, double minRating, boolean checkIntensity) {
    double parentMZ = parent.getAverageMZ();
    double candMZ = candidate.getAverageMZ();
    DataPoint[] points = pattern.getDataPoints();
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
      Logger.debug("ERROR: tempRating or deviation > 1 or < 0.\ttempRating: " + tempRating
          + "\tintensAcc: " + intensAcc); // TODO: can you do this without creating a new logger?
      return false;
    }

    if (checkIntensity)
      tempRating = intensAcc * tempRating;

    if (tempRating > rating && tempRating >= minRating) {
      rating = tempRating;

      row = candidate;
      mz = row.getAverageMZ();
      height = row.getAverageHeight();

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
      PeakListRow candidate, IsotopePattern pattern, int peakNum, double minRating,
      boolean checkIntensity) {
    double parentMZ = parent.getMZ();
    double candMZ = candidate.getAverageMZ();
    DataPoint[] points = pattern.getDataPoints();
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
      Logger.debug("ERROR: tempRating or deviation > 1 or < 0.\ttempRating: " + tempRating
          + "\tintensAcc: " + intensAcc); // TODO: can you do this without creating a new logger?
      return false;
    }

    if (checkIntensity)
      tempRating = intensAcc * tempRating;

    if (tempRating > rating && tempRating >= minRating) {
      rating = tempRating;

      row = candidate;
      mz = row.getAverageMZ();
      height = candidateIntensity;

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
  public boolean checkForBetterRating(ArrayList<PeakListRow> pL, int parentindex, int candindex,
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
      Logger.debug("ERROR: tempRating > 1 or < 0.\ttempRating: " + tempRating); // TODO: can you do
                                                                                // this without
                                                                                // creating a new
                                                                                // logger?
      return false;
    }

    if (tempRating > rating && tempRating >= minRating) {
      rating = tempRating;

      row = pL.get(candindex);
      mz = row.getAverageMZ();
      height = row.getAverageHeight();

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
    DataPoint[] points = pattern.getDataPoints();
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
      Logger.debug("ERROR: tempRating or deviation > 1 or < 0.\ttempRating: " + tempRating
          + "\tintensAcc: " + intensAcc); // TODO: can you do this without creating a new logger?
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
