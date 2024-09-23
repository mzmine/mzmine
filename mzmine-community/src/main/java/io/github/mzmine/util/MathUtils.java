/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

import io.github.mzmine.util.maths.CenterMeasure;
import io.github.mzmine.util.maths.Precision;
import io.github.mzmine.util.maths.Weighting;
import java.util.Arrays;
import java.util.stream.DoubleStream;

/**
 * Mathematical calculation-related helper class
 */
public class MathUtils {


  /**
   * Cantor pairing function for integers >= 0 to produce unique ids for a pair. The result of a and
   * b is undirected so the arguments of a and b can be switched
   *
   * @param a >=0
   * @param b >=0
   * @return unique undirected pairing ID
   */
  public static int undirectedPairing(int a, int b) {
    if (a > b) {
      return directedPairing(b, a);
    } else {
      return directedPairing(a, b);
    }
  }

  /**
   * Cantor pairing function for integers >= 0 to produce unique ids for a pair. The result of a and
   * b is directed so changing the argument order will change the result.
   *
   * @param a >=0
   * @param b >=0
   * @return unique directed pairing ID
   */
  public static int directedPairing(int a, int b) {
    return ((a + b) * (a + b + 1) / 2) + a;
  }

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
   * @param measure
   * @param values
   * @param weights
   * @param weightTransform only used for center measure AVG (can also be Weighting.NONE)
   * @return
   */
  public static double calcCenter(CenterMeasure measure, double[] values, double[] weights,
      Weighting weightTransform) {
    return calcCenter(measure, values, weights, weightTransform, null, null);
  }

  public static double calcCenter(CenterMeasure measure, double[] values, double[] weights,
      Weighting weightTransform, Double noiseLevel, Double maxWeight) {
    switch (measure) {
      case AVG:
        return calcWeightedAvg(values, weights, weightTransform, noiseLevel, maxWeight);
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


  public static double calcQuantile(double[] values, double q, int totalScans) {
    assert totalScans >= values.length;
    if (values.length == 0) {
      return 0;
    }
    if (values.length == 1) {
      return values[0];
    }

    // 0-1
    q = Math.max(0d, Math.min(q, 1d));

    int zeroValues = totalScans - values.length;
    int ind1 = (int) Math.floor((totalScans - 1) * q) - zeroValues;
    int ind2 = (int) Math.ceil((totalScans - 1) * q) - zeroValues;

    if (ind2 < 0) {
      return 0d;
    }

    // sort values
    double[] vals = values.clone();
    Arrays.sort(vals);

    if (ind1 < 0) {
      return vals[ind2];
    } else {
      return (vals[ind1] + vals[ind2]) / 2;
    }
  }

  /**
   * Calculates q-quantile value of values. q=0.5 => median
   */
  public static double calcQuantile(double[] values, double q) {

    if (values.length == 0) {
      return 0;
    }

    if (values.length == 1) {
      return values[0];
    }

    if (q > 1) {
      q = 1;
    }

    if (q < 0) {
      q = 0;
    }

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
    return calcStd(values, 0, values.length);
  }

  public static double calcStd(double[] values, int start, int end) {
    double avg, stdev;
    double sum = 0;
    for (int i = start; i < end; i++) {
      double d = values[i];
      sum += d;
    }
    avg = sum / values.length;

    sum = 0;
    for (int i = start; i < end; i++) {
      double d = values[i];
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

    if (avg == 0) {
      return Double.NaN;
    }

    sum = 0;
    for (double d : values) {
      sum += (d - avg) * (d - avg);
    }

    stdev = Math.sqrt(sum / (values.length - 1));

    return stdev / avg;
  }

  public static double calcAvg(double[] values) {
    if (values.length == 0) {
      return Double.NaN;
    }

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
   * was different)
   */
  public static double calcWeightedAvg(double[] values, double[] weights, Weighting transform) {
    return calcWeightedAvg(values, weights, transform, null, null);
  }

  /**
   * @param values
   * @param weights
   * @param weightTransform
   * @param noiseLevel
   * @param maxWeight
   * @return
   */
  public static double calcWeightedAvg(double[] values, double[] weights, Weighting weightTransform,
      Double noiseLevel, Double maxWeight) {
    if (values == null || weights == null || values.length != weights.length) {
      return Double.NaN;
    }

    // transform
    double[] realWeights = weights;

    if (weightTransform != null) {
      realWeights = weightTransform.transform(weights, noiseLevel, maxWeight);
    }

    // sum of weights
    double weightSum = DoubleStream.of(realWeights).sum();

    // should never be 0, unless noiseLevel was too high
    if (weightSum == 0) {
      return calcAvg(values);
    }

    double avg = 0;
    for (int i = 0; i < realWeights.length; i++) {
      avg += values[i] * realWeights[i] / weightSum;
    }
    return avg;
  }

  /**
   * @param y The y value to calc x for.
   * @return X value for given points and y value based on linear interpolation.
   */
  public static double twoPointGetXForY(double x1, double y1, double x2, double y2, double y) {
    return (y - y1) * ((x2 - x1) / (y2 - y1)) + x1;
  }

  /**
   * @param x The y value to calc x for.
   * @return Y value for given points and X value based on linear interpolation.
   */
  public static double twoPointGetYForX(double x1, double y1, double x2, double y2, double x) {
    return (x - x1) * ((y2 - y1) / (x2 - x1)) + y1;
  }

  public static double getPpmDiff(double calc, double real) {
    return (real-calc) / Math.abs(calc) * 1E6;
  }

  /**
   * The resulting value will be within (inclusive) min max range and rounded to the number of
   * significant figures. 231.9 with 2 significant figures will be 230.
   *
   * @param value          will be constrained and rounded
   * @param min            minimum value
   * @param max            maximum value
   * @param roundPrecision number of significant digits
   * @return min <= result <= max rounded to precision number of significant figures
   */
  public static double within(double value, double min, double max, int roundPrecision) {
    return Precision.round(within(value, min, max), roundPrecision).doubleValue();
  }

  /**
   * The resulting value will be within (inclusive) min max range.
   *
   * @param value will be constrained
   * @param min   minimum value
   * @param max   maximum value
   * @return min <= result <= max
   */
  public static double within(double value, double min, double max) {
    if (value <= min) {
      return min;
    }
    if (value >= max) {
      return max;
    }
    return value;
  }
}
