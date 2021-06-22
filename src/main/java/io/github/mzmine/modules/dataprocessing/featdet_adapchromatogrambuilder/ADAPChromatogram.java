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
 *
 * Edited and modified by Owen Myers (Oweenm@gmail.com)
 */


package io.github.mzmine.modules.dataprocessing.featdet_adapchromatogrambuilder;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.datamodel.impl.SimpleFeatureInformation;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.util.scans.ScanUtils;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.Vector;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;


/**
 * Chromatogram.
 */
public class ADAPChromatogram {

  private static Logger logger = Logger.getLogger(ADAPChromatogram.class.getName());

  private SimpleFeatureInformation featureInfo;

  // Data file of this chromatogram
  private final RawDataFile dataFile;

  // Data points of the chromatogram (map of scan number -> m/z feature)
  // private Hashtable<Integer, DataPoint> dataPointsMap;
  private final TreeMap<Scan, DataPoint> dataPointsMap;

  // Chromatogram m/z, RT, height. The mz value will be the highest points mz value
  private double mz, rt, height, weightedMz;
  private Double fwhm = null, tf = null, af = null;

  // Top intensity scan, fragment scan
  private Scan representativeScan = null;
  private Scan fragmentScan = null;

  // All MS2 fragment scan numbers
  private Scan[] allMS2FragmentScanNumbers = new Scan[]{};

  // Ranges of raw data points
  private Range<Double> rawDataPointsIntensityRange, rawDataPointsMZRange;
  private Range<Float> rawDataPointsRTRange;

  // A set of scan numbers of a segment which is currently being connected
  private Vector<Scan> buildingSegment;
  // A full list of all the scan numbers as points get added
  private List<Scan> chromScanList;

  // Keep track of last added data point
  private DataPoint lastMzFeature;

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
  private double weightedMzSum = 0;
  private int weightedMzN = 0;
  private double sumOfWeights = 0;

  private double highPointMZ = 0;

  // full stack of scans
  private final Scan[] scanNumbers;

  public int tmp_see_same_scan_count = 0;

  // Feature list
  private FeatureList featureList;

  /**
   * Initializes this Chromatogram
   */
  public ADAPChromatogram(RawDataFile dataFile, Scan scanNumbers[]) {
    this.dataFile = dataFile;
    this.scanNumbers = scanNumbers;

    rawDataPointsRTRange = dataFile.getDataRTRange(1);

    dataPointsMap = new TreeMap<>();
    buildingSegment = new Vector<>();
    chromScanList = new ArrayList<>();
  }

  public double getHighPointMZ() {
    return highPointMZ;
  }

  public void setHighPointMZ(double toSet) {
    highPointMZ = toSet;
  }

  public List<Double> getIntensitiesForCDFOut() {
    // Need all scans with no intensity to be set to zero
    List<Double> intensityList = new ArrayList<>();

    for (int curScanNum = 0; curScanNum < scanNumbers.length; curScanNum++) {
      DataPoint dp = dataPointsMap.get(scanNumbers[curScanNum]);
      if (dp == null) {
        intensityList.add(0.0);
      } else {
        intensityList.add(dp.getIntensity());
      }
    }
    return intensityList;
  }

  public Collection<DataPoint> getDataPoints() {
    return dataPointsMap.values();
  }

  public int findNumberOfContinuousPointsAboveNoise(double noise) {
    // sort the array containing all of the scan numbers of the point added
    // loop over the sorted array now.
    // if you find a point with intensity higher than noise start the count
    // if the next scan contains a point higher than the noise update the count
    // otherwise start it oer when you hit a sufficiently high point.
    // keep track of the largest count which will be returned.
    chromScanList.sort(Comparator.comparingInt(Scan::getScanNumber));

    int bestCount = 0;
    int curCount = 0;
    Scan lastScanNum;
    int scanListLength = chromScanList.size();

    Scan curScanNum;
    DataPoint curDataPoint;

    for (int i = 1; i < scanListLength; i++) {
      curScanNum = chromScanList.get(i);
      curDataPoint = dataPointsMap.get(curScanNum);

      if (curDataPoint.getIntensity() > noise) {
        lastScanNum = chromScanList.get(i - 1);
        int lastScanNumsActIndex = Arrays.binarySearch(scanNumbers, lastScanNum);
        Scan seqNextScanShouldBe = scanNumbers[lastScanNumsActIndex + 1];

        if (seqNextScanShouldBe == curScanNum) {
          curCount += 1;
          if (curCount > bestCount) {
            bestCount = curCount;
          }
        } else {
          curCount = 0;
        }

      } else {
        curCount = 0;
      }
    }
    // System.out.println("bestCount");
    // System.out.println(bestCount);

    // plus one because first point considered in advancing curcount is actualy going to be the
    // second point/
    return bestCount + 1;
  }

  /**
   * This method adds a MzFeature to this Chromatogram. All values of this Chromatogram (rt, m/z,
   * intensity and ranges) are updated on request
   */
  public void addMzFeature(Scan scanNumber, DataPoint mzValue) {
    double curIntensity;
    // System.out.println("---------------- Adding MZ value to Chromatogram ----------------");

    // If we already have a mzvalue for the scan number then we need to add the intensities
    // together before putting it into the dataPointsMap, otherwise the chromatogram is only
    // representing the intesities of the llast added point for that scan.
    //
    // For now just don't add the point if we have it already. The highest point will be the
    // first one added
    if (dataPointsMap.containsKey(scanNumber)) {
      tmp_see_same_scan_count += 1;
      return;
    }
    if (mzValue == null) {
      return;
    }

    dataPointsMap.put(scanNumber, mzValue);
    lastMzFeature = mzValue;
    mzSum += mzValue.getMZ();
    mzN++;
    mz = mzSum / mzN;
    buildingSegment.add(scanNumber);
    chromScanList.add(scanNumber);

    curIntensity = mzValue.getIntensity();

    weightedMzN++;
    weightedMzSum += curIntensity * mzValue.getMZ();
    sumOfWeights += curIntensity;

    weightedMz = weightedMzSum / sumOfWeights;
  }

  public DataPoint getDataPoint(Scan scanNumber) {
    return dataPointsMap.get(scanNumber);
  }

  /**
   * Returns m/z value of last added data point
   */
  public DataPoint getLastMzFeature() {
    return lastMzFeature;
  }

  /**
   * This method returns m/z value of the chromatogram
   */
  public double getMZ() {
    return mz;
  }

  /**
   * This method returns weighted mean of m/z values comprising the chromatogram
   */
  public double getWeightedMZ() {
    return weightedMz;
  }


  /**
   * This method returns a string with the basic information that defines this feature
   *
   * @return String information
   */
  @Override
  public String toString() {
    return "Chromatogram " + MZmineCore.getConfiguration().getMZFormat().format(mz) + " m/z";
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

  public @NotNull
  FeatureStatus getFeatureStatus() {
    return FeatureStatus.DETECTED;
  }

  public double getRT() {
    return rt;
  }

  public @NotNull
  Range<Double> getRawDataPointsIntensityRange() {
    return rawDataPointsIntensityRange;
  }

  public @NotNull
  Range<Double> getRawDataPointsMZRange() {
    return rawDataPointsMZRange;
  }

  public @NotNull
  Range<Float> getRawDataPointsRTRange() {
    return rawDataPointsRTRange;
  }

  public Scan getRepresentativeScanNumber() {
    return representativeScan;
  }

  public @NotNull
  Scan[] getScanNumbers() {
    return dataPointsMap.keySet().toArray(Scan[]::new);
  }

  public @NotNull
  RawDataFile getDataFile() {
    return dataFile;
  }

  public IsotopePattern getIsotopePattern() {
    return isotopePattern;
  }

  public void setIsotopePattern(@NotNull IsotopePattern isotopePattern) {
    this.isotopePattern = isotopePattern;
  }

  public void outputChromToFile() {
    // int allScanNumbers[] = Ints.toArray(dataPointsMap.keySet());
    Scan allScanNumbers[] = getScanNumbers();
    Arrays.sort(allScanNumbers);
    try {
      String fileName = dataFile.getName();

      String fileNameNoExtention = fileName.split("\\.")[0];

      // PrintWriter fileWriter = new PrintWriter("owen_mzmine_chrom_out.txt","UTF-8");
      PrintWriter fileWriter = new PrintWriter(
          new FileOutputStream(new File(fileNameNoExtention + "_mzmine_chrom_out.csv"), true));

      String mzStr = String.valueOf(getMZ());
      // fileWriter.println("New chromatogram. mz: "+mzStr);
      String curIntStr;
      String curRtStr;
      String curMzStr;
      double curInt;
      double curRt;
      double curMz;
      String spacer = ",";

      for (Scan scan : allScanNumbers) {
        curRt = scan.getRetentionTime();
        DataPoint mzFeature = getDataPoint(scan);

        if (mzFeature == null) {
          curInt = 0.0;
          curMz = -1;
        } else {
          curInt = mzFeature.getIntensity();
          curMz = mzFeature.getMZ();
        }

        curIntStr = String.valueOf(curInt);
        curRtStr = String.valueOf(curRt);
        curMzStr = String.valueOf(curMz);

        fileWriter.println(curRtStr + spacer + curIntStr + spacer + curMzStr);
      }
      fileWriter.close();
    } catch (FileNotFoundException ex) {
      ex.printStackTrace();
    }
  }


  public void finishChromatogram() {
    Scan allScanNumbers[] = dataPointsMap.keySet().stream()
        .sorted(Comparator.comparingInt(Scan::getScanNumber)).toArray(Scan[]::new);

    mz = highPointMZ;

    // Update raw data point ranges, height, rt and representative scan
    height = Double.MIN_VALUE;
    rawDataPointsRTRange = null;

    for (int i = 0; i < allScanNumbers.length; i++) {

      DataPoint mzFeature = dataPointsMap.get(allScanNumbers[i]);

      if (i == 0) {
        rawDataPointsIntensityRange = Range.singleton(mzFeature.getIntensity());
        rawDataPointsMZRange = Range.singleton(mzFeature.getMZ());
      } else {
        rawDataPointsIntensityRange =
            rawDataPointsIntensityRange.span(Range.singleton(mzFeature.getIntensity()));
        rawDataPointsMZRange = rawDataPointsMZRange.span(Range.singleton(mzFeature.getMZ()));
      }

      if (height < mzFeature.getIntensity()) {
        height = mzFeature.getIntensity();
        rt = allScanNumbers[i].getRetentionTime();
        representativeScan = allScanNumbers[i];
      }

      // retention time
      float scanRt = allScanNumbers[i].getRetentionTime();

      if (mzFeature.getIntensity() != 0.0) {
        if (rawDataPointsRTRange == null) {
          rawDataPointsRTRange = Range.singleton(scanRt);
        } else {
          rawDataPointsRTRange = rawDataPointsRTRange.span(Range.singleton(scanRt));
        }
      }
    }

    // Update fragment scan
    fragmentScan =
        ScanUtils.findBestFragmentScan(dataFile, dataFile.getDataRTRange(1), rawDataPointsMZRange);

    allMS2FragmentScanNumbers = ScanUtils.findAllMS2FragmentScans(dataFile,
        dataFile.getDataRTRange(1), rawDataPointsMZRange);

    if (fragmentScan != null) {
      int precursorCharge = fragmentScan.getPrecursorCharge();
      if (precursorCharge > 0) {
        this.charge = precursorCharge;
      }
    }

    // Discard the fields we don't need anymore
    buildingSegment = null;
    lastMzFeature = null;
  }

  public double getBuildingSegmentLength() {
    if (buildingSegment.size() < 2) {
      return 0;
    }
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
    for (Scan scanNumber : buildingSegment) {
      dataPointsMap.remove(scanNumber);
    }
    buildingSegment.clear();
  }

  public void commitBuildingSegment() {
    buildingSegment.clear();
    numOfCommittedSegments++;
  }

  public void addDataPointsFromChromatogram(ADAPChromatogram ch) {
    for (Entry<Scan, DataPoint> dp : ch.dataPointsMap.entrySet()) {
      addMzFeature(dp.getKey(), dp.getValue());
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

  public void setFeatureInformation(SimpleFeatureInformation featureInfoIn) {
    this.featureInfo = featureInfoIn;
  }

  public SimpleFeatureInformation getFeatureInformation() {
    return featureInfo;
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

  public FeatureList getFeatureList() {
    return featureList;
  }

  public void setFeatureList(FeatureList featureList) {
    this.featureList = featureList;
  }

  public void addZerosForEmptyScans(Scan[] scans) {
    for (Scan scan : scans) {
      dataPointsMap.computeIfAbsent(scan, s -> new SimpleDataPoint(getMZ(), 0d));
    }
  }

  /**
   * Adds zeros on the side of each consecutive number of scans.
   *
   * @param minGap The minimum number of missing scans to be found, to fill up with zeros.
   * @param zeros  The number of zeros to add. zeros <= minGap
   */
  public void addNZeros(int minGap, int zeros) {
    assert minGap >= zeros;

    int allScansIndex = 0; // contains the index of the next dp after a gap
    Map<Scan, DataPoint> dataPointsToAdd = new HashMap<>();

    Scan[] detectedScans = dataPointsMap.keySet().toArray(Scan[]::new);

    // loop through all scans
    int nextDetectedScanInAllIndex = -1;
    int nextDetectedScanIndex = 0;
    int currentGap = 0;
    int added;
    for (allScansIndex = 0; allScansIndex < scanNumbers.length; allScansIndex++) {
      added = 0;
      // was a DP detected in this scan?
      if (scanNumbers[allScansIndex] == detectedScans[nextDetectedScanIndex]) {
        if (currentGap >= minGap) {
          // add leading zeros before allScansIndex
          for (int i = 1; i <= zeros && i <= currentGap && (allScansIndex - i) >= 0; i++) {
            // add zero data points
            dataPointsToAdd.put(scanNumbers[allScansIndex - i], new SimpleDataPoint(getMZ(), 0d));
            added++;
          }
          currentGap -= added;
          // add trailing zeros after last detected
          if (currentGap > 0 && nextDetectedScanInAllIndex >= 0) {
            for (int i = 1; i <= zeros && i <= currentGap
                && (nextDetectedScanInAllIndex + i) < scanNumbers.length; i++) {
              // add zero data points after
              dataPointsToAdd.put(scanNumbers[nextDetectedScanInAllIndex + i],
                  new SimpleDataPoint(getMZ(), 0d));
            }
          }
        }
        currentGap = 0;
        nextDetectedScanIndex++;
        nextDetectedScanInAllIndex = allScansIndex;

        // no more detected scans
        if (nextDetectedScanIndex == detectedScans.length) {
          // add trailing zeros after last detected
          for (int i = 1; i <= zeros && (nextDetectedScanInAllIndex + i) < scanNumbers.length;
              i++) {
            // add zero data points after
            dataPointsToAdd
                .put(scanNumbers[nextDetectedScanInAllIndex + i], new SimpleDataPoint(getMZ(), 0d));
          }
          break;
        }
      } else {
        currentGap++;
      }

      // last datapoint
      if (allScansIndex == scanNumbers.length - 1) {
        if (currentGap >= minGap) {
          // add trailing zeros after last detected
          if (currentGap > 0 && nextDetectedScanInAllIndex >= 0) {
            for (int i = 1; i <= zeros && i <= currentGap
                && (nextDetectedScanInAllIndex + i) < scanNumbers.length; i++) {
              // add zero data points after
              dataPointsToAdd.put(scanNumbers[nextDetectedScanInAllIndex + i],
                  new SimpleDataPoint(getMZ(), 0d));
            }
          }
        }
      }
    }
    dataPointsMap.putAll(dataPointsToAdd);
  }
}
