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

package net.sf.mzmine.util.maths.similarity.spectra.impl.cosine;

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

public class WeightedCosineSpectralSimilarity extends SpectralSimilarityFunction {

  /**
   * Returns mass and intensity values detected in given scan
   */
  @Override
  public SpectraSimilarity getSimilarity(ParameterSet parameters, MZTolerance mzTol, int minMatch,
      DataPoint[] library, DataPoint[] query) {
    Weights weights =
        parameters.getParameter(WeightedCosineSpectralSimilarityParameters.weight).getValue();
    double minCos =
        parameters.getParameter(WeightedCosineSpectralSimilarityParameters.minCosine).getValue();
    boolean removeUnmatched = parameters
        .getParameter(WeightedCosineSpectralSimilarityParameters.removeUnmatched).getValue();

    // align
    List<DataPoint[]> aligned = alignDataPoints(mzTol, library, query);
    // removes all signals which were not found in both masslists
    if (removeUnmatched)
      aligned = removeUnaligned(aligned);
    // overlapping within mass tolerance
    int overlap = calcOverlap(aligned);

    if (overlap >= minMatch) {
      // weighted cosine
      double[][] diffArray =
          ScanAlignment.toIntensityMatrixWeighted(aligned, weights.getIntensity(), weights.getMz());
      double diffCosine = Similarity.COSINE.calc(diffArray);
      if (diffCosine >= minCos)
        return new SpectraSimilarity(getName(), diffCosine, overlap, library, query, aligned);
      else
        return null;
    }
    return null;
  }

  @Override
  @Nonnull
  public String getName() {
    return "Weighted dot-product cosine";
  }

  @Override
  public @Nullable Class<? extends ParameterSet> getParameterSetClass() {
    return WeightedCosineSpectralSimilarityParameters.class;
  }
}
