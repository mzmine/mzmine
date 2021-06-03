package io.github.mzmine.datamodel.features.correlation;

import io.github.mzmine.util.maths.similarity.SimilarityMeasure;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;

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
  double getR();

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
        case PEARSON -> getR();
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

}
