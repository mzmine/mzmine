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

package io.github.mzmine.modules.dataprocessing.filter_isotopefinder.engine;

import org.jetbrains.annotations.NotNull;

/**
 * A charge-resolved, predicted isotope intensity envelope. Intensities are relative to the base
 * peak (the most intense offset is normalized to 1.0). Index {@code i} of the arrays corresponds to
 * the nominal isotope offset {@code i} (0 = monoisotopic anchor, 1 = M+1, ...) where the m/z
 * spacing between consecutive offsets is {@link #spacingDa()} = 13C distance / charge.
 * <p>
 * {@link #expected()} is the best estimate of the relative intensity at each offset and is used for
 * the envelope-fit score and to decide the carbon-driven shape. {@link #upperBound()} is the
 * maximum plausible relative intensity at each offset (widened for heavy isotopes such as S, Cl, Br
 * in the signal-based model, or taken as the maximum across all candidate formulas in the
 * formula-prediction model). The upper bound drives inclusion/termination: an observed signal that
 * exceeds it is implausible for this charge hypothesis.
 */
public record IsotopeEnvelope(@NotNull double[] expected, @NotNull double[] upperBound,
                              double spacingDa, int charge) {

  public int maxOffset() {
    return expected.length - 1;
  }

  public double expectedAt(int offset) {
    return offset >= 0 && offset < expected.length ? expected[offset] : 0d;
  }

  public double upperBoundAt(int offset) {
    return offset >= 0 && offset < upperBound.length ? upperBound[offset] : 0d;
  }

  /**
   * @return the offset of the most intense expected peak (the base peak of the predicted envelope)
   */
  public int baseOffset() {
    int idx = 0;
    double max = -1d;
    for (int i = 0; i < expected.length; i++) {
      if (expected[i] > max) {
        max = expected[i];
        idx = i;
      }
    }
    return idx;
  }
}
