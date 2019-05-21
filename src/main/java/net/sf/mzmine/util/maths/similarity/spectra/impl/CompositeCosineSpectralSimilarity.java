/*
 * Copyright 2006-2018 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package net.sf.mzmine.util.maths.similarity.spectra.impl;

import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import net.sf.mzmine.util.maths.similarity.Similarity;
import net.sf.mzmine.util.maths.similarity.spectra.SpectraSimilarity;
import net.sf.mzmine.util.maths.similarity.spectra.SpectralSimilarityFunction;
import net.sf.mzmine.util.maths.similarity.spectra.Weights;
import net.sf.mzmine.util.scans.ScanAlignment;

/**
 * Similar to NIST search algorithm for GC-MS data with lots of signals (more an identity check than
 * similarity)
 * 
 * @author
 *
 */
public class CompositeCosineSpectralSimilarity extends SpectralSimilarityFunction {

  /**
   * Returns mass and intensity values detected in given scan
   */
  @Override
  public SpectraSimilarity getSimilarity(ParameterSet parameters, MZTolerance mzTol, int minMatch,
      DataPoint[] library, DataPoint[] query) {
    Weights weights =
        parameters.getParameter(CompositeCosineSpectralSimilarityParameters.weight).getValue();
    double minCos =
        parameters.getParameter(CompositeCosineSpectralSimilarityParameters.minCosine).getValue();

    // align
    List<DataPoint[]> aligned = align(mzTol, library, query);
    int queryN = query.length;
    int overlap = calcOverlap(aligned);

    if (overlap >= minMatch) {
      double relativeFactor = calcRelativeNeighbourFactor(aligned);

      // weighted cosine
      double[][] diffArray =
          ScanAlignment.toIntensityMatrixWeighted(aligned, weights.getIntensity(), weights.getMz());
      double diffCosine = Similarity.COSINE.calc(diffArray);

      // composite dot product identity score
      // NIST search similar
      double composite =
          1000 / (queryN + overlap) * (queryN * diffCosine + overlap * relativeFactor);


      if (composite >= minCos)
        return new SpectraSimilarity(composite, overlap);
      else
        return null;
    }
    return null;
  }

  /**
   * sum of relative ratios of neighbours in both mass lists
   * 
   * @param aligned
   * @return
   */
  private double calcRelativeNeighbourFactor(List<DataPoint[]> aligned) {
    // remove all unaligned signals
    List<DataPoint[]> filtered = removeUnaligned(aligned);
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
    return factor / (overlap - 1);
  }


  @Override
  @Nonnull
  public String getName() {
    return "Composite dot -product identity (similar to NIST search)";
  }

  @Override
  public @Nullable Class<? extends ParameterSet> getParameterSetClass() {
    return CompositeCosineSpectralSimilarityParameters.class;
  }
}
