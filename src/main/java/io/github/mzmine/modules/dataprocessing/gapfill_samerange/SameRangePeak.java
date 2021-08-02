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

package io.github.mzmine.modules.dataprocessing.gapfill_samerange;

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
import java.text.Format;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import org.jetbrains.annotations.NotNull;

/**
 * This class represents a manually picked chromatographic peak.
 */
public class SameRangePeak{
  private SimpleFeatureInformation peakInfo;

  private RawDataFile dataFile;

  // Raw M/Z, RT, Height and Area
  private double mz, rt, height, area;
  private Double fwhm = null, tf = null, af = null;

  // Boundaries of the peak
  private Range<Double> mzRange;
  private Range<Float> rtRange, intensityRange;

  // Map of scan number and data point
  private TreeMap<Scan, DataPoint> mzPeakMap;

  // Number of most intense fragment scan
  private Scan fragmentScan, representativeScan;

  // Numbers of all MS2 fragment scans
  private Scan[] allMS2FragmentScanNumbers;

  // Isotope pattern. Null by default but can be set later by deisotoping
  // method.
  private IsotopePattern isotopePattern;
  private int charge = 0;

  /**
   * Initializes empty peak for adding data points
   */
  SameRangePeak(RawDataFile dataFile) {
    this.dataFile = dataFile;
    mzPeakMap = new TreeMap<>();
  }

  /**
   * This peak is always a result of manual feature detection, therefore MANUAL
   */
  public @NotNull FeatureStatus getFeatureStatus() {
    return FeatureStatus.ESTIMATED;
  }

  /**
   * This method returns M/Z value of the peak
   */
  public double getMZ() {
    return mz;
  }

  /**
   * This method returns retention time of the peak
   */
  public double getRT() {
    return rt;
  }

  /**
   * This method returns the raw height of the peak
   */
  public double getHeight() {
    return height;
  }

  /**
   * This method returns the raw area of the peak
   */
  public double getArea() {
    return area;
  }

  /**
   * This method returns numbers of scans that contain this peak
   */
  public @NotNull Scan[] getScanNumbers() {
    return mzPeakMap.keySet().toArray(Scan[]::new);
  }

  /**
   * This method returns a representative datapoint of this peak in a given scan
   */
  public DataPoint getDataPoint(Scan scan) {
    return mzPeakMap.get(scan);
  }

  public @NotNull Range<Float> getRawDataPointsIntensityRange() {
    return intensityRange;
  }

  public @NotNull Range<Double> getRawDataPointsMZRange() {
    return mzRange;
  }

  public @NotNull Range<Float> getRawDataPointsRTRange() {
    return rtRange;
  }

  public @NotNull RawDataFile getRawDataFile() {
    return dataFile;
  }

  /**
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    StringBuffer buf = new StringBuffer();
    Format mzFormat = MZmineCore.getConfiguration().getMZFormat();
    Format timeFormat = MZmineCore.getConfiguration().getRTFormat();
    buf.append("m/z ");
    buf.append(mzFormat.format(getMZ()));
    buf.append(" (");
    buf.append(timeFormat.format(getRT()));
    buf.append(" min) [" + getRawDataFile().getName() + "]");
    return buf.toString();
  }

  /**
   * Adds a new data point to this peak
   *
   * @param scan
   * @param dataPoint
   */
  void addDatapoint(Scan scan, DataPoint dataPoint) {
    float rt = scan.getRetentionTime();

    if (mzPeakMap.isEmpty()) {
      rtRange = Range.singleton(rt);
      mzRange = Range.singleton(dataPoint.getMZ());
      intensityRange = Range.singleton((float) dataPoint.getIntensity());
    } else {
      rtRange = rtRange.span(Range.singleton(rt));
      mzRange = mzRange.span(Range.singleton(dataPoint.getMZ()));
      intensityRange = intensityRange.span(Range.singleton((float) dataPoint.getIntensity()));
    }

    mzPeakMap.put(scan, dataPoint);
  }

  void finalizePeak() {

    // Trim the zero-intensity data points from the beginning and end
    while (!mzPeakMap.isEmpty()) {
      Scan scanNumber = mzPeakMap.firstKey();
      if (mzPeakMap.get(scanNumber).getIntensity() > 0)
        break;
      mzPeakMap.remove(scanNumber);
    }
    while (!mzPeakMap.isEmpty()) {
      Scan scanNumber = mzPeakMap.lastKey();
      if (mzPeakMap.get(scanNumber).getIntensity() > 0)
        break;
      mzPeakMap.remove(scanNumber);
    }

    // Check if we have any data points
    if (mzPeakMap.isEmpty()) {
      throw (new IllegalStateException("Peak can not be finalized without any data points"));
    }

    // Get all scan numbers
    Entry<Scan, DataPoint> [] allScanNumbers = mzPeakMap.entrySet().toArray(Entry[]::new);

    // Find the data point with top intensity and use its RT and height
    for(Map.Entry<Scan, DataPoint> entry : allScanNumbers) {
      DataPoint dataPoint = entry.getValue();
      double rt = entry.getKey().getRetentionTime();
      if (dataPoint.getIntensity() > height) {
        height = dataPoint.getIntensity();
        representativeScan = entry.getKey();
        this.rt = rt;
      }
    }

    // Calculate peak area
    area = 0;
    for (int i = 1; i < allScanNumbers.length; i++) {
      // For area calculation, we use retention time in seconds
      double previousRT = allScanNumbers[i - 1].getKey().getRetentionTime() * 60d;
      double currentRT = allScanNumbers[i].getKey().getRetentionTime() * 60d;

      double rtDifference = currentRT - previousRT;

      // Intensity at the beginning and end of the interval
      double previousIntensity = allScanNumbers[i - 1].getValue().getIntensity();
      double thisIntensity = allScanNumbers[i].getValue().getIntensity();
      double averageIntensity = (previousIntensity + thisIntensity) / 2;

      // Calculate area of the interval
      area += (rtDifference * averageIntensity);
    }

    // Calculate median MZ
    double mzArray[] = new double[allScanNumbers.length];
    for (int i = 0; i < allScanNumbers.length; i++) {
      mzArray[i] = allScanNumbers[i].getValue().getMZ();
    }
    this.mz = MathUtils.calcQuantile(mzArray, 0.5f);

    fragmentScan = ScanUtils.findBestFragmentScan(dataFile, rtRange, mzRange);
    allMS2FragmentScanNumbers = ScanUtils.findAllMS2FragmentScans(dataFile, rtRange, mzRange);

    if (fragmentScan != null) {
      int precursorCharge = fragmentScan.getPrecursorCharge();
      if ((precursorCharge > 0) && (this.charge == 0))
        this.charge = precursorCharge;
    }
  }

  public void setMZ(double mz) {
    this.mz = mz;
  }

  public Scan getRepresentativeScanNumber() {
    return representativeScan;
  }

  public Scan getMostIntenseFragmentScanNumber() {
    return fragmentScan;
  }

  public Scan[] getAllMS2FragmentScanNumbers() {
    return allMS2FragmentScanNumbers;
  }

  public IsotopePattern getIsotopePattern() {
    return isotopePattern;
  }

  public void setIsotopePattern(@NotNull IsotopePattern isotopePattern) {
    this.isotopePattern = isotopePattern;
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

  // dulab Edit
  public void outputChromToFile() {
    int nothing = -1;
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
        if (tic < scan.getTIC())
          best = scan;
      }
    }
    setFragmentScanNumber(best);
  }
  // End dulab Edit

  private FeatureList peakList;

  public FeatureList getPeakList() {
    return peakList;
  }

  public void setPeakList(FeatureList peakList) {
    this.peakList = peakList;
  }


  public Collection<DataPoint> getDataPoints() {
    return mzPeakMap.values();
  }
}
