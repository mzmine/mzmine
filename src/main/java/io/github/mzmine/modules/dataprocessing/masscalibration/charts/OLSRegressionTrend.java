/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.dataprocessing.masscalibration.charts;


import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression;
import org.jfree.data.xy.XYDataItem;
import org.jfree.data.xy.XYSeries;

import java.util.ArrayList;
import java.util.Arrays;

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
  protected OLSMultipleLinearRegression olsRegression;
  protected double[] beta;

  public OLSRegressionTrend(int polynomialDegree, boolean exponentialFeature, boolean logarithmicFeature) {
    this.polynomialDegree = polynomialDegree;
    this.exponentialFeature = exponentialFeature;
    this.logarithmicFeature = logarithmicFeature;
    setDataset(new XYSeries("empty"));
  }

  @Override
  public String getName() {
    if (beta.length == 0) {
      return "OLS regression";
    }

    ArrayList<String> trend = new ArrayList<>();
    String[] descriptions = featuresDescription();
    for (int i = 0; i < descriptions.length; i++) {
      if (i == descriptions.length - 2 && exponentialFeature) {
        trend.add(String.format("%.4f*%s", beta[i], descriptions[i]));
      }
      else if(i == descriptions.length - 1 && logarithmicFeature) {
        trend.add(String.format("%.4f*%s", beta[i], descriptions[i]));
      }
      else if(i < descriptions.length - 2) {
        trend.add(String.format("%.4f*%s", beta[i], descriptions[i]));
      }
    }

    String trendString = String.join(" + ", trend);

    return "OLS regression " + trendString;
  }

  protected String[] featuresDescription() {
    ArrayList<String> desc = new ArrayList<>();
    for (int i = 0; i < polynomialDegree; i++){
      desc.add("x^" + String.valueOf(i));
    }
    desc.add("e^x");
    desc.add("ln(x)");
    return desc.toArray(new String[0]);
  }

  @Override
  public double getValue(double x) {
    if (items.length == 0) {
      return 0;
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
//      x[i][0] = items[i].getXValue();
      x[i] = generateFeatures(items[i].getXValue());
      y[i] = items[i].getYValue();
    }
    olsRegression.newSampleData(y, x);
    beta = olsRegression.estimateRegressionParameters();
  }

  protected double[] generateFeatures(double x) {
    double[] powers = polynomialFeatures(x, polynomialDegree);
    double exponential = Math.exp(x);
    double logarithmic = x > 0 ? Math.log(x) : 0;
    /*return ArrayUtils.addAll(powers,
            exponentialFeature ? exponential : 0,
            logarithmicFeature ? logarithmic : 0);*/
    int length = powers.length + (exponentialFeature ? 1 : 0) + (logarithmicFeature ? 1 : 0);
    double[] features = new double[length];
    int i = 0;
    for(i = 0; i < powers.length; i++){
      features[i] = powers[i];
    }
    if (exponentialFeature) {
      features[i] = exponential;
      i++;
    }
    if (logarithmicFeature) {
      features[i] = logarithmic;
      i++;
    }
    return features;
  }

  protected double[] polynomialFeatures(double x, int polynomialDegree) {
    double[] powers = new double[polynomialDegree + 1];
    double power = 1;
    for(int i = 0; i < polynomialDegree + 1; i++){
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
    if (items.length > 0){
      performOLSRegression();
    }
  }
}
