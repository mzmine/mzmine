/*
 *
 *  * Copyright 2006-2020 The MZmine Development Team
 *  *
 *  * This file is part of MZmine.
 *  *
 *  * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 *  * General Public License as published by the Free Software Foundation; either version 2 of the
 *  * License, or (at your option) any later version.
 *  *
 *  * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 *  * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 *  * Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License along with MZmine; if not,
 *  * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 *  * USA
 *
 *
 */

package io.github.mzmine.util;

import com.google.common.collect.Range;
import io.github.mzmine.modules.dataprocessing.featdet_mobilogrambuilder.MobilityDataPoint;
import io.github.mzmine.modules.dataprocessing.featdet_mobilogrambuilder.Mobilogram;
import io.github.mzmine.modules.dataprocessing.featdet_mobilogrambuilder.SimpleMobilogram;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class MobilogramUtils {

  /**
   * If the number of consecutively empty scans exceeds this constant, it's interpreted as a
   * decrease to 0 intensity.
   */
  public static final int MIN_GAP = 1;

  /**
   * @return a list of the added data points.
   */
  public static List<MobilityDataPoint> fillMissingScanNumsWithZero(SimpleMobilogram mobilogram) {
    List<MobilityDataPoint> dataPoints = mobilogram.getDataPoints();
    if (dataPoints.size() <= 3) {
      return Collections.emptyList();
    }

    // find smallest mobility distance between two points
    // get two dp
    double minDist = getMobilityStepSize(mobilogram);

    List<MobilityDataPoint> newDps = new ArrayList<>();
    MobilityDataPoint dp = dataPoints.stream().findFirst().get();
    int nextScanNum = dp.getScanNum() + 1;
    double lastMobility = dp.getMobility();
    Iterator<MobilityDataPoint> iterator = dataPoints.iterator();

    if (iterator.hasNext()) {
      iterator.next();
    }
    while (iterator.hasNext()) {
      MobilityDataPoint nextDp = iterator.next();

      while (nextDp.getScanNum() != nextScanNum) {
        MobilityDataPoint newDp = new MobilityDataPoint(mobilogram.getMZ(), 0.0d,
            lastMobility - minDist, nextScanNum);
        newDps.add(newDp);
        nextScanNum++;
        lastMobility -= minDist;
      }
      lastMobility = nextDp.getMobility();
      nextScanNum = nextDp.getScanNum() + 1;
    }
    newDps.forEach(mobilogram::addDataPoint);
    mobilogram.calc();
    return newDps;
  }


  /**
   * Manually adds a 0 intensity data point to the edges of this mobilogram, if a gap of more than
   * minGap scans is detected.
   *
   * @param minGap
   */
  private static void fillEdgesWithZeros(SimpleMobilogram mobilogram, int minGap) {
    List<MobilityDataPoint> dataPoints = mobilogram.getDataPoints();
    List<MobilityDataPoint> newDataPoints = new ArrayList<>();
    final double minStep = getMobilityStepSize(mobilogram);

    for (MobilityDataPoint dp : dataPoints) {
      final int gap = getNumberOfConsecutiveEmptyScans(mobilogram, dp.getScanNum());
      if (gap > minGap) {
        MobilityDataPoint firstDp = new MobilityDataPoint(
            mobilogram.getMZ(), 0.0, dp.getMobility() - minStep,
            dp.getScanNum() + 1);
        MobilityDataPoint lastDp = new MobilityDataPoint(mobilogram.getMZ(), 0.0,
            dp.getMobility() - minStep * (gap - 1),
            dp.getScanNum() + gap - 1);
        newDataPoints.add(firstDp);
        newDataPoints.add(lastDp);
      }
    }
    newDataPoints.forEach(mobilogram::addDataPoint);
    mobilogram.calc();
  }

  public static int getNumberOfConsecutiveEmptyScans(Mobilogram mobilogram, int startScanNum) {
    return getNextAvailableScanNumber(mobilogram, startScanNum) - startScanNum;
  }

  public static int getNextAvailableScanNumber(Mobilogram mobilogram, int startScanNum) {
    boolean foundStartKey = false;
    List<MobilityDataPoint> dataPoints = mobilogram.getDataPoints().stream()
        .sorted(Comparator.comparingInt(MobilityDataPoint::getScanNum)).collect(
            Collectors.toList());

    for (int i = 0; i < dataPoints.size(); i++) {
      int scanNum = dataPoints.get(i).getScanNum();
      if (scanNum == startScanNum) {
        foundStartKey = true;
        continue;
      }
      if (foundStartKey) {
        return scanNum;
      }
    }
    return dataPoints.get(dataPoints.size() - 1).getScanNum();
  }

  private static double getMobilityStepSize(Mobilogram mobilogram) {
    List<MobilityDataPoint> dataPoints = mobilogram.getDataPoints().stream()
        .sorted(Comparator.comparingInt(MobilityDataPoint::getScanNum)).collect(
            Collectors.toList());
    // find smallest mobility distance between two points get two dp
    MobilityDataPoint aDp = null;
    for (MobilityDataPoint dp : dataPoints) {
      if (aDp == null) {
        aDp = dp;
      } else {
        return Math
            .abs((aDp.getMobility() - dp.getMobility()) / (aDp.getScanNum() - dp.getScanNum()));
      }
    }
    return 0;
  }

  public static SimpleMobilogram removeZeroIntensityDataPoints(Mobilogram mobilogram) {
    SimpleMobilogram newMobilogram = new SimpleMobilogram(mobilogram.getMobilityType(),
        mobilogram.getRawDataFile());

    for (MobilityDataPoint dp : mobilogram.getDataPoints()) {
      if (Double.compare(dp.getIntensity(), 0) == 0.0d) {
        newMobilogram.addDataPoint(dp);
      }
    }

    fillEdgesWithZeros(newMobilogram, MIN_GAP);
    newMobilogram.calc();
    return newMobilogram;
  }

  // deconvolution

  /**
   * Basically a general version of local minimum search.
   *
   * @param x
   * @param y
   * @param indices
   * @param peakDuration
   * @param searchRTRange
   * @param minRatio
   * @param minAbsHeight
   * @param minRelHeight
   * @param chromatographicThresholdLevel
   * @return
   * @see io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.minimumsearch.MinimumSearchPeakDetector
   */
  public static Set<Set<Integer>> resolveMobilogramLocalMinimum(final double x[], final double y[],
      final int[] indices,/* List<MobilityDataPoint> dps,*/ Range<Double> peakDuration,
      final float searchRTRange,
      final double minRatio, final double minAbsHeight, final double minRelHeight,
      final double chromatographicThresholdLevel) {

    // Important: empty scans need to be represented by a 0!
    final int scanCount = x.length;
    double mobilities[] = x;
    double intensities[] = y;
    double maximumIntensity = Arrays.stream(y).max().getAsDouble();

    Set<Set<Integer>> resolved = new HashSet<>();

    final int lastScan = scanCount - 1;
    assert scanCount > 0;

    final double minHeight = Math.max(minAbsHeight, minRelHeight * maximumIntensity);

    // First, remove all data points below chromatographic threshold.
    for (int i = 0; i < intensities.length; i++) {
      if (intensities[i] < chromatographicThresholdLevel) {
        intensities[i] = 0.0;
      }
    }

    // Current region is a region between two minima, representing a
    // candidate for a resolved peak.
    startSearch:
    for (int currentRegionStart = 0; currentRegionStart < lastScan
        - 2; currentRegionStart++) {

      // Find at least two consecutive non-zero data points
      if (intensities[currentRegionStart] == 0.0 || intensities[currentRegionStart + 1] == 0.0) {
        continue;
      }

      double currentRegionHeight = intensities[currentRegionStart];

      endSearch:
      for (int currentRegionEnd =
          currentRegionStart + 1; currentRegionEnd < scanCount; currentRegionEnd++) {

        // Update height of current region.
        currentRegionHeight = Math.max(currentRegionHeight, intensities[currentRegionEnd]);

        // If we reached the end, or if the next intensity is 0, we
        // have to stop here.
        if (currentRegionEnd == lastScan || intensities[currentRegionEnd + 1] == 0.0) {

          // Find the intensity at the sides (lowest data points).
          final double peakMinLeft = intensities[currentRegionStart];
          final double peakMinRight = intensities[currentRegionEnd];

          // Check the shape of the peak.
          if (currentRegionHeight >= minHeight && currentRegionHeight >= peakMinLeft * minRatio
              && currentRegionHeight >= peakMinRight * minRatio && peakDuration.contains(
              mobilities[currentRegionEnd] - mobilities[currentRegionStart])) {

//            resolvedPeaks.add(new ResolvedPeak(mobilogram, currentRegionStart, currentRegionEnd,
//                mzCenterFunction, msmsRange, rTRangeMSMS));

            int finalCurrentRegionStart = currentRegionStart;
            int finalCurrentRegionEnd = currentRegionEnd;
//            resolved.add(Arrays.stream(identifiers)
//                .filter(id -> (id >= finalCurrentRegionStart && id <= finalCurrentRegionEnd))
//                .boxed().collect(Collectors.toSet()));

            Set<Integer> set = new HashSet<>();
            for (Integer id : indices) {
              if (id.intValue() >= finalCurrentRegionStart && id <= finalCurrentRegionEnd) {
                set.add(id);
              }
            }
            resolved.add(set);
          }

          // Set the next region start to current region end - 1
          // because it will be immediately
          // increased +1 as we continue the for-cycle.
          currentRegionStart = currentRegionEnd - 1;
          continue startSearch;
        }

        // Minimum duration of peak must be at least searchRTRange.
        if (mobilities[currentRegionEnd]
            - mobilities[currentRegionStart] >= searchRTRange) {

          // Set the RT range to check
          final Range<Double> checkRange =
              Range.closed(mobilities[currentRegionEnd] - searchRTRange,
                  mobilities[currentRegionEnd] + searchRTRange);

          // Search if there is lower data point on the left from
          // current peak i.
          for (int i = currentRegionEnd - 1; i > 0; i--) {

            if (!checkRange.contains(mobilities[i])) {
              break;
            }

            if (intensities[i] < intensities[currentRegionEnd]) {

              continue endSearch;
            }
          }

          // Search on the right from current peak i.
          for (int i = currentRegionEnd + 1; i < scanCount; i++) {

            if (!checkRange.contains(mobilities[i])) {
              break;
            }

            if (intensities[i] < intensities[currentRegionEnd]) {

              continue endSearch;
            }
          }

          // Find the intensity at the sides (lowest data points).
          final double peakMinLeft = intensities[currentRegionStart];
          final double peakMinRight = intensities[currentRegionEnd];

          // If we have reached a minimum which is non-zero, but
          // the peak shape would not fulfill the
          // ratio condition, continue searching for next minimum.
          if (currentRegionHeight >= peakMinRight * minRatio) {

            // Check the shape of the peak.
            if (currentRegionHeight >= minHeight && currentRegionHeight >= peakMinLeft * minRatio
                && currentRegionHeight >= peakMinRight * minRatio && peakDuration.contains(
                mobilities[currentRegionEnd] - mobilities[currentRegionStart])) {

              int finalCurrentRegionStart = currentRegionStart;
              int finalCurrentRegionEnd = currentRegionEnd;
//              resolved.add(Arrays.stream(identifiers)
//                  .filter(id -> (id >= finalCurrentRegionStart && id <= finalCurrentRegionEnd))
//                  .boxed().collect(Collectors.toSet()));

              Set<Integer> set = new HashSet<>();
              for (Integer id : indices) {
                if (id.intValue() >= finalCurrentRegionStart
                    && id.intValue() <= finalCurrentRegionEnd) {
                  set.add(id);
                }
              }
              resolved.add(set);
            }

            // Set the next region start to current region end-1
            // because it will be immediately
            // increased +1 as we continue the for-cycle.
            currentRegionStart = currentRegionEnd - 1;
            continue startSearch;
          }
        }
      }
    }

    return resolved;
  }

}
