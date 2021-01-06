package io.github.mzmine.util;

import io.github.mzmine.datamodel.DataPoint;
import java.util.Collection;
import java.util.logging.Logger;

public class DataPointUtils {

  private static final Logger logger = Logger.getLogger(DataPointUtils.class.getName());

  private static int counter = 0;

  /**
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
}