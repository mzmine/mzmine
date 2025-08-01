/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

import com.google.common.collect.Range;
import io.github.msdk.MSDKRuntimeException;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.modules.dataprocessing.id_isotopepeakscanner.IsotopePeakScannerTask.RatingType;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import java.util.Arrays;
import java.util.logging.Logger;
import javafx.collections.ObservableList;

/**
 * This class is used to manage objects of the Candidate class and to calculate an average rating if
 * specified, after all peaks have been assigned. This methods use an instance of a PeakListHandler
 * since its easier to handle row ids than row indexes.
 *
 * @author Steffen Heuckeroth steffen.heuckeroth@gmx.de / s_heuc03@uni-muenster.de
 */
public class Candidates {

  private static final Logger logger = Logger.getLogger(Candidates.class.getName());

  private IsotopePattern pattern;
  private MZTolerance mzTolerance;
  private double minHeight;
  private double avgRating[];
  private double avgHeight[];
  private Candidate[] candidate;
  RatingType ratingType;
  PeakListHandler plh;

  public Candidates(int size, double minHeight, MZTolerance mzTolerance, IsotopePattern pattern,
      PeakListHandler plh, RatingType ratingType) {
    this.candidate = new Candidate[size];
    for (int i = 0; i < size; i++) {
      candidate[i] = new Candidate();
    }
    avgRating = new double[size];
    Arrays.fill(avgRating, -1.0);
    avgHeight = new double[size];
    this.minHeight = minHeight;
    this.mzTolerance = mzTolerance;
    this.pattern = pattern;
    this.plh = plh;
    this.ratingType = ratingType;

    for (Candidate c : candidate) {
      if (c == null) {
        logger.info("failed to initialize candidate");
      }
    }
  }

  /**
   * Contstructor for neutral loss scans, no need for pattern and mass last
   *
   * @param size
   * @param minHeight
   * @param mzTolerance
   * @param plh
   */
  public Candidates(int size, double minHeight, MZTolerance mzTolerance, PeakListHandler plh) {
    this.candidate = new Candidate[size];
    for (int i = 0; i < size; i++) {
      candidate[i] = new Candidate();
    }
    avgRating = new double[size];
    Arrays.fill(avgRating, -1.0);
    avgHeight = new double[size];
    this.minHeight = minHeight;
    this.mzTolerance = mzTolerance;
    this.pattern = null;
    this.plh = plh;
    this.ratingType = RatingType.HIGHEST;
  }

  /**
   * @param index integer index
   * @return Candidate with specified index
   */
  public Candidate get(int index) {
    if (index >= candidate.length) {
      throw new MSDKRuntimeException("Candidates.get(index): index > length");
    }
    return candidate[index];
  }

  /**
   * @param index
   * @return average rating of specified peak. -1 if not set
   */
  public double getAvgRating(int index) {
    if (pattern == null) // if we run a neutral loss scan this doesn't exist
    {
      return -1.0;
    }

    if (index >= candidate.length) {
      throw new MSDKRuntimeException("Candidates.get(index): index > length");
    }
    return avgRating[index];
  }

  /**
   * @return total average rating of all data points in the detected pattern
   */
  public double getAvgAccAvgRating() {
    if (pattern == null) // if we run a neutral loss scan this doesn't exist
    {
      return -1.0;
    }

    if (avgRating.length == 0) {
      return 0.0;
    }

    double buffer = 0.0;
    for (double rating : avgRating) {
      buffer += rating;
    }

    return buffer / avgRating.length;
  }

  public double getSimpleAvgRating() {
    if (pattern == null) // if we run a neutral loss scan this doesn't exist
    {
      return -1.0;
    }

    double buffer = 0.0;
    for (int i = 0; i < candidate.length; i++) {
      buffer += candidate[i].getRating();
    }

    return buffer / candidate.length;
  }

  public int size() {
    return candidate.length;
  }

  /**
   * @return all candidate objects
   */
  public Candidate[] getCandidates() {
    return candidate;
  }

  /**
   * @return IsotopePattern object the pattern got initialized with
   */
  public IsotopePattern getPattern() {
    return pattern;
  }

  /**
   * sets isotope pattern, should not be used when there is a different number of data points in the
   * new pattern.
   *
   * @param pattern
   */
  public void setPattern(IsotopePattern pattern) {
    this.pattern = pattern;
  }

  /**
   * @param index
   * @return returns average intensity of a single peak. -1.0 if calculation failed.
   */
  public double getAvgHeight(int index) {
    if (pattern == null) // if we run a neutral loss scan this doesnt exist
    {
      return -1.0;
    }

    if (index > candidate.length || avgHeight == null) {
      return 0.0;
    }
    return avgHeight[index];
  }

  /**
   * For isotope pattern searches
   *
   * @param index
   * @param parent         row of monoisotopic mass
   * @param cand           row of candidate peak
   * @param minRating      minimum rating
   * @param checkIntensity
   * @return true if better, false if worse
   */
  public boolean checkForBetterRating(int index, FeatureListRow parent, FeatureListRow cand,
      double minRating, boolean checkIntensity) {
    if (ratingType == RatingType.HIGHEST) {
      return candidate[index].checkForBetterRating(parent, cand, pattern, index, minRating,
          checkIntensity);
    } else if (ratingType == RatingType.TEMPAVG) {
      DataPoint dpParent = new SimpleDataPoint(parent.getAverageMZ(),
          calcAvgPeakHeight(parent.getID()));
      double candidateIntensity = calcAvgPeakHeight(cand.getID());

      if (candidateIntensity == -1.0) {
        return false;
      }

      return candidate[index].checkForBetterRating(dpParent, candidateIntensity, cand, pattern,
          index, minRating, checkIntensity);
    } else {
      throw new MSDKRuntimeException("Error: Invalid RatingType.");
    }
  }

  /**
   * @param index index
   * @return average peak intensity over all mass lists it is contained in
   */
  public double calcTemporaryAvgRating(int index) {
    if (pattern == null) // if we run a neutral loss scan this doesn't exist
    {
      return -1.0;
    }

    if (index >= candidate.length) {
      return 0.0;
    }

    double parentHeight = calcAvgPeakHeight(candidate[0].getCandID());
    double childHeight = calcAvgPeakHeight(candidate[index].getCandID());

    double[] avg = new double[candidate.length];
    avg[0] = parentHeight;
    avg[index] = childHeight;

    return candidate[index].recalcRatingWithAvgIntensities(candidate[0].getMZ(), pattern, index,
        avg);
  }

  /**
   * @return array of all avg ratings
   */
  public double[] calcAvgRatings() {
    if (pattern == null) // if we run a neutral loss scan this doesn't exist
    {
      return new double[candidate.length];
    }

    int[] ids = new int[candidate.length];

    for (int i = 0; i < candidate.length; i++) {
      ids[i] = candidate[i].getCandID();
    }

    avgHeight = getAvgPeakHeights(ids);

    if (avgHeight == null || avgHeight[0] == 0.0) {
      return avgRating;
    }

    for (int i = 0; i < candidate.length; i++) {
      avgRating[i] = candidate[i].recalcRatingWithAvgIntensities(candidate[0].getMZ(), pattern, i,
          avgHeight);
    }
    return avgRating;
  }

  /**
   * needed by calcTemporaryAvgRating
   *
   * @param ID
   * @return avPeakHeight
   */
  private double calcAvgPeakHeight(int ID) {
    FeatureListRow row = plh.getRowByID(ID);

    RawDataFile[] raws = row.getRawDataFiles().toArray(new RawDataFile[0]);

    if (raws.length < 1) {
      return 0.0;
    }

    double mz = row.getAverageMZ();
    double avgIntensity = 0.0;
    int pointsAdded = 0;

    for (RawDataFile raw : raws) {
      if (!raw.getDataMZRange().contains(mz)) {
        continue;
      }

      ObservableList<Scan> scanNums = raw.getScans();

      for (int i = 0; i < scanNums.size(); i++) {
        Scan scan = scanNums.get(i);

        MassList list = scan.getMassList();

        if (list == null) {
          continue;
        }

        DataPoint[] points = getMassListDataPointsByMass(list, mzTolerance.getToleranceRange(mz));

        if (points.length == 0) {
          continue;
        }

        DataPoint dp = getClosestDataPoint(points, mz, minHeight);

        if (dp != null) {
          avgIntensity += dp.getIntensity();
          pointsAdded++;
        }
      }
    }

    if (pointsAdded != 0) {
      return avgIntensity / pointsAdded;
    } else {
      return -1.0;
    }
  }

  /**
   * @param ID
   * @return avg heights of all with the ids, but only if they are contained in same scans and mass
   * lists
   */
  private double[] getAvgPeakHeights(int[] ID) {
    FeatureListRow[] rows = plh.getRowsByID(ID);

    RawDataFile[] raws = rows[0].getRawDataFiles().toArray(new RawDataFile[0]);

    if (raws.length < 1) {
      return null;
    }

    double[] mzs = new double[ID.length];

    for (int i = 0; i < rows.length; i++) {
      mzs[i] = rows[i].getAverageMZ();
    }

    double[] avgHeights = new double[ID.length];
    int pointsAdded = 0;

    for (RawDataFile raw : raws) {

      if (!raw.getDataMZRange().contains(rows[0].getAverageMZ())) {
        continue;
      }

      ObservableList<Scan> scanNums = raw.getScans();

      for (int i = 0; i < scanNums.size(); i++) {
        Scan scan = scanNums.get(i);

        MassList list = scan.getMassList();

        if (list == null || !massListContainsEveryMZ(list, mzs, minHeight)) {
          continue;
        }

        double[] avgBuffer = new double[mzs.length];
        boolean allFound = true;

        for (int j = 0; j < mzs.length; j++) {
          DataPoint[] points = getMassListDataPointsByMass(list,
              mzTolerance.getToleranceRange(mzs[j]));

          if (points.length == 0) {
            continue;
          }

          DataPoint dp = getClosestDataPoint(points, rows[j].getAverageMZ(), minHeight);

          if (dp == null) // yes the list contained something close to
          // every datapoint that was over
          // minHeight, BUT
          { // the closest might not have been. Check is done inside
            // getClosestDataPoint();
            allFound = false;
            break;
          }
          avgBuffer[j] = dp.getIntensity();
        }

        if (allFound) {
          pointsAdded++;
          for (int j = 0; j < mzs.length; j++) {
            avgHeights[j] += avgBuffer[j];
          }
        }
      }
    }

    if (pointsAdded == 0) {
      logger.warning("Error: Peaks with ids: " + Arrays.toString(ID)
          + " were not in same scans at all. Please update the parameters.");
      return null;
    }
    for (int i = 0; i < avgHeights.length; i++) {
      avgHeights[i] /= (pointsAdded/* /mzs.length */);
    }

    return avgHeights;
  }

  /**
   * @param dp
   * @param mz
   * @param minHeight
   * @return closest data point to given mz above minimum intensity in a given set of data points;
   * null if no DataPoint over given intensity
   */
  private DataPoint getClosestDataPoint(DataPoint[] dp, double mz, double minHeight) {
    if (dp == null || dp[0] == null || dp.length == 0) {
      return null;
    }

    DataPoint n = new SimpleDataPoint(0.0, 0.0);

    for (DataPoint p : dp) {
      if (Math.abs(p.getMZ() - mz) < Math.abs(mz - n.getMZ()) && p.getIntensity() >= minHeight) {
        n = p;
      }
    }

    if (n.getIntensity() == 0.0) {
      // System.out.println("Info: Closest data point not above min
      // intensity. m/z: " + mz);
      return null;
    }
    return n;
  }

  /**
   * @param list      MassList to check
   * @param mz        array of mzs that need to be contained
   * @param minHeight minimum peak intensity
   * @return true or false
   */
  private boolean massListContainsEveryMZ(MassList list, double[] mz, double minHeight) {
    DataPoint[] dps = list.getDataPoints();
    if (dps.length < 1) {
      return false;
    }

    for (int i = 0; i < mz.length; i++) {
      boolean aboveMinHeight = false;

      for (DataPoint p : dps) {
        if (p.getMZ() < (mz[i] - mzTolerance.getMzTolerance())) {
          continue;
        }

        if (p.getMZ() > (mz[i] + mzTolerance.getMzTolerance())) {
          break;
        }

        if (p.getIntensity() >= minHeight && mzTolerance.checkWithinTolerance(p.getMZ(), mz[i])) {
          aboveMinHeight = true;
        }
      }

      if (!aboveMinHeight) {
        // System.out.println("Info: Mass list " + list.getName() + "
        // does not contain every mz: " +
        // mz.toString());
        return false;
      }
    }
    return true;
  }

  /**
   * @param list
   * @param massRange
   * @return dataPoints within given massRange contained in mass list
   */
  private DataPoint[] getMassListDataPointsByMass(MassList list, Range<Double> massRange) {
    DataPoint[] dps = list.getDataPoints();
    int start = 0, end = 0;

    for (start = 0; start < dps.length; start++) {
      if (massRange.lowerEndpoint() >= dps[start].getMZ()) {
        break;
      }
    }

    for (end = start; end < dps.length; end++) {
      if (massRange.upperEndpoint() < dps[end].getMZ()) {
        break;
      }
    }

    DataPoint[] dpReturn = new DataPoint[end - start];

    System.arraycopy(dps, start, dpReturn, 0, end - start);

    return dpReturn;
  }
}
