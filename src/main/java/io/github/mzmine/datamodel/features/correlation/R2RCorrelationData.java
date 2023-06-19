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
