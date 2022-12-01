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
import java.nio.DoubleBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class DataPointUtils {

  /**
   * Used to keep legacy-modules running, not to be used in new modules. Directly access the
   * underlying DoubleBuffers of Scans or {@link io.github.mzmine.datamodel.featuredata.IonSeries}
   * and extending classes.
   *
   * @return 2-d array with dimension double[2][dataPoints.length]. [0][i] will contain mz, [1][i]
   * will contain intensity values.
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
    assert mzValues.capacity() == intensityValues.capacity();

    double[][] data = new double[2][];
    data[0] = new double[mzValues.capacity()];
    data[1] = new double[mzValues.capacity()];
    for (int i = 0; i < mzValues.capacity(); i++) {
      data[0][i] = mzValues.get(i);
      data[1][i] = intensityValues.get(i);
    }
    return data;
  }

  /**
   * Used when copying an {@link io.github.mzmine.datamodel.featuredata.IonSpectrumSeries} and
   * subclasses. Usually, the data should be accessed directly via the buffer.
   */
  public static double[] getDoubleBufferAsArray(DoubleBuffer values) {
    double[] data = new double[values.capacity()];

    for (int i = 0; i < values.capacity(); i++) {
      data[i] = values.get(i);
    }
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
    assert rawMzs.capacity() == rawIntensities.capacity();

    List<Double> mzs = new ArrayList<>();
    List<Double> intensities = new ArrayList<>();

    for (int i = 0; i < rawMzs.capacity(); i++) {
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
}
