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

package io.github.mzmine.modules.tools.msmsspectramerge;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A spectrum of merged peaks with meta information
 */
public class MergedSpectrum implements MassSpectrum {

  /**
   * spectral information
   */
  public final MergedDataPoint[] data;

  /**
   * data files which contribute to this merged spectrum
   */
  public final RawDataFile[] origins;

  /**
   * scans which contribute to this merged spectrum
   */
  public final int[] scanIds;

  /**
   * polarity of the feature
   */
  public final PolarityType polarity;

  /**
   * precursor charge of the feature
   */
  public final int precursorCharge;

  /**
   * precursor m/z of the feature
   */
  public final double precursorMz;

  /**
   * the maximum score over all source spectra. The reflects the quality of the MS/MS (high
   * intensity, low chimeric contamination)
   */
  protected double bestFragmentScanScore;

  /**
   * number of scans which were rejected due to low quality
   */
  public int removedScansByLowQuality;

  /**
   * number of scans which were rejected due to low cosine similarity
   */
  public int removedScansByLowCosine;

  private final static MergedSpectrum EMPTY = new MergedSpectrum(new MergedDataPoint[0],
      new RawDataFile[0], new int[0], 0d, PolarityType.UNKNOWN, 0, 0, 0, 0d);

  /**
   * @return an empty merged spectrum
   */
  public static MergedSpectrum empty() {
    return EMPTY;
  }

  /**
   * @return an empty merged spectrum with the information how many scans were rejected
   */
  public static MergedSpectrum empty(int removedScansByLowQuality) {
    return new MergedSpectrum(new MergedDataPoint[0], new RawDataFile[0], new int[0], 0d,
        PolarityType.UNKNOWN, 0, removedScansByLowQuality, 0, 0d);
  }

  public MergedSpectrum(MergedDataPoint[] data, Set<RawDataFile> origins, Set<Integer> scanIds,
      double precursorMz, PolarityType polarity, int precursorCharge, int removedScansByLowQuality,
      int removedScansByLowCosine) {
    this(data, origins.toArray(new RawDataFile[origins.size()]),
        scanIds.stream().sorted().mapToInt(x -> x).toArray(), precursorMz, polarity,
        precursorCharge, removedScansByLowQuality, removedScansByLowCosine, 0d);
  }

  /**
   * build a merged spectrum containing only a single spectrum
   * 
   * @param single the spectrum
   */
  public MergedSpectrum(Scan single) {
    DataPoint[] dataPoints = single.getMassList().getDataPoints();
    this.data = new MergedDataPoint[dataPoints.length];
    for (int k = 0; k < dataPoints.length; ++k) {
      this.data[k] =
          new MergedDataPoint(MzMergeMode.MOST_INTENSE, IntensityMergeMode.MAXIMUM, dataPoints[k]);
    }
    this.origins = new RawDataFile[] {single.getDataFile()};
    this.scanIds = new int[] {single.getScanNumber()};
    this.polarity = single.getPolarity();
    this.precursorCharge = Objects.requireNonNullElse(single.getPrecursorCharge(), 0);
    this.removedScansByLowCosine = 0;
    this.removedScansByLowQuality = 0;
    this.precursorMz = Objects.requireNonNullElse(single.getPrecursorMz(), 0d);
  }

  public MergedSpectrum(MergedDataPoint[] data, RawDataFile[] origins, int[] scanIds,
      double precursorMz, PolarityType polarity, int precursorCharge, int removedScansByLowQuality,
      int removedScansByLowCosine, double bestFragmentScanScore) {
    this.data = data;
    this.origins = origins;
    this.scanIds = scanIds;
    this.bestFragmentScanScore = bestFragmentScanScore;
    this.precursorMz = precursorMz;
    this.removedScansByLowQuality = removedScansByLowQuality;
    this.removedScansByLowCosine = removedScansByLowCosine;
    this.polarity = polarity;
    this.precursorCharge = precursorCharge;
  }

  @Override
  public String toString() {
    if (origins.length > 0) {
      final String name = origins[0].getName();
      if (origins.length > 1) {
        return "Merged spectrum from " + name + " and " + (origins.length - 1) + " others";
      } else {
        return "Merged spectrum from " + name;
      }
    } else
      return "merged spectrum";
  }

  public double getBestFragmentScanScore() {
    return bestFragmentScanScore;
  }

  /**
   * @return the total number of scans that contribute to this merged spectrum, including all
   *         rejected (low quality) scans
   */
  public int totalNumberOfScans() {
    return scanIds.length + removedScansByLowCosine + removedScansByLowQuality;
  }

  public String getMergeStatsDescription() {
    return String.format(Locale.US,
        "%d / %d (%d removed due to low quality, %d removed due to low cosine).", scanIds.length,
        totalNumberOfScans(), removedScansByLowQuality, removedScansByLowCosine);
  }

  /**
   * merge to spectra. The merging of data points itself is done within the MsMsSpectraMerge#merge
   * method
   * 
   * @param right spectra to merge
   * @param mergedSpectrum merged data points
   * @return merged spectrum containing meta information of both sources and the given merged data
   *         points
   */
  public MergedSpectrum merge(MergedSpectrum right, MergedDataPoint[] mergedSpectrum) {
    final RawDataFile[] norigs = Arrays.copyOf(origins, origins.length + right.origins.length);
    System.arraycopy(right.origins, 0, norigs, origins.length, right.origins.length);
    final int[] nscans = Arrays.copyOf(scanIds, scanIds.length + right.scanIds.length);
    System.arraycopy(right.scanIds, 0, nscans, scanIds.length, right.scanIds.length);
    MergedSpectrum newMerged = new MergedSpectrum(mergedSpectrum, norigs, nscans, right.precursorMz,
        right.polarity, right.precursorCharge,
        removedScansByLowQuality + right.removedScansByLowQuality,
        removedScansByLowCosine + right.removedScansByLowCosine,
        Math.max(bestFragmentScanScore, right.bestFragmentScanScore));
    return newMerged;
  }

  @Override
  public int getNumberOfDataPoints() {
    return data.length;
  }

  @Override
  public MassSpectrumType getSpectrumType() {
    return MassSpectrumType.CENTROIDED;
  }

  @Override
  public double[] getMzValues(@NotNull double[] dst) {
    throw new UnsupportedOperationException("Not supported for " + this.getClass().getName());
  }

  @Override
  public double[] getIntensityValues(@NotNull double[] dst) {
    throw new UnsupportedOperationException("Not supported for " + this.getClass().getName());
  }

  @Override
  public double getMzValue(int index) {
    throw new UnsupportedOperationException("Not supported for " + this.getClass().getName());
  }

  @Override
  public double getIntensityValue(int index) {
    throw new UnsupportedOperationException("Not supported for " + this.getClass().getName());
  }

  @Override
  public @Nullable Double getBasePeakMz() {
    throw new UnsupportedOperationException("Not supported for " + this.getClass().getName());
  }

  @Override
  public @Nullable Double getBasePeakIntensity() {
    throw new UnsupportedOperationException("Not supported for " + this.getClass().getName());
  }

  @Override
  public @Nullable Integer getBasePeakIndex() {
    throw new UnsupportedOperationException("Not supported for " + this.getClass().getName());
  }

  @Override
  public @Nullable Range<Double> getDataPointMZRange() {
    throw new UnsupportedOperationException("Not supported for " + this.getClass().getName());
  }

  /**
   * @return calculate the total ion count of this merged spectrum
   */
  public Double getTIC() {
    double tic = 0d;
    for (MergedDataPoint p : data) {
      tic += p.intensity;
    }
    return tic;
  }

  @Override
  public int binarySearch(double mz, boolean defaultToClosestMz) {
    return MassSpectrum.super.binarySearch(mz, defaultToClosestMz);
  }

  @Override
  public int binarySearch(double mz, boolean defaultToClosestMz, int fromIndex, int toIndex) {
    return MassSpectrum.super.binarySearch(mz, defaultToClosestMz, fromIndex, toIndex);
  }

  @Override
  public int indexOf(double mz, boolean defaultToClosestMz) {
    return MassSpectrum.super.indexOf(mz, defaultToClosestMz);
  }

  /**
   * @param minimumRelativeNumberOfScans in how many scans the peaks have to be contained (in
   *                                     percent)
   * @return new merged spectrum containing only peaks that occur consistently in source spectra
   */
  public MergedSpectrum filterByRelativeNumberOfScans(double minimumRelativeNumberOfScans) {
    int minNum = (int) (scanIds.length * minimumRelativeNumberOfScans);
    if (minNum > 1) {
      return filterByNumberOfScans(minNum);
    } else
      return this;
  }

  /**
   * @param minimumNumberOfScans in how many scans the peaks have to be contained
   * @return new merged spectrum containing only peaks that occur consistently in source spectra
   */
  public MergedSpectrum filterByNumberOfScans(int minimumNumberOfScans) {
    return new MergedSpectrum(
        Arrays.stream(data).filter(x -> x.sources.length >= minimumNumberOfScans)
            .toArray(MergedDataPoint[]::new), origins, scanIds, precursorMz, polarity,
        precursorCharge, removedScansByLowQuality, removedScansByLowCosine, bestFragmentScanScore

    );
  }

  @NotNull
  @Override
  public Iterator<DataPoint> iterator() {
    throw new UnsupportedOperationException("Not supported for " + this.getClass().getName());
  }

  @Override
  public void forEach(Consumer<? super DataPoint> action) {
    throw new UnsupportedOperationException("Not supported for " + this.getClass().getName());
  }

  @Override
  public Spliterator<DataPoint> spliterator() {
    throw new UnsupportedOperationException("Not supported for " + this.getClass().getName());
  }
}
