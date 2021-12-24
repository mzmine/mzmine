/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.modules.dataprocessing.gapfill_peakfinder;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.featuredata.impl.SimpleIonTimeSeries;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.util.DataPointUtils;
import io.github.mzmine.util.RangeUtils;
import io.github.mzmine.util.scans.ScanUtils;
import java.util.ArrayList;
import java.util.List;

public class Gap {

  protected FeatureListRow peakListRow;
  protected RawDataFile rawDataFile;

  protected Range<Double> mzRange;
  protected Range<Float> rtRange;
  protected double intTolerance;

  // These store information about peak that is currently under construction
  protected List<GapDataPoint> currentPeakDataPoints;
  protected List<GapDataPoint> bestPeakDataPoints;
  protected double bestPeakHeight;

  /**
   * Constructor: Initializes an empty gap
   *
   * @param mzRange M/Z coordinate of this empty gap
   * @param rtRange RT coordinate of this empty gap
   */
  public Gap(FeatureListRow peakListRow, RawDataFile rawDataFile, Range<Double> mzRange,
      Range<Float> rtRange, double intTolerance) {

    this.peakListRow = peakListRow;
    this.rawDataFile = rawDataFile;
    this.intTolerance = intTolerance;
    this.mzRange = mzRange;
    this.rtRange = rtRange;
  }

  public void offerNextScan(Scan scan) {

    double scanRT = scan.getRetentionTime();

    // If not yet inside the RT range
    if (scanRT < rtRange.lowerEndpoint()) {
      return;
    }

    // If we have passed the RT range and finished processing last peak
    if ((scanRT > rtRange.upperEndpoint()) && (currentPeakDataPoints == null)) {
      return;
    }

    // Find top m/z peak in our range
    DataPoint basePeak = ScanUtils.findBasePeak(scan, mzRange);

    GapDataPointImpl currentDataPoint;
    if (basePeak != null) {
      currentDataPoint = new GapDataPointImpl(scan, basePeak.getMZ(), scanRT,
          basePeak.getIntensity());
    } else {
      currentDataPoint = new GapDataPointImpl(scan, RangeUtils.rangeCenter(mzRange), scanRT, 0);
    }

    // If we have not yet started, just create a new peak
    if (currentPeakDataPoints == null) {
      currentPeakDataPoints = new ArrayList<>();
      currentPeakDataPoints.add(currentDataPoint);
      return;
    }

    // Check if this continues previous peak?
    if (checkRTShape(currentDataPoint)) {
      // Yes, continue this peak.
      currentPeakDataPoints.add(currentDataPoint);
    } else {

      // No, new peak is starting

      // Check peak formed so far
      if (currentPeakDataPoints != null) {
        checkCurrentPeak();
        currentPeakDataPoints = null;
      }

    }

  }

  /**
   * Finalizes the gap, adds a peak
   */
  public boolean noMoreOffers() {
    return noMoreOffers(1);
  }

  /**
   * Finalizes the gap, adds a peak
   */
  public boolean noMoreOffers(int minDataPoints) {

    // Check peak that was last constructed
    if (currentPeakDataPoints != null) {
      checkCurrentPeak();
      currentPeakDataPoints = null;
    }

    // does not meet filters
    if (bestPeakDataPoints == null || bestPeakDataPoints.size() < minDataPoints) {
      return false;
    }

    return addFeatureToRow();
  }

  protected boolean addFeatureToRow() {
    final double[][] mzIntensities = DataPointUtils.getDataPointsAsDoubleArray(bestPeakDataPoints);
    final IonTimeSeries<?> series = new SimpleIonTimeSeries(
        ((ModularFeatureList) peakListRow.getFeatureList()).getMemoryMapStorage(), mzIntensities[0],
        mzIntensities[1], bestPeakDataPoints.stream().map(GapDataPoint::getScan).toList());

    final Feature newPeak = new ModularFeature((ModularFeatureList) peakListRow.getFeatureList(),
        rawDataFile, series, FeatureStatus.MANUAL);

    // Fill the gap
    peakListRow.addFeature(rawDataFile, newPeak);
    return true;
  }

  /**
   * This function check for the shape of the peak in RT direction, and determines if it is possible
   * to add given m/z peak at the end of the peak.
   */
  protected boolean checkRTShape(GapDataPoint dp) {

    if (dp.getRT() < rtRange.lowerEndpoint()) {
      double prevInt = currentPeakDataPoints.get(currentPeakDataPoints.size() - 1).getIntensity();
      if (dp.getIntensity() > (prevInt * (1 - intTolerance))) {
        return true;
      }
    }

    if (rtRange.contains((float) dp.getRT())) {
      return true;
    }

    if (dp.getRT() > rtRange.upperEndpoint()) {
      double prevInt = currentPeakDataPoints.get(currentPeakDataPoints.size() - 1).getIntensity();
      if (dp.getIntensity() < (prevInt * (1 + intTolerance))) {
        return true;
      }
    }

    return false;

  }

  protected void checkCurrentPeak() {

    // 1) Check if currentpeak has a local maximum inside the search range
    int highestMaximumInd = -1;
    double currentMaxHeight = 0f;
    for (int i = 1; i < currentPeakDataPoints.size() - 1; i++) {

      if (rtRange.contains((float) currentPeakDataPoints.get(i).getRT())) {

        if ((currentPeakDataPoints.get(i).getIntensity() >= currentPeakDataPoints.get(i + 1)
            .getIntensity()) && (currentPeakDataPoints.get(i).getIntensity()
                                 >= currentPeakDataPoints.get(i - 1).getIntensity())) {

          if (currentPeakDataPoints.get(i).getIntensity() > currentMaxHeight) {

            currentMaxHeight = currentPeakDataPoints.get(i).getIntensity();
            highestMaximumInd = i;
          }
        }
      }
    }

    // If no local maximum, return
    if (highestMaximumInd == -1) {
      return;
    }

    // 2) Find elution start and stop
    int startInd = highestMaximumInd;
    double currentInt = currentPeakDataPoints.get(startInd).getIntensity();
    while (startInd > 0) {
      double nextInt = currentPeakDataPoints.get(startInd - 1).getIntensity();
      if (currentInt < (nextInt * (1 - intTolerance))) {
        break;
      }
      startInd--;
      if (nextInt == 0) {
        break;
      }
      currentInt = nextInt;
    }

    // Since subList does not include toIndex value then find highest
    // possible value of stopInd+1 and currentPeakDataPoints.size()
    int stopInd = highestMaximumInd, toIndex = highestMaximumInd;
    currentInt = currentPeakDataPoints.get(stopInd).getIntensity();
    while (stopInd < (currentPeakDataPoints.size() - 1)) {
      double nextInt = currentPeakDataPoints.get(stopInd + 1).getIntensity();
      if (nextInt > (currentInt * (1 + intTolerance))) {
        toIndex = Math.min(currentPeakDataPoints.size(), stopInd + 1);
        break;
      }
      stopInd++;
      toIndex = Math.min(currentPeakDataPoints.size(), stopInd + 1);
      if (nextInt == 0) {
        stopInd++;
        toIndex = stopInd;
        break;
      }
      currentInt = nextInt;
    }

    // 3) Check if this is the best candidate for a peak
    if ((bestPeakDataPoints == null) || (bestPeakHeight < currentMaxHeight)) {
      bestPeakDataPoints = currentPeakDataPoints.subList(startInd, toIndex);
    }

  }

}
