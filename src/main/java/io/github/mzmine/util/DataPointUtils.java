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

package io.github.mzmine.util;

import com.google.common.collect.Range;
import com.google.common.primitives.Doubles;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.util.collections.BinarySearch;
import java.nio.DoubleBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.IntToDoubleFunction;

public class DataPointUtils {

  /**
   * Used to keep legacy-modules running, not to be used in new modules. Directly access the
   * underlying DoubleBuffers of Scans or {@link io.github.mzmine.datamodel.featuredata.IonSeries}
   * and extending classes.
   *
   * @return 2-d array [mzs, intensities] with dimension double[2][dataPoints.length]. [0][i] will
   * contain mz, [1][i] will contain intensity values.
   */
  public static double[][] getDataPointsAsDoubleArray(DataPoint[] dataPoints) {
    double[][] data = new double[2][];
    data[0] = new double[dataPoints.length];
    data[1] = new double[dataPoints.length];
    for (int i = 0; i < dataPoints.length; i++) {
      data[0][i] = dataPoints[i].getMZ();
      data[1][i] = dataPoints[i].getIntensity();
    }
    return data;
  }

  /**
   * Used to keep legacy-modules running, not to be used in new modules. Directly access the
   * underlying DoubleBuffers of Scans or the
   * {@link io.github.mzmine.datamodel.features.ModularFeature}'s
   * {@link io.github.mzmine.datamodel.featuredata.IonSeries} and extending classes.
   *
   * @return array of [2][] with [mzs, intensities]
   * @see ModularFeature#getFeatureData()
   */
  public static double[][] getDataPointsAsDoubleArray(Collection<? extends DataPoint> dataPoints) {
    double[][] data = new double[2][];
    data[0] = new double[dataPoints.size()];
    data[1] = new double[dataPoints.size()];

    int i = 0;
    for (DataPoint dp : dataPoints) {
      data[0][i] = dp.getMZ();
      data[1][i] = dp.getIntensity();
      i++;
    }
    return data;
  }

  /**
   * Used when copying an {@link io.github.mzmine.datamodel.featuredata.IonSpectrumSeries} and
   * subclasses. Usually, the data should be accessed directly via the buffer.
   *
   * @return array of [2][] with [mzs, intensities]
   */
  public static double[][] getDataPointsAsDoubleArray(DoubleBuffer mzValues,
      DoubleBuffer intensityValues) {
    assert mzValues.limit() == intensityValues.limit();
    return new double[][]{getDoubleBufferAsArray(mzValues),
        getDoubleBufferAsArray(intensityValues)};
  }

  /**
   * Used when copying an {@link io.github.mzmine.datamodel.featuredata.IonSpectrumSeries} and
   * subclasses. Usually, the data should be accessed directly via the buffer.
   */
  public static double[] getDoubleBufferAsArray(DoubleBuffer values) {
    double[] data = new double[values.limit()];
    // set start to 0 to get absolute array not relative to point
    values.get(0, data);
    return data;
  }

  /**
   * Used in legacy classes and to keep up compatibility. Do not use in new classes, directly refer
   * to the DoubleBuffers of Scans or {@link io.github.mzmine.datamodel.featuredata.IonSeries} and
   * extending classes.
   */
  @Deprecated
  public static double[] getMZsAsDoubleArray(DataPoint[] dataPoints) {
    double[] data = new double[dataPoints.length];

    for (int i = 0; i < dataPoints.length; i++) {
      data[i] = dataPoints[i].getMZ();
    }
    return data;
  }

  /**
   * Used in legacy classes and to keep up compatibility. Do not use in new classes, directly refer
   * to the DoubleBuffers of Scans or {@link io.github.mzmine.datamodel.featuredata.IonSeries} and
   * extending classes.
   */
  @Deprecated
  public static double[] getIntenstiesAsDoubleArray(DataPoint[] dataPoints) {
    double[] data = new double[dataPoints.length];

    for (int i = 0; i < dataPoints.length; i++) {
      data[i] = dataPoints[i].getIntensity();
    }
    return data;
  }

  public static double[][] getDatapointsAboveNoiseLevel(DoubleBuffer rawMzs,
      DoubleBuffer rawIntensities, double noiseLevel) {
    assert rawMzs.limit() == rawIntensities.limit();

    List<Double> mzs = new ArrayList<>();
    List<Double> intensities = new ArrayList<>();

    for (int i = 0; i < rawMzs.limit(); i++) {
      if (rawIntensities.get(i) > noiseLevel) {
        mzs.add(rawMzs.get(i));
        intensities.add(rawIntensities.get(i));
      }
    }
    double[][] data = new double[2][];
    data[0] = Doubles.toArray(mzs);
    data[1] = Doubles.toArray(intensities);
    return data;
  }

  public static double[][] getDatapointsAboveNoiseLevel(double[] rawMzs, double[] rawIntensities,
      double noiseLevel) {
    assert rawMzs.length == rawIntensities.length;

    List<Double> mzs = new ArrayList<>();
    List<Double> intensities = new ArrayList<>();

    for (int i = 0; i < rawMzs.length; i++) {
      if (rawIntensities[i] > noiseLevel) {
        mzs.add(rawMzs[i]);
        intensities.add(rawIntensities[i]);
      }
    }
    double[][] data = new double[2][];
    data[0] = Doubles.toArray(mzs);
    data[1] = Doubles.toArray(intensities);
    return data;
  }

  /**
   * @param rawMzs         array of mz values
   * @param rawIntensities array of intensity values
   * @param mzRange        the mz range
   * @return double[2][n], [0][] being mz values, [1][] being intensity values
   */
  public static double[][] getDataPointsInMzRange(double[] rawMzs, double[] rawIntensities,
      Range<Double> mzRange) {
    assert rawMzs.length == rawIntensities.length;

    List<Double> mzs = new ArrayList<>();
    List<Double> intensities = new ArrayList<>();

    for (int i = 0; i < rawMzs.length; i++) {
      if (mzRange.contains(rawMzs[i])) {
        mzs.add(rawMzs[i]);
        intensities.add(rawIntensities[i]);
      } else if (mzRange.upperEndpoint() < rawMzs[i] || rawMzs[i] == 0.0) {
        break;
      }
    }
    double[][] data = new double[2][];
    data[0] = Doubles.toArray(mzs);
    data[1] = Doubles.toArray(intensities);
    return data;
  }

  /**
   * @return array of data points
   */
  public static DataPoint[] getDataPoints(double[] mzs, double[] intensities) {
    assert mzs.length == intensities.length;
    DataPoint[] dps = new DataPoint[mzs.length];
    for (int i = 0; i < mzs.length; i++) {
      dps[i] = new SimpleDataPoint(mzs[i], intensities[i]);
    }
    return dps;
  }

  /**
   * Sorts the two arrays as data points
   *
   * @param mzs         mz values to be sorted
   * @param intensities intensity values to be sorted
   * @param sorter      sorting direction and property
   * @return sorted array of [2][length] for [mz, intensity]
   */
  public static double[][] sort(double[] mzs, double[] intensities, DataPointSorter sorter) {
    assert mzs.length == intensities.length;
    DataPoint[] dps = DataPointUtils.getDataPoints(mzs, intensities);
    Arrays.sort(dps, sorter);
    return getDataPointsAsDoubleArray(dps);
  }

  /**
   * Ensure sorting by mz ascending. Only applied if input data was unsorted.
   *
   * @param mzs         input mzs
   * @param intensities input intensities
   * @return [mzs, intensities], either the input arrays if already sorted or new sorted arrays
   */
  public static double[][] ensureSortingMzAscendingDefault(final double[] mzs,
      final double[] intensities) {
    for (int i = 1; i < mzs.length; i++) {
      if (mzs[i - 1] > mzs[i]) {
        return sort(mzs, intensities, DataPointSorter.DEFAULT_MZ_ASCENDING);
      }
    }
    return new double[][]{mzs, intensities};
  }


  /**
   * Apply intensityPercentage filter so that the returned array contains all data points that make
   * X % of the total intensity. The result is further cropped to a maxNumSignals.
   *
   * @param intensitySorted           sorted by intensity descending
   * @param targetIntensityPercentage intensity percentage,e.g., 0.99
   * @param maxNumSignals             maximum signals to crop to
   * @return filtered data points array that make either >=X% of intensity or have reached the
   * maxNumSignals
   */
  public static DataPoint[] filterDataByIntensityPercent(final DataPoint[] intensitySorted,
      double targetIntensityPercentage, int maxNumSignals) {
    double total = 0;
    for (final DataPoint dp : intensitySorted) {
      total += dp.getIntensity();
    }

    double sum = 0;
    for (int i = 0; i < intensitySorted.length; i++) {
      if (i >= maxNumSignals - 1) {
        // max signals reached
        return Arrays.copyOf(intensitySorted, maxNumSignals);
      }
      sum += intensitySorted[i].getIntensity();
      if (sum / total >= targetIntensityPercentage) {
        // intensity percentage reached
        return Arrays.copyOf(intensitySorted, Math.min(i + 1, maxNumSignals));
      }
    }
    // percent not reached - should not happen
    return intensitySorted.length > maxNumSignals ? Arrays.copyOf(intensitySorted, maxNumSignals)
        : intensitySorted;
  }

  /**
   * Remove all signals that fall within precursorMZ +- removePrecursorMz
   * @param mzSorted sorted by mz ascending
   * @param precursorMz center of the signals to be removed
   * @param removePrecursorMz +-delta to remove signals
   * @return the filtered list
   */
  public static DataPoint[] removePrecursorMz(final DataPoint[] mzSorted, final double precursorMz,
      final double removePrecursorMz) {
    var numDps = mzSorted.length;
    if (numDps == 0) {
      return mzSorted;
    }

    IntToDoubleFunction mzExtractor = index -> mzSorted[index].getMZ();
    // might be higher or lower or -1
    final double lowerMzBound = precursorMz - removePrecursorMz;
    int lower = BinarySearch.binarySearch(lowerMzBound, true, numDps, mzExtractor);
    if (lower == -1) {
      // no signal found
      return mzSorted;
    }
    double lowerMz = mzSorted[lower].getMZ();

    final double upperMzBound = precursorMz + removePrecursorMz;
    if (lowerMz < lowerMzBound) {
      lower++; // increment to be within mz range or higher
      if (lower >= numDps) {
        return mzSorted;
      }
      lowerMz = mzSorted[lower].getMZ();
    }
    if (lowerMz > upperMzBound) {
      // nothing in range
      return mzSorted;
    }

    int upper = BinarySearch.binarySearch(upperMzBound, true, numDps, mzExtractor);
    double upperMz = mzSorted[upper].getMZ();
    if (upperMz <= upperMzBound) {
      upper = Math.min(numDps, upper + 1); // increment to be above mz range
    }

    // all the last signals are within range
    if (upper == numDps) {
      return Arrays.copyOf(mzSorted, lower); // lower points to the first index within range
    } else {
      // concat ranges
      var upperLength = numDps - upper;
      DataPoint[] results = new DataPoint[lower + upperLength];
      System.arraycopy(mzSorted, 0, results, 0, lower);
      System.arraycopy(mzSorted, upper, results, lower, upperLength);
      return results;
    }
  }

  public static boolean inRange(final double tested, final double center, final double delta) {
    return tested >= center - delta && tested <= center + delta;
  }


}
