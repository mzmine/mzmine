/*
 * Copyright 2006-2019 The MZmine 2 Development Team
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

package net.sf.mzmine.util;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.checkerframework.common.reflection.qual.GetMethod;
import com.google.common.collect.Range;
import com.google.common.primitives.Ints;
import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.project.impl.RawDataFileImpl;

public class PeakAlignmentUtils {

  private static Logger logger = Logger.getLogger(PeakAlignmentUtils.class.getName());

  public static double getRTScore(@Nonnull PeakListRow row1, @Nonnull PeakListRow row2) {
    double rt1 = row1.getAverageRT();
    double rt2 = row2.getAverageRT();

    double score = rt1 / rt2;
    score = (score <= 1.0) ? score : 1 / score;

    return score;
  }

  public static double getPeakShapeScore(@Nonnull PeakListRow row1, @Nonnull PeakListRow row2) {
    Feature f1 = row1.getBestPeak();
    Feature f2 = row2.getBestPeak();

    Feature icpFeature, mainFeature;
    // double icpPeakHeight, mainPeakHeight;

    if (f1.getRepresentativeScan().getNumberOfDataPoints() > f2.getRepresentativeScan()
        .getNumberOfDataPoints()) {
      icpFeature = f2;
      mainFeature = f1;
    } else {
      mainFeature = f2;
      icpFeature = f1;
    }

    // icpPeakHeight = icpFeature.getHeight();
    // mainPeakHeight = mainFeature.getHeight();

    // compare the RT of the scans
    List<ScanMatch> matchingScans = getMatchingScansAligned(icpFeature, mainFeature);

    if (matchingScans.size() < 2)
      return 0.0;

    logger.info("Calculating peakShapeScore for m/zs = " + icpFeature.getMZ() + ", "
        + mainFeature.getMZ() + " over " + matchingScans.size() + " + scans.");

    // get the slope between the data points
    double icpSlope[] = new double[matchingScans.size() - 1];
    double mainSlope[] = new double[matchingScans.size() - 1];
    double maxIcpSlope = 0, maxMainSlope = 0;
    for (int i = 0; i < matchingScans.size() - 1; i++) {
      Scan[] pair1 = matchingScans.get(i).getScans();
      Scan[] pair2 = matchingScans.get(i + 1).getScans();

      // calculate the relative feature slope -> absolute delta
      double iicp1 = icpFeature.getDataPoint(pair2[0].getScanNumber()).getIntensity();
      double iicp0 = icpFeature.getDataPoint(pair1[0].getScanNumber()).getIntensity();
      int scannum = pair2[1].getScanNumber();
      int scannum2 = pair1[1].getScanNumber();

      // somehow this returns null someimes. might be a problem in the chromatogram builder
      if (mainFeature.getDataPoint(scannum) == null || mainFeature.getDataPoint(scannum2) == null)
        continue;
      double imain1 = mainFeature.getDataPoint(scannum).getIntensity();
      double imain0 = mainFeature.getDataPoint(scannum2).getIntensity();
      icpSlope[i] = (iicp1 - iicp0);
      mainSlope[i] = (imain1 - imain0);

      maxIcpSlope = (icpSlope[i] > maxIcpSlope) ? icpSlope[i] : maxIcpSlope;
      maxMainSlope = (mainSlope[i] > maxMainSlope) ? mainSlope[i] : maxMainSlope;
    }

    // normalize the slopes
    for (int i = 0; i < matchingScans.size() - 1; i++) {
      icpSlope[i] /= maxIcpSlope;
      mainSlope[i] /= maxMainSlope;
    }

    // get the absolute of the slope differences
    double deltaSlope[] = new double[icpSlope.length];
    for (int i = 0; i < deltaSlope.length; i++)
      deltaSlope[i] = Math.abs((icpSlope[i] - mainSlope[i]));

    double score = 1 - MathUtils.calcAvg(deltaSlope);

    // double icpTailing = icpFeature.getTailingFactor();
    // double icpAsymmetry = icpFeature.getAsymmetryFactor();
    // double mainTailing = mainFeature.getTailingFactor();
    // double mainAsymmetry = mainFeature.getAsymmetryFactor();
    // logger.info("Icp - tailing: " + icpTailing + " asymmetry: " + icpAsymmetry);
    // logger.info("Main - tailing: " + mainTailing + " asymmetry: " + mainAsymmetry);
    logger
        .info("Score for m/zs " + icpFeature.getMZ() + ", " + mainFeature.getMZ() + " = " + score);
    return score;
    
    
  }

  private static class ScanMatch {
    private final Scan scan1, scan2;

    public ScanMatch(Scan icpScan, Scan mainScan) {
      this.scan1 = icpScan;
      this.scan2 = mainScan;
    }

    public Scan[] getScans() {
      return new Scan[] {scan1, scan2};
    }
  }

  public static int[] getFeatureScanNumbersRTRange(Feature f, Range<Double> rt) {
    int[] featureScanNums = f.getScanNumbers();
    RawDataFile file = f.getDataFile();

    // TODO
    List<Integer> scanNums = new ArrayList<Integer>();
    for (int sn : featureScanNums) {
      if (rt.contains(file.getScan(sn).getRetentionTime())) {
        scanNums.add(sn);
      }
    }
    return Ints.toArray(scanNums);
  }

  public static List<ScanMatch> getMatchingScans(Feature f1, Feature f2) {

    List<ScanMatch> matched = new ArrayList<ScanMatch>();

    Range<Double> rtRange1 = f1.getRawDataPointsRTRange();
    Range<Double> rtRange2 = f2.getRawDataPointsRTRange();

    // check if the RT ranges have something in common
    if (!rtRange1.isConnected(rtRange2))
      return matched;

    // check how much scans per minute (spm) there are in both data files, so we know how to align
    // them.
    Range<Double> connected = RangeUtils.getConnected(rtRange1, rtRange2);
    // TODO: only get the feature scan numbers in the rt range
    int scanNum1[] = getFeatureScanNumbersRTRange(f1, connected);
    int scanNum2[] = getFeatureScanNumbersRTRange(f2, connected);
    double spm1 = scanNum1.length / (connected.upperEndpoint() - connected.lowerEndpoint());
    double spm2 = scanNum2.length / (connected.upperEndpoint() - connected.lowerEndpoint());
    // logger.finest("Scans per min in raw file: " + f1.getDataFile().getName() + " " + spm1);
    // logger.finest("Scans per min in raw file: " + f2.getDataFile().getName() + " " + spm2);

    Feature fLessspm, fMorespm;
    int[] lessspmScanNums;
    if (spm1 > spm2) {
      fMorespm = f1;
      fLessspm = f2;
      lessspmScanNums = scanNum2;
    } else {
      fMorespm = f2;
      fLessspm = f1;
      lessspmScanNums = scanNum1;
    }

    int minScanNum = 1;

    for (int i = 0; i < lessspmScanNums.length; i++) {
      Scan scan1 = fLessspm.getDataFile().getScan(lessspmScanNums[i]);

      Scan scan2 = getClosestScanByRT(scan1, fMorespm, minScanNum, 0);
      if (scan2 == null)
        continue;
      minScanNum = scan2.getScanNumber() + 1;

      // the icp scan should have less data points than the molecule-ms one
      if (scan1.getNumberOfDataPoints() < scan2.getNumberOfDataPoints())
        matched.add(new ScanMatch(scan1, scan2));
      else
        matched.add(new ScanMatch(scan2, scan1));
    }

    return matched;
  }

  public static List<ScanMatch> getMatchingScansAligned(Feature f1, Feature f2) {
    List<ScanMatch> matched = new ArrayList<ScanMatch>();

    Range<Double> rtRange1 = f1.getRawDataPointsRTRange();
    Range<Double> rtRange2 = f2.getRawDataPointsRTRange();
   
    Range<Double> smallerRange;
    if((rtRange1.upperEndpoint() - rtRange1.lowerEndpoint()) > (rtRange2.upperEndpoint() - rtRange2.lowerEndpoint()))
        smallerRange = rtRange2;
    else smallerRange = rtRange1;
    
    // check how much scans per minute (spm) there are in both data files, so we know how to align
    // them.
    int scanNum1[] = f1.getScanNumbers();
    int scanNum2[] = f2.getScanNumbers();
    // use the smaller range. Maybe due to split injection, the peak shape is a bit distorted
    double spm1 = scanNum1.length / (smallerRange.upperEndpoint() - smallerRange.lowerEndpoint());
    double spm2 = scanNum2.length / (smallerRange.upperEndpoint() - smallerRange.lowerEndpoint());
    
    Feature fLessspm, fMorespm;
    int[] lessspmScanNums;
    if (spm1 > spm2) {
      fMorespm = f1;
      fLessspm = f2;
      lessspmScanNums = scanNum2;
    } else {
      fMorespm = f2;
      fLessspm = f1;
      lessspmScanNums = scanNum1;
    }
    
    double rtOffset = fMorespm.getRT()- fLessspm.getRT();
    
    logger.finest("Rt lessSpm: " + fLessspm.getRT() + " " + "\tRt moreSpm: " + fMorespm.getRT() + "\tmoreSpm will be adjudsted by: -" + rtOffset);
    
    int minScanNum = 1;
    for (int i = 0; i < lessspmScanNums.length; i++) {
      Scan scan1 = fLessspm.getDataFile().getScan(lessspmScanNums[i]);

      Scan scan2 = getClosestScanByRT(scan1, fMorespm, minScanNum, rtOffset);
      if (scan2 == null)
        continue;
      minScanNum = scan2.getScanNumber() + 1;

      // the icp scan should have less data points than the molecule-ms one
      if (scan1.getNumberOfDataPoints() < scan2.getNumberOfDataPoints())
        matched.add(new ScanMatch(scan1, scan2));
      else
        matched.add(new ScanMatch(scan2, scan1));
    }
    
    return matched;
  }

  /**
   * Used to find the closest scan by rt of a specific feature to a scan from a different file. Used
   * to align different HPLC runs.
   * 
   * @param scan The scan a corresponding scan should be searched for.
   * @param feature The feature the corresponding scan shall be extracted from.
   * @param minScanNum The minimum scan number. Used if a previous feature was aligned already.
   * @return The scan of the feature that is closest to the actual scan provided. Null if the
   *         feature has no data file or the minScanNumber is higher than the actual number of
   *         scans.
   */
  public static @Nullable Scan getClosestScanByRT(Scan scan, Feature feature, int minScanNum, double rtOffset) {

    if (feature.getDataFile() == null) {
      logger.warning("Feature of m/z " + feature.getMZ()
          + " did not have a raw data file associated. Cannot find closest scans.");
      return null;
    }
    RawDataFile raw = feature.getDataFile();

    Scan bestScan = null;

    int[] scanNumbers = feature.getScanNumbers();

    // the minimum Scan number is not within the assigned scan numbers of the feature
    if (minScanNum > scanNumbers[scanNumbers.length - 1])
      return bestScan;

    double rt = scan.getRetentionTime();
    double smallestDiff = 1E10;
    for (int i = minScanNum; i < scanNumbers.length; i++) {
      double rtDiff = Math.abs(rt - (raw.getScan(scanNumbers[i]).getRetentionTime() - rtOffset));
      if (rtDiff < smallestDiff) {
        smallestDiff = rtDiff;
        bestScan = raw.getScan(scanNumbers[i]);
      } else {
        // logger.info("Aligning scans #" + scan.getScanNumber() + " (" + scan.getRetentionTime()
        // + ") and #" + bestScan.getScanNumber() + " (" + bestScan.getRetentionTime() + ")");
        // if the difference is getting bigger, we're already past the best scan and can break here
        break;
      }
    }
    return bestScan;
  }



  private static boolean arrayContains(int array[], int num) {
    for (int a : array) {
      if (a == num) {
        return true;
      }
    }
    return false;
  }
}
