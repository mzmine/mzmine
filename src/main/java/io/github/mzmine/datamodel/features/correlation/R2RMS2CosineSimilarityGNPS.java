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
import org.jetbrains.annotations.NotNull;

/**
 * Modified cosine similarity between two rows imported from GNPS results
 */
public class R2RMS2CosineSimilarityGNPS extends AbstractRowsRelationship {

  private final double cosine;
  private final String annotation;
  private final String edgeType;

  /**
   * Modified cosine similarity imported from GNPS
   *
   * @param a      the two rows
   * @param b      the two rows
   * @param cosine cosine similarity
   */
  public R2RMS2CosineSimilarityGNPS(FeatureListRow a, FeatureListRow b, double cosine,
      String annotation, String edgeType) {
    super(a, b);
    this.cosine = cosine;
    this.annotation = annotation;
    this.edgeType = edgeType;
  }

  /**
   * The edge type string used by GNPS
   */
  public String getGNPSEdgeType() {
    return edgeType;
  }

  public double getCosineSimilarity() {
    return cosine;
  }

  @Override
  public double getScore() {
    return getCosineSimilarity();
  }

  @NotNull
  @Override
  public Type getType() {
    return Type.MS2_GNPS_COSINE_SIM;
  }

  @NotNull
  @Override
  public String getAnnotation() {
    return annotation == null || annotation.strip().isEmpty() ? "cos=" + getScoreFormatted()
        : annotation;
  }

}
