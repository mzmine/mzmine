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

package io.github.mzmine.modules.dataprocessing.featdet_chromatogrambuilder;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.impl.SimpleFeatureInformation;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.util.MathUtils;
import io.github.mzmine.util.scans.ScanUtils;
import java.util.Collection;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Vector;
import org.jetbrains.annotations.NotNull;

/**
 * Chromatogram implementing ChromatographicPeak.
 */
public class Chromatogram{

  private SimpleFeatureInformation peakInfo;

  // Data file of this chromatogram
  private RawDataFile dataFile;

  // Data points of the chromatogram (map of scan number -> m/z peak)
  private Hashtable<Scan, DataPoint> dataPointsMap;

  // Chromatogram m/z, RT, height, area
  private double mz, rt, height, area;
  private Double fwhm = null, tf = null, af = null;

  // Top intensity scan, fragment scan
  private Scan representativeScan = null;
  private Scan fragmentScan = null;

  // All MS2 fragment scan numbers
  private Scan[] allMS2FragmentScanNumbers = new Scan[] {};

  // Ranges of raw data points
  private Range<Double> rawDataPointsMZRange;
  private Range<Float> rawDataPointsRTRange, rawDataPointsIntensityRange;

  // A set of scan numbers of a segment which is currently being connected
  private Vector<Scan> buildingSegment;

  // Keep track of last added data point
  private DataPoint lastMzPeak;

  // Number of connected segments, which have been committed by
  // commitBuildingSegment()
  private int numOfCommittedSegments = 0;

  // Isotope pattern. Null by default but can be set later by deisotoping
  // method.
  private IsotopePattern isotopePattern;
  private int charge = 0;

  // Victor Trevino
  private double mzSum = 0;
  private int mzN = 0;

  private final Scan scanNumbers[];

  public void outputChromToFile() {
    System.out.println("does nothing");
  }

  /**
   * Initializes this Chromatogram
   */
  public Chromatogram(RawDataFile dataFile, Scan scanNumbers[]) {
    this.dataFile = dataFile;
    this.scanNumbers = scanNumbers;

    rawDataPointsRTRange = dataFile.getDataRTRange(1);

    dataPointsMap = new Hashtable<Scan, DataPoint>();
    buildingSegment = new Vector<Scan>(128);
  }

  /**
   * This method adds a MzPeak to this Chromatogram. All values of this Chromatogram (rt, m/z,
   * intensity and ranges) are updated on request
   *
   * @param mzValue
   */
  public void addMzPeak(Scan scanNumber, DataPoint mzValue) {
    dataPointsMap.put(scanNumber, mzValue);
    lastMzPeak = mzValue;
    mzSum += mzValue.getMZ();
    mzN++;
    mz = mzSum / mzN;
    buildingSegment.add(scanNumber);
  }

  public DataPoint getDataPoint(Scan scanNumber) {
    return dataPointsMap.get(scanNumber);
  }

  /**
   * Returns m/z value of last added data point
   */
  public DataPoint getLastMzPeak() {
    return lastMzPeak;
  }

  /**
   * This method returns m/z value of the chromatogram
   */
  public double getMZ() {
    return mz;
  }

  /**
   * This method returns a string with the basic information that defines this peak
   *
   * @return String information
   */
  @Override
  public String toString() {
    return "Chromatogram " + MZmineCore.getConfiguration().getMZFormat().format(mz) + " m/z";
  }

  public double getArea() {
    return area;
  }

  public double getHeight() {
    return height;
  }

  public Scan getMostIntenseFragmentScanNumber() {
    return fragmentScan;
  }

  public Scan[] getAllMS2FragmentScanNumbers() {
    return allMS2FragmentScanNumbers;
  }

  public @NotNull FeatureStatus getFeatureStatus() {
    return FeatureStatus.DETECTED;
  }

  public double getRT() {
    return rt;
  }

  public @NotNull Range<Float> getRawDataPointsIntensityRange() {
    return rawDataPointsIntensityRange;
  }

  public @NotNull Range<Double> getRawDataPointsMZRange() {
    return rawDataPointsMZRange;
  }

  public @NotNull Range<Float> getRawDataPointsRTRange() {
    return rawDataPointsRTRange;
  }

  public Scan getRepresentativeScanNumber() {
    return representativeScan;
  }

  public @NotNull Scan[] getScanNumbers() {
    return scanNumbers;
  }

  public @NotNull RawDataFile getRawDataFile() {
    return dataFile;
  }

  public IsotopePattern getIsotopePattern() {
    return isotopePattern;
  }

  public void setIsotopePattern(@NotNull IsotopePattern isotopePattern) {
    this.isotopePattern = isotopePattern;
  }

  public void finishChromatogram() {

    Scan allScanNumbers[] = dataPointsMap.keySet().stream().sorted(
        Comparator.comparingInt(Scan::getScanNumber)).toArray(Scan[]::new);

    // Calculate median m/z
    double allMzValues[] = new double[allScanNumbers.length];
    for (int i = 0; i < allScanNumbers.length; i++) {
      allMzValues[i] = dataPointsMap.get(allScanNumbers[i]).getMZ();
    }
    mz = MathUtils.calcQuantile(allMzValues, 0.5f);

    // Update raw data point ranges, height, rt and representative scan
    height = Double.MIN_VALUE;
    for (int i = 0; i < allScanNumbers.length; i++) {

      DataPoint mzPeak = dataPointsMap.get(allScanNumbers[i]);

      // Replace the MzPeak instance with an instance of SimpleDataPoint,
      // to reduce the memory usage. After we finish this Chromatogram, we
      // don't need the additional data provided by the MzPeak

      dataPointsMap.put(allScanNumbers[i], mzPeak);

      if (i == 0) {
        rawDataPointsIntensityRange = Range.singleton((float) mzPeak.getIntensity());
        rawDataPointsMZRange = Range.singleton(mzPeak.getMZ());
      } else {
        rawDataPointsIntensityRange =
            rawDataPointsIntensityRange.span(Range.singleton((float) mzPeak.getIntensity()));
        rawDataPointsMZRange = rawDataPointsMZRange.span(Range.singleton(mzPeak.getMZ()));
      }

      if (height < mzPeak.getIntensity()) {
        height = mzPeak.getIntensity();
        rt = allScanNumbers[i].getRetentionTime();
        representativeScan = allScanNumbers[i];
      }
    }

    // Update area
    area = 0;
    for (int i = 1; i < allScanNumbers.length; i++) {
      // For area calculation, we use retention time in seconds
      double previousRT = allScanNumbers[i - 1].getRetentionTime() * 60d;
      double currentRT = allScanNumbers[i].getRetentionTime() * 60d;
      double previousHeight = dataPointsMap.get(allScanNumbers[i - 1]).getIntensity();
      double currentHeight = dataPointsMap.get(allScanNumbers[i]).getIntensity();
      area += (currentRT - previousRT) * (currentHeight + previousHeight) / 2;
    }

    // Update fragment scan
    fragmentScan = ScanUtils.streamAllMS2FragmentScans(dataFile, dataFile.getDataRTRange(1),
        rawDataPointsMZRange).findFirst().orElse(null);

    if (fragmentScan != null) {
      int precursorCharge = Objects.requireNonNullElse(fragmentScan.getPrecursorCharge(), 0);
      if (precursorCharge > 0)
        this.charge = precursorCharge;
    }

    rawDataPointsRTRange = null;

    for (Scan scanNum : allScanNumbers) {
      double scanRt = scanNum.getRetentionTime();
      DataPoint dp = getDataPoint(scanNum);

      if ((dp == null) || (dp.getIntensity() == 0.0))
        continue;

      if (rawDataPointsRTRange == null)
        rawDataPointsRTRange = Range.singleton((float) scanRt);
      else
        rawDataPointsRTRange = rawDataPointsRTRange.span(Range.singleton((float) scanRt));
    }

    // Discard the fields we don't need anymore
    buildingSegment = null;
    lastMzPeak = null;

  }

  public double getBuildingSegmentLength() {
    if (buildingSegment.size() < 2)
      return 0;
    Scan firstScan = buildingSegment.firstElement();
    Scan lastScan = buildingSegment.lastElement();
    double firstRT = firstScan.getRetentionTime();
    double lastRT = lastScan.getRetentionTime();
    return (lastRT - firstRT);
  }

  public int getNumberOfCommittedSegments() {
    return numOfCommittedSegments;
  }

  public void removeBuildingSegment() {
    for (Scan scanNumber : buildingSegment)
      dataPointsMap.remove(scanNumber);
    buildingSegment.clear();
  }

  public void commitBuildingSegment() {
    buildingSegment.clear();
    numOfCommittedSegments++;
  }

  public void addDataPointsFromChromatogram(Chromatogram ch) {
    for (Entry<Scan, DataPoint> dp : ch.dataPointsMap.entrySet()) {
      addMzPeak(dp.getKey(), dp.getValue());
    }
  }

  public int getCharge() {
    return charge;
  }

  public void setCharge(int charge) {
    this.charge = charge;
  }

  public Double getFWHM() {
    return fwhm;
  }

  public void setFWHM(Double fwhm) {
    this.fwhm = fwhm;
  }

  public Double getTailingFactor() {
    return tf;
  }

  public void setTailingFactor(Double tf) {
    this.tf = tf;
  }

  public Double getAsymmetryFactor() {
    return af;
  }

  public void setAsymmetryFactor(Double af) {
    this.af = af;
  }

  public void setPeakInformation(SimpleFeatureInformation peakInfoIn) {
    this.peakInfo = peakInfoIn;
  }

  public SimpleFeatureInformation getPeakInformation() {
    return peakInfo;
  }

  public void setFragmentScanNumber(Scan fragmentScanNumber) {
    this.fragmentScan = fragmentScanNumber;
  }

  public void setAllMS2FragmentScanNumbers(Scan[] allMS2FragmentScanNumbers) {
    this.allMS2FragmentScanNumbers = allMS2FragmentScanNumbers;
    // also set best scan by TIC
    Scan best = null;
    double tic = 0;
    if (allMS2FragmentScanNumbers != null) {
      for (Scan scan : allMS2FragmentScanNumbers) {
        if (tic < scan.getTIC()) {
          best = scan;
          tic = scan.getTIC();
        }
      }
    }
    setFragmentScanNumber(best);
  }

  private FeatureList peakList;

  public FeatureList getPeakList() {
    return peakList;
  }

  public void setPeakList(FeatureList peakList) {
    this.peakList = peakList;
  }

  public Collection<DataPoint> getDataPoints() {
    return dataPointsMap.values();
  }

  public Hashtable<Scan, DataPoint> getDataPointsMap() {
    return dataPointsMap;
  }
}
