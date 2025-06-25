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

package io.github.mzmine.datamodel.features.correlation;

import io.github.mzmine.datamodel.features.FeatureListRow;
import org.jetbrains.annotations.NotNull;


/**
 * MS2Deepscore similarity between two rows (the best MS2 spectra)
 */
public class R2RSimpleSimilarity extends InternalTypedRowsRelationship {

  private final float similarity;

  /**
   * MS2Deepscore similarity
   *
   * @param a          the two rows
   * @param b          the two rows
   * @param similarity cosine similarity
   */
  public R2RSimpleSimilarity(FeatureListRow a, FeatureListRow b, Type type, float similarity) {
    super(a, b, type);
    this.similarity = similarity;
  }

  @Override
  public double getScore() {
    return similarity;
  }

  @NotNull
  @Override
  public String getAnnotation() {
    return "score=" + getScoreFormatted();
  }

}