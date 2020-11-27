/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.dataprocessing.featdet_manual;

import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.impl.SimpleFeatureInformation;
import io.github.mzmine.main.MZmineCore;
import java.text.Format;
import java.util.TreeMap;
import javax.annotation.Nonnull;
import com.google.common.collect.Range;
import com.google.common.primitives.Ints;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.util.MathUtils;
import io.github.mzmine.util.scans.ScanUtils;

/**
 * This class represents a manually picked chromatographic feature.
 */
public class ManualFeature {

  private SimpleFeatureInformation featureInfo;

  private RawDataFile dataFile;

  // Raw M/Z, RT, Height and Area
  private double mz, height, area;
  private float rt;
  private Double fwhm = null, tf = null, af = null;

  // Boundaries of the feature
  private Range<Double> mzRange;
  private Range<Float> intensityRange, rtRange;

  // Map of scan number and features point
  private TreeMap<Integer, DataPoint> dataPointMap;

  // Number of most intense fragment scan
  private int fragmentScan, representativeScan;

  // Number of all MS2 fragment scans
  private int[] allMS2FragmentScanNumbers;

  // Isotope pattern. Null by default but can be set later by deisotoping
  // method.
  private IsotopePattern isotopePattern;
  private int charge = 0;

  /**
   * Initializes empty feature for adding features points
   */
  public ManualFeature(RawDataFile dataFile) {
    this.dataFile = dataFile;
    dataPointMap = new TreeMap<Integer, DataPoint>();
  }

  /**
   * This feature is always a result of manual feature detection, therefore MANUAL
   */
  public @Nonnull FeatureStatus getFeatureStatus() {
    return FeatureStatus.MANUAL;
  }

  /**
   * This method returns M/Z value of the feature
   */
  public double getMZ() {
    return mz;
  }

  /**
   * This method returns retention time of the feature
   */
  public float getRT() {
    return rt;
  }

  /**
   * This method returns the raw height of the feature
   */
  public double getHeight() {
    return height;
  }

  /**
   * This method returns the raw area of the feature
   */
  public double getArea() {
    return area;
  }

  /**
   * This method returns numbers of scans that contain this feature
   */
  public @Nonnull int[] getScanNumbers() {
    return Ints.toArray(dataPointMap.keySet());
  }

  /**
   * This method returns a representative datapoint of this feature in a given scan
   */
  public DataPoint getDataPoint(int scanNumber) {
    return dataPointMap.get(scanNumber);
  }

  public @Nonnull Range<Float> getRawDataPointsIntensityRange() {
    return intensityRange;
  }

  public @Nonnull Range<Double> getRawDataPointsMZRange() {
    return mzRange;
  }

  public @Nonnull Range<Float> getRawDataPointsRTRange() {
    return rtRange;
  }

  /**
   * @see Feature#getRawDataFile()
   */
  public @Nonnull RawDataFile getRawDataFile() {
    return dataFile;
  }

  public String getName() {
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

  public IsotopePattern getIsotopePattern() {
    return isotopePattern;
  }

  public void setIsotopePattern(@Nonnull IsotopePattern isotopePattern) {
    this.isotopePattern = isotopePattern;
  }

  /**
   * Adds a new features point to this feature
   *
   * @param scanNumber
   * @param dataPoint
   */
  public void addDatapoint(int scanNumber, DataPoint dataPoint) {

    float rt = dataFile.getScan(scanNumber).getRetentionTime();

    if (dataPointMap.isEmpty()) {
      rtRange = Range.singleton(rt);
      mzRange = Range.singleton(dataPoint.getMZ());
      intensityRange = Range.singleton((float) dataPoint.getIntensity());
    } else {
      rtRange = rtRange.span(Range.singleton(rt));
      mzRange = mzRange.span(Range.singleton(dataPoint.getMZ()));
      intensityRange = intensityRange.span(Range.singleton((float) dataPoint.getIntensity()));
    }

    dataPointMap.put(scanNumber, dataPoint);

  }

  public void finalizeFeature() {

    // Trim the zero-intensity features points from the beginning and end
    while (!dataPointMap.isEmpty()) {
      int scanNumber = dataPointMap.firstKey();
      if (dataPointMap.get(scanNumber).getIntensity() > 0)
        break;
      dataPointMap.remove(scanNumber);
    }
    while (!dataPointMap.isEmpty()) {
      int scanNumber = dataPointMap.lastKey();
      if (dataPointMap.get(scanNumber).getIntensity() > 0)
        break;
      dataPointMap.remove(scanNumber);
    }

    // Check if we have any features points
    if (dataPointMap.isEmpty()) {
      throw (new IllegalStateException("Feature can not be finalized without any features points"));
    }

    // Get all scan numbers
    int allScanNumbers[] = Ints.toArray(dataPointMap.keySet());

    // Find the features point with top intensity and use its RT and height
    for (int i = 0; i < allScanNumbers.length; i++) {
      DataPoint dataPoint = dataPointMap.get(allScanNumbers[i]);
      float rt = dataFile.getScan(allScanNumbers[i]).getRetentionTime();
      if (dataPoint.getIntensity() > height) {
        height = dataPoint.getIntensity();
        representativeScan = allScanNumbers[i];
        this.rt = rt;
      }
    }

    // Calculate feature area
    area = 0;
    for (int i = 1; i < allScanNumbers.length; i++) {

      // For area calculation, we use retention time in seconds
      double previousRT = dataFile.getScan(allScanNumbers[i - 1]).getRetentionTime() * 60d;
      double currentRT = dataFile.getScan(allScanNumbers[i]).getRetentionTime() * 60d;

      double rtDifference = currentRT - previousRT;

      // Intensity at the beginning and end of the interval
      double previousIntensity = dataPointMap.get(allScanNumbers[i - 1]).getIntensity();
      double thisIntensity = dataPointMap.get(allScanNumbers[i]).getIntensity();
      double averageIntensity = (previousIntensity + thisIntensity) / 2;

      // Calculate area of the interval
      area += (rtDifference * averageIntensity);

    }

    // Calculate median MZ
    double mzArray[] = new double[allScanNumbers.length];
    for (int i = 0; i < allScanNumbers.length; i++) {
      mzArray[i] = dataPointMap.get(allScanNumbers[i]).getMZ();
    }
    this.mz = MathUtils.calcQuantile(mzArray, 0.5f);

    fragmentScan = ScanUtils.findBestFragmentScan(dataFile, rtRange, mzRange);

    allMS2FragmentScanNumbers = ScanUtils.findAllMS2FragmentScans(dataFile, rtRange, mzRange);

    if (fragmentScan > 0) {
      Scan fragmentScanObject = dataFile.getScan(fragmentScan);
      int precursorCharge = fragmentScanObject.getPrecursorCharge();
      if ((precursorCharge > 0) && (this.charge == 0))
        this.charge = precursorCharge;
    }

  }

  public int getRepresentativeScanNumber() {
    return representativeScan;
  }

  public int getMostIntenseFragmentScanNumber() {
    return fragmentScan;
  }

  public int[] getAllMS2FragmentScanNumbers() {
    return allMS2FragmentScanNumbers;
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

  public void setFeatureInformation(SimpleFeatureInformation featureInfoIn) {
    this.featureInfo = featureInfoIn;
  }

  public SimpleFeatureInformation getFeatureInformation() {
    return featureInfo;
  }

  public void setFragmentScanNumber(int fragmentScanNumber) {
    this.fragmentScan = fragmentScanNumber;
  }

  public void setAllMS2FragmentScanNumbers(int[] allMS2FragmentScanNumbers) {
    this.allMS2FragmentScanNumbers = allMS2FragmentScanNumbers;
    // also set best scan by TIC
    int best = -1;
    double tic = 0;
    if (allMS2FragmentScanNumbers != null) {
      for (int i : allMS2FragmentScanNumbers) {
        if (tic < dataFile.getScan(i).getTIC())
          best = i;
      }
    }
    setFragmentScanNumber(best);
  }
  // End dulab Edit

  private FeatureList featureList;

  public FeatureList getFeatureList() {
    return featureList;
  }

  public void setFeatureList(FeatureList featureList) {
    this.featureList = featureList;
  }


}
