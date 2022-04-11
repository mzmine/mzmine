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
import org.apache.commons.math.MathException;
import org.apache.commons.math.stat.regression.SimpleRegression;

/**
 * correlation of two feature shapes. Does not store data and full regression to reduce memory
 * footprint
 *
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public class SimpleCorrelationData implements CorrelationData {

  private final int numDP;
  private final double pearsonR;
  private final double cosineSim;
  private final double regressionSignificance;
  private final double slope;

  public SimpleCorrelationData(double[][] data) {
    SimpleRegression reg = new SimpleRegression();
    reg.addData(data);
    pearsonR = reg.getR();
    slope = reg.getSlope();
    double significance;
    try {
      significance = reg.getSignificance();
    } catch (MathException e) {
      significance = Double.NaN;
    }
    regressionSignificance = significance;
    numDP = data.length;
    cosineSim = Similarity.COSINE.calc(data);
  }

  @Override
  public int getDPCount() {
    return numDP;
  }

  @Override
  public double getPearsonR() {
    return pearsonR;
  }

  @Override
  public double[][] getData() {
    return null;
  }

  @Override
  public double getCosineSimilarity() {
    return cosineSim;
  }

  @Override
  public double getSlope() {
    return slope;
  }

  @Override
  public double getRegressionSignificance() throws MathException {
    return regressionSignificance;
  }
}
