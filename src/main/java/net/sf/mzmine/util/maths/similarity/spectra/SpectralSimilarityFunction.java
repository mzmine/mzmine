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

package net.sf.mzmine.util.maths.similarity.spectra;

import java.util.List;
import javax.annotation.Nullable;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.modules.MZmineModule;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import net.sf.mzmine.util.maths.similarity.spectra.impl.composite.CompositeCosineSpectralSimilarity;
import net.sf.mzmine.util.maths.similarity.spectra.impl.cosine.WeightedCosineSpectralSimilarity;
import net.sf.mzmine.util.scans.ScanAlignment;

/**
 * https://bioconductor.org/packages/devel/bioc/vignettes/msPurity/inst/doc/msPurity-lcmsms-data-processing-and-spectral-matching-vignette.html
 * 
 * @author
 *
 */
public abstract class SpectralSimilarityFunction implements MZmineModule {

  public static SpectralSimilarityFunction[] FUNCTIONS = new SpectralSimilarityFunction[] {
      new WeightedCosineSpectralSimilarity(), new CompositeCosineSpectralSimilarity()};

  /**
   * 
   * @param parameters
   * @param mzTol
   * @param minMatch minimum overlap in signals
   * @param a
   * @param b
   * @return A spectra similarity if all requirements were met - otherwise null
   */
  @Nullable
  public abstract SpectraSimilarity getSimilarity(ParameterSet parameters, MZTolerance mzTol,
      int minMatch, DataPoint[] library, DataPoint[] query);


  /**
   * Align two mass lists. Override if alignement is changed in a specific spectral similarity
   * function.
   * 
   * @param mzTol
   * @param a
   * @param b
   * @return
   */
  public List<DataPoint[]> alignDataPoints(MZTolerance mzTol, DataPoint[] a, DataPoint[] b) {
    return ScanAlignment.align(mzTol, a, b);
  }

  /**
   * Calculate overlap
   * 
   * @param aligned
   * @return
   */
  protected int calcOverlap(List<DataPoint[]> aligned) {
    return (int) aligned.stream().filter(dp -> dp[0] != null && dp[1] != null).count();
  }

  /**
   * Remove unaligned signals (not present in all masslists)
   * 
   * @param aligned
   * @return
   */
  protected List<DataPoint[]> removeUnaligned(List<DataPoint[]> aligned) {
    return ScanAlignment.removeUnaligned(aligned);
  }
}
