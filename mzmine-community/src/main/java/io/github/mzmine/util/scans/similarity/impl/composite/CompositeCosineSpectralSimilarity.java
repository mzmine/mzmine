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

package io.github.mzmine.util.scans.similarity.impl.composite;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.maths.similarity.Similarity;
import io.github.mzmine.util.scans.ScanAlignment;
import io.github.mzmine.util.scans.similarity.HandleUnmatchedSignalOptions;
import io.github.mzmine.util.scans.similarity.SpectralSimilarity;
import io.github.mzmine.util.scans.similarity.SpectralSimilarityFunction;
import io.github.mzmine.util.scans.similarity.Weights;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Similar to NIST search algorithm for GC-MS data with lots of signals (more an identity check than
 * similarity).<br> Uses the relative intensity ratios of adjacent signals.
 *
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public class CompositeCosineSpectralSimilarity extends SpectralSimilarityFunction {

  private static final String name = "Composite cosine identity (e.g., GC-EI-MS; similar to NIST search)";
  private final Weights weights;
  private final double minCos;
  private final HandleUnmatchedSignalOptions handleUnmatched;

  /**
   * required default constructor in module for initial instance in config
   */
  public CompositeCosineSpectralSimilarity() {
    weights = Weights.SQRT;
    minCos = 0.7;
    handleUnmatched = HandleUnmatchedSignalOptions.KEEP_ALL_AND_MATCH_TO_ZERO;
  }

  public CompositeCosineSpectralSimilarity(final ParameterSet parameters) {
    weights = parameters.getParameter(CompositeCosineSpectralSimilarityParameters.weight)
        .getValue();
    minCos = parameters.getParameter(CompositeCosineSpectralSimilarityParameters.minCosine)
        .getValue();
    handleUnmatched = parameters.getParameter(
        CompositeCosineSpectralSimilarityParameters.handleUnmatched).getValue();
  }

  /**
   * Returns mass and intensity values detected in given scan
   */
  @Override
  public SpectralSimilarity getSimilarity(MZTolerance mzTol, int minMatch, DataPoint[] library,
      DataPoint[] query) {
    return getSimilarity(weights, minCos, handleUnmatched, mzTol, minMatch, library, query);
  }

  public static SpectralSimilarity getSimilarity(Weights weights, double minCos,
      HandleUnmatchedSignalOptions handleUnmatched, MZTolerance mzTol, int minMatch,
      DataPoint[] library, DataPoint[] query) {

    // align
    List<DataPoint[]> aligned = ScanAlignment.align(mzTol, library, query);
    // removes all signals which were not found in both masslists
    aligned = handleUnmatched.handleUnmatched(aligned);

    int queryN = query.length;
    int overlap = calcOverlap(aligned);

    if (overlap >= minMatch) {
      // relative factor ranges from 0-1
      double relativeFactor = calcRelativeNeighbourFactor(aligned);

      // weighted cosine
      double[][] diffArray = ScanAlignment.toIntensityMatrixWeighted(aligned,
          weights.getIntensity(), weights.getMz());
      double diffCosine = Similarity.COSINE.calc(diffArray);

      // composite dot product identity score
      // NIST search similar
      double composite = (queryN * diffCosine + overlap * relativeFactor) / (queryN + overlap);

      if (composite >= minCos) {
        return new SpectralSimilarity(name, composite, overlap, library, query, aligned);
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
  private static double calcRelativeNeighbourFactor(List<DataPoint[]> aligned) {
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
  private static void sortByMZ(List<DataPoint[]> filtered) {
    filtered.sort(Comparator.comparingDouble(CompositeCosineSpectralSimilarity::getMinMZ));
  }

  /**
   * Minimum mz of all aligned data points
   *
   * @param dp array of aligned data points
   * @return minimum mz of aligned data points
   */
  private static double getMinMZ(DataPoint[] dp) {
    return Arrays.stream(dp).filter(Objects::nonNull).mapToDouble(DataPoint::getMZ).min().orElse(0);
  }

  @Override
  @NotNull
  public String getName() {
    return name;
  }

  @Override
  public @Nullable Class<? extends ParameterSet> getParameterSetClass() {
    return CompositeCosineSpectralSimilarityParameters.class;
  }
}
