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

package io.github.mzmine.util.scans;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.ImsMsMsInfo;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.MergedMsMsSpectrum;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.exceptions.MissingMassListException;
import io.github.mzmine.util.scans.sorting.ScanSortMode;
import io.github.mzmine.util.scans.sorting.ScanSorter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.text.Format;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Scan related utilities
 */
public class ScanUtils {

  /**
   * Common utility method to be used as Scan.toString() method in various Scan implementations
   *
   * @param scan Scan to be converted to String
   * @return String representation of the scan
   */
  public static @Nonnull String scanToString(@Nonnull Scan scan, @Nonnull Boolean includeFileName) {
    StringBuffer buf = new StringBuffer();
    Format rtFormat = MZmineCore.getConfiguration().getRTFormat();
    Format mzFormat = MZmineCore.getConfiguration().getMZFormat();
    if (includeFileName) {
      buf.append(scan.getDataFile().getName());
    }
    buf.append("#");
    buf.append(scan.getScanNumber());
    buf.append(" @");
    buf.append(rtFormat.format(scan.getRetentionTime()));
    buf.append(" MS");
    buf.append(scan.getMSLevel());
    if (scan.getMSLevel() > 1) {
      buf.append(" (" + mzFormat.format(scan.getPrecursorMZ()) + ")");
    }
    switch (scan.getSpectrumType()) {
      case CENTROIDED:
        buf.append(" c");
        break;
      case PROFILE:
        buf.append(" p");
        break;
      case THRESHOLDED:
        buf.append(" t");
        break;
    }

    buf.append(" ");
    buf.append(scan.getPolarity().asSingleChar());

    if(scan instanceof MergedMsMsSpectrum) {
      buf.append(" merged");
      buf.append(((MergedMsMsSpectrum) scan).getSourceSpectra().size());
      buf.append(" spectra");
    }

    /*
     * if ((scan.getScanDefinition() != null) && (scan.getScanDefinition().length() > 0)) {
     * buf.append(" ("); buf.append(scan.getScanDefinition()); buf.append(")"); }
     */

    return buf.toString();
  }

  @Deprecated
  public static DataPoint[] extractDataPoints(MassSpectrum spectrum) {
    DataPoint result[] = new DataPoint[spectrum.getNumberOfDataPoints()];
    for (int i = 0; i < spectrum.getNumberOfDataPoints(); i++) {
      result[i] = new SimpleDataPoint(spectrum.getMzValue(i), spectrum.getIntensityValue(i));
    }
    return result;
  }

  /**
   * Find a base peak of a given scan in a given m/z range
   *
   * @param scan    Scan to search
   * @param mzRange mz range to search in
   * @return double[2] containing base peak m/z and intensity
   */
  @Nullable
  public static DataPoint findBasePeak(@Nonnull Scan scan, @Nonnull Range<Double> mzRange) {

    double baseMz = 0d;
    double baseIntensity = 0d;
    for (int i = 0; i < scan.getNumberOfDataPoints(); i++) {
      double mz = scan.getMzValue(i);
      if (!mzRange.contains(mz)) {
        continue;
      }
      double intensity = scan.getIntensityValue(i);
      if (intensity > baseIntensity) {
        baseIntensity = intensity;
        baseMz = mz;
      }
    }
    return new SimpleDataPoint(baseMz, baseIntensity);
  }

  /**
   * Calculate the total ion count of a scan within a given mass range.
   *
   * @param scan the scan.
   * @param mzRange mass range.
   * @return the total ion count of the scan within the mass range.
   */
  public static double calculateTIC(Scan scan, Range<Double> mzRange) {

    double tic = 0.0;
    for (final DataPoint dataPoint : selectDataPointsByMass(extractDataPoints(scan), mzRange)) {
      tic += dataPoint.getIntensity();
    }
    return tic;
  }

  /**
   * Selects data points within given m/z range
   */
  public static DataPoint[] selectDataPointsByMass(DataPoint dataPoints[], Range<Double> mzRange) {
    ArrayList<DataPoint> goodPoints = new ArrayList<DataPoint>();
    for (DataPoint dp : dataPoints) {
      if (mzRange.contains(dp.getMZ())) {
        goodPoints.add(dp);
      }
    }
    return goodPoints.toArray(new DataPoint[0]);
  }

  /**
   * Selects data points with intensity >= given intensity
   */
  public static DataPoint[] selectDataPointsOverIntensity(DataPoint dataPoints[],
      double minIntensity) {
    ArrayList<DataPoint> goodPoints = new ArrayList<DataPoint>();
    for (DataPoint dp : dataPoints) {
      if (dp.getIntensity() >= minIntensity) {
        goodPoints.add(dp);
      }
    }
    return goodPoints.toArray(new DataPoint[0]);
  }

  /**
   * This method bins values on x-axis. Each bin is assigned biggest y-value of all values in the
   * same bin.
   *
   * @param x X-coordinates of the data
   * @param y Y-coordinates of the data
   * @param binRange x coordinates of the left and right edge of the first bin
   * @param numberOfBins Number of bins
   * @param interpolate If true, then empty bins will be filled with interpolation using other bins
   * @param binningType Type of binning (sum of all 'y' within a bin, max of 'y', min of 'y', avg of
   *        'y')
   * @return Values for each bin
   */
  public static double[] binValues(double[] x, double[] y, Range<Double> binRange, int numberOfBins,
      boolean interpolate, BinningType binningType) {

    Double[] binValues = new Double[numberOfBins];
    double binWidth = (binRange.upperEndpoint() - binRange.lowerEndpoint()) / numberOfBins;

    double beforeX = Double.MIN_VALUE;
    double beforeY = 0.0f;
    double afterX = Double.MAX_VALUE;
    double afterY = 0.0f;

    double[] noOfEntries = null;

    // Binnings
    for (int valueIndex = 0; valueIndex < x.length; valueIndex++) {

      // Before first bin?
      if ((x[valueIndex] - binRange.lowerEndpoint()) < 0) {
        if (x[valueIndex] > beforeX) {
          beforeX = x[valueIndex];
          beforeY = y[valueIndex];
        }
        continue;
      }

      // After last bin?
      if ((binRange.upperEndpoint() - x[valueIndex]) < 0) {
        if (x[valueIndex] < afterX) {
          afterX = x[valueIndex];
          afterY = y[valueIndex];
        }
        continue;
      }

      int binIndex = (int) ((x[valueIndex] - binRange.lowerEndpoint()) / binWidth);

      // in case x[valueIndex] is exactly lastBinStop, we would overflow
      // the array
      if (binIndex == binValues.length) {
        binIndex--;
      }

      switch (binningType) {
        case MAX:
          if (binValues[binIndex] == null) {
            binValues[binIndex] = y[valueIndex];
          } else {
            if (binValues[binIndex] < y[valueIndex]) {
              binValues[binIndex] = y[valueIndex];
            }
          }
          break;
        case MIN:
          if (binValues[binIndex] == null) {
            binValues[binIndex] = y[valueIndex];
          } else {
            if (binValues[binIndex] > y[valueIndex]) {
              binValues[binIndex] = y[valueIndex];
            }
          }
          break;
        case AVG:
          if (noOfEntries == null) {
            noOfEntries = new double[binValues.length];
          }
          if (binValues[binIndex] == null) {
            noOfEntries[binIndex] = 1;
            binValues[binIndex] = y[valueIndex];
          } else {
            noOfEntries[binIndex]++;
            binValues[binIndex] += y[valueIndex];
          }
          break;

        case SUM:
        default:
          if (binValues[binIndex] == null) {
            binValues[binIndex] = y[valueIndex];
          } else {
            binValues[binIndex] += y[valueIndex];
          }
          break;

      }

    }

    // calculate the AVG
    if (binningType.equals(BinningType.AVG)) {
      assert noOfEntries != null;
      for (int binIndex = 0; binIndex < binValues.length; binIndex++) {
        if (binValues[binIndex] != null) {
          binValues[binIndex] /= noOfEntries[binIndex];
        }
      }
    }

    // Interpolation
    if (interpolate) {

      for (int binIndex = 0; binIndex < binValues.length; binIndex++) {
        if (binValues[binIndex] == null) {

          // Find exisiting left neighbour
          double leftNeighbourValue = beforeY;
          int leftNeighbourBinIndex =
              (int) Math.floor((beforeX - binRange.lowerEndpoint()) / binWidth);
          for (int anotherBinIndex = binIndex - 1; anotherBinIndex >= 0; anotherBinIndex--) {
            if (binValues[anotherBinIndex] != null) {
              leftNeighbourValue = binValues[anotherBinIndex];
              leftNeighbourBinIndex = anotherBinIndex;
              break;
            }
          }

          // Find existing right neighbour
          double rightNeighbourValue = afterY;
          int rightNeighbourBinIndex = (binValues.length - 1)
              + (int) Math.ceil((afterX - binRange.upperEndpoint()) / binWidth);
          for (int anotherBinIndex =
              binIndex + 1; anotherBinIndex < binValues.length; anotherBinIndex++) {
            if (binValues[anotherBinIndex] != null) {
              rightNeighbourValue = binValues[anotherBinIndex];
              rightNeighbourBinIndex = anotherBinIndex;
              break;
            }
          }

          double slope = (rightNeighbourValue - leftNeighbourValue)
              / (rightNeighbourBinIndex - leftNeighbourBinIndex);
          binValues[binIndex] =
              new Double(leftNeighbourValue + slope * (binIndex - leftNeighbourBinIndex));

        }

      }

    }

    double[] res = new double[binValues.length];
    for (int binIndex = 0; binIndex < binValues.length; binIndex++) {
      res[binIndex] = binValues[binIndex] == null ? 0 : binValues[binIndex];
    }
    return res;

  }

  /**
   * sort the data points by their m/z value. This method should be called before using other search
   * methods to do binary search in logarithmic time.
   *
   * @param dataPoints spectrum that should be sorted
   */
  public static void sortDataPointsByMz(DataPoint[] dataPoints) {
    Arrays.sort(dataPoints, Comparator.comparingDouble(DataPoint::getMZ));
  }

  /**
   * Returns the index of the datapoint with lowest m/z within the given datapoints which is within
   * the given mass range
   *
   * @param dataPoints sorted(!) list of datapoints
   * @param mzRange m/z range to search in
   * @return index of datapoint or -1, if no datapoint is in range
   */
  public static int findFirstFeatureWithin(DataPoint[] dataPoints, Range<Double> mzRange) {
    final int insertionPoint =
        Arrays.binarySearch(dataPoints, new SimpleDataPoint(mzRange.lowerEndpoint(), 0d),
            (u, v) -> Double.compare(u.getMZ(), v.getMZ()));
    if (insertionPoint < 0) {
      final int k = -insertionPoint - 1;
      if (k < dataPoints.length && mzRange.contains(dataPoints[k].getMZ())) {
        return k;
      } else {
        return -1;
      }
    } else {
      return insertionPoint;
    }
  }

  /**
   * Returns the index of the datapoint with largest m/z within the given datapoints which is within
   * the given mass range
   *
   * @param dataPoints sorted(!) list of datapoints
   * @param mzRange m/z range to search in
   * @return index of datapoint or -1, if no datapoint is in range
   */
  public static int findLastFeatureWithin(DataPoint[] dataPoints, Range<Double> mzRange) {
    final int insertionPoint =
        Arrays.binarySearch(dataPoints, new SimpleDataPoint(mzRange.upperEndpoint(), 0d),
            (u, v) -> Double.compare(u.getMZ(), v.getMZ()));
    if (insertionPoint < 0) {
      final int k = -insertionPoint - 2;
      if (k >= 0 && mzRange.contains(dataPoints[k].getMZ())) {
        return k;
      } else {
        return -1;
      }
    } else {
      return insertionPoint;
    }
  }

  /**
   * Returns the index of the datapoint with highest intensity within the given datapoints which is
   * within the given mass range
   *
   * @param dataPoints sorted(!) list of datapoints
   * @param mzRange m/z range to search in
   * @return index of datapoint or -1, if no datapoint is in range
   */
  public static int findMostIntenseFeatureWithin(DataPoint[] dataPoints, Range<Double> mzRange) {
    int k = findFirstFeatureWithin(dataPoints, mzRange);
    if (k < 0) {
      return -1;
    }
    int mostIntense = k;
    for (; k < dataPoints.length; ++k) {
      if (!mzRange.contains(dataPoints[k].getMZ())) {
        break;
      }
      if (dataPoints[k].getIntensity() > dataPoints[mostIntense].getIntensity()) {
        mostIntense = k;
      }
    }
    return mostIntense;
  }

  /**
   * Returns index of m/z value in a given array, which is closest to given value, limited by given
   * m/z tolerance. We assume the m/z array is sorted.
   *
   * @return index of best match, or -1 if no datapoint was found
   */
  public static int findClosestDatapoint(double key, double mzValues[], double mzTolerance) {

    int index = Arrays.binarySearch(mzValues, key);

    if (index >= 0) {
      return index;
    }

    // Get "insertion point"
    index = (index * -1) - 1;

    // If key value is bigger than biggest m/z value in array
    if (index == mzValues.length) {
      index--;
    } else if (index > 0) {
      // Check insertion point value and previous one, see which one
      // is closer
      if (Math.abs(mzValues[index - 1] - key) < Math.abs(mzValues[index] - key)) {
        index--;
      }
    }

    // Check m/z tolerancee
    if (Math.abs(mzValues[index] - key) <= mzTolerance) {
      return index;
    }

    // Nothing was found
    return -1;

  }

  /**
   * Determines if the spectrum represented by given array of data points is centroided or
   * continuous (profile or thresholded). Profile spectra are easy to detect, because they contain
   * zero-intensity data points. However, distinguishing centroided from thresholded spectra is not
   * trivial. MZmine uses multiple checks for that purpose, as described in the code comments.
   */
  /*
   * Adapted from MSDK: https://github.com/msdk/msdk/blob/master/msdk-spectra/
   * msdk-spectra-spectrumtypedetection/src/main/java/io/github/
   * msdk/spectra/spectrumtypedetection/SpectrumTypeDetectionAlgorithm.java
   */
  public static MassSpectrumType detectSpectrumType(@Nonnull double[] mzValues,
      double[] intensityValues) {

    // If the spectrum has less than 5 data points, it should be centroided.
    if (mzValues.length < 5) {
      return MassSpectrumType.CENTROIDED;
    }

    int basePeakIndex = 0;
    boolean hasZeroDataPoint = false;

    // Go through the data points and find the highest one
    int size = mzValues.length;
    for (int i = 0; i < size; i++) {

      // Update the maxDataPointIndex accordingly
      if (intensityValues[i] > intensityValues[basePeakIndex]) {
        basePeakIndex = i;
      }

      if (intensityValues[i] == 0.0) {
        hasZeroDataPoint = true;
      }
    }

    final double scanMzSpan = mzValues[size - 1] - mzValues[0];

    // Find the all data points around the base peak that have intensity
    // above half maximum
    final double halfIntensity = intensityValues[basePeakIndex] / 2.0;
    int leftIndex = basePeakIndex;
    while ((leftIndex > 0) && intensityValues[leftIndex - 1] > halfIntensity) {
      leftIndex--;
    }
    int rightIndex = basePeakIndex;
    while ((rightIndex < size - 1) && intensityValues[rightIndex + 1] > halfIntensity) {
      rightIndex++;
    }
    final double mainFeatureMzSpan = mzValues[rightIndex] - mzValues[leftIndex];
    final int mainFeatureDataPointCount = rightIndex - leftIndex + 1;

    // If the main feature has less than 3 data points above half intensity, it
    // indicates a centroid spectrum. Further, if the m/z span of the main
    // feature is more than 0.1% of the scan m/z range, it also indicates a
    // centroid spectrum. These criteria are empirical and probably not
    // bulletproof. However, it works for all the test cases we have.
    if ((mainFeatureDataPointCount < 3) || (mainFeatureMzSpan > (scanMzSpan / 1000.0))) {
      return MassSpectrumType.CENTROIDED;
    } else {
      if (hasZeroDataPoint) {
        return MassSpectrumType.PROFILE;
      } else {
        return MassSpectrumType.THRESHOLDED;
      }
    }

  }

  /**
   * Finds the MS/MS scan with highest intensity, within given retention time range and with
   * precursor m/z within given m/z range
   */
  public static Scan findBestFragmentScan(RawDataFile dataFile, Range<Float> rtRange,
      Range<Double> mzRange) {

    assert dataFile != null;
    assert rtRange != null;
    assert mzRange != null;

    return dataFile.getScanNumbers(2).stream()
        .filter(s -> rtRange.contains(s.getRetentionTime()) && mzRange.contains(s.getPrecursorMZ()))
        .max(Comparator.comparingDouble(s -> s.getBasePeakIntensity())).orElse(null);
  }

  /**
   * Finds all MS/MS scans on MS2 level within given retention time range and with precursor m/z
   * within given m/z range
   */
  public static Scan[] findAllMS2FragmentScans(RawDataFile dataFile, Range<Float> rtRange,
      Range<Double> mzRange) {
    assert dataFile != null;
    assert rtRange != null;
    assert mzRange != null;

    return dataFile.getScanNumbers(2).stream()
        .filter(s -> rtRange.contains(s.getRetentionTime()) && mzRange.contains(s.getPrecursorMZ()))
        .toArray(Scan[]::new);
  }

  /**
   *
   * @param dataFile
   * @param msLevel 0 for all scans
   * @return
   */
  public static Stream<Scan> streamScans(RawDataFile dataFile, int msLevel) {
    return dataFile.getScanNumbers(msLevel).stream();
  }

  @Nullable
  public static List<ImsMsMsInfo> findMsMsInfos(IMSRawDataFile imsRawDataFile,
      Range<Double> mzRange, Range<Float> rtRange) {
    List<ImsMsMsInfo> featureMsMsInfos = new ArrayList<>();
    Collection<? extends Frame> ms2Frames = imsRawDataFile.getFrames(2, rtRange);
    for (Frame frame : ms2Frames) {
      Set<ImsMsMsInfo> frameMsMsInfos = frame.getImsMsMsInfos();
      for (ImsMsMsInfo msmsInfo : frameMsMsInfos) {
        if (mzRange.contains(msmsInfo.getLargestPeakMz())) {
          featureMsMsInfos.add(msmsInfo);
        }
      }
    }
    if (featureMsMsInfos.isEmpty()) {
      return null;
    }
    return featureMsMsInfos;
  }

  /**
   * Find the highest data point in array
   */
  public static @Nonnull DataPoint findTopDataPoint(@Nonnull DataPoint dataPoints[]) {

    DataPoint topDP = null;

    for (DataPoint dp : dataPoints) {
      if ((topDP == null) || (dp.getIntensity() > topDP.getIntensity())) {
        topDP = dp;
      }
    }

    return topDP;
  }

  /**
   * Find the highest data point index in array
   */
  public static int findTopDataPoint(@Nonnull double intensityValues[]) {

    int basePeak = 0;
    for (int i = 0; i < intensityValues.length; i++) {

      if (intensityValues[i] > intensityValues[basePeak]) {
        basePeak = i;
      }
    }
    return basePeak;
  }

  /**
   * Find the m/z range of the data points in the array. We assume there is at least one data point,
   * and the data points are sorted by m/z.
   */
  public static @Nonnull Range<Double> findMzRange(@Nonnull DataPoint dataPoints[]) {

    assert dataPoints.length > 0;

    double lowMz = dataPoints[0].getMZ();
    double highMz = lowMz;
    for (int i = 1; i < dataPoints.length; i++) {
      if (dataPoints[i].getMZ() < lowMz) {
        lowMz = dataPoints[i].getMZ();
        continue;
      }
      if (dataPoints[i].getMZ() > highMz) {
        highMz = dataPoints[i].getMZ();
      }
    }

    return Range.closed(lowMz, highMz);
  }

  /**
   * Find the m/z range of the data points in the array. We assume there is at least one data point,
   * and the data points are sorted by m/z.
   */
  public static @Nonnull Range<Double> findMzRange(@Nonnull double mzValues[]) {

    assert mzValues.length > 0;

    double lowMz = mzValues[0];
    double highMz = lowMz;
    for (int i = 1; i < mzValues.length; i++) {
      if (mzValues[i] < lowMz) {
        lowMz = mzValues[i];
        continue;
      }
      if (mzValues[i] > highMz) {
        highMz = mzValues[i];
      }
    }

    return Range.closed(lowMz, highMz);
  }

  /**
   * Find the RT range of given scans. We assume there is at least one scan.
   */
  public static @Nonnull Range<Float> findRtRange(@Nonnull Scan scans[]) {

    assert scans.length > 0;

    float lowRt = scans[0].getRetentionTime();
    float highRt = lowRt;
    for (int i = 1; i < scans.length; i++) {
      if (scans[i].getRetentionTime() < lowRt) {
        lowRt = scans[i].getRetentionTime();
        continue;
      }
      if (scans[i].getRetentionTime() > highRt) {
        highRt = scans[i].getRetentionTime();
      }
    }

    return Range.closed(lowRt, highRt);
  }

  public static byte[] encodeDataPointsToBytes(DataPoint dataPoints[]) {
    ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
    DataOutputStream featureStream = new DataOutputStream(byteStream);
    for (int i = 0; i < dataPoints.length; i++) {

      try {
        featureStream.writeDouble(dataPoints[i].getMZ());
        featureStream.writeDouble(dataPoints[i].getIntensity());
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    byte featureBytes[] = byteStream.toByteArray();
    return featureBytes;
  }

  public static char[] encodeDataPointsBase64(DataPoint dataPoints[]) {
    byte featureBytes[] = encodeDataPointsToBytes(dataPoints);
    char encodedData[] = Base64.getEncoder().encodeToString(featureBytes).toCharArray();
    return encodedData;
  }

  public static DataPoint[] decodeDataPointsFromBytes(byte bytes[]) {
    // each double is 8 bytes and we need one for m/z and one for intensity
    int dpCount = bytes.length / 2 / 8;

    // make a data input stream
    ByteArrayInputStream byteStream = new ByteArrayInputStream(bytes);
    DataInputStream featureStream = new DataInputStream(byteStream);

    DataPoint dataPoints[] = new DataPoint[dpCount];

    for (int i = 0; i < dataPoints.length; i++) {
      try {
        double mz = featureStream.readDouble();
        double intensity = featureStream.readDouble();
        dataPoints[i] = new SimpleDataPoint(mz, intensity);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    return dataPoints;
  }

  public static DataPoint[] decodeDataPointsBase64(char encodedData[]) {
    byte[] bytes = Base64.getDecoder().decode(new String(encodedData));
    DataPoint dataPoints[] = decodeDataPointsFromBytes(bytes);
    return dataPoints;
  }

  public static Stream<Scan> streamAllFragmentScans(FeatureListRow row) {
    return row.getAllMS2Fragmentations().stream();
  }

  /**
   * Sorted list (best first) of all MS2 fragmentation scans with n signals >= noiseLevel in the
   * specified or first massList, if none was specified
   *
   * @param row all MS2 scans of all features in this row
   * @param noiseLevel
   * @param minNumberOfSignals
   * @param sort the sorting property (best first, index=0)
   * @return
   */
  @Nonnull
  public static List<Scan> listAllFragmentScans(FeatureListRow row,
      double noiseLevel, int minNumberOfSignals, ScanSortMode sort)
      throws MissingMassListException {
    List<Scan> scans = listAllFragmentScans(row, noiseLevel, minNumberOfSignals);
    // first entry is the best scan
    scans.sort(Collections.reverseOrder(new ScanSorter(noiseLevel, sort)));
    return scans;
  }

  /**
   * List of all MS2 fragmentation scans with n signals >= noiseLevel in the specified or first
   * massList, if none was specified
   *
   * @param row
   * @param noiseLevel
   * @param minNumberOfSignals
   * @return
   */
  @Nonnull
  public static ObservableList<Scan> listAllFragmentScans(FeatureListRow row,
      double noiseLevel, int minNumberOfSignals)
      throws MissingMassListException {
    ObservableList<Scan> scans = row.getAllMS2Fragmentations();
    return listAllScans(scans, noiseLevel, minNumberOfSignals);
  }

  /**
   * Sorted list of all MS1 {@link Feature#getRepresentativeScan()} of all features. scans with n
   * signals >= noiseLevel in the specified or first massList, if none was specified
   *
   * @param row all representative MS1 scans of all features in this row
   * @param noiseLevel
   * @param minNumberOfSignals
   * @param sort the sorting property (best first, index=0)
   * @return
   */
  @Nonnull
  public static List<Scan> listAllMS1Scans(FeatureListRow row, double noiseLevel,
      int minNumberOfSignals, ScanSortMode sort) throws MissingMassListException {
    List<Scan> scans = listAllMS1Scans(row, noiseLevel, minNumberOfSignals);
    // first entry is the best scan
    scans.sort(Collections.reverseOrder(new ScanSorter(noiseLevel, sort)));
    return scans;
  }

  /**
   * List of all MS1 {@link Feature#getRepresentativeScan()} of all features. scans with n signals
   * >= noiseLevel in the specified or first massList, if none was specified
   *
   * @param row
   * @param noiseLevel
   * @param minNumberOfSignals
   * @return
   */
  @Nonnull
  public static ObservableList<Scan> listAllMS1Scans(FeatureListRow row,
      double noiseLevel, int minNumberOfSignals)
      throws MissingMassListException {
    ObservableList<Scan> scans = getAllMostIntenseMS1Scans(row);
    return listAllScans(scans, noiseLevel, minNumberOfSignals);
  }

  /**
   * Array of all {@link Feature#getRepresentativeScan()} of all features
   *
   * @param row
   * @return
   */
  public static ObservableList<Scan> getAllMostIntenseMS1Scans(FeatureListRow row) {
    return row.getFeatures().stream().map(Feature::getRepresentativeScan).filter(Objects::nonNull)
        .collect(Collectors.toCollection(FXCollections::observableArrayList));
  }

  /**
   * List of all scans with n signals >= noiseLevel in the specified or first massList, if none was
   * specified
   *
   * @param noiseLevel
   * @param minNumberOfSignals
   * @return
   */
  @Nonnull
  public static ObservableList<Scan> listAllScans(ObservableList<Scan> scans,
      double noiseLevel, int minNumberOfSignals, ScanSortMode sort)
      throws MissingMassListException {
    ObservableList<Scan> filtered =
        listAllScans(scans, noiseLevel, minNumberOfSignals);
    // first entry is the best scan
    filtered.sort(Collections.reverseOrder(new ScanSorter(noiseLevel, sort)));
    return filtered;
  }

  /**
   * List of all scans with n signals >= noiseLevel in the specified or first massList, if none was
   * specified
   *
   * @param noiseLevel
   * @param minNumberOfSignals
   * @return
   */
  @Nonnull
  public static ObservableList<Scan> listAllScans(ObservableList<Scan> scans,
      double noiseLevel, int minNumberOfSignals)
      throws MissingMassListException {
    ObservableList<Scan> filtered = FXCollections.observableArrayList();
    for (Scan scan : scans) {
      // find mass list: with name or first
      final MassList massList = scan.getMassList();
      if (massList == null) {
        throw new MissingMassListException(scan);
      }

      // minimum number of signals >= noiseLevel
      int signals = getNumberOfSignals(massList, noiseLevel);
      if (signals >= minNumberOfSignals) {
        filtered.add(scan);
      }
    }
    return filtered;
  }

  /**
   * Sum of intensity of all data points >= noiseLevel
   *
   * @param data
   * @param noiseLevel
   * @return
   */
  public static double getTIC(DataPoint[] data, double noiseLevel) {
    return Stream.of(data).mapToDouble(DataPoint::getIntensity).filter(i -> i >= noiseLevel).sum();
  }

  /**
   * threshold: keep data points >= noiseLevel
   *
   * @param data
   * @param noiseLevel
   * @return
   */
  public static DataPoint[] getFiltered(DataPoint[] data, double noiseLevel) {
    return Stream.of(data).filter(dp -> dp.getIntensity() >= noiseLevel).toArray(DataPoint[]::new);
  }

  /**
   * below threshold: keep data points < noiseLevel
   *
   * @param data
   * @param noiseLevel
   * @return
   */
  public static DataPoint[] getBelowThreshold(DataPoint[] data, double noiseLevel) {
    return Stream.of(data).filter(dp -> dp.getIntensity() < noiseLevel).toArray(DataPoint[]::new);
  }

  /**
   * Number of signals >=noiseLevel
   *
   * @param data
   * @param noiseLevel
   * @return
   */
  public static int getNumberOfSignals(DataPoint[] data, double noiseLevel) {
    int n = 0;
    for (DataPoint dp : data) {
      if (dp.getIntensity() >= noiseLevel) {
        n++;
      }
    }
    return n;
  }


  /**
   * Sum of intensity of all data points >= noiseLevel
   *
   * @param spec
   * @param noiseLevel
   * @return
   */
  public static double getTIC(MassSpectrum spec, double noiseLevel) {
    int size = spec.getNumberOfDataPoints();
    double sum = 0;
    for(int i=0; i<size; i++) {
      double intensity = spec.getIntensityValue(i);
      if(intensity >= noiseLevel)
        sum += intensity;
    }
    return sum;
  }

  /**
   * Number of signals >=noiseLevel
   *
   * @param spec
   * @param noiseLevel
   * @return
   */
  public static int getNumberOfSignals(MassSpectrum spec, double noiseLevel) {
    int size = spec.getNumberOfDataPoints();
    int n = 0;
    for(int i=0; i<size; i++) {
      if(spec.getIntensityValue(i) >= noiseLevel)
        n++;
    }
    return n;
  }

  /**
   * Finds the first MS1 scan preceding the given MS2 scan. If no such scan exists, returns null.
   */
  public static @Nullable Scan findPrecursorScan(@Nonnull Scan scan) {

    assert scan != null;
    final RawDataFile dataFile = scan.getDataFile();
    final ObservableList<Scan> scanNumbers = dataFile.getScans();

    int startIndex = scanNumbers.indexOf(scan);

    for (int i = startIndex; i >= 0; i--) {
      Scan s = scanNumbers.get(i);
      if (s.getMSLevel() == 1) {
        return s;
      }
    }

    // Didn't find any MS1 scan
    return null;
  }

  /**
   * Finds the first MS1 scan succeeding the given MS2 scan. If no such scan exists, returns null.
   */
  @Nullable
  public static Scan findSucceedingPrecursorScan(@Nonnull Scan scan) {
    assert scan != null;
    final RawDataFile dataFile = scan.getDataFile();
    final ObservableList<Scan> scanNumbers = dataFile.getScans();

    int startIndex = scanNumbers.indexOf(scan);

    for (int i = startIndex; i < scanNumbers.size(); i++) {
      Scan s = scanNumbers.get(i);
      if (s.getMSLevel() == 1) {
        return s;
      }
    }

    // Didn't find any MS1 scan
    return null;
  }

  /**
   * Selects best N MS/MS scans from a feature list row
   */
  public static @Nonnull Collection<Scan> selectBestMS2Scans(@Nonnull FeatureListRow row,
      @Nonnull Integer topN) throws MissingMassListException {
    final @Nonnull List<Scan> allMS2Scans = row.getAllMS2Fragmentations();
    return selectBestMS2Scans(allMS2Scans, topN);
  }

  /**
   * Selects best N MS/MS scans from a collection of scans
   */
  public static @Nonnull Collection<Scan> selectBestMS2Scans(@Nonnull Collection<Scan> scans,
      @Nonnull Integer topN) throws MissingMassListException {
    assert scans != null;
    assert topN != null;

    // Keeps MS2 scans sorted by decreasing quality
    TreeSet<Scan> sortedScans = new TreeSet<>(
        Collections.reverseOrder(new ScanSorter(0, ScanSortMode.MAX_TIC)));
    sortedScans.addAll(scans);

    // Filter top N scans into an immutable list
    final List<Scan> topNScansList =
        sortedScans.stream().limit(topN).collect(ImmutableList.toImmutableList());

    return topNScansList;
  }

  /**
   * Move the mass window given by binRange across the spectrum, keep only the
   * numberOfFeaturesPerBin most intense features within the window. This is a very simple and
   * robust method to remove most noise in the spectrum without having to estimate any noise
   * intensity parameter.
   *
   * @param dataPoints spectrum
   * @param binRange sliding mass window. Is shifted in each step by its width.
   * @param numberOfFeaturesPerBin number of features to keep within the sliding mass window
   * @return
   */
  public static DataPoint[] extractMostIntenseFeaturesAcrossMassRange(DataPoint[] dataPoints,
      Range<Double> binRange, int numberOfFeaturesPerBin) {
    double offset = binRange.lowerEndpoint();
    final double width = binRange.upperEndpoint() - binRange.lowerEndpoint();
    final HashMap<Integer, List<DataPoint>> bins = new HashMap<>();
    for (DataPoint p : dataPoints) {
      final int bin = (int) Math.floor((p.getMZ() - offset) / width);
      if (bin >= 0) {
        bins.computeIfAbsent(bin, (x) -> new ArrayList<>()).add(p);
      }
    }
    final List<DataPoint> finalDataPoints = new ArrayList<>();
    for (Integer bin : bins.keySet()) {
      List<DataPoint> list = bins.get(bin);
      Collections.sort(list, (u, v) -> Double.compare(v.getIntensity(), u.getIntensity()));
      for (int i = 0; i < Math.min(list.size(), numberOfFeaturesPerBin); ++i) {
        finalDataPoints.add(list.get(i));
      }
    }
    DataPoint[] spectrum = finalDataPoints.toArray(new DataPoint[0]);
    sortDataPointsByMz(spectrum);
    return spectrum;
  }

  /**
   * Generalization of the cosine similarity for high resolution. See Algorithmic Mass Spectrometry
   * by Sebastian Böcker, chapter 4.2 While the cosine similarity transforms the spectrum into a
   * finite dimensional vector, the probability product transforms it into a mixture of continuous
   * gaussians.
   * <p>
   * As for cosine similarity it is recommended to first take the square root of all feature
   * intensities, before calling this method.
   *
   * @param scanLeft the first spectrum
   * @param scanRight the second spectrum
   * @param expectedMassDeviationInPPM the width of the gaussians (corresponds to the expected mass
   *        deviation). Rather use a larger than a small value! Value is given in ppm and Dalton.
   * @param noiseLevel the lowest intensity for a feature to be considered
   * @param mzRange the m/z range in which the features are compared. use null for the whole
   *        spectrum
   */
  public static double probabilityProduct(DataPoint[] scanLeft, DataPoint[] scanRight,
      MZTolerance expectedMassDeviationInPPM, double noiseLevel, @Nullable Range<Double> mzRange) {
    double d = probabilityProductUnnormalized(scanLeft, scanRight, expectedMassDeviationInPPM,
        noiseLevel, mzRange);
    double l = probabilityProductUnnormalized(scanLeft, scanLeft, expectedMassDeviationInPPM,
        noiseLevel, mzRange);
    double r = probabilityProductUnnormalized(scanRight, scanRight, expectedMassDeviationInPPM,
        noiseLevel, mzRange);
    return d / Math.sqrt(l * r);

  }

  /**
   * Calculates the probability product without normalization. Usually, this method is only useful
   * if you plan to normalize the spectra (or value) yourself.
   *
   * @see #probabilityProduct(DataPoint[], DataPoint[], MZTolerance, double, Range)
   */
  public static double probabilityProductUnnormalized(DataPoint[] scanLeft, DataPoint[] scanRight,
      MZTolerance expectedMassDeviationInPPM, double noiseLevel, @Nullable Range<Double> mzRange) {
    int i, j;
    double score = 0d;
    final int nl, nr;// =left.length, nr=right.length;
    if (mzRange == null) {
      nl = scanLeft.length;
      nr = scanRight.length;
      i = 0;
      j = 0;
    } else {
      nl = findLastFeatureWithin(scanLeft, mzRange) + 1;
      nr = findLastFeatureWithin(scanRight, mzRange) + 1;
      i = findFirstFeatureWithin(scanLeft, mzRange);
      j = findFirstFeatureWithin(scanRight, mzRange);
      if (i < 0 || j < 0) {
        return 0d;
      }
    }
    // gaussians are set to zero above allowedDifference to speed up
    // computation
    final double allowedDifference = expectedMassDeviationInPPM.getMzToleranceForMass(1000d) * 5;
    while (i < nl && j < nr) {
      DataPoint lp = scanLeft[i];
      if (lp.getIntensity() < noiseLevel) {
        ++i;
        continue;
      }
      DataPoint rp = scanRight[j];
      if (rp.getIntensity() < noiseLevel) {
        ++j;
        continue;
      }
      final double difference = lp.getMZ() - rp.getMZ();
      if (Math.abs(difference) <= allowedDifference) {
        final double mzabs = expectedMassDeviationInPPM
            .getMzToleranceForMass(Math.round((lp.getMZ() + rp.getMZ()) / 2d));
        final double variance = mzabs * mzabs;
        double matchScore = probabilityProductScore(lp, rp, variance);
        score += matchScore;
        for (int k = i + 1; k < nl; ++k) {
          DataPoint lp2 = scanLeft[k];
          final double difference2 = lp2.getMZ() - rp.getMZ();
          if (Math.abs(difference2) <= allowedDifference) {
            matchScore = probabilityProductScore(lp2, rp, variance);
            score += matchScore;
          } else {
            break;
          }
        }
        for (int l = j + 1; l < nr; ++l) {
          DataPoint rp2 = scanRight[l];
          final double difference2 = lp.getMZ() - rp2.getMZ();
          if (Math.abs(difference2) <= allowedDifference) {
            matchScore = probabilityProductScore(lp, rp2, variance);
            score += matchScore;
          } else {
            break;
          }
        }
        ++i;
        ++j;
      } else if (difference > 0) {
        ++j;

      } else {
        ++i;
      }
    }
    return score;
  }

  /**
   * Calculates the product of the integrals of two gaussians centered in lp and rp with given
   * variance
   */
  private static double probabilityProductScore(DataPoint lp, DataPoint rp, double variance) {
    final double mzDiff = Math.abs(lp.getMZ() - rp.getMZ());
    final double constTerm = 1.0 / (Math.PI * variance * 4);
    final double propOverlap = constTerm * Math.exp(-(mzDiff * mzDiff) / (4 * variance));
    return (lp.getIntensity() * rp.getIntensity()) * propOverlap;
  }

  /**
   * Converts DataPoint ion mz to int.
   * <p>
   * Function adapted from module: adap.mspexport.
   *
   * @param dataPoints spectra to convert.
   * @param intMode conversion method: MAX or SUM.
   * @return DataPoint array converted to integers.
   */
  public static DataPoint[] integerDataPoints(final DataPoint[] dataPoints,
      final IntegerMode intMode) {

    int size = dataPoints.length;

    Map<Double, Double> integerDataPoints = new HashMap<>();

    for (int i = 0; i < size; ++i) {
      double mz = Math.round(dataPoints[i].getMZ());
      double intensity = dataPoints[i].getIntensity();
      Double prevIntensity = integerDataPoints.get(mz);
      if (prevIntensity == null) {
        prevIntensity = 0.0;
      }

      switch (intMode) {
        case MAX:
          integerDataPoints.put(mz, prevIntensity + intensity);
          break;
        case SUM:
          integerDataPoints.put(mz, Math.max(prevIntensity, intensity));
          break;
      }
    }

    DataPoint[] result = new DataPoint[integerDataPoints.size()];
    int count = 0;
    for (Map.Entry<Double, Double> e : integerDataPoints.entrySet()) {
      result[count++] = new SimpleDataPoint(e.getKey(), e.getValue());
    }

    return result;
  }

  /**
   * @param dataPoints Sorted (by mz, ascending) array of data points
   * @param mzRange
   * @return
   */
  @Nonnull
  public static DataPoint[] getDataPointsByMass(@Nonnull DataPoint[] dataPoints,
      @Nonnull Range<Double> mzRange) {

    int startIndex, endIndex;
    for (startIndex = 0; startIndex < dataPoints.length; startIndex++) {
      if (dataPoints[startIndex].getMZ() >= mzRange.lowerEndpoint()) {
        break;
      }
    }

    for (endIndex = startIndex; endIndex < dataPoints.length; endIndex++) {
      if (dataPoints[endIndex].getMZ() > mzRange.upperEndpoint()) {
        break;
      }
    }

    DataPoint pointsWithinRange[] = new DataPoint[endIndex - startIndex];

    // Copy the relevant points
    System.arraycopy(dataPoints, startIndex, pointsWithinRange, 0, endIndex - startIndex);

    return pointsWithinRange;
  }


  /**
   * Binning modes
   */
  public static enum BinningType {
    SUM, MAX, MIN, AVG
  }

  /**
   * Integer conversion methods.
   */
  public static enum IntegerMode {

    SUM("Merging mode: Sum"), MAX("Merging mode: Maximum");

    private final String intMode;

    IntegerMode(String intMode) {
      this.intMode = intMode;
    }

    @Override
    public String toString() {
      return this.intMode;
    }
  }
}
