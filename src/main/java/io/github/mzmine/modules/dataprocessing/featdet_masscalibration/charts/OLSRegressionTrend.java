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

package io.github.mzmine.modules.dataprocessing.featdet_masscalibration.charts;


import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression;
import org.apache.commons.text.WordUtils;
import org.jfree.data.xy.XYDataItem;
import org.jfree.data.xy.XYSeries;
import io.github.mzmine.modules.dataprocessing.featdet_masscalibration.errormodeling.BiasEstimator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Logger;

/**
 * OLS regression trend,
 * for two dimensional dataset performs OLS regression on polynomial, exponential and logarithmic features
 */
public class OLSRegressionTrend implements Trend2D {

  protected XYSeries dataset;
  protected XYDataItem[] items;

  protected int polynomialDegree;
  protected boolean exponentialFeature;
  protected boolean logarithmicFeature;
  protected String[] featuresDescriptions;

  protected OLSMultipleLinearRegression olsRegression;
  protected double[] beta;
  protected double rSquared;

  protected double arithmeticMean;
  protected boolean estimated = true;

  public OLSRegressionTrend(int polynomialDegree, boolean exponentialFeature, boolean logarithmicFeature) {
    this.polynomialDegree = polynomialDegree;
    this.exponentialFeature = exponentialFeature;
    this.logarithmicFeature = logarithmicFeature;
    this.featuresDescriptions = generateFeaturesDescriptions();
    setDataset(new XYSeries("empty"));
  }

  @Override
  public String getName() {
    if (beta == null || beta.length == 0 || estimated == false) {
      return "OLS regression";
    }

    ArrayList<String> featuresWithParameters = new ArrayList<>();

    for (int i = 0; i < featuresDescriptions.length; i++) {
      featuresWithParameters.add(String.format("%s*%s", beta[i], featuresDescriptions[i]));
    }

    String trendString = String.join(" + ", featuresWithParameters);
    trendString = WordUtils.wrap(trendString, 100);
    trendString += "\nR^2 = " + rSquared;
    trendString += "\nR^2 = " + ChartUtils.calculateRSquared(items, this);

    return "OLS regression\n" + trendString;
  }

  protected String[] generateFeaturesDescriptions() {
    ArrayList<String> desc = new ArrayList<>();
    for (int i = 0; i < polynomialDegree + 1; i++) {
      desc.add("x^" + i);
    }
    if (exponentialFeature) {
      desc.add("e^(x/10)");
    }
    if (logarithmicFeature) {
      desc.add("ln(x)");
    }
    return desc.toArray(new String[0]);
  }

  @Override
  public double getValue(double x) {
    if (items.length == 0) {
      return 0;
    }

    if (estimated == false) {
      return arithmeticMean;
    }

    double[] features = generateFeatures(x);
    double y = 0;
    for (int i = 0; i < features.length; i++) {
      y += beta[i] * features[i];
    }
    return y;
  }

  protected void performOLSRegression() {
    olsRegression = new OLSMultipleLinearRegression();
    olsRegression.setNoIntercept(true);
    double[] y = new double[items.length];
    double[][] x = new double[items.length][1];
    for (int i = 0; i < items.length; i++) {
      x[i] = generateFeatures(items[i].getXValue());
      y[i] = items[i].getYValue();
    }
    arithmeticMean = BiasEstimator.arithmeticMean(Arrays.asList(ArrayUtils.toObject(y)));
    try {
      olsRegression.newSampleData(y, x);
      beta = olsRegression.estimateRegressionParameters();
      rSquared = olsRegression.calculateRSquared();
      estimated = true;
    } catch (IllegalArgumentException ex) {
      estimated = false;
      Logger.getLogger(this.getClass().getName()).info("OLS regression exception " + ex);
    }
  }

  protected double[] generateFeatures(double x) {
    double[] powers = polynomialFeatures(x, polynomialDegree);
    double exponential = Math.exp(x / 10);
    double logarithmic = x > 0 ? Math.log(x) : 0;

    if (Double.isInfinite(exponential)) {
      exponential = Double.MAX_VALUE;
    }
    if (Double.isInfinite(logarithmic)) {
      logarithmic = -Double.MAX_VALUE;
    }

    ArrayList<Double> features = new ArrayList<>();
    features.addAll(Arrays.asList(ArrayUtils.toObject(powers)));
    if (exponentialFeature) {
      features.add(exponential);
    }
    if (logarithmicFeature) {
      features.add(logarithmic);
    }

    return ArrayUtils.toPrimitive(features.toArray(new Double[0]));
  }

  protected double[] polynomialFeatures(double x, int polynomialDegree) {
    double[] powers = new double[polynomialDegree + 1];
    double power = 1;
    for (int i = 0; i < polynomialDegree + 1; i++) {
      powers[i] = power;
      power *= x;
    }
    return powers;
  }

  public XYSeries getDataset() {
    return dataset;
  }

  @Override
  public void setDataset(XYSeries dataset) {
    this.dataset = dataset;
    this.items = (XYDataItem[]) dataset.getItems().toArray(new XYDataItem[0]);
    if (items.length > 0) {
      performOLSRegression();
    }
  }
}
