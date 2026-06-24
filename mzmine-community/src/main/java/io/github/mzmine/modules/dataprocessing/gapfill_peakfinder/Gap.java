/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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
import io.github.mzmine.datamodel.features.types.gapfilling.GapFillMzMatchDuplicateType;
import io.github.mzmine.util.DataPointUtils;
import io.github.mzmine.util.RangeUtils;
import io.github.mzmine.util.collections.BinarySearch;
import io.github.mzmine.util.collections.BinarySearch.DefaultTo;
import io.github.mzmine.util.maths.Precision;
import io.github.mzmine.util.scans.ScanUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

public class Gap {

  protected FeatureListRow featureListRow;
  protected RawDataFile rawDataFile;

  protected Range<Double> mzRange;
  protected Range<Float> rtRange;
  private final boolean validateRtShape;
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
    this(peakListRow, rawDataFile, mzRange, rtRange, intTolerance, true);
  }

  public Gap(FeatureListRow peakListRow, RawDataFile rawDataFile, Range<Double> mzRange,
      Range<Float> rtRange, double intTolerance, boolean validateRtShape) {

    this.featureListRow = peakListRow;
    this.rawDataFile = rawDataFile;
    this.intTolerance = intTolerance;
    this.mzRange = mzRange;
    this.rtRange = rtRange;
    this.validateRtShape = validateRtShape;
  }

  public void offerNextScan(Scan scan) {

    float scanRT = scan.getRetentionTime();

    // If not yet inside the RT range
    if (!rtRange.contains(scanRT)) {
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
        ((ModularFeatureList) featureListRow.getFeatureList()).getMemoryMapStorage(),
        mzIntensities[0], mzIntensities[1],
        bestPeakDataPoints.stream().map(GapDataPoint::getScan).toList());

    final ModularFeature newPeak = new ModularFeature(
        (ModularFeatureList) featureListRow.getFeatureList(), rawDataFile, series,
        FeatureStatus.ESTIMATED);

    tagPotentialDuplicates(newPeak);

    // Fill the gap
    featureListRow.addFeature(rawDataFile, newPeak, false);
    return true;
  }

  /**
   * Tags this gap-filled feature when it is a potential duplicate of another row's feature from the
   * same raw data file (caused by (mis)alignment during gap filling). Sets two feature data types:
   * and {@link GapFillMzMatchDuplicateType} (another row within this gap's m/z and RT window holds
   * a feature of the same file, additionally, &ge; 50 % of the gap-filled feature's m/z values
   * match such a feature, compared per scan).
   *
   * @param newFeature the freshly gap-filled feature
   */
  protected void tagPotentialDuplicates(@NotNull final ModularFeature newFeature) {
    final ModularFeatureList flist = (ModularFeatureList) featureListRow.getFeatureList();
    // make the columns available in the feature table (addFeatureType de-dupes by class)
    flist.addFeatureType(new GapFillMzMatchDuplicateType());

    final List<FeatureListRow> candidates = flist.getRowsInsideScanAndMZRange(rtRange, mzRange);

    final List<Integer> mzIds = new ArrayList<>();
    for (final FeatureListRow other : candidates) {
      if (other == featureListRow) {
        continue;
      }
      final Feature otherFeature = other.getFeature(rawDataFile);
      if (otherFeature == null) {
        continue;
      }
      // decision: only real peaks in other rows count as duplicates, not other gap-fills. This also
      // keeps the result deterministic regardless of multithreaded gap-fill order.
      final FeatureStatus status = otherFeature.getFeatureStatus();
      if (status != FeatureStatus.DETECTED && status != FeatureStatus.MANUAL) {
        continue;
      }
      if (fractionOfEqualMz(newFeature, otherFeature) >= 0.5) {
        mzIds.add(other.getID());
      }
    }

    newFeature.set(GapFillMzMatchDuplicateType.class, formatTag(mzIds));
  }

  /**
   * Fraction of the gap-filled feature's data points whose scan (matched by retention time) is also
   * present in the other feature with an equal m/z. m/z is unaffected by smoothing, so a true
   * duplicate matches. Overridden in {@code ImsGap} for ion mobility data.
   *
   * @param gapFilled the gap-filled feature
   * @param other     a real feature from the same raw data file in another row
   * @return fraction in [0, 1]
   */
  protected double fractionOfEqualMz(@NotNull final Feature gapFilled,
      @NotNull final Feature other) {
    final IonTimeSeries<?> a = gapFilled.getFeatureData();
    final IonTimeSeries<?> b = other.getFeatureData();
    final int n = a.getNumberOfValues();
    final int m = b.getNumberOfValues();
    if (n == 0 || m == 0) {
      return 0d;
    }
    int matches = 0;
    for (int i = 0; i < n; i++) {
      // same scan = same RT (b is sorted ascending in RT); negative result means no shared scan
      final int j = BinarySearch.binarySearch(a.getRetentionTime(i),
          DefaultTo.MINUS_INSERTION_POINT, m, b::getRetentionTime);
//      if (j >= 0 && MZTolerance.NARROW_5_PPM_OR_1_MDA.checkWithinTolerance(a.getMZ(i), b.getMZ(j))) {
      if (j >= 0 && Precision.equalFloatSignificance(a.getMZ(i), b.getMZ(j))) {
        matches++;
      }
    }
    return (double) matches / n;
  }

  private static @NotNull String formatTag(@NotNull final List<Integer> ids) {
    return ids.isEmpty() ? "no"
        : "yes (IDs: " + ids.stream().map(String::valueOf).collect(Collectors.joining(", ")) + ")";
  }

  /**
   * This function check for the shape of the peak in RT direction, and determines if it is possible
   * to add given m/z peak at the end of the peak.
   */
  protected boolean checkRTShape(GapDataPoint dp) {
    if (!validateRtShape) {
      return true;
    }

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

  public FeatureListRow getFeatureListRow() {
    return featureListRow;
  }
}
