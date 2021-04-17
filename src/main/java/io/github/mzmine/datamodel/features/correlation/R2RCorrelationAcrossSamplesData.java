package io.github.mzmine.datamodel.features.correlation;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.util.maths.similarity.SimilarityMeasure;
import java.util.Map;
import java.util.Map.Entry;

/**
 * row to row correlation (2 rows) inter sample of best features
 * 
 * @author Robin Schmid
 *
 */
public class R2RCorrelationAcrossSamplesData extends R2RCorrelationData {

  // inter sample correlation of best features
  private CorrelationData bestFeatureCorrelation;

  public R2RCorrelationAcrossSamplesData(FeatureListRow a, FeatureListRow b, CorrelationData bestFeatureCorrelation) {
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
