/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
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
