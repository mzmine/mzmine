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

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.util.maths.similarity.SimilarityMeasure;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/**
 * correlation of one row to a group
 *
 * @author RibRob
 */
public class R2GroupCorrelationData {

  private final FeatureListRow row;
  // row index is xRow in corr data
  private List<R2RCorrelationData> corr;
  private final double maxHeight;
  // averages are calculated by dividing by the row count
  private double minHeightR, avgHeightR, maxHeightR;
  private double minShapeR, avgShapeR, maxShapeR, avgDPCount;

  // average cosine
  private double avgShapeCosineSim;
  private double avgCosineHeightCorr;

  // total Feature shape r
  private double avgTotalFeatureShapeR;

  public R2GroupCorrelationData(FeatureListRow row, List<R2RCorrelationData> corr,
      double maxHeight) {
    super();
    this.row = row;
    setCorrelation(corr);
    this.maxHeight = maxHeight;
  }

  /**
   * @param FeatureList
   * @return
   */
  public static Stream<R2GroupCorrelationData> streamFrom(FeatureList FeatureList) {
    if (FeatureList.getGroups() == null) {
      return Stream.empty();
    }
    return FeatureList.getGroups().stream().filter(g -> g instanceof CorrelationRowGroup)
        .map(g -> ((CorrelationRowGroup) g).getCorrelation()).flatMap(Arrays::stream);

  }


  public void setCorrelation(List<R2RCorrelationData> corr) {
    this.corr = corr;
    recalcCorrelation();
  }

  /**
   * Recalc correlation
   */
  public void recalcCorrelation() {
    // min max
    minHeightR = 1;
    maxHeightR = -1;
    avgHeightR = 0;
    minShapeR = 1;
    maxShapeR = -1;
    avgShapeR = 0;
    avgShapeCosineSim = 0;
    avgDPCount = 0;
    avgTotalFeatureShapeR = 0;
    avgCosineHeightCorr = 0;
    int cImax = 0;
    int cFeatureShape = 0;

    for (R2RCorrelationData r2r : corr) {
      if (r2r.hasHeightCorr()) {
        cImax++;
        avgCosineHeightCorr += r2r.getHeightCosineSimilarity();
        double iProfileR = r2r.getHeightPearsonR();
        avgHeightR += iProfileR;
        if (iProfileR < minHeightR) {
          minHeightR = iProfileR;
        }
        if (iProfileR > maxHeightR) {
          maxHeightR = iProfileR;
        }
      }

      // Feature shape correlation
      if (r2r.hasFeatureShapeCorrelation()) {
        cFeatureShape++;
        avgTotalFeatureShapeR += r2r.getTotalPearsonR();
        avgShapeR += r2r.getAvgShapeR();
        avgShapeCosineSim += r2r.getAvgShapeCosineSim();
        avgDPCount += r2r.getAvgDPcount();
        if (r2r.getMinShapeR() < minShapeR) {
          minShapeR = r2r.getMinShapeR();
        }
        if (r2r.getMaxShapeR() > maxShapeR) {
          maxShapeR = r2r.getMaxShapeR();
        }
      }
    }
    avgTotalFeatureShapeR = avgTotalFeatureShapeR / cFeatureShape;
    avgHeightR = avgHeightR / cImax;
    avgCosineHeightCorr = avgCosineHeightCorr / cImax;
    avgDPCount = avgDPCount / cFeatureShape;
    avgShapeR = avgShapeR / cFeatureShape;
    avgShapeCosineSim = avgShapeCosineSim / cFeatureShape;
  }


  /**
   * The similarity or NaN if data is null or empty
   *
   * @param type
   * @return
   */
  public double getAvgHeightSimilarity(SimilarityMeasure type) {
    double mean = 0;
    int n = 0;
    for (R2RCorrelationData r2r : corr) {
      double v = r2r.getHeightSimilarity(type);
      if (!Double.isNaN(v)) {
        mean += v;
        n++;
      }
    }
    return n > 0 ? mean / n : Double.NaN;
  }

  /**
   * The similarity or NaN if data is null or empty
   *
   * @param type the similarity type
   * @return the score or Double.NaN
   */
  public double getAvgTotalSimilarity(SimilarityMeasure type) {
    double mean = 0;
    int n = 0;
    for (R2RCorrelationData r2r : corr) {
      double v = r2r.getTotalSimilarity(type);
      if (!Double.isNaN(v)) {
        mean += v;
        n++;
      }
    }
    return n > 0 ? mean / n : Double.NaN;
  }

  /**
   * The similarity or NaN if data is null or empty
   *
   * @param type the similarity type
   * @return the score or Double.NaN
   */
  public double getAvgFeatureShapeSimilarity(SimilarityMeasure type) {
    double mean = 0;
    int n = 0;
    for (R2RCorrelationData r2r : corr) {
      double v = r2r.getAvgFeatureShapeSimilarity(type);
      if (!Double.isNaN(v)) {
        mean += v;
        n++;
      }
    }
    return n > 0 ? mean / n : Double.NaN;
  }

  public double getAvgShapeCosineSimilarity() {
    return avgShapeCosineSim;
  }

  public double getMaxHeight() {
    return maxHeight;
  }

  public List<R2RCorrelationData> getCorrelation() {
    return corr;
  }

  public double getMinHeightR() {
    return minHeightR;
  }

  public double getAvgHeightR() {
    return avgHeightR;
  }

  public double getMaxHeightR() {
    return maxHeightR;
  }

  public double getMinFeatureShapeR() {
    return minShapeR;
  }

  public double getAvgFeatureShapeR() {
    return avgShapeR;
  }

  public double getMaxFeatureShapeR() {
    return maxShapeR;
  }

  public double getAvgDPCount() {
    return avgDPCount;
  }

  public double getAvgTotalFeatureShapeR() {
    return avgTotalFeatureShapeR;
  }

  /**
   * Height correlation across samples
   *
   * @return
   */
  public double getAvgHeightCosineSimilarity() {
    return avgCosineHeightCorr;
  }


  public FeatureListRow getRow() {
    return row;
  }

  /**
   * @return the correlation data of this row to row[rowI]
   * @throws Exception
   */
  public R2RCorrelationData getCorrelationToRow(FeatureListRow row) {
    if (row != row) {
      return null;
    }
    for (R2RCorrelationData c : corr) {
      if (c.getRowA() == row || c.getRowB() == row) {
        return c;
      }
    }
    return null;
  }

}
