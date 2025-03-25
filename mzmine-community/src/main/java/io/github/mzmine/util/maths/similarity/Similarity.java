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

package io.github.mzmine.util.maths.similarity;

import io.github.mzmine.util.maths.Transform;
import java.util.Arrays;
import org.apache.commons.math3.stat.correlation.SpearmansCorrelation;
import org.apache.commons.math3.stat.regression.SimpleRegression;

/**
 * Different similarity measures such as COSINE SIMILARITY, PEARSON, SPEARMAN,...
 *
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 */
public abstract class Similarity {

  // #############################################
  // abstract methods

  /**
   * @param data data[dp][0,1]
   * @return
   */
  public double calc(double[][] data) {
    return calc(col(data, 0), col(data, 1));
  }


  public abstract double calc(double[] x, double[] y);


  // Measures
  public static final Similarity COSINE = new Similarity() {
    public double calc(double[][] data) {
      return dot(data) / cosineDivisor(data);
    }

    @Override
    public double calc(final double[] x, final double[] y) {
      return dot(x, y) / cosineDivisor(x, y);
    }
  };


  /**
   * Log ratio proportionality
   * https://journals.plos.org/ploscompbiol/article?id=10.1371/journal.pcbi.1004075
   */
  public static final Similarity LOG_VAR_PROPORTIONALITY = new Similarity() {

    @Override
    public double calc(final double[] x, final double[] y) {
      double[] logratioXY = transform(ratio(x, y), Transform.LOG);
      double[] logx = transform(x, Transform.LOG);
      return var(logratioXY) / var(logx);
    }
  };

  /**
   * Log ratio proportionality -1 to 1
   * https://journals.plos.org/ploscompbiol/article?id=10.1371/journal.pcbi.1004075
   */
  public static final Similarity LOG_VAR_CONCORDANCE = new Similarity() {
    @Override
    public double calc(final double[] x, final double[] y) {
      double[] logx = transform(x, Transform.LOG);
      double[] logy = transform(y, Transform.LOG);
      return 2 * covar(logx, logy) / (var(logx) + var(logy));
    }
  };

  /**
   * Spearmans correlation:
   */
  public static final Similarity SPEARMANS_CORR = new Similarity() {

    @Override
    public double calc(final double[] x, final double[] y) {
      SpearmansCorrelation corr = new SpearmansCorrelation();

      return corr.correlation(x, y);
    }
  };
  /**
   * Pearson correlation: optimized to avoid data copies
   */
  public static final Similarity PEARSONS_CORR = new Similarity() {
    @Override
    public double calc(final double[] xs, final double[] ys) {
      int values = 0;
      double sumX = 0.0, sumY = 0.0, sumXY = 0.0;
      double sumX2 = 0.0, sumY2 = 0.0;

      for (int i = 0; i < xs.length; i++) {
        values++;

        double x = xs[i];
        double y = ys[i];

        sumX += x;
        sumY += y;
        sumXY += x * y;
        sumX2 += x * x;
        sumY2 += y * y;
      }

      double numerator = values * sumXY - sumX * sumY;
      double denominator = Math.sqrt(
          (values * sumX2 - sumX * sumX) * (values * sumY2 - sumY * sumY));

      if (denominator == 0) {
        return 0d;
      }

      return numerator / denominator;
    }
  };

  /**
   * slope
   */
  public static final Similarity REGRESSION_SLOPE = new Similarity() {

    @Override
    public double calc(double[][] data) {
      return getRegression(data).getSlope();
    }

    @Override
    public double calc(final double[] x, final double[] y) {
      return getRegression(x, y).getSlope();
    }

    /**
     * @param data data[dp][indexOfX]
     */
    public double getSignificance(double[][] data) {
      return getRegression(data).getSignificance();
    }
  };

  /**
   * slope
   */
  public static final Similarity REGRESSION_SLOPE_SIGNIFICANCE = new Similarity() {


    public double calc(double[][] data) {
      return getRegression(data).getSignificance();
    }

    @Override
    public double calc(final double[] x, final double[] y) {
      return 0;
    }

  };

  /**
   * @param data data[dp][indexOfX]
   * @return
   */
  public static SimpleRegression getRegression(double[][] data) {
    SimpleRegression reg = new SimpleRegression();
    reg.addData(data);
    return reg;
  }

  public static SimpleRegression getRegression(final double[] x, final double[] y) {
    SimpleRegression reg = new SimpleRegression();
    for (int i = 0; i < x.length; i++) {
      reg.addData(x[i], y[i]);
    }
    return reg;
  }

  /**
   * Maximum fold-change
   *
   * @param data data[dp][indexOfX]
   */
  public static double maxFoldChange(double[][] data, final int i) {
    double min = Arrays.stream(data).mapToDouble(d -> d[i]).min().getAsDouble();
    double max = Arrays.stream(data).mapToDouble(d -> d[i]).max().getAsDouble();
    return max / min;
  }

  /**
   * @param data data[dp][indexOfX]
   * @param i    column index
   */
  public double[] col(double[][] data, int i) {
    double[] v = new double[data.length];
    for (int d = 0; d < data.length; d++) {
      v[d] = data[d][i];
    }
    return v;
  }

  /**
   * the divisor for calculating the cosine similarity
   *
   * @param data [data point][set 0,1]
   */
  public static double cosineDivisor(double[][] data) {
    return (Math.sqrt(norm(data, 0)) * Math.sqrt(norm(data, 1)));
  }

  /**
   * the divisor for calculating the cosine similarity
   */
  public static double cosineDivisor(double[] x, double[] y) {
    return (Math.sqrt(norm(x)) * Math.sqrt(norm(y)));
  }

  public double[] transform(double[] data, Transform transform) {
    double[] v = new double[data.length];
    for (int d = 0; d < data.length; d++) {
      v[d] = transform.transform(data[d]);
    }
    return v;
  }

  // ############################################
  // COMMON METHODS

  /**
   * @param pair          pair of intensities
   * @param cosineDivisor get by {@link #cosineDivisor}
   * @return the contribution of this signal to the cosine
   */
  public static double cosineSignalContribution(double[] pair, double cosineDivisor) {
    return pair[0] * pair[1] / cosineDivisor;
  }

  /**
   * sum(x*y)
   *
   * @param data data[dp][x,y]
   */
  public static double dot(double[][] data) {
    double sum = 0;
    for (double[] val : data) {
      sum += val[0] * val[1];
    }
    return sum;
  }

  public static double dot(double[] x, double[] y) {
    double sum = 0;
    for (int i = 0; i < x.length; i++) {
      sum += x[i] * y[i];
    }
    return sum;
  }

  public static double mean(double[][] data, int i) {
    double m = 0;
    for (int d = 0; d < data.length; d++) {
      m += data[d][i];
    }
    return m / data.length;
  }

  public static double var(double[][] data, int i) {
    double mean = mean(data, i);
    double var = 0;
    for (int d = 0; d < data.length; d++) {
      var += Math.pow(mean - data[d][i], 2);
    }
    return var / data.length;
  }

  public static double mean(double[] data) {
    double m = 0;
    for (final double datum : data) {
      m += datum;
    }
    return m / data.length;
  }

  public static double var(double[] data) {
    double mean = mean(data);
    double var = 0;
    for (int d = 0; d < data.length; d++) {
      var += Math.pow(mean - data[d], 2);
    }
    return var / (data.length - 1);
  }

  public static double covar(double[] a, double[] b) {
    double meanA = mean(a);
    double meanB = mean(b);
    double covar = 0;
    for (int d = 0; d < a.length; d++) {
      covar += (a[d] - meanA) * (b[d] - meanB);
    }
    return covar / (a.length - 1);
  }

  /**
   * Sample variance n-1
   *
   * @param data data[dp][indexOfX]
   * @param i
   * @return
   */
  public static double varN(double[][] data, int i) {
    double mean = mean(data, i);
    double var = 0;
    for (int d = 0; d < data.length; d++) {
      var += Math.pow(mean - data[d][i], 2);
    }
    return var / (data.length);
  }

  /**
   * @param data data[dp][indexOfX]
   */
  public static double stdev(double[][] data, int i) {
    return Math.sqrt(var(data, i));
  }

  /**
   * @param data data[dp][indexOfX]
   */
  public static double stdevN(double[][] data, int i) {
    return Math.sqrt(varN(data, i));
  }

  /**
   * Euclidean norm (self dot product). sum(x*x)
   *
   * @param data  data[dp][indexOfX]
   * @param index column
   * @return
   */
  public static double norm(double[][] data, int index) {
    double sum = 0;
    for (double[] val : data) {
      sum += val[index] * val[index];
    }
    return sum;
  }

  /**
   * Euclidean norm (self dot product). sum(x*x)
   */
  public static double norm(double[] data) {
    double sum = 0;
    for (double val : data) {
      sum += val * val;
    }
    return sum;
  }

  /**
   * ratio of a/b
   *
   * @param data data[dp][indexOfX]
   * @return
   */
  public double[] ratio(double[][] data, int a, int b) {
    double[] v = new double[data.length];
    for (int d = 0; d < data.length; d++) {
      v[d] = data[d][a] / data[d][b];
    }
    return v;
  }

  /**
   * ratio of a/b
   */
  public double[] ratio(double[] x, double[] y) {
    double[] v = new double[x.length];
    for (int d = 0; d < x.length; d++) {
      v[d] = x[d] / y[d];
    }
    return v;
  }
}
