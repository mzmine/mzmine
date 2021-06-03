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
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package io.github.mzmine.datamodel.features.correlation;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.util.maths.similarity.SimilarityMeasure;

/**
 * row to row correlation (2 rows) inter sample of best features
 *
 * @author Robin Schmid
 */
public class R2RCorrelationAcrossSamplesData extends R2RCorrelationData {

  // inter sample correlation of best features
  private final CorrelationData bestFeatureCorrelation;

  public R2RCorrelationAcrossSamplesData(FeatureListRow a, FeatureListRow b,
      CorrelationData bestFeatureCorrelation) {
    super(a, b);
    this.bestFeatureCorrelation = bestFeatureCorrelation;
  }

  public CorrelationData getCorrFeatureShape() {
    return bestFeatureCorrelation;
  }

  /**
   * Get average similarity score
   *
   * @param measure
   * @return
   */
  public double getSimilarity(SimilarityMeasure measure) {
    switch (measure) {
      case COSINE_SIM:
        return getAvgShapeCosineSim();
      case PEARSON:
        return getAvgShapeR();
    }
    return 0;
  }

  @Override
  public double getAvgShapeR() {
    return bestFeatureCorrelation.getR();
  }

  @Override
  public double getAvgShapeCosineSim() {
    return bestFeatureCorrelation.getCosineSimilarity();
  }

  public double getAvgDPcount() {
    return bestFeatureCorrelation.getDPCount();
  }

  @Override
  public boolean hasFeatureShapeCorrelation() {
    return true;
  }

}
