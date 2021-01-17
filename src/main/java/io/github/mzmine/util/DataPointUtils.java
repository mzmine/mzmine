package io.github.mzmine.util;

import com.google.common.primitives.Doubles;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.features.ModularFeature;
import java.nio.DoubleBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

public class DataPointUtils {

  private static final Logger logger = Logger.getLogger(DataPointUtils.class.getName());

  /**
   * Used to keep legacy-modules running, not to be used in new modules. Directly access the
   * underlying DoubleBuffers of Scans or {@link io.github.mzmine.datamodel.featuredata.IonSeries}
   * and extending classes.
   *
   * @param dataPoints
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
   * underlying DoubleBuffers of Scans or the {@link io.github.mzmine.datamodel.features.ModularFeature}'s
   * {@link io.github.mzmine.datamodel.featuredata.IonSeries} and extending classes.
   *
   * @param dataPoints
   * @return
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
   * @param mzValues
   * @param intensityValues
   * @return
   */
  public static double[][] getDataPointsAsDoubleArray(DoubleBuffer mzValues,
      DoubleBuffer intensityValues) {
    assert mzValues.capacity() == intensityValues.capacity();

    double data[][] = new double[2][];
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
   *
   * @param values
   * @return
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
   *
   * @param dataPoints
   * @return
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
   *
   * @param dataPoints
   * @return
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
      DoubleBuffer rawIntensities,
      double noiseLevel) {
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
}
