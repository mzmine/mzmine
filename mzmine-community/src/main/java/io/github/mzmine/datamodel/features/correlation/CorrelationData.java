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

import io.github.mzmine.util.maths.similarity.SimilarityMeasure;
import org.jetbrains.annotations.Nullable;
import org.apache.commons.math.MathException;

/**
 * correlation of two feature shapes
 *
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public interface CorrelationData {

  /**
   * @return Number of data points
   */
  int getDPCount();

  /**
   * Pearson correlation
   *
   * @return Pearson correlation r
   */
  double getPearsonR();

  /**
   * Cosine similarity (dot-product) from 1 as identical to 0 as completely different
   *
   * @return cosine similarity
   */
  double getCosineSimilarity();

  /**
   * The similarity or NaN if data is null or empty
   *
   * @param type the similarity measure type
   * @return the similarity or Double.NaN if no value or data are available for the {@link
   * SimilarityMeasure}
   */
  default double getSimilarity(SimilarityMeasure type) {
    return switch (type) {
      case COSINE_SIM -> getCosineSimilarity();
      case PEARSON -> getPearsonR();
      default -> {
        double[][] data = getData();
        if (data != null || data.length == 0 || data[0].length == 0) {
          yield type.calc(data);
        } else {
          yield Double.NaN;
        }
      }
    };
  }

  /**
   * Depending on the implementation, the data might be available or not
   *
   * @return the underlying data or null
   */
  @Nullable
  double[][] getData();

  /**
   * Simple check if the correlation is valid
   *
   * @return true if valid
   */
  default boolean isValid() {
    return getDPCount() > 0;
  }

  /**
   * Slope of regression
   *
   * @return slope
   */
  double getSlope();

  /**
   *
   * @return
   */
  double getRegressionSignificance() throws MathException;
}
