/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

public class R2RSimpleCorrelationData extends R2RCorrelationData {

  private final double totalSim;
  private final double heightSim;
  private final double avgShapeSim;
  private final double minShapeSim;
  private final double maxShapeSim;
  private final double avgDPcount;

  public R2RSimpleCorrelationData(R2RFullCorrelationData full) {
    super(full.getRowA(), full.getRowB());
    totalSim = full.getTotalPearsonR();
    heightSim = full.getHeightPearsonR();
    avgShapeSim = full.getAvgShapeR();
    minShapeSim = full.getMinShapeR();
    maxShapeSim = full.getMaxShapeR();
    avgDPcount = full.getAvgDPcount();
  }


  @Override
  public double getHeightSimilarity(final SimilarityMeasure type) {
    return heightSim;
  }

  @Override
  public double getTotalSimilarity(final SimilarityMeasure type) {
    return totalSim;
  }

  @Override
  public double getTotalPearsonR() {
    return totalSim;
  }

  @Override
  protected boolean hasTotalCorrelation() {
    return !Double.isNaN(totalSim);
  }

  @Override
  public double getAvgFeatureShapeSimilarity(final SimilarityMeasure type) {
    return avgShapeSim;
  }

  @Override
  public double getMinShapeR() {
    return minShapeSim;
  }

  @Override
  public double getMaxShapeR() {
    return maxShapeSim;
  }

  @Override
  public double getAvgShapeR() {
    return avgShapeSim;
  }

  @Override
  public double getAvgShapeCosineSim() {
    return avgShapeSim;
  }

  @Override
  public boolean hasHeightCorr() {
    return !Double.isNaN(heightSim);
  }

  @Override
  public boolean hasFeatureShapeCorrelation() {
    return !Double.isNaN(avgShapeSim);
  }

  @Override
  public double getAvgDPcount() {
    return avgDPcount;
  }

  @Override
  public boolean isValid() {
    return true;
  }

  @Override
  public double getHeightCosineSimilarity() {
    return heightSim;
  }

  @Override
  public double getHeightPearsonR() {
    return heightSim;
  }
}
