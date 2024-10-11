/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.util.scans.similarity.impl.cosine;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.maths.similarity.Similarity;
import io.github.mzmine.util.scans.ScanAlignment;
import io.github.mzmine.util.scans.similarity.HandleUnmatchedSignalOptions;
import io.github.mzmine.util.scans.similarity.SpectralSimilarity;
import io.github.mzmine.util.scans.similarity.SpectralSimilarityFunction;
import io.github.mzmine.util.scans.similarity.Weights;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Weighted (mz and intensity) cosine similarity. Similar to the NIST search / MassBank search
 *
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 */
public class WeightedCosineSpectralSimilarity extends SpectralSimilarityFunction {

  private final Weights weights;
  private final double minCos;
  private final HandleUnmatchedSignalOptions handleUnmatched;

  /**
   * required default constructor in module for initial instance in config
   */
  public WeightedCosineSpectralSimilarity() {
    weights = Weights.SQRT;
    minCos = 0.7;
    handleUnmatched = HandleUnmatchedSignalOptions.KEEP_ALL_AND_MATCH_TO_ZERO;
  }

  public WeightedCosineSpectralSimilarity(ParameterSet parameters) {
    weights = parameters.getParameter(WeightedCosineSpectralSimilarityParameters.weight).getValue();
    minCos = parameters.getParameter(WeightedCosineSpectralSimilarityParameters.minCosine)
        .getValue();
    handleUnmatched = parameters.getParameter(
        WeightedCosineSpectralSimilarityParameters.handleUnmatched).getValue();
  }

  /**
   * Returns mass and intensity values detected in given scan
   */
  @Override
  public SpectralSimilarity getSimilarity(MZTolerance mzTol, int minMatch, DataPoint[] library,
      DataPoint[] query) {
    // align
    List<DataPoint[]> aligned = alignDataPoints(mzTol, library, query);
    // removes all signals which were not found in both masslists
    aligned = handleUnmatched.handleUnmatched(aligned);

    // overlapping within mass tolerance
    int overlap = calcOverlap(aligned);

    if (overlap >= minMatch) {
      // weighted cosine
      double[][] diffArray = ScanAlignment.toIntensityMatrixWeighted(aligned,
          weights.getIntensity(), weights.getMz());
      double diffCosine = Similarity.COSINE.calc(diffArray);
      if (diffCosine >= minCos) {
        return new SpectralSimilarity(getName(), diffCosine, overlap, library, query, aligned);
      } else {
        return null;
      }
    }
    return null;
  }

  @Override
  @NotNull
  public String getName() {
    return "Weighted cosine similarity";
  }

  @Override
  public @Nullable Class<? extends ParameterSet> getParameterSetClass() {
    return WeightedCosineSpectralSimilarityParameters.class;
  }
}
