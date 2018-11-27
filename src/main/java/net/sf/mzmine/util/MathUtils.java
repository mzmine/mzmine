/*
 * Copyright 2006-2018 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package net.sf.mzmine.util;

import java.util.Arrays;
import java.util.stream.DoubleStream;
import net.sf.mzmine.util.maths.CenterMeasure;
import net.sf.mzmine.util.maths.Weighting;

/**
 * Mathematical calculation-related helper class
 */
public class MathUtils {

  /**
   * median or non-weighted average
   * 
   * @param center
   * @param values
   * @return
   */
  public static double calcCenter(CenterMeasure center, double[] values) {
    switch (center) {
      case AVG:
        return calcAvg(values);
      case MEDIAN:
        return calcMedian(values);
      default:
        return Double.NaN;
    }
  }

  /**
   * median or weighted average
   * 
   * @param center
   * @param values
   * @param weights
   * @param transform only used for center measure AVG (can also be Weighting.NONE)
   * @return
   */
  public static double calcCenter(CenterMeasure center, double[] values, double[] weights,
      Weighting transform) {
    switch (center) {
      case AVG:
        return calcWeightedAvg(values, weights, transform);
      case MEDIAN:
        return calcMedian(values);
      default:
        return Double.NaN;
    }
  }

  /**
   * Median
   * 
   * @param values
   * @return
   */
  public static double calcMedian(double[] values) {
    return calcQuantile(values, 0.5);
  }

  /**
   * Calculates q-quantile value of values. q=0.5 => median
   * 
   */
  public static double calcQuantile(double[] values, double q) {

    if (values.length == 0)
      return 0;

    if (values.length == 1)
      return values[0];

    if (q > 1)
      q = 1;

    if (q < 0)
      q = 0;

    double[] vals = values.clone();

    Arrays.sort(vals);

    int ind1 = (int) Math.floor((vals.length - 1) * q);
    int ind2 = (int) Math.ceil((vals.length - 1) * q);

    return (vals[ind1] + vals[ind2]) / 2;

  }

  public static double[] calcQuantile(double[] values, double[] qs) {

    double[] retVals = new double[qs.length];

    if (values.length == 0) {
      for (int qInd = 0; qInd < qs.length; qInd++) {
        retVals[qInd] = 0;
      }
      return retVals;
    }
    if (values.length == 1) {
      for (int qInd = 0; qInd < qs.length; qInd++) {
        retVals[qInd] = values[0];
      }
      return retVals;
    }

    double[] vals = values.clone();
    Arrays.sort(vals);

    double q;
    int ind1, ind2;
    for (int qInd = 0; qInd < qs.length; qInd++) {
      q = qs[qInd];

      if (q > 1) {
        q = 1;
      }
      if (q < 0) {
        q = 0;
      }

      ind1 = (int) Math.floor((vals.length - 1) * q);
      ind2 = (int) Math.ceil((vals.length - 1) * q);

      retVals[qInd] = (vals[ind1] + vals[ind2]) / 2;
    }

    return retVals;
  }

  public static double calcStd(double[] values) {

    double avg, stdev;
    double sum = 0;
    for (double d : values) {
      sum += d;
    }
    avg = sum / values.length;

    sum = 0;
    for (double d : values) {
      sum += (d - avg) * (d - avg);
    }

    stdev = Math.sqrt(sum / (values.length - 1));
    return stdev;
  }

  public static double calcCV(double[] values) {

    double avg, stdev;
    double sum = 0;
    for (double d : values) {
      sum += d;
    }
    avg = sum / values.length;

    if (avg == 0)
      return Double.NaN;

    sum = 0;
    for (double d : values) {
      sum += (d - avg) * (d - avg);
    }

    stdev = Math.sqrt(sum / (values.length - 1));

    return stdev / avg;
  }

  public static double calcAvg(double[] values) {
    if (values.length == 0)
      return Double.NaN;

    double sum = 0;
    for (double d : values) {
      sum += d;
    }
    return sum / values.length;
  }

  /**
   * Weighted average with linear weights
   * 
   * @param values
   * @param weights
   * @return
   * @throws Exception
   */
  public static double calcWeightedAvg(double[] values, double[] weights) {
    return calcWeightedAvg(values, weights, Weighting.LINEAR);
  }

  /**
   * Weighted average with weight transformation
   * 
   * @param values
   * @param weights
   * @param transform function to transform the weights
   * @return the weighted average (or Double.NaN if no values supplied or the lengths of the arrays
   *         was different)
   */
  public static double calcWeightedAvg(double[] values, double[] weights, Weighting transform) {
    if (values == null || weights == null || values.length != weights.length)
      return Double.NaN;

    // transform
    double[] realWeights = transform.transform(weights);

    // sum of weights
    double weightSum = DoubleStream.of(realWeights).sum();

    double avg = 0;
    for (int i = 0; i < realWeights.length; i++) {
      avg += values[i] * realWeights[i] / weightSum;
    }
    return avg;
  }
}
