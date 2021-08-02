/*
 * (C) Copyright 2015-2017 by MSDK Development Team
 *
 * This software is dual-licensed under either
 *
 * (a) the terms of the GNU Lesser General Public License version 2.1 as published by the Free
 * Software Foundation
 *
 * or (per the licensee's choosing)
 *
 * (b) the terms of the Eclipse Public License v1.0 as published by the Eclipse Foundation.
 */
package io.github.mzmine.modules.dataprocessing.align_adap3.algorithms;

import io.github.mzmine.modules.dataprocessing.align_adap3.algorithms.ADAP3DPeakDetectionAlgorithm.GoodPeakInfo;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.lang.Math;

import io.github.msdk.datamodel.MsScan;
import io.github.msdk.datamodel.RawDataFile;


/**
 * <p>
 * SliceSparseMatrix class is used for slicing the sparse matrix of raw data as per given mz value.
 * slice contains intensities for one mz value for different scans. In this class sparse matrix has
 * been implemented in the form of multikey map. Mz and scan number are keys and object containing
 * Intensities along information like retention time and whether the value is still in matrix or not
 * is the value. Consider Scan numbers as column index and mz values as row index. cell values as
 * object containing Intensities along with other information like retention time and whether the
 * value is still in matrix or not.
 * </p>
 */
public class SliceSparseMatrix {

  /**
   * <p>
   * tripletMap is used for creating MultiKeyMap type of hashmap from raw data file.
   * </p>
   */
  private final HashMap<Integer, Float> rtMap;

  private final List<Triplet> sortListAccordingToMzScan;
  private final List<Triplet> sortListAccordingToIntensity;
  private final List<Triplet> sortListAccordingToScan;
  Comparator<Triplet> compareMzScan;
  Comparator<Triplet> compareIntensity;
  Comparator<Triplet> compareScanMz;
  Comparator<Triplet> compareScan;


  /**
   * <p>
   * filterListOfTriplet is used for adding intensities for same mz values under same scan numbers.
   * </p>
   */
  private final List<Triplet> filterListOfTriplet;

  /**
   * <p>
   * maxIntensityIndex is used for keeping track of next maximum intensity in the loop.
   * </p>
   */
  private int maxIntensityIndex = 0;

  /**
   * <p>
   * roundMz is used for rounding mz value.
   * </p>
   */
  private final int roundMzFactor = 10000;

  /**
   * <p>
   * listOfScans is used for getting scan objects from raw data file.
   * </p>
   */
  private final List<MsScan> listOfScans;

  /**
   * <p>
   * mzValues is used to store all the mz values from raw file.
   * </p>
   */
  public final List<Integer> mzValues;

  /**
   * <p>
   * Triplet is used for representing elements of sparse matrix.
   * </p>
   */
  public static class Triplet {
    public int mz;
    public int scanListIndex;
    public float intensity;
    public byte removed;
  }

  /**
   * <p>
   * This is the data model for getting vertical slice from sparse matrix.
   * </p>
   */
  public static class VerticalSliceDataPoint {
    float mz;
    float intensity;
  }

  /**
   * <p>
   * This constructor takes raw data file and create the triplet map which contains information such
   * as mz,intensity,rt,scan number
   * </p>
   *
   * @param rawFile a {@link RawDataFile} object. This is raw data
   *        file object by which we can pass raw file.
   */
  public SliceSparseMatrix(RawDataFile rawFile) {
    this(rawFile, s -> true);
  }

  /**
   * <p>
   * This constructor takes raw data file and create the triplet map which contains information such
   * as mz,intensity,rt,scan number
   * </p>
   *
   * @param rawFile a {@link RawDataFile} object. This is raw data
   *        file object by which we can pass raw file.
   * @param msScanPredicate a {@link Predicate} object. Only MsScan which pass
   *        this predicate will be processed.
   */
  public SliceSparseMatrix(RawDataFile rawFile, Predicate<MsScan> msScanPredicate) {
    listOfScans =
        rawFile.getScans().stream().filter(msScanPredicate).collect(Collectors.<MsScan>toList());
    List<Triplet> listOfTriplet = new ArrayList<Triplet>();
    rtMap = new HashMap<Integer, Float>();

    for (int i = 0; i < listOfScans.size(); i++) {
      MsScan scan = listOfScans.get(i);

      if (scan == null)
        continue;

      double mzBuffer[];
      float intensityBuffer[];
      Float rt;
      mzBuffer = scan.getMzValues();
      intensityBuffer = scan.getIntensityValues();
      rt = scan.getRetentionTime();

      if (rt == null)
        continue;
      rtMap.put(i, rt);

      for (int j = 0; j < mzBuffer.length; j++) {
        Triplet triplet = new Triplet();
        triplet.intensity = intensityBuffer[j];
        triplet.mz = roundMZ(mzBuffer[j]);
        triplet.scanListIndex = i;
        triplet.removed = 0;
        listOfTriplet.add(triplet);
      }
    }



    compareScanMz = new Comparator<Triplet>() {

      @Override
      public int compare(Triplet o1, Triplet o2) {

        int scan1 = o1.scanListIndex;
        int scan2 = o2.scanListIndex;
        int scanCompare = Integer.compare(scan1, scan2);

        if (scanCompare != 0) {
          return scanCompare;
        } else {
          int mz1 = o1.mz;
          int mz2 = o2.mz;
          return Integer.compare(mz1, mz2);
        }
      }
    };


    Collections.sort(listOfTriplet, compareScanMz);

    filterListOfTriplet = new ArrayList<Triplet>();
    Triplet currTriplet = new Triplet();
    Triplet lastFilterTriplet = new Triplet();
    // tripletMap = new MultiKeyMap<Integer, Triplet>();
    int index = 0;
    Set<Integer> mzSet = new HashSet<Integer>();

    filterListOfTriplet.add(listOfTriplet.get(0));
    for (int i = 1; i < listOfTriplet.size(); i++) {
      currTriplet = listOfTriplet.get(i);
      mzSet.add(listOfTriplet.get(i).mz);
      lastFilterTriplet = filterListOfTriplet.get(index);
      if (currTriplet.intensity > 1000) {
        if (currTriplet.mz == lastFilterTriplet.mz
            && currTriplet.scanListIndex == lastFilterTriplet.scanListIndex) {
          lastFilterTriplet.intensity += currTriplet.intensity;
        } else {
          filterListOfTriplet.add(currTriplet);
          index++;
        }
      }
    }
    mzValues = new ArrayList<Integer>(mzSet);
    Collections.sort(mzValues);

    sortListAccordingToScan = new ArrayList<>(filterListOfTriplet);
    compareScan = new Comparator<Triplet>() {

      @Override
      public int compare(Triplet o1, Triplet o2) {

        int scan1 = o1.scanListIndex;
        int scan2 = o2.scanListIndex;
        int scanCompare = Integer.compare(scan1, scan2);
        return scanCompare;
      }
    };

    Collections.sort(sortListAccordingToScan, compareScan);


    sortListAccordingToIntensity = new ArrayList<>(filterListOfTriplet);

    compareIntensity = new Comparator<Triplet>() {

      @Override
      public int compare(Triplet o1, Triplet o2) {

        Float intensity1 = o1.intensity;
        Float intensity2 = o2.intensity;
        int intensityCompare = intensity2.compareTo(intensity1);
        return intensityCompare;
      }
    };
    Collections.sort(sortListAccordingToIntensity, compareIntensity);

    sortListAccordingToMzScan = new ArrayList<>(filterListOfTriplet);
    compareMzScan = new Comparator<Triplet>() {

      @Override
      public int compare(Triplet o1, Triplet o2) {

        int mz1 = o1.mz;
        int mz2 = o2.mz;
        int scanCompare = Integer.compare(mz1, mz2);

        if (scanCompare != 0) {
          return scanCompare;
        } else {
          int scan1 = o1.scanListIndex;
          int scan2 = o2.scanListIndex;
          return Integer.compare(scan1, scan2);
        }
      }
    };
    Collections.sort(sortListAccordingToMzScan, compareMzScan);
  }

  /**
   * <p>
   * This method returns the MultiKeyMap slice of data for given mz,lowerScanBound,upperScanBound
   * </p>
   *
   * @param mz a {@link Double} object. This is original m/z value from raw file.
   * @param lowerScanBound a {@link Integer} object. This is lowest scan number in the
   *        horizontal matrix slice.
   * @param upperScanBound a {@link Integer} object. This is highest scan number in the
   *        horizontal matrix slice.
   * @return sliceMap a {@link org.apache.commons.collections4.map.MultiKeyMap} object. This object
   *         contains horizontal slice with single m/z value,different scan numbers and different
   *         intensities along with retention time.
   */
  public List<Triplet> getHorizontalSlice(double mz, int lowerScanBound, int upperScanBound) {

    int roundedmz = roundMZ(mz);
    List<Triplet> sliceList = new ArrayList<Triplet>();

    for (int i = lowerScanBound; i <= upperScanBound; i++) {
      Triplet searchTriplet = new Triplet();
      searchTriplet.mz = roundedmz;
      searchTriplet.scanListIndex = i;
      int index = Collections.binarySearch(sortListAccordingToMzScan, searchTriplet, compareMzScan);
      if (index >= 0) {
        Triplet triplet = sortListAccordingToMzScan.get(index);
        sliceList.add(triplet);
      } else {
        searchTriplet.intensity = 0;
        searchTriplet.removed = 0;
        sliceList.add(searchTriplet);
      }
    }

    return sliceList;
  }

  /**
   * <p>
   * This method returns the MultiKeyMap slice of data for rounded mz,lowerScanBound,upperScanBound
   * </p>
   *
   * @param roundedMZ a {@link Double} object. This is rounded m/z value which is already
   *        multiplied by 10000.
   * @param lowerScanBound a {@link Integer} object. This is lowest scan number in the
   *        horizontal matrix slice.
   * @param upperScanBound a {@link Integer} object. This is highest scan number in the
   *        horizontal matrix slice.
   * @return sliceMap a {@link org.apache.commons.collections4.map.MultiKeyMap} object. This object
   *         contains horizontal slice with single m/z value,different scan numbers and different
   *         intensities along with retention time.
   */
  public List<Triplet> getHorizontalSlice(int roundedMZ, int lowerScanBound, int upperScanBound) {
    return getHorizontalSlice((double) roundedMZ / roundMzFactor, lowerScanBound, upperScanBound);
  }

  /**
   * <p>
   * This method returns the List of type VerticalSliceDataPoint for given Scan Number.
   * </p>
   *
   * @return datapointList a
   *         {@link VerticalSliceDataPoint}
   *         list. This is list containing m/z and intensities for one scan number.
   * @param scanNumber a int.
   */
  public List<VerticalSliceDataPoint> getVerticalSlice(int scanNumber) {

    List<Triplet> oneScanTriplet = new ArrayList<Triplet>();
    List<VerticalSliceDataPoint> datapointList = new ArrayList<VerticalSliceDataPoint>();

    Triplet searchTriplet = new Triplet();
    searchTriplet.scanListIndex = scanNumber;
    int index1 = Collections.binarySearch(sortListAccordingToScan, searchTriplet, compareScan);
    int index2 = index1;
    oneScanTriplet.add(sortListAccordingToScan.get(index1));

    while (index1 >= 0) {
      index1--;
      if (index1 < 0 || sortListAccordingToScan.get(index1).scanListIndex != scanNumber) {
        break;
      }
      oneScanTriplet.add(sortListAccordingToScan.get(index1));
    }

    while (index2 >= 0) {
      index2++;
      if (index2 > filterListOfTriplet.size() - 1
          || filterListOfTriplet.get(index2).scanListIndex != scanNumber) {
        break;
      }
      oneScanTriplet.add(filterListOfTriplet.get(index2));
    }

    Collections.sort(oneScanTriplet, compareIntensity);
    int maxIntensityMZ = oneScanTriplet.get(0).mz;
    Collections.sort(oneScanTriplet, compareScanMz);

    Triplet searchMz = new Triplet();
    for (int roundedMZ : mzValues) {
      VerticalSliceDataPoint datapoint = new VerticalSliceDataPoint();

      searchMz.mz = roundedMZ;
      searchMz.scanListIndex = scanNumber;
      if (roundedMZ >= (maxIntensityMZ - roundMzFactor)
          && roundedMZ <= (maxIntensityMZ + roundMzFactor)) {
        int mzInex = Collections.binarySearch(oneScanTriplet, searchMz, compareScanMz);
        if (mzInex >= 0) {
          datapoint.intensity = oneScanTriplet.get(mzInex).intensity;
          datapoint.mz = (float) roundedMZ / roundMzFactor;
          datapointList.add(datapoint);
        } else {
          datapoint.intensity = (float) 0.0;
          datapoint.mz = (float) roundedMZ / roundMzFactor;
          datapointList.add(datapoint);
        }
      }
    }
    return datapointList;
  }



  /**
   * <p>
   * This method finds next maximum intensity from filterListOfTriplet
   * </p>
   *
   * @return tripletObject a {@link Triplet} object. This is element of sparse matrix.
   */
  public Triplet findNextMaxIntensity() {

    Triplet tripletObject = null;

    for (int i = maxIntensityIndex; i < sortListAccordingToIntensity.size(); i++) {
      if (sortListAccordingToIntensity.get(i).removed == 0) {
        tripletObject = sortListAccordingToIntensity.get(i);
        maxIntensityIndex = i + 1;
        break;
      }

    }
    return tripletObject;
  }

  /**
   * <p>
   * This method returns sorted list of ContinuousWaveletTransform.DataPoint object.Object contain
   * retention time and intensity values.
   * </p>
   *
   * @param slice a {@link org.apache.commons.collections4.map.MultiKeyMap} object. This is
   *        horizontal slice from sparse matrix.
   * @return listOfDataPoint a {@link Triplet} list. This returns list of retention time and
   *         intensities.
   */
  public List<ContinuousWaveletTransform.DataPoint> getCWTDataPoint(List<Triplet> slice) {

    Iterator<Triplet> iterator = slice.iterator();
    List<ContinuousWaveletTransform.DataPoint> listOfDataPoint =
        new ArrayList<ContinuousWaveletTransform.DataPoint>();

    while (iterator.hasNext()) {
      ContinuousWaveletTransform.DataPoint dataPoint = new ContinuousWaveletTransform.DataPoint();
      Triplet triplet = (Triplet) iterator.next();
      if (triplet.intensity != 0 && triplet.removed == 0) {
        dataPoint.rt = rtMap.get(triplet.scanListIndex) / 60;
        dataPoint.intensity = triplet.intensity;
        listOfDataPoint.add(dataPoint);
      } else {
        dataPoint.rt = rtMap.get(triplet.scanListIndex) / 60;
        dataPoint.intensity = 0.0;
        listOfDataPoint.add(dataPoint);
      }
    }
    Comparator<ContinuousWaveletTransform.DataPoint> compare =
        new Comparator<ContinuousWaveletTransform.DataPoint>() {

          @Override
          public int compare(ContinuousWaveletTransform.DataPoint o1,
              ContinuousWaveletTransform.DataPoint o2) {
            Double rt1 = o1.rt;
            Double rt2 = o2.rt;
            return rt1.compareTo(rt2);
          }
        };

    Collections.sort(listOfDataPoint, compare);

    return listOfDataPoint;
  }

  /**
   * <p>
   * This method removes data points from whole data set for given mz,lowerscanbound and
   * upperscanbound
   * </p>
   *
   * @param lowerScanBound a {@link Integer} object.This is lowest scan number.
   * @param upperScanBound a {@link Integer} object.This is highest scan number.
   * @return tripletMap a {@link org.apache.commons.collections4.map.MultiKeyMap} object. This is
   *         whole sparse matrix.
   * @param roundedmz a int.
   */
  public List<Triplet> removeDataPoints(int roundedmz, int lowerScanBound, int upperScanBound) {

    Triplet searchTriplet = new Triplet();

    for (int i = lowerScanBound; i <= upperScanBound; i++) {

      searchTriplet.mz = roundedmz;
      searchTriplet.scanListIndex = i;
      int index = Collections.binarySearch(filterListOfTriplet, searchTriplet, compareScanMz);
      if (index >= 0) {
        Triplet triplet = filterListOfTriplet.get(index);
        triplet.removed = 1;
      }
    }
    return filterListOfTriplet;
  }

  /**
   * <p>
   * This method restores data points from whole data set for given mz,lowerscanbound and
   * upperscanbound
   * </p>
   *
   * @param lowerScanBound a {@link Integer} object.This is lowest scan number.
   * @param upperScanBound a {@link Integer} object.This is highest scan number.
   * @return tripletMap a {@link org.apache.commons.collections4.map.MultiKeyMap} object. This is
   *         whole sparse matrix.
   * @param roundedmz a int.
   */
  public List<Triplet> restoreDataPoints(int roundedmz, int lowerScanBound, int upperScanBound) {

    Triplet searchTriplet = new Triplet();

    for (int i = lowerScanBound; i <= upperScanBound; i++) {

      searchTriplet.mz = roundedmz;
      searchTriplet.scanListIndex = i;
      int index = Collections.binarySearch(filterListOfTriplet, searchTriplet, compareScanMz);
      if (index >= 0) {
        Triplet triplet = filterListOfTriplet.get(index);
        triplet.removed = 0;
      }
    }
    return filterListOfTriplet;
  }

  /**
   * <p>
   * This method rounds mz value based on roundMz variable
   * </p>
   *
   * @param mz a {@link Double} object. This takes original m/z value from raw file.
   * @return roundedmz a {@link Integer} object. This value is rounded by multiplying
   *         10000.
   */
  public int roundMZ(double mz) {
    int roundedmz = (int) Math.round(mz * roundMzFactor);
    return roundedmz;
  }

  /**
   * <p>
   * This method sets maxIntensityIndex to 0
   * </p>
   */
  public void setMaxIntensityIndexZero() {
    maxIntensityIndex = 0;
  }

  /**
   * <p>
   * This method returns size of raw data file in terms of total scans.
   * </p>
   *
   * @return size a {@link Integer} object. This is total number of scans in raw file.
   */
  public int getSizeOfRawDataFile() {
    int size = listOfScans.size();
    return size;
  }

  /**
   * <p>
   * This method returns retention time array for given upper and lower scan bounds.
   * </p>
   *
   * @return retentionTime a {@link Float} array.
   * @param lowerScanBound a int.
   * @param upperScanbound a int.
   */
  public float[] getRetentionTimeArray(int lowerScanBound, int upperScanbound) {

    float[] retentionTime = new float[upperScanbound - lowerScanBound + 1];

    for (int i = 0; i < upperScanbound - lowerScanBound + 1; i++) {
      retentionTime[i] = (float) getRetentionTime(i + lowerScanBound);
    }
    return retentionTime;
  }

  /**
   * <p>
   * This method returns intensity array for detected peak
   * </p>
   *
   * @return intensities a {@link Float} array.
   * @param peak a {@link ADAP3DPeakDetectionAlgorithm.GoodPeakInfo} object.
   */
  public float[] getIntensities(GoodPeakInfo peak) {

    float[] intensities = new float[peak.upperScanBound - peak.lowerScanBound + 1];

    for (int i = 0; i < peak.upperScanBound - peak.lowerScanBound + 1; i++) {
      Triplet searchTriplet = new Triplet();
      searchTriplet.scanListIndex = i + peak.lowerScanBound;
      searchTriplet.mz = roundMZ(peak.mz);

      int index = Collections.binarySearch(filterListOfTriplet, searchTriplet, compareScanMz);
      if (index >= 0) {
        intensities[i] = filterListOfTriplet.get(index).intensity;
      } else {
        intensities[i] = 0;
      }
    }

    return intensities;
  }

  /**
   * <p>
   * This method returns retention time for given scan number.
   * </p>
   *
   * @return {@link Double} object.
   * @param scanNumber a int.
   */
  public double getRetentionTime(int scanNumber) {
    return rtMap.get(scanNumber);
  }

  /**
   * <p>
   * This method tracks progress of algorithm
   * </p>
   *
   * @return progress a {@link Float} object.
   * @param maxIntensityTriplet a {@link Triplet} object.
   */
  public float getFinishedPercent(Triplet maxIntensityTriplet) {

    int index = Collections.binarySearch(sortListAccordingToIntensity, maxIntensityTriplet,
        compareIntensity);
    float progress = index / sortListAccordingToIntensity.size();
    return progress;

  }

  public double numOfScans() {return listOfScans.size();}
}
