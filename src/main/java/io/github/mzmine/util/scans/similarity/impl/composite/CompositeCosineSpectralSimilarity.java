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

package io.github.mzmine.util.scans.similarity.impl.composite;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.maths.similarity.Similarity;
import io.github.mzmine.util.scans.ScanAlignment;
import io.github.mzmine.util.scans.similarity.HandleUnmatchedSignalOptions;
import io.github.mzmine.util.scans.similarity.SpectralSimilarity;
import io.github.mzmine.util.scans.similarity.SpectralSimilarityFunction;
import io.github.mzmine.util.scans.similarity.Weights;
import java.util.Comparator;

/**
 * Similar to NIST search algorithm for GC-MS data with lots of signals (more an identity check than
 * similarity).<br> Uses the relative intensity ratios of adjacent signals.
 *
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public class CompositeCosineSpectralSimilarity extends SpectralSimilarityFunction {

  /**
   * Returns mass and intensity values detected in given scan
   */
  @Override
  public SpectralSimilarity getSimilarity(ParameterSet parameters, MZTolerance mzTol, int minMatch,
      DataPoint[] library, DataPoint[] query) {
    Weights weights =
        parameters.getParameter(CompositeCosineSpectralSimilarityParameters.weight).getValue();
    double minCos =
        parameters.getParameter(CompositeCosineSpectralSimilarityParameters.minCosine).getValue();
    HandleUnmatchedSignalOptions handleUnmatched = parameters
        .getParameter(CompositeCosineSpectralSimilarityParameters.handleUnmatched)
        .getValue();

    // align
    List<DataPoint[]> aligned = alignDataPoints(mzTol, library, query);
    // removes all signals which were not found in both masslists
    aligned = handleUnmatched.handleUnmatched(aligned);

    int queryN = query.length;
    int overlap = calcOverlap(aligned);

    if (overlap >= minMatch) {
      // relative factor ranges from 0-1
      double relativeFactor = calcRelativeNeighbourFactor(aligned);

      // weighted cosine
      double[][] diffArray =
          ScanAlignment.toIntensityMatrixWeighted(aligned, weights.getIntensity(), weights.getMz());
      double diffCosine = Similarity.COSINE.calc(diffArray);

      // composite dot product identity score
      // NIST search similar
      double composite = (queryN * diffCosine + overlap * relativeFactor) / (queryN + overlap);

      if (composite >= minCos) {
        return new SpectralSimilarity(getName(), composite, overlap, library, query, aligned);
      } else {
        return null;
      }
    }
    return null;
  }

  /**
   * sum of relative ratios of neighbours in both mass lists
   *
   * @param aligned list of aligned signals DataPoint[library, query]
   */
  private double calcRelativeNeighbourFactor(List<DataPoint[]> aligned) {
    // remove all unaligned signals
    List<DataPoint[]> filtered = removeUnaligned(aligned);
    // sort by mz
    sortByMZ(filtered);

    // overlapping within mass tolerance
    int overlap = calcOverlap(aligned);

    // sum of relative ratios of neighbours in both mass lists
    double factor = 0;
    for (int i = 1; i < filtered.size(); i++) {
      DataPoint[] match1 = filtered.get(i - 1);
      DataPoint[] match2 = filtered.get(i);

      // 0 is library
      // 1 is query
      double ratioLibrary = match2[0].getIntensity() / match1[0].getIntensity();
      double ratioQuery = match2[1].getIntensity() / match1[1].getIntensity();
      factor += Math.min(ratioLibrary, ratioQuery) / Math.max(ratioLibrary, ratioQuery);
    }
    // factor ranges from 0-1 * overlap
    return factor / (overlap);
  }

  /**
   * Sort aligned datapoints by their minimum mz values (ascending)
   *
   * @param filtered list of aligned signals DataPoint[library, query]
   */
  private void sortByMZ(List<DataPoint[]> filtered) {
    filtered.sort(Comparator.comparingDouble(this::getMinMZ));
  }

  /**
   * Minimum mz of all aligned data points
   *
   * @param dp array of aligned data points
   * @return minimum mz of aligned data points
   */
  private double getMinMZ(DataPoint[] dp) {
    return Arrays.stream(dp).filter(Objects::nonNull).mapToDouble(DataPoint::getMZ).min().orElse(0);
  }

  @Override
  @NotNull
  public String getName() {
    return "Composite dot -product identity (similar to NIST search)";
  }

  @Override
  public @Nullable Class<? extends ParameterSet> getParameterSetClass() {
    return CompositeCosineSpectralSimilarityParameters.class;
  }
}
