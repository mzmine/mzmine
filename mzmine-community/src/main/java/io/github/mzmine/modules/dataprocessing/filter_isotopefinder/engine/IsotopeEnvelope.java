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
 * A charge-resolved predicted isotope envelope. Array index {@code i} is the nominal isotope offset
 * (0 = monoisotopic anchor), consecutive offsets are {@link #spacingDa()} apart, and intensities are
 * relative to the most intense offset.
 * <p>
 * {@link #expected()} is the best estimate, driving the envelope-fit score. {@link #upperBound()} is
 * the maximum plausible intensity, widened for heavy isotopes, and drives inclusion/termination: an
 * observed signal above it is implausible for this charge hypothesis.
 * <p>
 * <b>The arrays are exposed directly and must never be mutated.</b> One envelope is built per
 * (m/z, charge) and shared by every scoring step, so a caller keeping a snapshot must clone.
 */
public record IsotopeEnvelope(@NotNull double[] expected, @NotNull double[] upperBound,
                              double spacingDa, int charge) {

  /**
   * Relative intensity from which on the envelope counts as supporting a peak. One definition on
   * purpose: the engine's coverage / self-consistency / termination and the cross-scan refiner's
   * recovery test must agree, or the refiner recovers offsets the engine terminated at.
   */
  public static final double SUPPORT_CUTOFF = 0.02;

  public int maxOffset() {
    return expected.length - 1;
  }

  public double expectedAt(final int offset) {
    return offset >= 0 && offset < expected.length ? expected[offset] : 0d;
  }

  public double upperBoundAt(final int offset) {
    return offset >= 0 && offset < upperBound.length ? upperBound[offset] : 0d;
  }

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
