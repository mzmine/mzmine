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

package io.github.mzmine.modules.dataprocessing.featdet_targeted;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.util.scans.ScanUtils;
import java.util.List;
import java.util.Vector;

class Gap {

  private FeatureListRow peakListRow;
  private RawDataFile rawDataFile;
  private Range<Double> mzRange;
  private Range<Float> rtRange;
  // These store information about peak that is currently under construction
  private List<GapDataPoint> currentPeakDataPoints;
  private List<GapDataPoint> bestPeakDataPoints;
  private double bestPeakHeight;
  private double intTolerance;
  private double noiseLevel;

  /**
   * Constructor: Initializes an empty gap
   *
   * @param mzRange M/Z coordinate of this empty gap
   * @param rtRange RT coordinate of this empty gap
   */
  Gap(FeatureListRow peakListRow, RawDataFile rawDataFile, Range<Double> mzRange,
      Range<Float> rtRange, double intTolerance, double noiseLevel) {

    this.peakListRow = peakListRow;
    this.rawDataFile = rawDataFile;
    this.mzRange = mzRange;
    this.rtRange = rtRange;
    this.intTolerance = intTolerance;
    this.noiseLevel = noiseLevel;
  }

  void offerNextScan(Scan scan) {

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

    GapDataPoint currentDataPoint;
    if (basePeak != null) {
      currentDataPoint =
          new GapDataPoint(scan, basePeak.getMZ(), scanRT, basePeak.getIntensity());
    } else {
      final double mzCenter = (mzRange.lowerEndpoint() + mzRange.upperEndpoint()) / 2.0;
      currentDataPoint = new GapDataPoint(scan, mzCenter, scanRT, 0);
    }

    // If we have not yet started, just create a new peak
    if (currentPeakDataPoints == null) {
      currentPeakDataPoints = new Vector<GapDataPoint>();
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

  public void noMoreOffers() {

    // Check peak that was last constructed
    if (currentPeakDataPoints != null) {
      checkCurrentPeak();
      currentPeakDataPoints = null;
    }

    // If we have best peak candidate, construct a SimpleChromatographicPeak
    if (bestPeakDataPoints != null) {

      double mz = 0;
      float rt = 0, area = 0, height = 0;
      Scan scanNumbers[] = new Scan[bestPeakDataPoints.size()];
      DataPoint finalDataPoint[] = new DataPoint[bestPeakDataPoints.size()];
      Range<Double> finalMZRange = null;
      Range<Float> finalRTRange = null, finalIntensityRange = null;
      Scan representativeScan = null;

      // Process all datapoints
      for (int i = 0; i < bestPeakDataPoints.size(); i++) {

        GapDataPoint dp = bestPeakDataPoints.get(i);

        if (i == 0) {
          finalRTRange = Range.singleton((float) dp.getRT());
          finalMZRange = Range.singleton(dp.getMZ());
          finalIntensityRange = Range.singleton((float) dp.getIntensity());
        } else {
          assert finalRTRange != null && finalMZRange != null && finalIntensityRange != null;
          finalRTRange = finalRTRange.span(Range.singleton((float) dp.getRT()));
          finalMZRange = finalMZRange.span(Range.singleton(dp.getMZ()));
          finalIntensityRange = finalIntensityRange.span(Range.singleton((float) dp.getIntensity()));
        }

        scanNumbers[i] = bestPeakDataPoints.get(i).getScan();
        finalDataPoint[i] = new SimpleDataPoint(dp.getMZ(), dp.getIntensity());
        mz += bestPeakDataPoints.get(i).getMZ();

        // Check height
        if (bestPeakDataPoints.get(i).getIntensity() > height) {
          height = (float) bestPeakDataPoints.get(i).getIntensity();
          rt = (float) bestPeakDataPoints.get(i).getRT();
          representativeScan = bestPeakDataPoints.get(i).getScan();
        }

        // Skip last data point
        if (i == bestPeakDataPoints.size() - 1) {
          break;
        }

        // X axis interval length
        double rtDifference =
            bestPeakDataPoints.get(i + 1).getRT() - bestPeakDataPoints.get(i).getRT();

        // Convert the RT scale to seconds
        rtDifference *= 60d;

        // intensity at the beginning and end of the interval
        double intensityStart = bestPeakDataPoints.get(i).getIntensity();
        double intensityEnd = bestPeakDataPoints.get(i + 1).getIntensity();

        // calculate area of the interval
        area += (float) (rtDifference * (intensityStart + intensityEnd) / 2);

      }

      // Calculate average m/z value
      mz /= bestPeakDataPoints.size();

      // Find all MS2 fragment scans, if available
      List<Scan> allMS2fragmentScanNumbers = ScanUtils.streamAllMS2FragmentScans(rawDataFile,
          finalRTRange, finalMZRange).toList();

      // Is intensity above the noise level?
      if (height >= noiseLevel) {
        ModularFeature newPeak = new ModularFeature(
            (ModularFeatureList) peakListRow.getFeatureList(), rawDataFile, mz, rt, height, area,
            scanNumbers, finalDataPoint, FeatureStatus.ESTIMATED, representativeScan,
            allMS2fragmentScanNumbers, finalRTRange, finalMZRange, finalIntensityRange);

        // Fill the gap
        peakListRow.addFeature(rawDataFile, newPeak);
      }
    }

  }

  /**
   * This function check for the shape of the peak in RT direction, and determines if it is possible
   * to add given m/z peak at the end of the peak.
   */
  private boolean checkRTShape(GapDataPoint dp) {

    if (dp.getRT() < rtRange.lowerEndpoint()) {
      double prevInt = currentPeakDataPoints.get(currentPeakDataPoints.size() - 1).getIntensity();
      if (dp.getIntensity() > prevInt * (1 - intTolerance)) {
        return true;
      }
    }

    if (rtRange.contains((float) dp.getRT())) {
      return true;
    }

    if (dp.getRT() > rtRange.upperEndpoint()) {
      double prevInt = currentPeakDataPoints.get(currentPeakDataPoints.size() - 1).getIntensity();
      if (dp.getIntensity() < prevInt * (1 + intTolerance)) {
        return true;
      }
    }

    return false;

  }

  private void checkCurrentPeak() {

    // 1) Check if currentpeak has a local maximum inside the search range
    int highestMaximumInd = -1;
    double currentMaxHeight = 0f;
    for (int i = 1; i < currentPeakDataPoints.size() - 1; i++) {

      if (rtRange.contains((float) currentPeakDataPoints.get(i).getRT())) {

        if ((currentPeakDataPoints.get(i).getIntensity() >= currentPeakDataPoints.get(i + 1)
            .getIntensity())
            && (currentPeakDataPoints.get(i).getIntensity() >= currentPeakDataPoints.get(i - 1)
                .getIntensity())) {

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
      if (currentInt < nextInt * (1 - intTolerance)) {
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
      if (nextInt > currentInt * (1 + intTolerance)) {
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
