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

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.util.maths.similarity.SimilarityMeasure;
import org.jetbrains.annotations.NotNull;

/**
 * row to row correlation (2 rows) Intensity profile and peak shape correlation
 *
 * @author Robin Schmid
 */
public abstract class R2RCorrelationData implements RowsRelationship {

  // correlation of a to b
  // id A < id B
  private final FeatureListRow a;
  private final FeatureListRow b;

  public R2RCorrelationData(FeatureListRow a, FeatureListRow b) {
    if (a.getID() < b.getID()) {
      this.a = a;
      this.b = b;
    } else {
      this.b = a;
      this.a = b;
    }
  }

  @Override
  public FeatureListRow getRowB() {
    return b;
  }

  @Override
  public FeatureListRow getRowA() {
    return a;
  }


  @Override
  public double getScore() {
    return getAvgShapeR();
  }

  @Override
  public String getScoreFormatted() {
    double score = getScore();
    return Double.isNaN(score) ? "NaN"
        : MZmineCore.getConfiguration().getScoreFormat().format(score);
  }

  @NotNull
  @Override
  public Type getType() {
    return Type.MS1_FEATURE_CORR;
  }

  @Override
  public String getAnnotation() {
    return "r=" + getScoreFormatted();
  }

  /**
   * Feature height similarity (height across all samples)
   *
   * @param type similarity measure
   * @return feature height similarity
   */
  public abstract double getHeightSimilarity(SimilarityMeasure type);

  /**
   * Total similarity (all data points of all features)
   *
   * @param type similarity measure
   * @return total similarity of features
   */
  public abstract double getTotalSimilarity(SimilarityMeasure type);

  /**
   * Total similarity (all data points of all features)
   *
   * @return total similarity of features
   */
  public abstract double getTotalPearsonR();

  /**
   * Has "total" correlation of all data points in all features as one correlation
   * @return true if total correlation available
   */
  protected abstract boolean hasTotalCorrelation();

  /**
   * Average feature shape similarity
   *
   * @param type similarity measure
   * @return average feature shape similarity
   */
  public abstract double getAvgFeatureShapeSimilarity(SimilarityMeasure type);

  /**
   * Minimum Pearson correlation of feature shapes
   *
   * @return min Pearson r
   */
  public abstract double getMinShapeR();

  /**
   * Maximum Pearson correlation of feature shapes
   *
   * @return max Pearson r
   */
  public abstract double getMaxShapeR();

  /**
   * Average Pearson correlation of feature shapes
   *
   * @return avg Pearson r
   */
  public abstract double getAvgShapeR();

  /**
   * Average cosine similarity of feature shapes
   *
   * @return avg cosine similarity
   */
  public abstract double getAvgShapeCosineSim();

  /**
   * Has feature Height correlation
   *
   * @return true if feature height correlation results are available
   */
  public abstract boolean hasHeightCorr();

  /**
   * Flag if feature shape correlation is available
   *
   * @return true if feature shape correlation is available
   */
  public abstract boolean hasFeatureShapeCorrelation();

  /**
   * @return
   */
  public abstract double getAvgDPcount();

  /**
   * Checks if this correlation data is valid
   *
   * @return true if any correlation is available
   */
  public abstract boolean isValid();

  /**
   * Cosine score of the feature height correlation
   *
   * @return feature height cosine score
   */
  public abstract double getHeightCosineSimilarity();

  /**
   * Pearson correlation score of the feature height correlation
   *
   * @return feature height Pearson correlation
   */
  public abstract double getHeightPearsonR();
}
