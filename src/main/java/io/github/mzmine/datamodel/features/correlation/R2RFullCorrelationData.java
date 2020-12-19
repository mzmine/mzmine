package io.github.mzmine.datamodel.features.correlation;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.util.maths.similarity.SimilarityMeasure;

import java.util.Map;
import java.util.Map.Entry;

/**
 * row to row correlation (2 rows) Intensity profile and Feature shape correlation
 * 
 * @author Robin Schmid
 *
 */
public class R2RFullCorrelationData extends R2RCorrelationData {

  public enum NegativMarker {
    // intensity range is not shared between these two rows
    // at least in one raw data file: the features are out of RT range
    // the features do not overlap with X % of their intensity
    FeaturesDoNotOverlap, //
    MinFeaturesRequirementNotMet; //
  }

  // correlation of all data points in one total correlation
  private CorrelationData corrTotal;
  // correlation to all Features
  private CorrelationData heightCorr;

  /**
   * Feature shape correlation in RawDataFiles
   */
  private Map<RawDataFile, CorrelationData> corrFeatureShape;
  // min max avg
  private double minShapeR, maxShapeR, avgShapeR, avgDPCount;
  // cosine
  private double avgShapeCosineSim;

  public R2RFullCorrelationData(FeatureListRow a, FeatureListRow b, CorrelationData corrIProfile,
                                Map<RawDataFile, CorrelationData> corrFeatureShape) {
    super(a, b);
    this.heightCorr = corrIProfile;
    setCorrFeatureShape(corrFeatureShape);
  }

  public Map<RawDataFile, CorrelationData> getCorrFeatureShape() {
    return corrFeatureShape;
  }

  public CorrelationData getCorrFeatureShape(RawDataFile raw) {
    return corrFeatureShape == null ? null : corrFeatureShape.get(raw);
  }


  /**
   * The similarity or NaN if data is null or empty
   * 
   * @param type
   * @return
   */
  public double getHeightSimilarity(SimilarityMeasure type) {
    if (getHeightCorr() == null)
      return Double.NaN;
    else
      return getHeightCorr().getSimilarity(type);
  }

  /**
   * The similarity or NaN if data is null or empty
   * 
   * @param type
   * @return
   */
  public double getTotalSimilarity(SimilarityMeasure type) {
    if (getTotalCorr() == null)
      return Double.NaN;
    else
      return getTotalCorr().getSimilarity(type);
  }

  /**
   * The similarity or NaN if data is null or empty
   * 
   * @param type
   * @return
   */
  public double getAvgFeatureShapeSimilarity(SimilarityMeasure type) {
    if (corrFeatureShape == null)
      return Double.NaN;
    else {
      double mean = 0;
      int n = 0;
      for (Entry<RawDataFile, CorrelationData> e : corrFeatureShape.entrySet()) {
        mean += e.getValue().getSimilarity(type);
        n++;
      }

      return mean / n;
    }
  }

  /**
   * 
   * @param corrFeatureShape
   */
  public void setCorrFeatureShape(Map<RawDataFile, CorrelationData> corrFeatureShape) {
    // set
    this.corrFeatureShape = corrFeatureShape;
    if (!hasFeatureShapeCorrelation()) {
      minShapeR = 0;
      maxShapeR = 0;
      avgShapeR = 0;
      avgShapeCosineSim = 0;
      avgDPCount = 0;
      corrTotal = null;
      return;
    }
    // min max
    minShapeR = 1;
    maxShapeR = -1;
    avgShapeR = 0;
    avgShapeCosineSim = 0;
    avgDPCount = 0;
    int c = corrFeatureShape.size();

    for (Entry<RawDataFile, CorrelationData> e : corrFeatureShape.entrySet()) {
      CorrelationData corr = e.getValue();
      avgShapeR += corr.getR();
      avgShapeCosineSim += corr.getCosineSimilarity();
      avgDPCount += corr.getDPCount();
      if (corr.getR() < minShapeR)
        minShapeR = corr.getR();
      if (corr.getR() > maxShapeR)
        maxShapeR = corr.getR();
    }

    avgDPCount = avgDPCount / c;
    avgShapeR = avgShapeR / c;
    avgShapeCosineSim = avgShapeCosineSim / c;

    // create new total corr
    corrTotal = CorrelationData.create(corrFeatureShape.values());
  }

  /**
   * Correlation between two rows with all data points of all raw data files
   * 
   * @return
   */
  public CorrelationData getTotalCorr() {
    return corrTotal;
  }

  public double getMinShapeR() {
    return minShapeR;
  }

  public double getMaxShapeR() {
    return maxShapeR;
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
    return avgShapeR;
  }

  @Override
  public double getAvgShapeCosineSim() {
    return avgShapeCosineSim;
  }

  public double getAvgDPcount() {
    return avgDPCount;
  }

  public CorrelationData getHeightCorr() {
    return heightCorr;
  }

  public void setCorrIProfileR(CorrelationData corrIProfile) {
    this.heightCorr = corrIProfile;
  }

  public boolean hasHeightCorr() {
    return heightCorr != null && heightCorr.getReg() != null && heightCorr.getReg().getN() > 0;
  }

  @Override
  public boolean hasFeatureShapeCorrelation() {
    return (corrFeatureShape != null && !corrFeatureShape.isEmpty());
  }

  /**
   * Either has Imax correlation or feature shape correlation
   * 
   * @return
   */
  public boolean isValid() {
    return hasFeatureShapeCorrelation() || hasHeightCorr();
  }

  /**
   * Validate this correlation data. If criteria is not met for ImaxCorrelation or
   * featureShapeCorrelation - the specific correlation is deleted
   * 
   */
  public void validate(double minTotalCorr, boolean useTotalCorr, double minShapePearsonR,
      SimilarityMeasure shapeSimMeasure) {
    if (hasFeatureShapeCorrelation() && ((avgShapeR < getSimilarity(shapeSimMeasure))
        || (useTotalCorr && getTotalCorr().getSimilarity(shapeSimMeasure) < minTotalCorr))) {
      // delete Feature shape corr
      setCorrFeatureShape(null);
    }
  }

  public double getCosineHeightCorr() {
    return hasHeightCorr() ? heightCorr.getCosineSimilarity() : 0;
  }
}
