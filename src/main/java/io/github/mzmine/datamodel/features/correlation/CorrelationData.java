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
