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

package io.github.mzmine.datamodel.features.correlation;

import io.github.mzmine.util.maths.similarity.Similarity;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.commons.math.MathException;
import org.apache.commons.math.stat.regression.SimpleRegression;

/**
 * correlation of two feature shapes
 *
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public class FullCorrelationData implements CorrelationData {

  // data points
  // [data point number intensity][feature a, b]
  private final double[][] data;
  private final SimpleRegression reg;

  // cosineSimilarity
  private final double cosineSim;

  public FullCorrelationData(double[][] data) {
    this.data = data;
    reg = new SimpleRegression();
    reg.addData(data);
    cosineSim = Similarity.COSINE.calc(data);
  }

  public FullCorrelationData(List<double[]> data) {
    this(data.toArray(new double[data.size()][2]));
  }

  /**
   * Extracts all data from all correlations
   *
   * @param corr
   */
  public static FullCorrelationData create(Collection<FullCorrelationData> corr) {
    List<double[]> dat = new ArrayList<>();
    for (FullCorrelationData c : corr) {
      for (double[] d : c.getData()) {
        dat.add(d);
      }
    }
    return create(dat);
  }

  /**
   * @param dat a list of data points [x,y]
   * @return
   */
  public static FullCorrelationData create(List<double[]> dat) {
    double[][] data = new double[dat.size()][2];
    for (int i = 0; i < dat.size(); i++) {
      data[i][0] = dat.get(i)[0];
      data[i][1] = dat.get(i)[1];
    }
    return new FullCorrelationData(data);
  }

  public SimpleRegression getRegression() {
    return reg;
  }

  @Override
  public int getDPCount() {
    return reg == null ? 0 : (int) reg.getN();
  }

  /**
   * Pearson correlation
   *
   * @return Pearson correlation r
   */
  @Override
  public double getPearsonR() {
    return reg == null ? 0 : reg.getR();
  }

  /**
   * Cosine similarity (dot-product) from 1 as identical to 0 as completely different
   *
   * @return cosine similarity
   */
  @Override
  public double getCosineSimilarity() {
    return cosineSim;
  }

  @Override
  public double[][] getData() {
    return data;
  }

  @Override
  public double getSlope() {
    return getRegression().getSlope();
  }

  @Override
  public double getRegressionSignificance() throws MathException {
    return getRegression().getSignificance();
  }
}
