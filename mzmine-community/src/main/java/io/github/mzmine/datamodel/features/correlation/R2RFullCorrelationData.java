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

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.util.maths.similarity.SimilarityMeasure;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;

/**
 * row to row correlation (2 rows) Intensity profile and Feature shape correlation
 *
 * @author Robin Schmid
 */
public class R2RFullCorrelationData extends R2RCorrelationData {

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

  /**
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
      avgShapeR += corr.getPearsonR();
      avgShapeCosineSim += corr.getCosineSimilarity();
      avgDPCount += corr.getDPCount();
      if (corr.getPearsonR() < minShapeR) {
        minShapeR = corr.getPearsonR();
      }
      if (corr.getPearsonR() > maxShapeR) {
        maxShapeR = corr.getPearsonR();
      }
    }

    avgDPCount = avgDPCount / c;
    avgShapeR = avgShapeR / c;
    avgShapeCosineSim = avgShapeCosineSim / c;

    // create new total corr
    double[][] data = corrFeatureShape.values().stream().map(CorrelationData::getData)
        .flatMap(Arrays::stream).toArray(double[][]::new);
    corrTotal = new FullCorrelationData(data);
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
  @Override
  public double getHeightSimilarity(SimilarityMeasure type) {
    return hasHeightCorr() ? getHeightCorr().getSimilarity(type) : Double.NaN;
  }

  /**
   * The similarity or NaN if data is null or empty
   *
   * @param type
   * @return
   */
  @Override
  public double getTotalSimilarity(SimilarityMeasure type) {
    return hasTotalCorrelation() ? getTotalCorr().getSimilarity(type) : Double.NaN;
  }

  @Override
  public double getTotalPearsonR() {
    return hasTotalCorrelation() ? getTotalCorr().getPearsonR() : Double.NaN;
  }

  @Override
  protected boolean hasTotalCorrelation() {
    return getTotalCorr() != null;
  }

  /**
   * The similarity or NaN if data is null or empty
   *
   * @param type
   * @return
   */
  @Override
  public double getAvgFeatureShapeSimilarity(SimilarityMeasure type) {
    if (corrFeatureShape == null || corrFeatureShape.isEmpty()) {
      return Double.NaN;
    } else {
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
   * Correlation between two rows with all data points of all raw data files
   *
   * @return
   */
  public CorrelationData getTotalCorr() {
    return corrTotal;
  }

  @Override
  public double getMinShapeR() {
    return minShapeR;
  }

  @Override
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
    return switch (measure) {
      case COSINE_SIM -> getAvgShapeCosineSim();
      case PEARSON -> getAvgShapeR();
      default -> 0;
    };
  }

  @Override
  public double getAvgShapeR() {
    return avgShapeR;
  }

  @Override
  public double getAvgShapeCosineSim() {
    return avgShapeCosineSim;
  }

  public CorrelationData getHeightCorr() {
    return heightCorr;
  }

  /**
   * Has feature height correlation
   *
   * @return true if feature height correlation is available
   */
  @Override
  public boolean hasHeightCorr() {
    return heightCorr != null && heightCorr.isValid();
  }

  @Override
  public boolean hasFeatureShapeCorrelation() {
    return (corrFeatureShape != null && !corrFeatureShape.isEmpty());
  }

  @Override
  public double getAvgDPcount() {
    return avgDPCount;
  }

  /**
   * Either has Imax correlation or feature shape correlation
   *
   * @return
   */
  @Override
  public boolean isValid() {
    return hasFeatureShapeCorrelation() || hasHeightCorr();
  }

  /**
   * Validate this correlation data. If criteria is not met for ImaxCorrelation or
   * featureShapeCorrelation - the specific correlation is deleted
   */
  public void validateFeatureCorrelation(double minTotalCorr, boolean useTotalCorr,
      double minSimilarityScore,
      SimilarityMeasure shapeSimMeasure) {
    if (hasFeatureShapeCorrelation()
        && ((getSimilarity(shapeSimMeasure) < minSimilarityScore)
            || (useTotalCorr && getTotalCorr().getSimilarity(shapeSimMeasure) < minTotalCorr))) {
      // delete Feature shape corr
      setCorrFeatureShape(null);
    }
  }


  @Override
  public double getHeightCosineSimilarity() {
    return hasHeightCorr() ? heightCorr.getCosineSimilarity() : 0;
  }

  @Override
  public double getHeightPearsonR() {
    return hasHeightCorr() ? heightCorr.getPearsonR() : 0;
  }

}
