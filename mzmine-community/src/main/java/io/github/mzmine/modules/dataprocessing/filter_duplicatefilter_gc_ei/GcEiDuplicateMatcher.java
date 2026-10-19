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

package io.github.mzmine.modules.dataprocessing.filter_duplicatefilter_gc_ei;

import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.scans.similarity.SpectralSimilarityFunction;
import org.jetbrains.annotations.NotNull;

/**
 * Decides if two GC-EI rows within the retention time tolerance are duplicates of the same
 * compound, based on their quantifier m/z and the similarity of their merged pseudo spectra.
 */
class GcEiDuplicateMatcher {

  private final @NotNull MZTolerance mzTolerance;
  private final @NotNull GcEiDuplicateMzCheck mzCheck;
  private final @NotNull SpectralSimilarityFunction similarityFunction;

  GcEiDuplicateMatcher(@NotNull final MZTolerance mzTolerance,
      @NotNull final GcEiDuplicateMzCheck mzCheck,
      @NotNull final SpectralSimilarityFunction similarityFunction) {
    this.mzTolerance = mzTolerance;
    this.mzCheck = mzCheck;
    this.similarityFunction = similarityFunction;
  }

  /**
   * The retention time needs to be checked before.
   *
   * @return true if the m/z check and the minimum spectral similarity are met
   */
  boolean isDuplicate(@NotNull final GcEiDuplicateCandidate a,
      @NotNull final GcEiDuplicateCandidate b) {
    final boolean mzMatch = switch (mzCheck) {
      // decision: duplicates split by the alignment often have different quantifier ions, but each
      // quantifier is still a signal of the other spectrum
      case QUANTIFIER_IN_BOTH_SPECTRA -> a.containsMz(b.quantifierMz(), mzTolerance)
          && b.containsMz(a.quantifierMz(), mzTolerance);
      case SAME_QUANTIFIER -> mzTolerance.checkWithinTolerance(a.quantifierMz(), b.quantifierMz());
      case NONE -> true;
    };
    if (!mzMatch) {
      return false;
    }
    // similarity functions return null if the minimum score is not reached
    return similarityFunction.getSimilarity(mzTolerance, 0, a.spectrum(), b.spectrum()) != null;
  }
}
